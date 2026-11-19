using System.Text.RegularExpressions;
using Contactos.Application.Dtos;
using Contactos.Application.Excepciones;
using Contactos.Application.Interfaces;
using Contactos.Domain.Entidades;

namespace Contactos.Application.Servicios;

/// <summary>
/// Lógica de negocio de contactos: valida los datos y el archivo,
/// coordina el guardado en disco y en la base de datos.
/// </summary>
public partial class ContactoService(IContactoRepository repositorio, IAlmacenArchivos almacen) : IContactoService
{
    // Teléfono: 8 a 15 dígitos, con "+" opcional al inicio (se ignoran espacios y guiones).
    [GeneratedRegex(@"^\+?[0-9]{8,15}$")]
    private static partial Regex PatronTelefono();

    // ---------- Crear (POST multipart) ----------
    public async Task<ContactoDto> CrearAsync(DatosContacto datos, ArchivoEntrada? archivo, CancellationToken ct)
    {
        // 1) Validar los campos de texto y que venga la firma
        var errores = ValidarDatos(datos);
        if (archivo is null || archivo.Tamano == 0)
            errores["archivo"] = ["La firma es obligatoria: envía una imagen PNG en el campo 'archivo'."];
        if (errores.Count > 0)
            throw new ExcepcionValidacion("Hay datos inválidos. Revisa los campos marcados.", errores);

        // 2) Validar el archivo: tamaño (400) y tipo (415)
        var bytes = await LeerYValidarArchivoAsync(archivo!, ct);

        // 3) Guardar el archivo en disco y, después, el registro en la base de datos
        using var memoria = new MemoryStream(bytes);
        var ruta = await almacen.GuardarAsync(memoria, ReglasArchivo.Extension, ct);

        var contacto = new Contacto
        {
            Nombre = Limpiar(datos.Nombre),
            Telefono = NormalizarTelefono(datos.Telefono),
            Latitud = datos.Latitud!.Value,
            Longitud = datos.Longitud!.Value,
            ArchivoRuta = ruta,
            ArchivoMime = ReglasArchivo.MimePermitido,
            ArchivoTamano = bytes.LongLength,
            FechaCreacion = DateTime.UtcNow,
        };

        try
        {
            await repositorio.AgregarAsync(contacto, ct);
        }
        catch
        {
            // Si falla la base de datos, no dejamos un archivo huérfano en disco
            almacen.Eliminar(ruta);
            throw;
        }

        return ADto(contacto);
    }

    // ---------- Consultar ----------
    public async Task<List<ContactoDto>> ListarAsync(string? buscar, CancellationToken ct)
    {
        var lista = await repositorio.ListarAsync(string.IsNullOrWhiteSpace(buscar) ? null : buscar.Trim(), ct);
        return lista.Select(ADto).ToList();
    }

    public async Task<ContactoDto> ObtenerAsync(int id, CancellationToken ct)
        => ADto(await ObtenerExistenteAsync(id, ct));

    /// <summary>Abre la firma guardada para devolverla con su Content-Type.</summary>
    public async Task<ArchivoSalida> ObtenerArchivoAsync(int id, CancellationToken ct)
    {
        var contacto = await ObtenerExistenteAsync(id, ct);
        var flujo = almacen.AbrirLectura(contacto.ArchivoRuta)
            ?? throw new ExcepcionNoEncontrado($"El archivo de la firma del contacto {id} no existe en el servidor.");
        return new ArchivoSalida(flujo, contacto.ArchivoMime);
    }

    // ---------- Actualizar (PUT) ----------
    public async Task<ContactoDto> ActualizarAsync(int id, DatosContacto datos, CancellationToken ct)
    {
        var contacto = await ObtenerExistenteAsync(id, ct);

        var errores = ValidarDatos(datos);
        if (errores.Count > 0)
            throw new ExcepcionValidacion("Hay datos inválidos. Revisa los campos marcados.", errores);

        contacto.Nombre = Limpiar(datos.Nombre);
        contacto.Telefono = NormalizarTelefono(datos.Telefono);
        contacto.Latitud = datos.Latitud!.Value;
        contacto.Longitud = datos.Longitud!.Value;

        await repositorio.ActualizarAsync(contacto, ct);
        return ADto(contacto);
    }

    // ---------- Eliminar (DELETE) ----------
    public async Task EliminarAsync(int id, CancellationToken ct)
    {
        var contacto = await ObtenerExistenteAsync(id, ct);
        await repositorio.EliminarAsync(contacto, ct);
        almacen.Eliminar(contacto.ArchivoRuta); // también se borra la firma del disco
    }

    // ---------- Auxiliares ----------

    private async Task<Contacto> ObtenerExistenteAsync(int id, CancellationToken ct)
        => await repositorio.ObtenerAsync(id, ct)
           ?? throw new ExcepcionNoEncontrado($"No existe un contacto con id {id}.");

    /// <summary>Reglas de los campos de texto. Devuelve los errores por campo.</summary>
    private static Dictionary<string, string[]> ValidarDatos(DatosContacto d)
    {
        var errores = new Dictionary<string, string[]>();

        var nombre = Limpiar(d.Nombre);
        if (nombre.Length == 0) errores["nombre"] = ["El nombre es obligatorio."];
        else if (nombre.Length > 100) errores["nombre"] = ["El nombre no puede tener más de 100 caracteres."];

        var telefono = NormalizarTelefono(d.Telefono);
        if (telefono.Length == 0) errores["telefono"] = ["El teléfono es obligatorio."];
        else if (!PatronTelefono().IsMatch(telefono)) errores["telefono"] = ["El teléfono debe tener de 8 a 15 dígitos."];

        if (d.Latitud is null) errores["latitud"] = ["La latitud es obligatoria: active el GPS para obtener la ubicación."];
        else if (d.Latitud is < -90 or > 90) errores["latitud"] = ["La latitud debe estar entre -90 y 90."];

        if (d.Longitud is null) errores["longitud"] = ["La longitud es obligatoria: active el GPS para obtener la ubicación."];
        else if (d.Longitud is < -180 or > 180) errores["longitud"] = ["La longitud debe estar entre -180 y 180."];

        return errores;
    }

    /// <summary>
    /// Revisa tamaño (máx. 1 MB → 400) y tipo (image/png → 415).
    /// Lee como máximo 1 MB + 1 byte, así nunca se carga en memoria un archivo enorme.
    /// </summary>
    private static async Task<byte[]> LeerYValidarArchivoAsync(ArchivoEntrada archivo, CancellationToken ct)
    {
        if (archivo.Tamano > ReglasArchivo.TamanoMaximo)
            throw ExcesoTamano(archivo.Tamano);

        var tipo = (archivo.TipoContenido ?? string.Empty).Split(';')[0].Trim().ToLowerInvariant();
        if (tipo != ReglasArchivo.MimePermitido)
            throw new ExcepcionTipoNoSoportado(
                $"Tipo de archivo no permitido ({(tipo.Length == 0 ? "sin tipo" : tipo)}). Solo se acepta {ReglasArchivo.MimePermitido}.");

        var bytes = new byte[ReglasArchivo.TamanoMaximo + 1];
        int leidos = 0, n;
        while (leidos < bytes.Length && (n = await archivo.Contenido.ReadAsync(bytes.AsMemory(leidos), ct)) > 0)
            leidos += n;

        if (leidos > ReglasArchivo.TamanoMaximo) throw ExcesoTamano(leidos);
        if (leidos == 0)
            throw new ExcepcionValidacion("El archivo de la firma está vacío.",
                new Dictionary<string, string[]> { ["archivo"] = ["El archivo de la firma está vacío."] });

        // El Content-Type dice PNG, pero confirmamos que el contenido realmente lo sea
        if (!ReglasArchivo.EsPngReal(bytes.AsSpan(0, leidos)))
            throw new ExcepcionTipoNoSoportado("El archivo no es una imagen PNG válida.");

        return bytes[..leidos];
    }

    private static ExcepcionValidacion ExcesoTamano(long tamano)
    {
        var mensaje = $"La firma pesa {tamano / 1024.0:0.#} KB y el máximo permitido es 1 MB.";
        return new ExcepcionValidacion(mensaje, new Dictionary<string, string[]> { ["archivo"] = [mensaje] });
    }

    private static string Limpiar(string? texto) => (texto ?? string.Empty).Trim();

    private static string NormalizarTelefono(string? telefono)
        => Limpiar(telefono).Replace(" ", "").Replace("-", "");

    private static ContactoDto ADto(Contacto c) => new(
        c.Id, c.Nombre, c.Telefono, c.Latitud, c.Longitud,
        c.ArchivoMime, c.ArchivoTamano, c.FechaCreacion,
        $"/api/contactos/{c.Id}/archivo");
}

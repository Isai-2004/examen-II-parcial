using Contactos.Application.Dtos;
using Contactos.Domain.Entidades;

namespace Contactos.Application.Interfaces;

/// <summary>Acceso a la tabla de contactos (lo implementa Infrastructure con EF Core).</summary>
public interface IContactoRepository
{
    Task<Contacto> AgregarAsync(Contacto contacto, CancellationToken ct);
    Task<List<Contacto>> ListarAsync(string? buscar, CancellationToken ct);
    Task<Contacto?> ObtenerAsync(int id, CancellationToken ct);
    Task ActualizarAsync(Contacto contacto, CancellationToken ct);
    Task EliminarAsync(Contacto contacto, CancellationToken ct);
}

/// <summary>Guarda y lee los archivos de las firmas (lo implementa Infrastructure en disco).</summary>
public interface IAlmacenArchivos
{
    /// <summary>Guarda el archivo y devuelve su ruta relativa (la que se guarda en la base de datos).</summary>
    Task<string> GuardarAsync(Stream contenido, string extension, CancellationToken ct);

    /// <summary>Abre el archivo para lectura; devuelve null si ya no existe.</summary>
    Stream? AbrirLectura(string rutaRelativa);

    void Eliminar(string rutaRelativa);
}

/// <summary>Casos de uso de contactos que expone la API.</summary>
public interface IContactoService
{
    Task<ContactoDto> CrearAsync(DatosContacto datos, ArchivoEntrada? archivo, CancellationToken ct);
    Task<List<ContactoDto>> ListarAsync(string? buscar, CancellationToken ct);
    Task<ContactoDto> ObtenerAsync(int id, CancellationToken ct);
    Task<ArchivoSalida> ObtenerArchivoAsync(int id, CancellationToken ct);
    Task<ContactoDto> ActualizarAsync(int id, DatosContacto datos, CancellationToken ct);
    Task EliminarAsync(int id, CancellationToken ct);
}

using Contactos.Application.Interfaces;

namespace Contactos.Infrastructure.Almacenamiento;

/// <summary>
/// Guarda las firmas como archivos en una carpeta del servidor (por defecto "almacen/firmas").
/// En la base de datos solo queda la ruta relativa, por ejemplo "firmas/3f2a...png".
/// </summary>
public class AlmacenArchivosLocal : IAlmacenArchivos
{
    private const string Subcarpeta = "firmas";
    private readonly string _raiz;

    public AlmacenArchivosLocal(string carpetaRaiz)
    {
        _raiz = Path.GetFullPath(carpetaRaiz);
        Directory.CreateDirectory(Path.Combine(_raiz, Subcarpeta));
    }

    public async Task<string> GuardarAsync(Stream contenido, string extension, CancellationToken ct)
    {
        // Nombre aleatorio: nunca se usa el nombre que manda el cliente
        var relativa = $"{Subcarpeta}/{Guid.NewGuid():N}{extension}";
        await using var destino = new FileStream(RutaCompleta(relativa), FileMode.CreateNew, FileAccess.Write);
        await contenido.CopyToAsync(destino, ct);
        return relativa;
    }

    public Stream? AbrirLectura(string rutaRelativa)
    {
        var ruta = RutaCompleta(rutaRelativa);
        return File.Exists(ruta) ? new FileStream(ruta, FileMode.Open, FileAccess.Read, FileShare.Read) : null;
    }

    public void Eliminar(string rutaRelativa)
    {
        var ruta = RutaCompleta(rutaRelativa);
        if (File.Exists(ruta)) File.Delete(ruta);
    }

    /// <summary>Convierte la ruta relativa en absoluta y evita que salga de la carpeta raíz.</summary>
    private string RutaCompleta(string relativa)
    {
        var completa = Path.GetFullPath(Path.Combine(_raiz, relativa));
        if (!completa.StartsWith(_raiz + Path.DirectorySeparatorChar, StringComparison.Ordinal))
            throw new InvalidOperationException("Ruta de archivo no válida.");
        return completa;
    }
}

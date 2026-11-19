namespace Contactos.Application.Dtos;

/// <summary>Datos de un contacto tal como los devuelve la API.</summary>
/// <param name="UrlArchivo">Ruta relativa para descargar la firma (GET).</param>
public record ContactoDto(
    int Id,
    string Nombre,
    string Telefono,
    double Latitud,
    double Longitud,
    string ArchivoMime,
    long ArchivoTamano,
    DateTime FechaCreacion,
    string UrlArchivo);

/// <summary>
/// Datos de texto de un contacto (crear o actualizar).
/// Son anulables a propósito: así la validación la hace el servicio y los mensajes salen en español.
/// </summary>
public record DatosContacto(string? Nombre, string? Telefono, double? Latitud, double? Longitud);

/// <summary>Archivo recibido en la petición, sin depender de ASP.NET (IFormFile).</summary>
public record ArchivoEntrada(Stream Contenido, string? TipoContenido, long Tamano);

/// <summary>Archivo listo para enviarse en la descarga.</summary>
public record ArchivoSalida(Stream Contenido, string TipoContenido);

namespace Contactos.Domain.Entidades;

/// <summary>
/// Contacto registrado desde la app: datos personales, ubicación y la firma digital.
/// La firma NO se guarda en la base de datos: aquí solo se guarda dónde está el archivo,
/// su tipo (MIME) y su tamaño.
/// </summary>
public class Contacto
{
    public int Id { get; set; }

    public string Nombre { get; set; } = string.Empty;
    public string Telefono { get; set; } = string.Empty;

    // Ubicación obtenida con el GPS del teléfono
    public double Latitud { get; set; }
    public double Longitud { get; set; }

    // Metadatos del archivo de la firma (el archivo vive en disco)
    public string ArchivoRuta { get; set; } = string.Empty;
    public string ArchivoMime { get; set; } = string.Empty;
    public long ArchivoTamano { get; set; }

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;
}

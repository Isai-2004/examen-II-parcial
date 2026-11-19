namespace Contactos.Api.Modelos;

/// <summary>Campos del formulario multipart/form-data para crear un contacto.</summary>
public class CrearContactoFormulario
{
    /// <example>Jose de Jesus Sarmiento</example>
    public string? Nombre { get; set; }

    /// <example>99765645</example>
    public string? Telefono { get; set; }

    /// <example>14.48</example>
    public double? Latitud { get; set; }

    /// <example>-88.78</example>
    public double? Longitud { get; set; }

    /// <summary>Imagen de la firma: image/png, máximo 1 MB.</summary>
    public IFormFile? Archivo { get; set; }
}

/// <summary>Cuerpo JSON para actualizar un contacto (la firma no cambia).</summary>
public class ActualizarContactoSolicitud
{
    /// <example>Jose de Jesus Sarmiento</example>
    public string? Nombre { get; set; }

    /// <example>99765645</example>
    public string? Telefono { get; set; }

    /// <example>14.48</example>
    public double? Latitud { get; set; }

    /// <example>-88.78</example>
    public double? Longitud { get; set; }
}

namespace Contactos.Application.Excepciones;

// Excepciones de negocio. El middleware de la API las convierte en
// respuestas JSON con el código HTTP correspondiente.

/// <summary>Datos inválidos o archivo que excede el tamaño → HTTP 400.</summary>
public class ExcepcionValidacion : Exception
{
    public IDictionary<string, string[]> Errores { get; }

    public ExcepcionValidacion(string mensaje, IDictionary<string, string[]>? errores = null) : base(mensaje)
        => Errores = errores ?? new Dictionary<string, string[]>();
}

/// <summary>Tipo de archivo no permitido → HTTP 415 (Unsupported Media Type).</summary>
public class ExcepcionTipoNoSoportado(string mensaje) : Exception(mensaje);

/// <summary>El contacto o su archivo no existe → HTTP 404.</summary>
public class ExcepcionNoEncontrado(string mensaje) : Exception(mensaje);

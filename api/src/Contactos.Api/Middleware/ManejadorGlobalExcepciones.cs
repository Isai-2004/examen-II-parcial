using Contactos.Application.Excepciones;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;

namespace Contactos.Api.Middleware;

/// <summary>
/// Convierte cualquier excepción en una respuesta JSON (ProblemDetails) con el código correcto.
/// La app Android muestra el campo "detail" y, si existen, los mensajes de "errors".
/// </summary>
public class ManejadorGlobalExcepciones(ILogger<ManejadorGlobalExcepciones> log, IHostEnvironment entorno) : IExceptionHandler
{
    public async ValueTask<bool> TryHandleAsync(HttpContext http, Exception ex, CancellationToken ct)
    {
        var (estado, titulo, detalle) = ex switch
        {
            ExcepcionValidacion v => (StatusCodes.Status400BadRequest, "Datos inválidos", v.Message),
            ExcepcionTipoNoSoportado t => (StatusCodes.Status415UnsupportedMediaType, "Tipo de archivo no permitido", t.Message),
            ExcepcionNoEncontrado n => (StatusCodes.Status404NotFound, "No encontrado", n.Message),
            BadHttpRequestException b when b.StatusCode == StatusCodes.Status413PayloadTooLarge
                => (b.StatusCode, "Petición demasiado grande", "La petición supera el tamaño permitido (la firma debe pesar máximo 1 MB)."),
            BadHttpRequestException b => (b.StatusCode, "Petición inválida", b.Message),
            _ => (StatusCodes.Status500InternalServerError, "Error interno",
                  entorno.IsDevelopment() ? ex.Message : "Ocurrió un error inesperado en el servidor."),
        };

        if (estado >= 500) log.LogError(ex, "Error no controlado");

        var problema = new ProblemDetails
        {
            Status = estado,
            Title = titulo,
            Detail = detalle,
            Instance = http.Request.Path,
        };
        if (ex is ExcepcionValidacion { Errores.Count: > 0 } val)
            problema.Extensions["errors"] = val.Errores;

        http.Response.StatusCode = estado;
        await http.Response.WriteAsJsonAsync(problema, (System.Text.Json.JsonSerializerOptions?)null, "application/problem+json", ct);
        return true;
    }
}

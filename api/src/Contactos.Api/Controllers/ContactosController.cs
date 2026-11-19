using Contactos.Api.Modelos;
using Contactos.Application.Dtos;
using Contactos.Application.Interfaces;
using Microsoft.AspNetCore.Mvc;

namespace Contactos.Api.Controllers;

/// <summary>Endpoints de contactos con firma digital.</summary>
[ApiController]
[Route("api/contactos")]
[Produces("application/json")]
public class ContactosController(IContactoService servicio) : ControllerBase
{
    // Límite de la petición completa (texto + archivo). La regla de 1 MB del archivo
    // la valida el servicio para responder un mensaje claro en JSON.
    private const long LimitePeticion = 5 * 1024 * 1024;

    /// <summary>Crea un contacto con su firma (multipart/form-data).</summary>
    /// <response code="201">Contacto creado.</response>
    /// <response code="400">Datos inválidos, firma faltante o mayor a 1 MB.</response>
    /// <response code="415">El archivo no es image/png.</response>
    [HttpPost]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(LimitePeticion)]
    [RequestFormLimits(MultipartBodyLengthLimit = LimitePeticion)]
    [ProducesResponseType(typeof(ContactoDto), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status415UnsupportedMediaType)]
    public async Task<ActionResult<ContactoDto>> Crear([FromForm] CrearContactoFormulario formulario, CancellationToken ct)
    {
        var datos = new DatosContacto(formulario.Nombre, formulario.Telefono, formulario.Latitud, formulario.Longitud);

        // Se adapta IFormFile al modelo de la capa de aplicación (que no conoce ASP.NET)
        await using var flujo = formulario.Archivo?.OpenReadStream();
        var archivo = formulario.Archivo is null
            ? null
            : new ArchivoEntrada(flujo!, formulario.Archivo.ContentType, formulario.Archivo.Length);

        var creado = await servicio.CrearAsync(datos, archivo, ct);
        return CreatedAtAction(nameof(Obtener), new { id = creado.Id }, creado);
    }

    /// <summary>Lista los contactos; con ?buscar= filtra por nombre o teléfono.</summary>
    [HttpGet]
    [ProducesResponseType(typeof(List<ContactoDto>), StatusCodes.Status200OK)]
    public async Task<ActionResult<List<ContactoDto>>> Listar([FromQuery] string? buscar, CancellationToken ct)
        => Ok(await servicio.ListarAsync(buscar, ct));

    /// <summary>Obtiene un contacto por su id.</summary>
    [HttpGet("{id:int}")]
    [ProducesResponseType(typeof(ContactoDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ContactoDto>> Obtener(int id, CancellationToken ct)
        => Ok(await servicio.ObtenerAsync(id, ct));

    /// <summary>Descarga la firma del contacto con su Content-Type (image/png).</summary>
    [HttpGet("{id:int}/archivo")]
    [Produces("image/png", "application/problem+json")]
    [ProducesResponseType(typeof(FileStreamResult), StatusCodes.Status200OK, "image/png")]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> DescargarArchivo(int id, CancellationToken ct)
    {
        var archivo = await servicio.ObtenerArchivoAsync(id, ct);
        return File(archivo.Contenido, archivo.TipoContenido); // File() cierra el flujo al terminar
    }

    /// <summary>Actualiza nombre, teléfono y ubicación (JSON). La firma se conserva.</summary>
    [HttpPut("{id:int}")]
    [Consumes("application/json")]
    [ProducesResponseType(typeof(ContactoDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ContactoDto>> Actualizar(int id, [FromBody] ActualizarContactoSolicitud solicitud, CancellationToken ct)
        => Ok(await servicio.ActualizarAsync(id,
            new DatosContacto(solicitud.Nombre, solicitud.Telefono, solicitud.Latitud, solicitud.Longitud), ct));

    /// <summary>Elimina el contacto y su firma del disco.</summary>
    [HttpDelete("{id:int}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(int id, CancellationToken ct)
    {
        await servicio.EliminarAsync(id, ct);
        return NoContent();
    }
}

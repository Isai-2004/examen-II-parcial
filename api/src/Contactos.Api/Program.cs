using System.Globalization;
using System.Reflection;
using Contactos.Api.Middleware;
using Contactos.Application;
using Contactos.Infrastructure;
using Microsoft.AspNetCore.Mvc;

// Cultura invariante: la latitud "14.48" siempre se interpreta con punto decimal,
// sin importar la configuración regional de Windows.
CultureInfo.DefaultThreadCurrentCulture = CultureInfo.InvariantCulture;
CultureInfo.DefaultThreadCurrentUICulture = CultureInfo.InvariantCulture;

var builder = WebApplication.CreateBuilder(args);

// ---------- Capas (arquitectura limpia) ----------
builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration, builder.Environment.ContentRootPath);

// ---------- Controladores y errores en JSON ----------
builder.Services.AddControllers(o =>
    {
        // La obligatoriedad de los campos la valida el servicio (mensajes en español)
        o.SuppressImplicitRequiredAttributeForNonNullableReferenceTypes = true;

        // Mensajes de conversión de datos en español
        var m = o.ModelBindingMessageProvider;
        m.SetAttemptedValueIsInvalidAccessor((valor, campo) => $"El valor '{valor}' no es válido para {campo}.");
        m.SetValueMustBeANumberAccessor(campo => $"El campo {campo} debe ser un número.");
        m.SetValueIsInvalidAccessor(valor => $"El valor '{valor}' no es válido.");
        m.SetMissingBindRequiredValueAccessor(campo => $"Falta el valor del campo {campo}.");
        m.SetMissingRequestBodyRequiredValueAccessor(() => "El cuerpo de la petición es obligatorio.");
        m.SetNonPropertyAttemptedValueIsInvalidAccessor(valor => $"El valor '{valor}' no es válido.");
    })
    .ConfigureApiBehaviorOptions(o =>
    {
        // Si un valor no se puede convertir (ej. latitud = "abc"), responde 400 en español
        o.InvalidModelStateResponseFactory = contexto =>
        {
            var problema = new ValidationProblemDetails(contexto.ModelState)
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "Datos inválidos",
                Detail = "Uno o más campos tienen un formato incorrecto.",
                Instance = contexto.HttpContext.Request.Path,
            };
            return new BadRequestObjectResult(problema) { ContentTypes = { "application/problem+json" } };
        };
    });
builder.Services.AddProblemDetails();
builder.Services.AddExceptionHandler<ManejadorGlobalExcepciones>();

// ---------- Swagger / OpenAPI ----------
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new()
    {
        Title = "API de Contactos con firma digital",
        Version = "v1",
        Description = "Tarea 2P · Grupo 2. Registra contactos con ubicación y firma (image/png, máximo 1 MB).",
    });
    var xml = Path.Combine(AppContext.BaseDirectory, $"{Assembly.GetExecutingAssembly().GetName().Name}.xml");
    if (File.Exists(xml)) c.IncludeXmlComments(xml);
});

var app = builder.Build();

// Crea la base de datos SQLite y la tabla si todavía no existen
await app.Services.InicializarBaseDatosAsync();

app.UseExceptionHandler();   // excepciones → JSON
app.UseStatusCodePages();    // 404/405/415 sin cuerpo → JSON

app.UseSwagger();
app.UseSwaggerUI(c => c.DocumentTitle = "API de Contactos");

app.MapGet("/", () => Results.Redirect("/swagger")).ExcludeFromDescription();
app.MapControllers();

app.Run();

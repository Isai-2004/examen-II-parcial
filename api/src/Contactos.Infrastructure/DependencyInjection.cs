using Contactos.Application.Interfaces;
using Contactos.Infrastructure.Almacenamiento;
using Contactos.Infrastructure.Persistencia;
using Contactos.Infrastructure.Repositorios;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Contactos.Infrastructure;

public static class DependencyInjection
{
    /// <summary>Registra la base de datos SQLite, el repositorio y el almacén de archivos.</summary>
    public static IServiceCollection AddInfrastructure(this IServiceCollection services,
        IConfiguration configuracion, string carpetaBase)
    {
        var cadena = configuracion.GetConnectionString("Contactos") ?? "Data Source=contactos.db";
        services.AddDbContext<ContactosDbContext>(o => o.UseSqlite(cadena));
        services.AddScoped<IContactoRepository, ContactoRepository>();

        var carpeta = configuracion["Almacenamiento:Carpeta"] ?? "almacen";
        var raiz = Path.IsPathRooted(carpeta) ? carpeta : Path.Combine(carpetaBase, carpeta);
        services.AddSingleton<IAlmacenArchivos>(new AlmacenArchivosLocal(raiz));

        return services;
    }

    /// <summary>Crea la base de datos y la tabla la primera vez que se ejecuta la API.</summary>
    public static async Task InicializarBaseDatosAsync(this IServiceProvider proveedor)
    {
        using var alcance = proveedor.CreateScope();
        var db = alcance.ServiceProvider.GetRequiredService<ContactosDbContext>();
        await db.Database.EnsureCreatedAsync();
    }
}

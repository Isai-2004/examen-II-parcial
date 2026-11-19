using Contactos.Application.Interfaces;
using Contactos.Application.Servicios;
using Microsoft.Extensions.DependencyInjection;

namespace Contactos.Application;

public static class DependencyInjection
{
    /// <summary>Registra los servicios de la capa de aplicación.</summary>
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<IContactoService, ContactoService>();
        return services;
    }
}

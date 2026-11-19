using Contactos.Domain.Entidades;
using Microsoft.EntityFrameworkCore;

namespace Contactos.Infrastructure.Persistencia;

/// <summary>Contexto de Entity Framework Core (base de datos SQLite).</summary>
public class ContactosDbContext(DbContextOptions<ContactosDbContext> options) : DbContext(options)
{
    public DbSet<Contacto> Contactos => Set<Contacto>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
        => modelBuilder.ApplyConfigurationsFromAssembly(typeof(ContactosDbContext).Assembly);
}

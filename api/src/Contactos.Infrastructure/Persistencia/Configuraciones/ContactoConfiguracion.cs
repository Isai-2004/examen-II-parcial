using Contactos.Domain.Entidades;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Contactos.Infrastructure.Persistencia.Configuraciones;

/// <summary>Cómo se guarda la entidad Contacto en la tabla "Contactos".</summary>
public class ContactoConfiguracion : IEntityTypeConfiguration<Contacto>
{
    public void Configure(EntityTypeBuilder<Contacto> b)
    {
        b.ToTable("Contactos");
        b.HasKey(c => c.Id);

        b.Property(c => c.Nombre).HasMaxLength(100).IsRequired();
        b.Property(c => c.Telefono).HasMaxLength(16).IsRequired();

        // Solo metadatos del archivo: la imagen está en disco
        b.Property(c => c.ArchivoRuta).HasMaxLength(260).IsRequired();
        b.Property(c => c.ArchivoMime).HasMaxLength(100).IsRequired();

        // SQLite no guarda si la fecha es UTC; se marca al leerla para que el JSON lleve la "Z"
        b.Property(c => c.FechaCreacion)
            .HasConversion(v => v, v => DateTime.SpecifyKind(v, DateTimeKind.Utc));

        b.HasIndex(c => c.Nombre); // acelera la búsqueda por nombre
    }
}

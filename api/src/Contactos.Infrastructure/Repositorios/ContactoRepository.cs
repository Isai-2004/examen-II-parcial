using Contactos.Application.Interfaces;
using Contactos.Domain.Entidades;
using Contactos.Infrastructure.Persistencia;
using Microsoft.EntityFrameworkCore;

namespace Contactos.Infrastructure.Repositorios;

/// <summary>Implementación del repositorio con EF Core.</summary>
public class ContactoRepository(ContactosDbContext db) : IContactoRepository
{
    public async Task<Contacto> AgregarAsync(Contacto contacto, CancellationToken ct)
    {
        db.Contactos.Add(contacto);
        await db.SaveChangesAsync(ct);
        return contacto;
    }

    /// <summary>Lista ordenada por nombre; si hay texto, filtra por nombre o teléfono.</summary>
    public Task<List<Contacto>> ListarAsync(string? buscar, CancellationToken ct)
    {
        var consulta = db.Contactos.AsNoTracking();
        if (buscar is not null)
        {
            var patron = $"%{buscar}%"; // LIKE en SQLite no distingue mayúsculas
            consulta = consulta.Where(c => EF.Functions.Like(c.Nombre, patron) || EF.Functions.Like(c.Telefono, patron));
        }
        return consulta.OrderBy(c => c.Nombre).ToListAsync(ct);
    }

    public Task<Contacto?> ObtenerAsync(int id, CancellationToken ct)
        => db.Contactos.FirstOrDefaultAsync(c => c.Id == id, ct);

    public Task ActualizarAsync(Contacto contacto, CancellationToken ct)
    {
        db.Contactos.Update(contacto);
        return db.SaveChangesAsync(ct);
    }

    public Task EliminarAsync(Contacto contacto, CancellationToken ct)
    {
        db.Contactos.Remove(contacto);
        return db.SaveChangesAsync(ct);
    }
}

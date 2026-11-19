namespace Contactos.Application.Servicios;

/// <summary>Reglas del archivo asignadas al Grupo 2: solo PNG y máximo 1 MB.</summary>
public static class ReglasArchivo
{
    public const string MimePermitido = "image/png";
    public const string Extension = ".png";
    public const long TamanoMaximo = 1 * 1024 * 1024; // 1 MB

    // Todo PNG real empieza con estos 8 bytes ("firma mágica").
    // Se revisan para no confiar solo en el Content-Type que manda el cliente.
    private static readonly byte[] FirmaPng = { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

    public static bool EsPngReal(ReadOnlySpan<byte> inicio)
        => inicio.Length >= FirmaPng.Length && inicio[..FirmaPng.Length].SequenceEqual(FirmaPng);
}

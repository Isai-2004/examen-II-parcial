package com.uth.contactos.red;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Petición multipart/form-data para Volley (Volley no la trae incluida).
 *
 * El cuerpo se arma a mano, igual que lo hace un formulario HTML que sube un archivo:
 *
 *   --BOUNDARY
 *   Content-Disposition: form-data; name="nombre"
 *
 *   Jose
 *   --BOUNDARY
 *   Content-Disposition: form-data; name="archivo"; filename="firma.png"
 *   Content-Type: image/png
 *
 *   (bytes del PNG)
 *   --BOUNDARY--
 *
 * El archivo viaja en binario, no en Base64.
 */
public class VolleyMultipartRequest extends Request<NetworkResponse> {

    private static final String SALTO = "\r\n";
    private static final String GUIONES = "--";

    /** Archivo que se adjunta en una parte del formulario. */
    public static class Archivo {
        final String nombreArchivo;
        final String tipoMime;
        final byte[] contenido;

        public Archivo(String nombreArchivo, String tipoMime, byte[] contenido) {
            this.nombreArchivo = nombreArchivo;
            this.tipoMime = tipoMime;
            this.contenido = contenido;
        }
    }

    // El boundary separa las partes; debe ser un texto que no aparezca dentro de los datos
    private final String boundary = "ContactosBoundary" + System.currentTimeMillis();
    private final Map<String, String> campos;
    private final Map<String, Archivo> archivos;
    private final Response.Listener<NetworkResponse> oyente;

    public VolleyMultipartRequest(int metodo, String url,
                                  Map<String, String> campos,
                                  Map<String, Archivo> archivos,
                                  Response.Listener<NetworkResponse> oyente,
                                  Response.ErrorListener oyenteError) {
        super(metodo, url, oyenteError);
        this.campos = campos;
        this.archivos = archivos;
        this.oyente = oyente;
    }

    /** Le dice al servidor que el cuerpo es multipart y cuál es el separador. */
    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    /** Arma el cuerpo completo: primero los campos de texto, luego los archivos y el cierre. */
    @Override
    public byte[] getBody() {
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();

        // ----- Campos de texto -----
        for (Map.Entry<String, String> campo : campos.entrySet()) {
            escribir(cuerpo, GUIONES + boundary + SALTO);
            escribir(cuerpo, "Content-Disposition: form-data; name=\"" + campo.getKey() + "\"" + SALTO);
            escribir(cuerpo, "Content-Type: text/plain; charset=UTF-8" + SALTO);
            escribir(cuerpo, SALTO);
            escribir(cuerpo, campo.getValue() + SALTO);
        }

        // ----- Archivos (bytes tal cual) -----
        for (Map.Entry<String, Archivo> entrada : archivos.entrySet()) {
            Archivo archivo = entrada.getValue();
            escribir(cuerpo, GUIONES + boundary + SALTO);
            escribir(cuerpo, "Content-Disposition: form-data; name=\"" + entrada.getKey()
                    + "\"; filename=\"" + archivo.nombreArchivo + "\"" + SALTO);
            escribir(cuerpo, "Content-Type: " + archivo.tipoMime + SALTO);
            escribir(cuerpo, SALTO);
            cuerpo.write(archivo.contenido, 0, archivo.contenido.length);
            escribir(cuerpo, SALTO);
        }

        // ----- Cierre: el boundary final lleva "--" al final -----
        escribir(cuerpo, GUIONES + boundary + GUIONES + SALTO);
        return cuerpo.toByteArray();
    }

    /** Escribe texto en UTF-8 (así los acentos y la ñ llegan bien al servidor). */
    private static void escribir(ByteArrayOutputStream salida, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        salida.write(bytes, 0, bytes.length);
    }

    /** Se entrega la respuesta completa (código, encabezados y cuerpo). */
    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse respuesta) {
        return Response.success(respuesta, HttpHeaderParser.parseCacheHeaders(respuesta));
    }

    @Override
    protected void deliverResponse(NetworkResponse respuesta) {
        oyente.onResponse(respuesta);
    }
}

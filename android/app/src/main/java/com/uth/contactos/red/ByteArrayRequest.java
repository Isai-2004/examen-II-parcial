package com.uth.contactos.red;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * Petición GET que devuelve el cuerpo en bytes (response.data).
 * Se usa para descargar la imagen PNG de la firma.
 */
public class ByteArrayRequest extends Request<byte[]> {

    private final Response.Listener<byte[]> oyente;

    public ByteArrayRequest(String url, Response.Listener<byte[]> oyente, Response.ErrorListener oyenteError) {
        super(Method.GET, url, oyenteError);
        this.oyente = oyente;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse respuesta) {
        return Response.success(respuesta.data, HttpHeaderParser.parseCacheHeaders(respuesta));
    }

    @Override
    protected void deliverResponse(byte[] datos) {
        oyente.onResponse(datos);
    }
}

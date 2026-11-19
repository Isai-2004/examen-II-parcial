package com.uth.contactos.red;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Traduce un error de Volley a un mensaje para el usuario.
 * - Sin red, API apagada o tiempo agotado → error de conectividad (se ofrece "Reintentar").
 * - 400/404/415/500 → se muestra el mensaje JSON que manda la API ("detail" y "errors").
 */
public class ErrorApi {

    public static final String MENSAJE_SIN_CONEXION = "Sin conexión con la API.\nIntente de nuevo.";

    public final boolean esConectividad;
    public final int codigoHttp;
    public final String mensaje;

    private ErrorApi(boolean esConectividad, int codigoHttp, String mensaje) {
        this.esConectividad = esConectividad;
        this.codigoHttp = codigoHttp;
        this.mensaje = mensaje;
    }

    public static ErrorApi desde(VolleyError error) {
        NetworkResponse respuesta = error.networkResponse;

        // 0) La API respondió, pero el contenido no se pudo leer
        if (error instanceof ParseError) {
            return new ErrorApi(false, 0, "La respuesta de la API no tiene el formato esperado.");
        }

        // 1) No hubo respuesta del servidor: problema de conectividad
        if (respuesta == null || error instanceof NoConnectionError || error instanceof TimeoutError) {
            return new ErrorApi(true, 0, MENSAJE_SIN_CONEXION);
        }

        // 2) El servidor respondió con error: leer su JSON (ProblemDetails)
        String texto = "Error del servidor (HTTP " + respuesta.statusCode + ").";
        if (respuesta.data != null && respuesta.data.length > 0) {
            try {
                JSONObject json = new JSONObject(new String(respuesta.data, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder(json.optString("detail", json.optString("title", texto)));

                // Mensajes por campo: { "errors": { "telefono": ["..."], ... } }
                JSONObject errores = json.optJSONObject("errors");
                if (errores != null) {
                    Iterator<String> campos = errores.keys();
                    while (campos.hasNext()) {
                        JSONArray lista = errores.optJSONArray(campos.next());
                        for (int i = 0; lista != null && i < lista.length(); i++) {
                            String linea = lista.optString(i);
                            if (!sb.toString().contains(linea)) {
                                sb.append("\n• ").append(linea);
                            }
                        }
                    }
                }
                texto = sb.toString();
            } catch (JSONException ignorada) {
                // La respuesta no era JSON: se queda el mensaje genérico con el código HTTP
            }
        }
        return new ErrorApi(false, respuesta.statusCode, texto);
    }
}

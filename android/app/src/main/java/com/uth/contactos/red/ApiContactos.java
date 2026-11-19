package com.uth.contactos.red;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.uth.contactos.BuildConfig;
import com.uth.contactos.modelo.Contacto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Todas las llamadas a la API de contactos, hechas con Volley.
 *
 *  POST   api/contactos               → VolleyMultipartRequest (firma PNG + datos)
 *  GET    api/contactos?buscar=       → JsonArrayRequest
 *  GET    api/contactos/{id}/archivo  → ByteArrayRequest (bytes del PNG)
 *  PUT    api/contactos/{id}          → JsonObjectRequest
 *  DELETE api/contactos/{id}          → StringRequest
 */
public class ApiContactos {

    /** Resultado de una llamada: éxito con datos o error ya traducido. */
    public interface Respuesta<T> {
        void exito(T datos);

        void error(ErrorApi error);
    }

    private static final String BASE = BuildConfig.API_BASE_URL + "api/contactos";
    private static final int TIEMPO_ESPERA_MS = 10_000;

    private final VolleySingleton volley;
    private final Object etiqueta;

    /** @param etiqueta identifica a la pantalla para cancelar sus peticiones al cerrarse. */
    public ApiContactos(Context contexto, Object etiqueta) {
        this.volley = VolleySingleton.obtener(contexto);
        this.etiqueta = etiqueta;
    }

    // ---------- POST multipart: crear contacto con su firma ----------
    public void crear(String nombre, String telefono, double latitud, double longitud,
                      byte[] firmaPng, Respuesta<Contacto> respuesta) {
        Map<String, String> campos = new HashMap<>();
        campos.put("nombre", nombre);
        campos.put("telefono", telefono);
        // Locale.US garantiza el punto decimal (14.48 y no 14,48)
        campos.put("latitud", String.format(Locale.US, "%.6f", latitud));
        campos.put("longitud", String.format(Locale.US, "%.6f", longitud));

        Map<String, VolleyMultipartRequest.Archivo> archivos = new HashMap<>();
        archivos.put("archivo", new VolleyMultipartRequest.Archivo("firma.png", "image/png", firmaPng));

        VolleyMultipartRequest peticion = new VolleyMultipartRequest(Request.Method.POST, BASE, campos, archivos,
                r -> {
                    try {
                        String json = new String(r.data, StandardCharsets.UTF_8);
                        respuesta.exito(Contacto.desdeJson(new JSONObject(json)));
                    } catch (JSONException e) {
                        respuesta.exito(null); // se guardó, aunque la respuesta no se pudo leer
                    }
                },
                e -> respuesta.error(ErrorApi.desde(e)));
        enviar(peticion, false);
    }

    // ---------- GET: lista (con búsqueda opcional) ----------
    public void listar(String buscar, Respuesta<List<Contacto>> respuesta) {
        String url = BASE;
        if (buscar != null && !buscar.trim().isEmpty()) {
            url += "?buscar=" + Uri.encode(buscar.trim());
        }
        JsonArrayRequest peticion = new JsonArrayRequest(Request.Method.GET, url, (JSONArray) null,
                arreglo -> {
                    try {
                        respuesta.exito(convertirLista(arreglo));
                    } catch (JSONException e) {
                        respuesta.error(ErrorApi.desde(new ParseError(e)));
                    }
                },
                e -> respuesta.error(ErrorApi.desde(e)));
        enviar(peticion, true);
    }

    // ---------- GET bytes: descargar la firma ----------
    public void descargarFirma(int id, Respuesta<Bitmap> respuesta) {
        ByteArrayRequest peticion = new ByteArrayRequest(BASE + "/" + id + "/archivo",
                bytes -> respuesta.exito(BitmapFactory.decodeByteArray(bytes, 0, bytes.length)),
                e -> respuesta.error(ErrorApi.desde(e)));
        enviar(peticion, true);
    }

    // ---------- PUT JSON: actualizar ----------
    public void actualizar(int id, String nombre, String telefono, double latitud, double longitud,
                           Respuesta<Contacto> respuesta) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombre", nombre);
            cuerpo.put("telefono", telefono);
            cuerpo.put("latitud", latitud);
            cuerpo.put("longitud", longitud);
        } catch (JSONException e) {
            throw new IllegalStateException(e); // no ocurre con valores válidos
        }
        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.PUT, BASE + "/" + id, cuerpo,
                json -> {
                    try {
                        respuesta.exito(Contacto.desdeJson(json));
                    } catch (JSONException e) {
                        respuesta.exito(null);
                    }
                },
                e -> respuesta.error(ErrorApi.desde(e)));
        enviar(peticion, false);
    }

    // ---------- DELETE ----------
    public void eliminar(int id, Respuesta<Void> respuesta) {
        StringRequest peticion = new StringRequest(Request.Method.DELETE, BASE + "/" + id,
                texto -> respuesta.exito(null),
                e -> respuesta.error(ErrorApi.desde(e)));
        enviar(peticion, false);
    }

    /** Cancela todas las peticiones pendientes de esta pantalla. */
    public void cancelarTodo() {
        volley.cancelar(etiqueta);
    }

    /**
     * Configuración común: 10 s de espera, sin caché y etiqueta de la pantalla.
     * Las consultas (GET) se reintentan 1 vez; POST/PUT/DELETE no se reintentan
     * para no crear o borrar dos veces si el servidor ya procesó la primera.
     */
    private <T> void enviar(Request<T> peticion, boolean reintentar) {
        peticion.setRetryPolicy(new DefaultRetryPolicy(TIEMPO_ESPERA_MS, reintentar ? 1 : 0, 1f));
        peticion.setShouldCache(false);
        peticion.setTag(etiqueta);
        volley.agregar(peticion);
    }

    private static List<Contacto> convertirLista(JSONArray arreglo) throws JSONException {
        List<Contacto> lista = new ArrayList<>();
        for (int i = 0; i < arreglo.length(); i++) {
            lista.add(Contacto.desdeJson(arreglo.getJSONObject(i)));
        }
        return lista;
    }
}

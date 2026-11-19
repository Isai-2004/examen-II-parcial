package com.uth.contactos.red;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Única RequestQueue de toda la app (patrón singleton).
 * Crear una cola por pantalla gastaría hilos y caché; con una sola se comparten.
 * Volley ejecuta cada petición en sus propios hilos y entrega la respuesta en el hilo principal,
 * así ninguna petición bloquea la interfaz.
 */
public final class VolleySingleton {

    private static VolleySingleton instancia;
    private final RequestQueue cola;

    private VolleySingleton(Context contexto) {
        // Se usa el contexto de la aplicación para no retener una Activity en memoria
        cola = Volley.newRequestQueue(contexto.getApplicationContext());
    }

    public static synchronized VolleySingleton obtener(Context contexto) {
        if (instancia == null) {
            instancia = new VolleySingleton(contexto);
        }
        return instancia;
    }

    public <T> void agregar(Request<T> peticion) {
        cola.add(peticion);
    }

    /** Cancela las peticiones de una pantalla cuando se cierra. */
    public void cancelar(Object etiqueta) {
        cola.cancelAll(etiqueta);
    }
}

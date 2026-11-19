package com.uth.contactos.modelo;

import org.json.JSONException;
import org.json.JSONObject;

/** Contacto tal como lo devuelve la API (JSON). */
public class Contacto {

    public final int id;
    public final String nombre;
    public final String telefono;
    public final double latitud;
    public final double longitud;
    public final String urlArchivo;

    public Contacto(int id, String nombre, String telefono, double latitud, double longitud, String urlArchivo) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.latitud = latitud;
        this.longitud = longitud;
        this.urlArchivo = urlArchivo;
    }

    /** Convierte un objeto JSON de la API en un Contacto. */
    public static Contacto desdeJson(JSONObject json) throws JSONException {
        return new Contacto(
                json.getInt("id"),
                json.getString("nombre"),
                json.getString("telefono"),
                json.getDouble("latitud"),
                json.getDouble("longitud"),
                json.optString("urlArchivo", "api/contactos/" + json.getInt("id") + "/archivo"));
    }
}

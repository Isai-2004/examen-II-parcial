package com.uth.contactos.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uth.contactos.R;
import com.uth.contactos.red.ErrorApi;

/** Alertas comunes y el indicador de carga que se muestra en cada petición. */
public final class Dialogos {

    private Dialogos() { }

    /** Alerta simple con título "Alerta", como en el boceto. */
    public static void alerta(Activity actividad, String mensaje) {
        if (actividad.isFinishing()) return;
        new MaterialAlertDialogBuilder(actividad)
                .setTitle("Alerta")
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    /**
     * Muestra el error de la API. Si es de conectividad, ofrece "Reintentar"
     * para repetir la misma operación.
     */
    public static void errorApi(Activity actividad, ErrorApi error, Runnable reintentar) {
        if (actividad.isFinishing()) return;
        MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(actividad)
                .setTitle("Alerta")
                .setMessage(error.mensaje);
        if (error.esConectividad && reintentar != null) {
            dialogo.setPositiveButton("Reintentar", (d, w) -> reintentar.run())
                    .setNegativeButton("Cerrar", null);
        } else {
            dialogo.setPositiveButton("Aceptar", null);
        }
        dialogo.show();
    }

    /** Diálogo de "cargando" que no se puede cerrar mientras la petición está en curso. */
    public static class Cargando {
        private final Activity actividad;
        private AlertDialog dialogo;

        public Cargando(Activity actividad) {
            this.actividad = actividad;
        }

        public void mostrar(String mensaje) {
            if (actividad.isFinishing()) return;
            ocultar();
            View vista = LayoutInflater.from(actividad).inflate(R.layout.dialogo_cargando, null);
            ((TextView) vista.findViewById(R.id.lblCargando)).setText(mensaje);
            dialogo = new MaterialAlertDialogBuilder(actividad)
                    .setView(vista)
                    .setCancelable(false)
                    .show();
        }

        public void ocultar() {
            if (dialogo != null && dialogo.isShowing()) {
                dialogo.dismiss();
            }
            dialogo = null;
        }
    }
}

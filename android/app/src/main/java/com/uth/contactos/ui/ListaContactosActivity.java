package com.uth.contactos.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.uth.contactos.BuildConfig;
import com.uth.contactos.R;
import com.uth.contactos.modelo.Contacto;
import com.uth.contactos.red.ApiContactos;
import com.uth.contactos.red.ErrorApi;

import java.util.List;
import java.util.Locale;

/**
 * Pantalla 2: contactos guardados.
 * Busca (GET ?buscar=), muestra la firma descargada (GET /archivo),
 * actualiza (PUT), elimina (DELETE), llama y abre la ubicación en el mapa.
 */
public class ListaContactosActivity extends AppCompatActivity {

    private static final long ESPERA_BUSQUEDA_MS = 400; // espera a que el usuario deje de escribir

    private ApiContactos api;
    private Dialogos.Cargando cargando;
    private ContactoAdapter adaptador;

    private EditText txtBuscar;
    private LinearProgressIndicator progresoLista;
    private TextView lblVacio;
    private TextView lblSeleccion;
    private ImageView imgFirma;
    private ProgressBar progresoFirma;

    private Contacto seleccionado;
    private int numeroConsulta; // descarta respuestas de búsquedas viejas

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Runnable buscarDiferido = this::cargarContactos;

    /** Respuesta al permiso de llamada: si se niega, se abre el marcador con el número. */
    private final ActivityResultLauncher<String> pedirPermisoLlamada = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedido -> {
                if (seleccionado == null) return;
                if (concedido) llamar(seleccionado);
                else marcar(seleccionado);
            });

    // ================= Ciclo de vida =================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista_contactos);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        api = new ApiContactos(this, this);
        cargando = new Dialogos.Cargando(this);

        MaterialToolbar barra = findViewById(R.id.barra);
        barra.setNavigationOnClickListener(v -> finish()); // botón "Atrás"

        txtBuscar = findViewById(R.id.txtBuscar);
        progresoLista = findViewById(R.id.progresoLista);
        lblVacio = findViewById(R.id.lblVacio);
        lblSeleccion = findViewById(R.id.lblSeleccion);
        imgFirma = findViewById(R.id.imgFirma);
        progresoFirma = findViewById(R.id.progresoFirma);

        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adaptador = new ContactoAdapter(this::alSeleccionar);
        lista.setAdapter(adaptador);

        configurarBusqueda();
        findViewById(R.id.btnEliminar).setOnClickListener(v -> confirmarEliminar());
        findViewById(R.id.btnActualizar).setOnClickListener(v -> mostrarActualizar());
        findViewById(R.id.btnLlamar).setOnClickListener(v -> solicitarLlamada());

        cargarContactos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manejador.removeCallbacks(buscarDiferido);
        api.cancelarTodo();
        cargando.ocultar();
    }

    // ================= Bloque 1: lista y búsqueda (GET) =================

    private void configurarBusqueda() {
        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                // Se consulta a la API 400 ms después de la última tecla, no en cada letra
                manejador.removeCallbacks(buscarDiferido);
                manejador.postDelayed(buscarDiferido, ESPERA_BUSQUEDA_MS);
            }
        });
        txtBuscar.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                manejador.removeCallbacks(buscarDiferido);
                cargarContactos();
                return true;
            }
            return false;
        });
    }

    private void cargarContactos() {
        final int consulta = ++numeroConsulta;
        progresoLista.setVisibility(View.VISIBLE);

        api.listar(txtBuscar.getText().toString(), new ApiContactos.Respuesta<List<Contacto>>() {
            @Override
            public void exito(List<Contacto> contactos) {
                if (consulta != numeroConsulta) return; // llegó una búsqueda más reciente
                progresoLista.setVisibility(View.GONE);
                adaptador.establecer(contactos);
                lblVacio.setVisibility(contactos.isEmpty() ? View.VISIBLE : View.GONE);
                if (seleccionado != null && !adaptador.contiene(seleccionado.id)) {
                    limpiarSeleccion();
                }
            }

            @Override
            public void error(ErrorApi error) {
                if (consulta != numeroConsulta) return;
                progresoLista.setVisibility(View.GONE);
                Dialogos.errorApi(ListaContactosActivity.this, error, ListaContactosActivity.this::cargarContactos);
            }
        });
    }

    // ================= Bloque 2: selección, firma (GET bytes) y mapa =================

    private void alSeleccionar(Contacto contacto) {
        seleccionado = contacto;
        lblSeleccion.setText(getString(R.string.descripcion_firma) + ": " + contacto.nombre);
        descargarFirma(contacto);
        preguntarIrAUbicacion(contacto);
    }

    private void descargarFirma(Contacto contacto) {
        imgFirma.setImageDrawable(null);
        progresoFirma.setVisibility(View.VISIBLE);

        api.descargarFirma(contacto.id, new ApiContactos.Respuesta<Bitmap>() {
            @Override
            public void exito(Bitmap imagen) {
                if (seleccionado == null || seleccionado.id != contacto.id) return; // ya eligió otro
                progresoFirma.setVisibility(View.GONE);
                if (imagen == null) {
                    lblSeleccion.setText("No se pudo mostrar la firma de " + contacto.nombre);
                } else {
                    imgFirma.setImageBitmap(imagen);
                }
            }

            @Override
            public void error(ErrorApi error) {
                progresoFirma.setVisibility(View.GONE);
                Dialogos.errorApi(ListaContactosActivity.this, error, () -> descargarFirma(contacto));
            }
        });
    }

    /** Diálogo "Acción" del boceto. */
    private void preguntarIrAUbicacion(Contacto contacto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Acción")
                .setMessage("¿Desea ir a la ubicación de " + contacto.nombre + "?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí", (d, w) -> abrirMapa(contacto))
                .show();
    }

    /**
     * Con clave de Google Maps configurada → pantalla 3 (mapa dentro de la app).
     * Sin clave → abre la ubicación en la app de Google Maps (o en el navegador).
     */
    private void abrirMapa(Contacto c) {
        if (BuildConfig.MAPS_CONFIGURADO) {
            Intent mapa = new Intent(this, MapaActivity.class);
            mapa.putExtra(MapaActivity.EXTRA_NOMBRE, c.nombre);
            mapa.putExtra(MapaActivity.EXTRA_LATITUD, c.latitud);
            mapa.putExtra(MapaActivity.EXTRA_LONGITUD, c.longitud);
            startActivity(mapa);
            return;
        }
        String coordenadas = String.format(Locale.US, "%.6f,%.6f", c.latitud, c.longitud);
        Uri geo = Uri.parse("geo:" + coordenadas + "?q=" + coordenadas + "(" + Uri.encode(c.nombre) + ")");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (ActivityNotFoundException sinAppMapas) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=" + coordenadas)));
            } catch (ActivityNotFoundException sinNavegador) {
                Dialogos.alerta(this, "No hay una aplicación para mostrar el mapa.");
            }
        }
    }

    // ================= Bloque 3: eliminar (DELETE) =================

    private void confirmarEliminar() {
        if (!haySeleccion()) return;
        Contacto c = seleccionado;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar contacto")
                .setMessage("¿Desea eliminar a " + c.nombre + "? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) -> eliminar(c))
                .show();
    }

    private void eliminar(Contacto c) {
        cargando.mostrar("Eliminando contacto…");
        api.eliminar(c.id, new ApiContactos.Respuesta<Void>() {
            @Override
            public void exito(Void nada) {
                cargando.ocultar();
                Toast.makeText(ListaContactosActivity.this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                limpiarSeleccion();
                cargarContactos();
            }

            @Override
            public void error(ErrorApi error) {
                cargando.ocultar();
                Dialogos.errorApi(ListaContactosActivity.this, error, () -> eliminar(c));
            }
        });
    }

    // ================= Bloque 4: actualizar (PUT) =================

    private void mostrarActualizar() {
        if (!haySeleccion()) return;
        Contacto c = seleccionado;

        View formulario = getLayoutInflater().inflate(R.layout.dialogo_actualizar, null);
        EditText txtNombre = formulario.findViewById(R.id.txtNombre);
        EditText txtTelefono = formulario.findViewById(R.id.txtTelefono);
        txtNombre.setText(c.nombre);
        txtTelefono.setText(c.telefono);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Actualizar contacto")
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null) // se asigna abajo para validar sin cerrar el diálogo
                .show();

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString().trim();
            String telefono = txtTelefono.getText().toString().trim();
            if (nombre.isEmpty()) {
                txtNombre.setError("Escriba el nombre");
                return;
            }
            if (telefono.replaceAll("[^0-9]", "").length() < 8) {
                txtTelefono.setError("Mínimo 8 dígitos");
                return;
            }
            dialogo.dismiss();
            actualizar(c, nombre, telefono);
        });
    }

    private void actualizar(Contacto c, String nombre, String telefono) {
        cargando.mostrar("Actualizando contacto…");
        // La ubicación y la firma se conservan; solo cambian nombre y teléfono
        api.actualizar(c.id, nombre, telefono, c.latitud, c.longitud, new ApiContactos.Respuesta<Contacto>() {
            @Override
            public void exito(Contacto actualizado) {
                cargando.ocultar();
                Toast.makeText(ListaContactosActivity.this, "Contacto actualizado", Toast.LENGTH_SHORT).show();
                if (actualizado != null) {
                    seleccionado = actualizado;
                    lblSeleccion.setText(getString(R.string.descripcion_firma) + ": " + actualizado.nombre);
                }
                cargarContactos();
            }

            @Override
            public void error(ErrorApi error) {
                cargando.ocultar();
                Dialogos.errorApi(ListaContactosActivity.this, error, () -> actualizar(c, nombre, telefono));
            }
        });
    }

    // ================= Bloque 5: llamar =================

    private void solicitarLlamada() {
        if (!haySeleccion()) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            llamar(seleccionado);
        } else {
            pedirPermisoLlamada.launch(Manifest.permission.CALL_PHONE);
        }
    }

    /** Código del documento: Intent ACTION_CALL con "tel:" + número (requiere CALL_PHONE). */
    private void llamar(Contacto c) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            marcar(c);
            return;
        }
        Intent llamada = new Intent(Intent.ACTION_CALL);
        llamada.setData(Uri.parse("tel:" + c.telefono));
        try {
            startActivity(llamada);
        } catch (ActivityNotFoundException | SecurityException e) {
            marcar(c);
        }
    }

    /** Alternativa sin permiso: abre el marcador con el número escrito. */
    private void marcar(Contacto c) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.telefono)));
        } catch (ActivityNotFoundException e) {
            Dialogos.alerta(this, "Este dispositivo no puede realizar llamadas.");
        }
    }

    // ================= Auxiliares =================

    private boolean haySeleccion() {
        if (seleccionado == null) {
            Dialogos.alerta(this, "Seleccione un contacto de la lista.");
            return false;
        }
        return true;
    }

    private void limpiarSeleccion() {
        seleccionado = null;
        adaptador.limpiarSeleccion();
        lblSeleccion.setText(R.string.seleccione_contacto);
        imgFirma.setImageDrawable(null);
    }
}

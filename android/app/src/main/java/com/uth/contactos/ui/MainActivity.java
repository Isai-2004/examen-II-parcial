package com.uth.contactos.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.uth.contactos.R;
import com.uth.contactos.modelo.Contacto;
import com.uth.contactos.red.ApiContactos;
import com.uth.contactos.red.ErrorApi;

import java.util.Locale;

/**
 * Pantalla 1: registro de contacto.
 * Captura la firma, el nombre, el teléfono y la ubicación en tiempo real,
 * y lo envía todo a la API en una petición multipart.
 */
public class MainActivity extends AppCompatActivity {

    private static final long TAMANO_MAXIMO_FIRMA = 1024 * 1024; // 1 MB (misma regla que la API)
    private static final long INTERVALO_UBICACION_MS = 5_000;

    // Vistas
    private FirmaView firma;
    private TextInputLayout capaNombre;
    private TextInputLayout capaTelefono;
    private EditText txtNombre;
    private EditText txtTelefono;
    private EditText txtLatitud;
    private EditText txtLongitud;
    private TextView lblEstadoUbicacion;

    // Ubicación
    private FusedLocationProviderClient clienteUbicacion;
    private Double latitud;
    private Double longitud;
    private boolean recibiendoUbicacion;
    private boolean permisoYaSolicitado;
    private AlertDialog dialogoGps;

    // Red
    private ApiContactos api;
    private Dialogos.Cargando cargando;

    /** Recibe las actualizaciones del GPS (cada 5 s) y llena latitud/longitud. */
    private final LocationCallback oyenteUbicacion = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult resultado) {
            Location ultima = resultado.getLastLocation();
            if (ultima != null) {
                mostrarUbicacion(ultima);
            }
        }
    };

    /** Respuesta del usuario al cuadro de permiso de ubicación. */
    private final ActivityResultLauncher<String[]> pedirPermisoUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            resultado -> {
                if (tienePermisoUbicacion()) {
                    verificarUbicacion();
                } else {
                    permisoDenegado();
                }
            });

    // ================= Ciclo de vida =================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        firma = findViewById(R.id.firma);
        capaNombre = findViewById(R.id.capaNombre);
        capaTelefono = findViewById(R.id.capaTelefono);
        txtNombre = findViewById(R.id.txtNombre);
        txtTelefono = findViewById(R.id.txtTelefono);
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);
        lblEstadoUbicacion = findViewById(R.id.lblEstadoUbicacion);

        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this);
        api = new ApiContactos(this, this);
        cargando = new Dialogos.Cargando(this);

        findViewById(R.id.btnLimpiarFirma).setOnClickListener(v -> firma.limpiar());
        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvarContacto());
        findViewById(R.id.btnContactos).setOnClickListener(v ->
                startActivity(new Intent(this, ListaContactosActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarUbicacion(); // también al volver de Ajustes (GPS o permiso)
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerUbicacion(); // ahorra batería cuando la pantalla no está visible
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        api.cancelarTodo();
        cargando.ocultar();
    }

    // ================= Bloque 1: guardar contacto =================

    /** Valida en el orden del boceto y, si todo está bien, envía a la API. */
    private void salvarContacto() {
        capaNombre.setError(null);
        capaTelefono.setError(null);
        String nombre = texto(txtNombre);
        String telefono = texto(txtTelefono);

        if (firma.estaVacia()) {
            Dialogos.alerta(this, "No se ha capturado la firma.");
            return;
        }
        if (nombre.isEmpty()) {
            capaNombre.setError("Escriba el nombre");
            txtNombre.requestFocus();
            return;
        }
        if (telefono.replaceAll("[^0-9]", "").length() < 8) {
            capaTelefono.setError("El teléfono debe tener al menos 8 dígitos");
            txtTelefono.requestFocus();
            return;
        }
        if (!tienePermisoUbicacion()) {
            permisoDenegado();
            return;
        }
        if (!gpsActivo()) {
            mostrarGpsApagado();
            return;
        }
        if (latitud == null || longitud == null) {
            Dialogos.alerta(this, "Debe describir la ubicación.\nEspere a que el GPS obtenga la latitud y la longitud.");
            return;
        }

        byte[] png = firma.exportarPng();
        if (png.length > TAMANO_MAXIMO_FIRMA) {
            Dialogos.alerta(this, "La firma supera el tamaño máximo de 1 MB.");
            return;
        }
        enviar(nombre, telefono, latitud, longitud, png);
    }

    /** POST multipart con indicador de carga; si no hay conexión se ofrece "Reintentar". */
    private void enviar(String nombre, String telefono, double lat, double lon, byte[] png) {
        cargando.mostrar("Guardando contacto…");
        api.crear(nombre, telefono, lat, lon, png, new ApiContactos.Respuesta<Contacto>() {
            @Override
            public void exito(Contacto creado) {
                cargando.ocultar();
                Toast.makeText(MainActivity.this, "Contacto guardado", Toast.LENGTH_SHORT).show();
                txtNombre.setText("");
                txtTelefono.setText("");
                firma.limpiar();
            }

            @Override
            public void error(ErrorApi error) {
                cargando.ocultar();
                Dialogos.errorApi(MainActivity.this, error, () -> enviar(nombre, telefono, lat, lon, png));
            }
        });
    }

    // ================= Bloque 2: ubicación (FusedLocationProviderClient) =================

    /** Revisa permiso y GPS antes de pedir ubicaciones. */
    private void verificarUbicacion() {
        if (!tienePermisoUbicacion()) {
            if (!permisoYaSolicitado) {
                permisoYaSolicitado = true;
                solicitarPermiso();
            } else {
                lblEstadoUbicacion.setText("Permiso de ubicación denegado.");
            }
            return;
        }
        if (!gpsActivo()) {
            lblEstadoUbicacion.setText("GPS apagado.");
            mostrarGpsApagado();
            return;
        }
        iniciarUbicacion();
    }

    private void solicitarPermiso() {
        String[] permisos = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // El usuario ya lo rechazó una vez: se explica para qué se usa
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permiso de ubicación")
                    .setMessage("La app usa su ubicación para guardar la latitud y longitud del contacto.")
                    .setPositiveButton("Continuar", (d, w) -> pedirPermisoUbicacion.launch(permisos))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            pedirPermisoUbicacion.launch(permisos);
        }
    }

    /** Permiso denegado: se explica y se ofrece concederlo (o abrir Ajustes si se negó para siempre). */
    private void permisoDenegado() {
        lblEstadoUbicacion.setText("Permiso de ubicación denegado.");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Alerta")
                .setMessage("Sin permiso de ubicación no se puede registrar el contacto.\nConceda el permiso para continuar.")
                .setPositiveButton("Conceder", (d, w) -> {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        solicitarPermiso();
                    } else {
                        // "No volver a preguntar": solo se puede activar desde los ajustes de la app
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null)));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Alerta "GPS no está activo" con acceso directo a los ajustes de ubicación. */
    private void mostrarGpsApagado() {
        if (dialogoGps != null && dialogoGps.isShowing()) return;
        dialogoGps = new MaterialAlertDialogBuilder(this)
                .setTitle("Alerta")
                .setMessage("GPS no está activo.\nActívelo para obtener la ubicación.")
                .setPositiveButton("Activar GPS", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Pide ubicaciones en tiempo real (cada 5 s) mientras la pantalla está visible. */
    private void iniciarUbicacion() {
        if (recibiendoUbicacion) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest solicitud = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_UBICACION_MS)
                .setMinUpdateIntervalMillis(2_000)
                .build();
        clienteUbicacion.requestLocationUpdates(solicitud, oyenteUbicacion, Looper.getMainLooper());
        recibiendoUbicacion = true;

        if (latitud == null) {
            lblEstadoUbicacion.setText(R.string.buscando_ubicacion);
            // La última ubicación conocida llena los campos de inmediato mientras llega una nueva
            clienteUbicacion.getLastLocation().addOnSuccessListener(this, ultima -> {
                if (ultima != null && latitud == null) mostrarUbicacion(ultima);
            });
        }
    }

    private void detenerUbicacion() {
        if (recibiendoUbicacion) {
            clienteUbicacion.removeLocationUpdates(oyenteUbicacion);
            recibiendoUbicacion = false;
        }
    }

    private void mostrarUbicacion(Location ubicacion) {
        latitud = ubicacion.getLatitude();
        longitud = ubicacion.getLongitude();
        txtLatitud.setText(String.format(Locale.US, "%.6f", latitud));
        txtLongitud.setText(String.format(Locale.US, "%.6f", longitud));
        lblEstadoUbicacion.setText(String.format(Locale.getDefault(),
                "Ubicación en tiempo real · precisión ±%d m", Math.round(ubicacion.getAccuracy())));
    }

    private boolean tienePermisoUbicacion() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** true si la ubicación del teléfono (GPS) está encendida. */
    private boolean gpsActivo() {
        LocationManager administrador = (LocationManager) getSystemService(LOCATION_SERVICE);
        return administrador != null && LocationManagerCompat.isLocationEnabled(administrador);
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}

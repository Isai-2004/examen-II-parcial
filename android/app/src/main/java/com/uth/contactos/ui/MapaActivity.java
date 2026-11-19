package com.uth.contactos.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.uth.contactos.R;

/** Pantalla 3: mapa de Google con un marcador en la ubicación del contacto. */
public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_NOMBRE = "nombre";
    public static final String EXTRA_LATITUD = "latitud";
    public static final String EXTRA_LONGITUD = "longitud";

    private GoogleMap mMap;
    private String nombre;
    private double latitud;
    private double longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        // Datos del contacto elegido en la pantalla 2
        nombre = getIntent().getStringExtra(EXTRA_NOMBRE);
        latitud = getIntent().getDoubleExtra(EXTRA_LATITUD, 0);
        longitud = getIntent().getDoubleExtra(EXTRA_LONGITUD, 0);

        MaterialToolbar barra = findViewById(R.id.barra);
        barra.setTitle(nombre);
        barra.setNavigationOnClickListener(v -> finish());

        // El mapa se carga en segundo plano y avisa en onMapReady
        SupportMapFragment fragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        if (fragmento != null) {
            fragmento.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Marcador con el nombre del contacto
        LatLng ubicacion = new LatLng(latitud, longitud);
        Marker marcador = mMap.addMarker(new MarkerOptions().position(ubicacion).title(nombre));
        if (marcador != null) {
            marcador.showInfoWindow();
        }

        // Centra y acerca la cámara a nivel de calle
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 16f));
    }
}

package com.uth.contactos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.uth.contactos.R;

import java.io.ByteArrayOutputStream;

/**
 * Vista de dibujo para capturar la firma con el dedo.
 * Cada trazo se guarda en un Path y se pinta en el Canvas en onDraw().
 */
public class FirmaView extends View {

    // Tamaño mínimo (en píxeles) que debe ocupar la firma para no considerarse vacía
    private static final float TAMANO_MINIMO_PX = 20f;

    private final Path trazo = new Path();
    private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float ultimoX;
    private float ultimoY;

    public FirmaView(Context contexto, @Nullable AttributeSet atributos) {
        super(contexto, atributos);
        pincel.setColor(ContextCompat.getColor(contexto, R.color.tinta_firma));
        pincel.setStyle(Paint.Style.STROKE);
        pincel.setStrokeJoin(Paint.Join.ROUND);
        pincel.setStrokeCap(Paint.Cap.ROUND);
        pincel.setStrokeWidth(4f * getResources().getDisplayMetrics().density); // 4 dp
    }

    // ---------- Dibujo ----------
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(trazo, pincel);
    }

    // ---------- Captura del dedo ----------
    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        float x = evento.getX();
        float y = evento.getY();

        switch (evento.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Evita que el ScrollView se desplace mientras se firma
                getParent().requestDisallowInterceptTouchEvent(true);
                trazo.moveTo(x, y);
                ultimoX = x;
                ultimoY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                // Curva entre el punto anterior y el actual para un trazo suave
                trazo.quadTo(ultimoX, ultimoY, (x + ultimoX) / 2, (y + ultimoY) / 2);
                ultimoX = x;
                ultimoY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                trazo.lineTo(x, y);
                getParent().requestDisallowInterceptTouchEvent(false);
                invalidate();
                performClick();
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    // ---------- Operaciones públicas ----------

    /** Borra la firma para volver a firmar. */
    public void limpiar() {
        trazo.reset();
        invalidate();
    }

    /** La firma está vacía si no hay trazos o si son solo un punto (un toque accidental). */
    public boolean estaVacia() {
        RectF limites = new RectF();
        trazo.computeBounds(limites, true);
        return trazo.isEmpty() || (limites.width() < TAMANO_MINIMO_PX && limites.height() < TAMANO_MINIMO_PX);
    }

    /** Exporta la firma a PNG (fondo blanco, tinta azul) y devuelve sus bytes. */
    public byte[] exportarPng() {
        Bitmap imagen = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(imagen);
        lienzo.drawColor(Color.WHITE);
        lienzo.drawPath(trazo, pincel);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        imagen.compress(Bitmap.CompressFormat.PNG, 100, salida);
        imagen.recycle();
        return salida.toByteArray();
    }
}

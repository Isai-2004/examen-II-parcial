package com.uth.contactos.ui;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.uth.contactos.R;
import com.uth.contactos.modelo.Contacto;

import java.util.ArrayList;
import java.util.List;

/** Adaptador de la lista de contactos; resalta el contacto seleccionado. */
public class ContactoAdapter extends RecyclerView.Adapter<ContactoAdapter.Fila> {

    public interface AlSeleccionar {
        void seleccionar(Contacto contacto);
    }

    private final List<Contacto> contactos = new ArrayList<>();
    private final AlSeleccionar oyente;
    private int idSeleccionado = -1;

    public ContactoAdapter(AlSeleccionar oyente) {
        this.oyente = oyente;
    }

    @SuppressWarnings("NotifyDataSetChanged") // la lista completa cambia en cada búsqueda
    public void establecer(List<Contacto> nuevos) {
        contactos.clear();
        contactos.addAll(nuevos);
        notifyDataSetChanged();
    }

    public boolean contiene(int id) {
        for (Contacto c : contactos) {
            if (c.id == id) return true;
        }
        return false;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    public void limpiarSeleccion() {
        idSeleccionado = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_contacto, padre, false);
        return new Fila(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        Contacto c = contactos.get(posicion);
        fila.lblNombre.setText(c.nombre);
        fila.lblTelefono.setText(c.telefono);

        if (c.id == idSeleccionado) {
            fila.itemView.setBackgroundColor(ContextCompat.getColor(fila.itemView.getContext(), R.color.seleccion));
        } else {
            fila.itemView.setBackgroundResource(fila.fondoNormal);
        }

        fila.itemView.setOnClickListener(v -> {
            int pos = fila.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Contacto elegido = contactos.get(pos);
            int anterior = indiceDe(idSeleccionado);
            idSeleccionado = elegido.id;
            if (anterior >= 0) notifyItemChanged(anterior);
            notifyItemChanged(pos);
            oyente.seleccionar(elegido);
        });
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }

    private int indiceDe(int id) {
        for (int i = 0; i < contactos.size(); i++) {
            if (contactos.get(i).id == id) return i;
        }
        return -1;
    }

    static class Fila extends RecyclerView.ViewHolder {
        final TextView lblNombre;
        final TextView lblTelefono;
        final int fondoNormal; // efecto de toque del tema

        Fila(View vista) {
            super(vista);
            lblNombre = vista.findViewById(R.id.lblNombre);
            lblTelefono = vista.findViewById(R.id.lblTelefono);
            TypedValue valor = new TypedValue();
            vista.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, valor, true);
            fondoNormal = valor.resourceId;
        }
    }
}

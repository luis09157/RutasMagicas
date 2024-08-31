package com.ninodev.rutasmagicas.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.R

class EstadosAdapter(
    private val context: Context,
    private val items: List<EstadoModel>
) : BaseAdapter() {

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): EstadoModel {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.custom_view_estados, parent, false)
            holder = ViewHolder()
            holder.imageView = view.findViewById(R.id.imagen_estado)
            holder.txt_titulo = view.findViewById(R.id.txt_titulo)
            holder.txt_descripcion = view.findViewById(R.id.txt_descripcion)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)
        holder.txt_titulo?.text = item.nombreEstado
        holder.txt_descripcion?.text = item.descripcion

        // Usar Glide para cargar la imagen desde la URL
        Glide.with(context)
            .load(item.imagen)
            .placeholder(R.drawable.ic_launcher_background) // Imagen de marcador de posición
            .error(R.drawable.estado_nuevo_leon) // Imagen de error en caso de fallo
            .into(holder.imageView!!)

        return view
    }

    private class ViewHolder {
        var imageView: ImageView? = null
        var txt_titulo: TextView? = null
        var txt_descripcion: TextView? = null
    }
}

package com.ninodev.rutasmagicas.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ninodev.rutasmagicas.Helper.HelperUser
import com.ninodev.rutasmagicas.Model.EstadoModel
import com.ninodev.rutasmagicas.R
import com.ninodev.rutasmagicas.ui.FirestoreDBHelper

class EstadosAdapter(
    private val context: Context,
    private var items: List<EstadoModel> // Cambiar a var para poder actualizar la lista
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
            holder = ViewHolder().apply {
                imageView = view.findViewById(R.id.imagen_estado)
                txt_titulo = view.findViewById(R.id.txt_titulo)
                txt_pueblos_visitados = view.findViewById(R.id.txt_pueblos_visitados)
                txt_numero_pueblos = view.findViewById(R.id.txt_numero_pueblos)
            }
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)
        holder.txt_titulo?.text = item.nombreEstado
        val numeroPueblosInt = item.numeroPueblos.toIntOrNull() ?: 0

        // Actualiza el TextView con la información de visitas
        puebloVisitados(HelperUser.getUserId()!!, item.nombreEstado, holder.txt_pueblos_visitados!!, item.municipios.size)

        val texto = if (numeroPueblosInt == 1) {
            "$numeroPueblosInt Pueblo"
        } else {
            "$numeroPueblosInt Pueblos"
        }

        holder.txt_numero_pueblos?.text = texto

        // Usar Glide para cargar la imagen desde la URL
        Glide.with(context)
            .load(item.imagen)
            .placeholder(R.drawable.ic_launcher_background) // Imagen de marcador de posición
            .error(R.drawable.estado_nuevo_leon) // Imagen de error en caso de fallo
            .into(holder.imageView!!)

        return view
    }

    private fun puebloVisitados(idUsuario: String, nombreEstado: String, textView: TextView, totalPueblos: Int) {
        val firestoreDBHelper = FirestoreDBHelper()
        firestoreDBHelper.contarPueblosVisitadosEnEstado(idUsuario, nombreEstado, { pueblosVisitados, _ ->
            textView.text = when {
                // No ha visitado ningún pueblo
                pueblosVisitados == 0 -> "No has visitado ningún pueblo."
                // Ha visitado algunos pero no todos
                pueblosVisitados in 1 until totalPueblos -> {
                    val mensaje = if (pueblosVisitados == 1) "pueblo" else "pueblos"
                    "Has visitado $pueblosVisitados $mensaje."
                }
                // Ha visitado todos los pueblos
                pueblosVisitados == totalPueblos -> "¡Todos los pueblos visitados!"
                // Caso inesperado
                else -> "Estado de visitas desconocido."
            }
        }, { exception ->
            // Error al obtener los datos
            textView.text = "Error. Intenta de nuevo."
            Log.e("Visitas", "Error al contar las visitas: ${exception.message}")
        })
    }

    // Método para actualizar la lista de elementos
    fun updateList(newItems: List<EstadoModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    private class ViewHolder {
        var imageView: ImageView? = null
        var txt_titulo: TextView? = null
        var txt_pueblos_visitados: TextView? = null
        var txt_numero_pueblos: TextView? = null
    }
}

package com.example.taller3

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import com.squareup.picasso.Picasso
import kotlin.collections.get

class UserAdapter(private val context: Context, private val users: List<User>) :
    android.widget.BaseAdapter() {

    override fun getCount(): Int = users.size

    override fun getItem(position: Int): Any = users[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.activity_user_adapter, parent, false)
        val user = users[position]

        val imageView = view.findViewById<ImageView>(R.id.userImage)
        val nameView = view.findViewById<TextView>(R.id.userName)
        val button = view.findViewById<Button>(R.id.btnViewLocation)

        // Configurar el nombre del usuario
        nameView.text = user.name

        // Verificar si la URL de la imagen es válida
        if (user.imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(user.imageUrl)
                .placeholder(R.drawable.default_profile)
                .into(imageView)// Imagen de marcador de posición
        } else {
            // Cargar una imagen predeterminada si la URL está vacía
            imageView.setImageResource(R.drawable.default_profile)
        }

        button.setOnClickListener {
            val intent = Intent(context, MapsActivity::class.java).apply {
                putExtra("userId", user.id) // Enviar el ID del usuario
            }
            context.startActivity(intent)
        }

        return view
    }
}
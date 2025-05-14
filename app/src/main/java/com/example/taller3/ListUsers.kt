package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ListUsers : AppCompatActivity() {

    private lateinit var listView: ListView
    private val users = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_users)

        listView = findViewById(R.id.lista)

        val currentUserId = Firebase.auth.currentUser?.uid

        // Cargar usuarios desde Firestore
        val db = Firebase.firestore
        db.collection("users").get().addOnSuccessListener { result ->
            for (document in result) {
                val userId = document.id
                val disponible = document.getBoolean("disponible") ?: false

                // Excluir al usuario en sesión y filtrar solo usuarios disponibles
                if (disponible && userId != currentUserId) {
                    val user = User(
                        id = userId,
                        name = "${document.getString("firstName") ?: ""} ${document.getString("lastName") ?: ""}",
                        imageUrl = document.getString("profileImageUrl") ?: "",
                        latitude = document.getDouble("latitude") ?: 0.0,
                        longitude = document.getDouble("longitude") ?: 0.0,
                        disponible = disponible
                    )
                    users.add(user)
                    Log.d("ListUsers", "Usuario disponible cargado: $user")
                }
            }
            val adapter = UserAdapter(this, users)
            listView.adapter = adapter
        }
    }
}
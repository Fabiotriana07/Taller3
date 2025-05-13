package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ListUsers : AppCompatActivity() {

    private lateinit var listView: ListView
    private val users = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_users)

        listView = findViewById(R.id.lista)

        // Cargar usuarios desde Firestore
        val db = Firebase.firestore
        db.collection("users").get().addOnSuccessListener { result ->
            for (document in result) {
                val user = User(
                    id = document.id,
                    name = "${document.getString("firstName") ?: ""} ${document.getString("lastName") ?: ""}",
                    imageUrl = document.getString("profileImageUrl") ?: "",
                    latitude = document.getDouble("latitude") ?: 0.0,
                    longitude = document.getDouble("longitude") ?: 0.0
                )
                users.add(user)
                Log.d("ListUsers", "Usuario cargado: $user")
            }
            val adapter = UserAdapter(this, users)
            listView.adapter = adapter
        }
    }
}
package com.example.taller3

data class User(
    val id: String,
    val name: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val disponible: Boolean
)
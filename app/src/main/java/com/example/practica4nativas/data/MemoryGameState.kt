package com.example.practica4nativas.data
data class MemoryGameState(
    val nivel: Int,
    var puntuacion: Int,
    val tablero: List<String>,
    val descubiertos: MutableList<Boolean>,
    var movimientos: Int,
    val tiempo: String
)

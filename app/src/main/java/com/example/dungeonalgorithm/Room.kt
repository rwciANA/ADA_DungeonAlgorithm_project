package com.example.dungeonalgorithm

import kotlin.math.abs
import kotlin.math.sqrt

/** Representa una habitación rectangular en el mapa de la mazmorra */
data class Room(
    val id: Int,
    val x: Int,       // columna del extremo superior izquierdo
    val y: Int,       // fila del extremo superior izquierdo
    val width: Int,
    val height: Int
) {
    fun area() = width * height

    /** Centro de la habitación */
    fun centerX() = x + width / 2
    fun centerY() = y + height / 2

    /** Distancia Euclidiana entre centros de dos habitaciones */
    fun distanceTo(other: Room): Double {
        val dx = (centerX() - other.centerX()).toDouble()
        val dy = (centerY() - other.centerY()).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /** Verifica si dos habitaciones se solapan (con margen de 1 celda) */
    fun overlaps(other: Room): Boolean {
        return x - 1 < other.x + other.width &&
               x + width + 1 > other.x &&
               y - 1 < other.y + other.height &&
               y + height + 1 > other.y
    }
}

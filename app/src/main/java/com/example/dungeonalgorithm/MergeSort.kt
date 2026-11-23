package com.example.dungeonalgorithm

/**
 * ======================================================
 * ALGORITMO 1: MERGE SORT
 * ======================================================
 * Propósito en el juego:
 *   Ordena las habitaciones por área (de mayor a menor)
 *   para que Kruskal conecte primero las más grandes
 *   (las habitaciones grandes son más importantes).
 *
 * Complejidad:
 *   Tiempo: O(n log n) — siempre, en todos los casos
 *   Espacio: O(n) — usa espacio auxiliar para el merge
 */
object MergeSort {

    /**
     * Punto de entrada público.
     * Recibe una lista de habitaciones y devuelve una nueva lista ordenada
     * por área de MAYOR a MENOR (habitaciones grandes primero).
     */
    fun sortByArea(rooms: List<Room>): List<Room> {
        if (rooms.size <= 1) return rooms

        val mid = rooms.size / 2
        val left = sortByArea(rooms.subList(0, mid))       // Mitad izquierda
        val right = sortByArea(rooms.subList(mid, rooms.size)) // Mitad derecha

        return merge(left, right)
    }

    /**
     * Fusiona dos listas YA ORDENADAS en una sola lista ordenada.
     * Este es el corazón del algoritmo.
     */
    private fun merge(left: List<Room>, right: List<Room>): List<Room> {
        val result = mutableListOf<Room>()
        var i = 0
        var j = 0

        // Compara elemento por elemento y agrega el mayor
        while (i < left.size && j < right.size) {
            if (left[i].area() >= right[j].area()) {
                result.add(left[i])
                i++
            } else {
                result.add(right[j])
                j++
            }
        }

        // Agrega los elementos restantes de cada mitad
        while (i < left.size) { result.add(left[i]); i++ }
        while (j < right.size) { result.add(right[j]); j++ }

        return result
    }

    /**
     * Variante: ordenar por distancia al origen (0,0)
     * Útil para decidir cuál habitación es la "entrada" (la más cercana al origen)
     */
    fun sortByDistanceToOrigin(rooms: List<Room>): List<Room> {
        if (rooms.size <= 1) return rooms

        val mid = rooms.size / 2
        val left = sortByDistanceToOrigin(rooms.subList(0, mid))
        val right = sortByDistanceToOrigin(rooms.subList(mid, rooms.size))

        return mergeByDistance(left, right)
    }

    private fun mergeByDistance(left: List<Room>, right: List<Room>): List<Room> {
        val result = mutableListOf<Room>()
        var i = 0; var j = 0

        fun dist(r: Room) = r.centerX() * r.centerX() + r.centerY() * r.centerY()

        while (i < left.size && j < right.size) {
            if (dist(left[i]) <= dist(right[j])) { result.add(left[i]); i++ }
            else { result.add(right[j]); j++ }
        }
        while (i < left.size) { result.add(left[i]); i++ }
        while (j < right.size) { result.add(right[j]); j++ }
        return result
    }
}

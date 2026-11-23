package com.example.dungeonalgorithm

import java.util.PriorityQueue

/**
 * ======================================================
 * ALGORITMO 3: DIJKSTRA — CAMINO MÁS CORTO
 * ======================================================
 * Propósito en el juego:
 *   1. IA del ENEMIGO: el monstruo calcula la ruta más corta
 *      hacia el jugador usando el mapa real de la mazmorra.
 *   2. AYUDA al jugador: muestra el camino óptimo hacia la salida.
 *
 * Complejidad:
 *   Con PriorityQueue (min-heap): O((V + E) log V)
 *   En un grid 2D con 4 vecinos: E ≈ 4V  →  O(V log V)
 *
 * Optimización incluida: EARLY EXIT (parar al llegar al destino)
 */

/** Celda del grid: fila (row) y columna (col) */
data class Cell(val row: Int, val col: Int)

/** Nodo en la cola de prioridad: (distancia acumulada, celda) */
private data class DijkstraNode(val dist: Int, val cell: Cell) : Comparable<DijkstraNode> {
    override fun compareTo(other: DijkstraNode) = this.dist.compareTo(other.dist)
}

object Dijkstra {

    // Las 4 direcciones posibles: arriba, abajo, izquierda, derecha
    private val DIRECTIONS = listOf(
        Cell(-1, 0), // arriba
        Cell(1, 0),  // abajo
        Cell(0, -1), // izquierda
        Cell(0, 1)   // derecha
    )

    /**
     * Calcula el camino más corto desde [start] hasta [end] en el [grid].
     *
     * @param grid  Mapa 2D: 0 = celda libre, 1 = pared (bloqueada)
     * @param start Celda de inicio
     * @param end   Celda de destino
     * @return Lista de celdas que forman el camino (incluye start y end),
     *         o lista vacía si no hay camino.
     */
    fun findPath(grid: Array<IntArray>, start: Cell, end: Cell): List<Cell> {
        val rows = grid.size
        val cols = grid[0].size

        // Tabla de distancias: infinito al principio
        val dist = Array(rows) { IntArray(cols) { Int.MAX_VALUE } }
        // Tabla de predecesores para reconstruir el camino
        val prev = Array(rows) { arrayOfNulls<Cell>(cols) }

        dist[start.row][start.col] = 0

        // Cola de prioridad (min-heap): primero el nodo con menor distancia
        val pq = PriorityQueue<DijkstraNode>()
        pq.add(DijkstraNode(0, start))

        while (pq.isNotEmpty()) {
            val (d, curr) = pq.poll()

            // EARLY EXIT: si llegamos al destino, ya terminamos
            if (curr == end) break

            // Si esta entrada en la cola ya está desactualizada, ignorar
            if (d > dist[curr.row][curr.col]) continue

            // Explorar los 4 vecinos
            for (dir in DIRECTIONS) {
                val nr = curr.row + dir.row
                val nc = curr.col + dir.col

                // Verificar límites del grid
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
                // Verificar que no sea pared
                if (grid[nr][nc] == 1) continue

                val newDist = dist[curr.row][curr.col] + 1  // Peso = 1 por paso

                // RELAJACIÓN: si encontramos una ruta más corta, actualizar
                if (newDist < dist[nr][nc]) {
                    dist[nr][nc] = newDist
                    prev[nr][nc] = curr
                    pq.add(DijkstraNode(newDist, Cell(nr, nc)))
                }
            }
        }

        // Reconstruir el camino desde end hasta start (usando prev)
        return reconstructPath(prev, start, end)
    }

    /**
     * Recorre la tabla prev desde el destino hasta el origen
     * para obtener el camino completo en orden origen→destino.
     */
    private fun reconstructPath(
        prev: Array<Array<Cell?>>,
        start: Cell,
        end: Cell
    ): List<Cell> {
        val path = mutableListOf<Cell>()
        var current: Cell? = end

        while (current != null) {
            path.add(0, current) // Insertar al frente para invertir el orden
            if (current == start) break
            current = prev[current.row][current.col]
        }

        return if (path.isNotEmpty() && path.first() == start) path else emptyList()
    }

    /**
     * Calcula solo el SIGUIENTE PASO hacia el destino.
     * Útil para mover al enemigo un paso a la vez cada turno.
     *
     * @return La siguiente celda a la que debe moverse el enemigo,
     *         o null si no hay camino.
     */
    fun nextStep(grid: Array<IntArray>, from: Cell, to: Cell): Cell? {
        val path = findPath(grid, from, to)
        return if (path.size >= 2) path[1] else null // path[0] es 'from', path[1] es el siguiente
    }
}

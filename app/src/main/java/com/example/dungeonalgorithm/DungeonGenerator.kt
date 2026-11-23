package com.example.dungeonalgorithm

import kotlin.random.Random

/**
 * Motor principal de generación de mazmorras.
 * Coordina los 3 algoritmos en orden:
 *   1. Genera habitaciones aleatorias
 *   2. MERGE SORT → ordena por área
 *   3. KRUSKAL    → conecta habitaciones con pasillos
 *   4. Coloca jugador, salida y enemigos
 *   5. DIJKSTRA   → lo usa el enemigo cada turno
 */
class DungeonGenerator(
    val cols: Int = 40,
    val rows: Int = 25,
    val numRooms: Int = 8,
    val seed: Long = System.currentTimeMillis()
) {
    // Valores del grid
    companion object {
        const val WALL = 1
        const val FLOOR = 0
        const val PLAYER = 2
        const val EXIT = 3
        const val ENEMY = 4
        const val PATH = 5   // Para visualizar camino de Dijkstra
    }

    var grid = Array(rows) { IntArray(cols) { WALL } }
        private set
    var rooms = listOf<Room>()
        private set
    var corridors = listOf<Corridor>()
        private set
    var playerPos = Cell(0, 0)
        private set
    var exitPos = Cell(0, 0)
        private set
    var enemyPos = Cell(0, 0)
        private set
    var currentPath = listOf<Cell>()   // Camino actual del Dijkstra (para visualización)
        private set

    private val rng = Random(seed)

    /** Genera el mapa completo */
    fun generate() {
        // Resetear grid
        grid = Array(rows) { IntArray(cols) { WALL } }

        // 1. Generar habitaciones aleatorias sin solapamiento
        val generatedRooms = generateRooms()

        // 2. MERGE SORT: ordenar habitaciones por área (mayor primero)
        rooms = MergeSort.sortByArea(generatedRooms)

        // 3. Tallar las habitaciones en el grid
        for (room in rooms) carveRoom(room)

        // 4. KRUSKAL: conectar habitaciones con pasillos mínimos
        corridors = Kruskal.buildCorridors(rooms)
        for (corridor in corridors) carveCorridor(corridor)

        // 5. Colocar jugador en la habitación más grande (rooms[0] por Merge Sort)
        playerPos = Cell(rooms.first().centerY(), rooms.first().centerX())
        grid[playerPos.row][playerPos.col] = PLAYER

        // 6. Colocar salida en la habitación más lejana al jugador
        val farthestRoom = rooms.maxByOrNull { r ->
            val dr = (r.centerY() - playerPos.row)
            val dc = (r.centerX() - playerPos.col)
            dr * dr + dc * dc
        } ?: rooms.last()
        exitPos = Cell(farthestRoom.centerY(), farthestRoom.centerX())
        grid[exitPos.row][exitPos.col] = EXIT

        // 7. Colocar enemigo en una habitación intermedia
        val enemyRoom = rooms.getOrElse(rooms.size / 2) { rooms.last() }
        enemyPos = Cell(enemyRoom.centerY(), enemyRoom.centerX())
        grid[enemyPos.row][enemyPos.col] = ENEMY
    }

    /** Genera habitaciones aleatorias sin solapamiento */
    private fun generateRooms(): List<Room> {
        val result = mutableListOf<Room>()
        var attempts = 0
        var id = 0

        while (result.size < numRooms && attempts < numRooms * 10) {
            attempts++
            val w = rng.nextInt(4, 10)
            val h = rng.nextInt(3, 7)
            val x = rng.nextInt(1, cols - w - 1)
            val y = rng.nextInt(1, rows - h - 1)
            val newRoom = Room(id, x, y, w, h)

            if (result.none { it.overlaps(newRoom) }) {
                result.add(newRoom)
                id++
            }
        }
        return result
    }

    /** Dibuja una habitación (pone FLOORs) en el grid */
    private fun carveRoom(room: Room) {
        for (r in room.y until room.y + room.height)
            for (c in room.x until room.x + room.width)
                if (r in 0 until rows && c in 0 until cols)
                    grid[r][c] = FLOOR
    }

    /** Dibuja un pasillo en L entre los centros de dos habitaciones */
    private fun carveCorridor(corridor: Corridor) {
        val r1 = corridor.roomA.centerY()
        val c1 = corridor.roomA.centerX()
        val r2 = corridor.roomB.centerY()
        val c2 = corridor.roomB.centerX()

        // Pasillo horizontal primero, luego vertical
        val minC = minOf(c1, c2); val maxC = maxOf(c1, c2)
        for (c in minC..maxC)
            if (r1 in 0 until rows && c in 0 until cols) grid[r1][c] = FLOOR

        val minR = minOf(r1, r2); val maxR = maxOf(r1, r2)
        for (r in minR..maxR)
            if (r in 0 until rows && c2 in 0 until cols) grid[r][c2] = FLOOR
    }

    // ================================================================
    // MECÁNICAS DE JUEGO (usan Dijkstra)
    // ================================================================

    /** Mueve al jugador si la celda destino es transitable. Devuelve true si ganó. */
    fun movePlayer(dRow: Int, dCol: Int): Boolean {
        val nr = playerPos.row + dRow
        val nc = playerPos.col + dCol
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return false
        if (grid[nr][nc] == WALL) return false

        val won = grid[nr][nc] == EXIT

        // Limpiar posición anterior
        grid[playerPos.row][playerPos.col] = FLOOR
        playerPos = Cell(nr, nc)
        grid[playerPos.row][playerPos.col] = PLAYER

        return won
    }

    /**
     * Mueve al enemigo UN PASO hacia el jugador usando DIJKSTRA.
     * Devuelve true si el enemigo alcanzó al jugador (game over).
     */
    fun moveEnemy(): Boolean {
        // Preparar grid sin jugador/enemigo para que Dijkstra funcione
        val pathGrid = Array(rows) { r -> IntArray(cols) { c ->
            when (grid[r][c]) {
                WALL -> 1
                else -> 0
            }
        }}

        // DIJKSTRA: encontrar el siguiente paso del enemigo hacia el jugador
        val next = Dijkstra.nextStep(pathGrid, enemyPos, playerPos) ?: return false

        // Actualizar camino completo para visualización
        currentPath = Dijkstra.findPath(pathGrid, enemyPos, playerPos)

        // Mover el enemigo
        grid[enemyPos.row][enemyPos.col] = FLOOR
        enemyPos = next

        return if (enemyPos == playerPos) {
            true  // ¡El enemigo atrapó al jugador! GAME OVER
        } else {
            grid[enemyPos.row][enemyPos.col] = ENEMY
            false
        }
    }

    /**
     * Calcula y devuelve el camino óptimo del jugador a la salida (modo ayuda).
     */
    fun getPathToExit(): List<Cell> {
        val pathGrid = Array(rows) { r -> IntArray(cols) { c ->
            if (grid[r][c] == WALL) 1 else 0
        }}
        return Dijkstra.findPath(pathGrid, playerPos, exitPos)
    }
}

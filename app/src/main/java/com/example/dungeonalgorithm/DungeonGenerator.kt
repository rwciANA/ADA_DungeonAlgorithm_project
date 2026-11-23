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
        //    (si el azar no logra colocar al menos 2, se reintenta: con 0 rooms.first() fallaba)
        var generatedRooms = generateRooms()
        var intentos = 1
        while (generatedRooms.size < 2 && intentos < 20) {
            generatedRooms = generateRooms()
            intentos++
        }
        require(generatedRooms.isNotEmpty()) { "El mapa ${cols}x$rows es muy chico para generar habitaciones" }

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

        // 7. Colocar enemigo en una habitación intermedia, distinta a la del jugador y la salida
        //    (con pocas habitaciones rooms[size/2] podía ser la de la salida y la tapaba)
        val intermedias = rooms.filter { it != rooms.first() && it != farthestRoom }
        val enemyRoom = intermedias.getOrNull(intermedias.size / 2)
        enemyPos = if (enemyRoom != null) {
            Cell(enemyRoom.centerY(), enemyRoom.centerX())
        } else {
            // Solo hay 1 o 2 habitaciones: el enemigo va a una esquina de la sala de salida
            Cell(farthestRoom.y, farthestRoom.x)
        }
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

    /** Resultado de intentar mover al jugador una celda. */
    enum class MoveResult { BLOCKED, MOVED, WON, CAUGHT }

    /** Mueve al jugador si la celda destino es transitable. */
    fun movePlayer(dRow: Int, dCol: Int): MoveResult {
        val nr = playerPos.row + dRow
        val nc = playerPos.col + dCol
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return MoveResult.BLOCKED
        if (grid[nr][nc] == WALL) return MoveResult.BLOCKED

        // Caminar hacia el monstruo también es perder (antes se podía atravesar)
        if (Cell(nr, nc) == enemyPos) return MoveResult.CAUGHT

        val won = grid[nr][nc] == EXIT

        // Limpiar posición anterior
        grid[playerPos.row][playerPos.col] = FLOOR
        playerPos = Cell(nr, nc)
        grid[playerPos.row][playerPos.col] = PLAYER

        return if (won) MoveResult.WON else MoveResult.MOVED
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

        // Mover el enemigo (si estaba parado sobre la salida, la salida se restaura)
        grid[enemyPos.row][enemyPos.col] = if (enemyPos == exitPos) EXIT else FLOOR
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

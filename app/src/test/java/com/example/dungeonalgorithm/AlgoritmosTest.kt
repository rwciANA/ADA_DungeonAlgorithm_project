package com.example.dungeonalgorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Pruebas de los 3 algoritmos (Merge Sort, Kruskal, Dijkstra) y del generador de mazmorras. */
class AlgoritmosTest {

    // ---------------- MERGE SORT ----------------

    @Test
    fun mergeSort_ordenaPorAreaDeMayorAMenor() {
        val rng = Random(7)
        val rooms = List(50) { Room(it, 0, 0, rng.nextInt(1, 10), rng.nextInt(1, 10)) }
        val areas = MergeSort.sortByArea(rooms).map { it.area() }
        assertEquals(rooms.map { it.area() }.sortedDescending(), areas)
    }

    @Test
    fun mergeSort_esEstable() {
        // Misma área: deben conservar su orden original (ids 0, 1, 2)
        val rooms = listOf(Room(0, 0, 0, 2, 3), Room(1, 0, 0, 3, 2), Room(2, 0, 0, 6, 1), Room(3, 0, 0, 5, 5))
        assertEquals(listOf(3, 0, 1, 2), MergeSort.sortByArea(rooms).map { it.id })
    }

    @Test
    fun mergeSort_genericoIgualAlSortDeKotlin() {
        val rng = Random(42)
        val numeros = List(1000) { rng.nextInt(-500, 500) }
        assertEquals(numeros.sorted(), MergeSort.sort(numeros, naturalOrder()))
        assertEquals(emptyList<Int>(), MergeSort.sort(emptyList<Int>(), naturalOrder()))
    }

    // ---------------- KRUSKAL ----------------

    private fun salas(n: Int, seed: Int): List<Room> {
        val rng = Random(seed)
        return List(n) { Room(it, rng.nextInt(0, 100), rng.nextInt(0, 100), 4, 3) }
    }

    @Test
    fun kruskal_tieneNMenos1AristasYConectaTodo() {
        for (n in listOf(2, 5, 12)) {
            val rooms = salas(n, n)
            val mst = Kruskal.buildCorridors(rooms)
            assertEquals(n - 1, mst.size)
            val uf = UnionFind(n)
            for (c in mst) uf.union(c.roomA.id, c.roomB.id)
            assertTrue("el árbol no conecta todas las salas", rooms.all { uf.find(it.id) == uf.find(0) })
        }
    }

    @Test
    fun kruskal_pesoMinimoIgualAFuerzaBruta() {
        // Con 5 salas hay 5^3 = 125 árboles posibles (Cayley): se prueban todos los subconjuntos de 4 aristas
        val rooms = salas(5, 3)
        val aristas = rooms.indices.flatMap { i -> (i + 1 until rooms.size).map { j -> i to j } }
        var mejor = Double.MAX_VALUE
        for (mask in 0 until (1 shl aristas.size)) {
            if (Integer.bitCount(mask) != rooms.size - 1) continue
            val uf = UnionFind(rooms.size)
            var peso = 0.0
            var ok = true
            for (k in aristas.indices) if (mask shr k and 1 == 1) {
                val (a, b) = aristas[k]
                if (!uf.union(a, b)) { ok = false; break }
                peso += rooms[a].distanceTo(rooms[b])
            }
            if (ok) mejor = minOf(mejor, peso)
        }
        assertEquals(mejor, Kruskal.buildCorridors(rooms).sumOf { it.weight }, 1e-9)
    }

    // ---------------- DIJKSTRA ----------------

    @Test
    fun dijkstra_encuentraElCaminoMasCorto() {
        // 0 = libre, 1 = pared. El camino más corto de (0,0) a (0,4) rodea la pared: 10 pasos
        val grid = arrayOf(
            intArrayOf(0, 1, 0, 1, 0),
            intArrayOf(0, 1, 0, 1, 0),
            intArrayOf(0, 0, 0, 1, 0),
            intArrayOf(1, 1, 0, 0, 0),
        )
        val path = Dijkstra.findPath(grid, Cell(0, 0), Cell(0, 4))
        assertEquals(11, path.size)  // 10 pasos = 11 celdas
        assertEquals(Cell(0, 0), path.first())
        assertEquals(Cell(0, 4), path.last())
        // Cada paso es a una celda vecina y libre
        for ((a, b) in path.zipWithNext()) {
            assertEquals(1, Math.abs(a.row - b.row) + Math.abs(a.col - b.col))
            assertEquals(0, grid[b.row][b.col])
        }
    }

    @Test
    fun dijkstra_sinCaminoDevuelveListaVacia() {
        val grid = arrayOf(intArrayOf(0, 1, 0))
        assertTrue(Dijkstra.findPath(grid, Cell(0, 0), Cell(0, 2)).isEmpty())
    }

    // ---------------- GENERADOR ----------------

    @Test
    fun generador_laSalidaSiempreEsAlcanzable() {
        for (seed in 1L..200L) {
            val d = DungeonGenerator(cols = 35, rows = 22, numRooms = 7, seed = seed)
            d.generate()
            assertTrue("semilla $seed: salida inalcanzable", d.getPathToExit().isNotEmpty())
            assertTrue("semilla $seed: enemigo sobre jugador o salida",
                d.enemyPos != d.playerPos && d.enemyPos != d.exitPos)
        }
    }

    @Test
    fun generador_elEnemigoNoBorraLaSalida() {
        // El jugador se queda quieto y el enemigo avanza hasta atraparlo; la salida debe seguir en el mapa
        for (seed in 1L..200L) {
            val d = DungeonGenerator(cols = 35, rows = 22, numRooms = 7, seed = seed)
            d.generate()
            var turnos = 0
            while (!d.moveEnemy() && turnos++ < 500) {
                val celda = d.grid[d.exitPos.row][d.exitPos.col]
                assertTrue("semilla $seed: la salida desapareció",
                    celda == DungeonGenerator.EXIT || celda == DungeonGenerator.ENEMY)
            }
        }
    }

    @Test
    fun generador_caminarHaciaElEnemigoEsPerder() {
        // El enemigo avanza hasta quedar pegado al jugador y luego el jugador camina hacia él
        val d = DungeonGenerator(cols = 35, rows = 22, numRooms = 7, seed = 5)
        d.generate()
        fun pegados() = Math.abs(d.enemyPos.row - d.playerPos.row) + Math.abs(d.enemyPos.col - d.playerPos.col) == 1
        var turnos = 0
        while (!pegados() && turnos++ < 500) assertTrue("el enemigo atrapó antes de quedar pegado", !d.moveEnemy())
        assertTrue(pegados())
        assertEquals(DungeonGenerator.MoveResult.CAUGHT,
            d.movePlayer(d.enemyPos.row - d.playerPos.row, d.enemyPos.col - d.playerPos.col))
    }
}

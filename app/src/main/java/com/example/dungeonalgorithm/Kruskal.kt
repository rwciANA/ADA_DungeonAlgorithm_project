package com.example.dungeonalgorithm

/**
 * ======================================================
 * ALGORITMO 2: KRUSKAL + UNION-FIND
 * ======================================================
 * Propósito en el juego:
 *   Genera el Árbol de Expansión Mínima (MST) que conecta
 *   TODAS las habitaciones con el mínimo de pasillos,
 *   garantizando que el mapa sea completamente navegable.
 *
 * Complejidad:
 *   Ordenamiento de aristas: O(E log E)  donde E = n*(n-1)/2
 *   Union-Find (con optimizaciones): O(α(n)) ≈ O(1) por operación
 *   Total: O(E log E)
 */

/** Representa un pasillo entre dos habitaciones */
data class Corridor(val roomA: Room, val roomB: Room, val weight: Double)

/**
 * Union-Find (Disjoint Set Union) con dos optimizaciones clave:
 *
 * 1. PATH COMPRESSION: Al buscar la raíz de un nodo, apunta directamente
 *    a la raíz todos los nodos en el camino → future finds son O(1)
 *
 * 2. UNION BY RANK: Siempre se une el árbol más pequeño debajo del más grande
 *    → el árbol nunca crece más de O(log n) de altura
 *
 * Juntas hacen que find() y union() sean casi O(1) (técnicamente O(α(n)))
 */
class UnionFind(size: Int) {
    private val parent = IntArray(size) { it }  // Cada nodo es su propio padre
    private val rank = IntArray(size) { 0 }      // Altura aproximada del árbol

    /**
     * Encuentra la raíz del conjunto al que pertenece x.
     * Aplica PATH COMPRESSION: hace que todos los nodos visitados
     * apunten directamente a la raíz.
     */
    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x]) // Recursión + comprimir ruta
        }
        return parent[x]
    }

    /**
     * Une los conjuntos de x e y.
     * Devuelve TRUE si se unieron (no había ciclo).
     * Devuelve FALSE si ya estaban en el mismo conjunto (habría ciclo).
     * Aplica UNION BY RANK: el árbol de menor rank queda debajo.
     */
    fun union(x: Int, y: Int): Boolean {
        val rootX = find(x)
        val rootY = find(y)

        if (rootX == rootY) return false  // ¡Ya conectados! → ciclo detectado

        // Unir el de menor rank debajo del de mayor rank
        when {
            rank[rootX] < rank[rootY] -> parent[rootX] = rootY
            rank[rootX] > rank[rootY] -> parent[rootY] = rootX
            else -> { parent[rootY] = rootX; rank[rootX]++ }
        }
        return true
    }
}

/** Implementación del algoritmo de Kruskal */
object Kruskal {

    /**
     * Calcula el MST de las habitaciones.
     * Recibe una lista de habitaciones YA ORDENADAS (por Merge Sort).
     * Devuelve la lista de Corredores (pasillos) que forman el MST.
     */
    fun buildCorridors(rooms: List<Room>): List<Corridor> {
        if (rooms.size < 2) return emptyList()

        // PASO 1: Generar TODAS las aristas posibles con sus pesos (distancias)
        val edges = mutableListOf<Corridor>()
        for (i in rooms.indices) {
            for (j in i + 1 until rooms.size) {
                val weight = rooms[i].distanceTo(rooms[j])
                edges.add(Corridor(rooms[i], rooms[j], weight))
            }
        }

        // PASO 2: Ordenar las aristas por peso (distancia) de menor a mayor
        // Aquí podríamos usar nuestro Merge Sort, o el sort estándar
        edges.sortBy { it.weight }

        // PASO 3: Iterar aristas e ir agregando las que no formen ciclo
        val uf = UnionFind(rooms.size)
        // Map para encontrar índice de habitación por id
        val roomIndex = rooms.mapIndexed { idx, room -> room.id to idx }.toMap()

        val mst = mutableListOf<Corridor>()

        for (edge in edges) {
            val idxA = roomIndex[edge.roomA.id] ?: continue
            val idxB = roomIndex[edge.roomB.id] ?: continue

            if (uf.union(idxA, idxB)) {
                // No forma ciclo → agregar pasillo al MST
                mst.add(edge)

                // Optimización: si ya tenemos n-1 aristas, terminamos
                if (mst.size == rooms.size - 1) break
            }
        }

        return mst
    }
}

# 🏰 memory.md — Todo lo que necesitas saber sobre DungeonAlgorithm

> Este documento explica el juego completo: qué hace, por qué, cómo está organizado
> y dónde vive cada algoritmo. Léelo de arriba a abajo una vez y ya entiendes el 100%.

---

## 📁 Mapa de archivos del proyecto

```
app/src/main/java/com/example/dungeonalgorithm/
│
├── Room.kt             → Define qué es una "habitación"
├── MergeSort.kt        → ALGORITMO 1: ordena las habitaciones
├── Kruskal.kt          → ALGORITMO 2: conecta habitaciones con pasillos
├── Dijkstra.kt         → ALGORITMO 3: busca el camino más corto
├── DungeonGenerator.kt → Motor del juego (usa los 3 algoritmos)
└── MainActivity.kt     → Pantalla visual (lo que el usuario ve y toca)
```

---

## 🌊 Flujo del juego de inicio a fin

```
Usuario abre la app
        │
        ▼
MainActivity crea DungeonGenerator
        │
        ▼
dungeon.generate() ──────────────────────────────────────────┐
        │                                                     │
        ├─ 1. generateRooms()   → Crea habitaciones random   │
        │                                                     │
        ├─ 2. MergeSort         → Ordena por área            │
        │                                                     │  GENERACIÓN
        ├─ 3. carveRoom()       → Dibuja rooms en el grid    │  DEL MAPA
        │                                                     │
        ├─ 4. Kruskal           → Genera los pasillos        │
        │                                                     │
        ├─ 5. carveCorridor()   → Dibuja pasillos en grid    │
        │                                                     │
        └─ 6. Coloca jugador, salida y enemigo ───────────────┘
                │
                ▼
        Juego activo: el usuario presiona ▲▼◀▶
                │
                ▼
        onPlayerMove(dRow, dCol)
                │
                ├─ movePlayer() → mueve el jugador 1 celda
                │       └─ ¿llegó a EXIT? → ¡VICTORIA!
                │
                └─ moveEnemy() ← usa DIJKSTRA cada turno
                        └─ ¿alcanzó al jugador? → GAME OVER
```

---

## 📄 Archivo 1: `Room.kt` — La habitación

**¿Qué es?** Una data class simple. Guarda las medidas de cada cuarto del mapa.

```
Room(id=0, x=5, y=3, width=8, height=5)

Significa:
  - Número de habitación: 0
  - Empieza en columna 5, fila 3
  - Mide 8 columnas × 5 filas
  - Área = 8 × 5 = 40 celdas
```

**Funciones clave:**

| Función | Qué hace |
|---------|---------|
| `area()` | Devuelve `width × height`. Usada por Merge Sort para ordenar. |
| `centerX()` | Columna del centro de la habitación. Usado para dibujar pasillos. |
| `centerY()` | Fila del centro de la habitación. Usado para dibujar pasillos. |
| `distanceTo(other)` | Distancia Euclidiana entre centros. Usada por Kruskal como "peso" de la arista. |
| `overlaps(other)` | Si dos habitaciones se tocan o superponen. Usada en la generación para evitar solapamientos. |

---

## 📄 Archivo 2: `MergeSort.kt` — Algoritmo 1

**¿Para qué sirve en el juego?**
Ordena las habitaciones de **mayor a menor área** antes de conectarlas. Esto garantiza que:
- El jugador siempre empieza en la habitación más grande (más cómoda).
- Kruskal recibe la lista ordenada (ventaja para decidir prioridades).

**¿Cómo funciona paso a paso?**

```
Lista original (por área):
  Room0(área=15), Room1(área=40), Room2(área=8), Room3(área=25)

PASO 1 — Dividir hasta tener elementos sueltos:
  [Room0, Room1]      [Room2, Room3]
  [Room0] [Room1]     [Room2] [Room3]

PASO 2 — Fusionar (merge) comparando de a dos:
  [Room0] vs [Room1] → 40 > 15 → [Room1, Room0]
  [Room2] vs [Room3] → 25 > 8  → [Room3, Room2]

PASO 3 — Fusionar las dos mitades:
  [Room1(40), Room0(15)] vs [Room3(25), Room2(8)]
  40 > 25 → Room1
  25 > 15 → Room3
  15 > 8  → Room0
  Queda   → Room2

RESULTADO: [Room1(40), Room3(25), Room0(15), Room2(8)] ✅
```

**Funciones del archivo:**

| Función | Qué hace |
|---------|---------|
| `sortByArea(rooms)` | Punto de entrada. Llama a sí misma recursivamente (divide) y llama a `merge()` (fusiona). |
| `merge(left, right)` | Fusiona dos listas ya ordenadas comparando elemento a elemento. **Corazón del algoritmo.** |
| `sortByDistanceToOrigin(rooms)` | Variante alternativa: ordena por cercanía al punto (0,0). |

**Complejidad:**
- Tiempo: **O(n log n)** siempre (mejor, promedio y peor caso)
- Espacio: **O(n)** — necesita listas auxiliares temporales

---

## 📄 Archivo 3: `Kruskal.kt` — Algoritmo 2

**¿Para qué sirve en el juego?**
Una vez que las habitaciones están en el mapa, necesita conectarlas con pasillos. El objetivo es:
- Que **todas las habitaciones sean accesibles** (ninguna queda aislada).
- Usar la **menor cantidad de pasillos** posible (n-1 pasillos para n habitaciones).
- Que **no haya ciclos** innecesarios (el mapa se parece a un árbol, no a una red).

Esto se llama **Árbol de Expansión Mínima (MST — Minimum Spanning Tree)**.

**¿Cómo funciona paso a paso?**

```
Habitaciones: A, B, C, D (ya ordenadas por Merge Sort)

PASO 1 — Generar TODAS las conexiones posibles con sus distancias:
  A-B: 5     A-C: 12    A-D: 20
  B-C: 7     B-D: 15    C-D: 6

PASO 2 — Ordenar por distancia (de menor a mayor):
  A-B: 5    C-D: 6    B-C: 7    A-C: 12    B-D: 15    A-D: 20

PASO 3 — Recorrer en orden. Agregar si NO forma ciclo:
  A-B: 5  → ✅ Agregar (A y B no estaban conectados)
  C-D: 6  → ✅ Agregar (C y D no estaban conectados)
  B-C: 7  → ✅ Agregar (conecta el grupo AB con el grupo CD)
  A-C: 12 → ❌ SALTAR  (A y C ya están conectados via B)
  ...

RESULTADO MST: A-B, C-D, B-C  → 3 pasillos para 4 habitaciones (n-1) ✅
```

**La magia del Union-Find:**
Para detectar si agregar una arista formaría ciclo se usa **Union-Find** (también llamado Disjoint Set Union):

```
union(A, B) → A y B ahora en el mismo grupo → devuelve TRUE (se une)
union(C, D) → C y D ahora en el mismo grupo → devuelve TRUE (se une)
union(B, C) → une el grupo {A,B} con {C,D}  → devuelve TRUE (se une)
union(A, C) → A y C ya están en el mismo grupo → devuelve FALSE (ciclo!)
```

**Optimizaciones del Union-Find incluidas:**

1. **Path Compression** — Al buscar la raíz de un nodo, apunta directamente a la raíz todos los nodos del camino. Futuras búsquedas son casi instantáneas.
2. **Union by Rank** — Siempre pone el árbol más pequeño debajo del más grande. El árbol nunca se vuelve muy alto.

**Funciones del archivo:**

| Clase/Función | Qué hace |
|--------------|---------|
| `UnionFind(size)` | Crea la estructura para detectar ciclos. |
| `find(x)` | Devuelve la "raíz" del grupo al que pertenece x. Con Path Compression. |
| `union(x, y)` | Une los grupos de x e y. Devuelve `true` si se pudo (no había ciclo). |
| `Kruskal.buildCorridors(rooms)` | Punto de entrada. Genera aristas → ordena → aplica Union-Find → devuelve MST. |

**Complejidad:**
- Tiempo: **O(E log E)** donde E = número de aristas = n×(n-1)/2
- Con Union-Find optimizado: cada `find()`/`union()` ≈ **O(1)** amortizado

---

## 📄 Archivo 4: `Dijkstra.kt` — Algoritmo 3

**¿Para qué sirve en el juego?**
Tiene dos usos:
1. **IA del enemigo:** Cada vez que el jugador se mueve, el enemigo calcula la ruta más corta hacia el jugador y avanza un paso.
2. **Botón de ayuda 💡:** Muestra al jugador el camino óptimo hacia la salida (celdas resaltadas en azul).

**¿Cómo funciona paso a paso?**

```
Grid (0=libre, 1=pared). Enemigo en E, Jugador en P:

  # # # # # # #
  # E . . # . #
  # . # . . . #
  # . # . # P #
  # # # # # # #

Dijkstra desde E hacia P:

Inicio: dist[E] = 0, todo lo demás = ∞
Cola de prioridad (min-heap): [(0, E)]

Turno 1: Saco E (dist=0). Exploro vecinos libres:
  → abajo (1,1): dist=1  → agrego a cola
  → derecha (1,2): dist=1 → agrego a cola

Turno 2: Saco (1, abajo). Exploro vecinos:
  → abajo (2,1): dist=2  ...y así sigue

Resultado: Camino más corto encontrado
  E → derecha → derecha → abajo → derecha → abajo → P
  Total: 6 pasos
```

**¿Por qué PriorityQueue y no array normal?**

| Método | Complejidad | ¿Cuándo usar? |
|--------|-------------|---------------|
| Array lineal | O(V²) | Grafos pequeños o muy densos |
| **PriorityQueue (min-heap)** | **O((V+E) log V)** | **Grafos grandes o dispersos (como nuestro grid)** |

En un grid 2D de 35×22 = 770 nodos, con 4 vecinos cada uno, la PriorityQueue es mucho más rápida.

**Funciones del archivo:**

| Función | Qué hace |
|---------|---------|
| `findPath(grid, start, end)` | Calcula el camino completo más corto. Devuelve lista de celdas. |
| `reconstructPath(prev, start, end)` | Recorre la tabla de predecesores de atrás hacia adelante para armar el camino. |
| `nextStep(grid, from, to)` | Atajo: devuelve solo la SIGUIENTE celda del camino. El enemigo la usa para moverse 1 paso. |

**Tabla de predecesores (prev):**
Durante Dijkstra se llena una tabla que para cada celda guarda "¿desde qué celda llegué aquí?". Al terminar, se recorre al revés desde el destino hasta el origen para armar el camino completo.

---

## 📄 Archivo 5: `DungeonGenerator.kt` — El Motor

**¿Qué es?** La clase que lo orquesta todo. Es el "cerebro" del juego.

**Variables principales:**

| Variable | Qué guarda |
|----------|-----------|
| `grid` | El mapa completo: una matriz 2D de enteros (22 filas × 35 columnas) |
| `rooms` | Lista de habitaciones (ya ordenadas por Merge Sort) |
| `corridors` | Lista de pasillos generados por Kruskal |
| `playerPos` | Fila y columna actual del jugador |
| `exitPos` | Fila y columna de la salida 🔶 |
| `enemyPos` | Fila y columna del enemigo 🔴 |

**Valores del grid (cada celda es un número):**

| Número | Significado | Color en pantalla |
|--------|------------|------------------|
| `0` = FLOOR | Suelo transitable | Beige/arena |
| `1` = WALL | Pared (bloqueada) | Azul oscuro |
| `2` = PLAYER | Posición del jugador | Azul + círculo blanco |
| `3` = EXIT | Salida de la mazmorra | Naranja |
| `4` = ENEMY | Posición del enemigo | Rojo + círculo amarillo |

**Función `generate()` — La más importante:**

```kotlin
fun generate() {
    // 1. Todo el mapa empieza siendo PARED (1)
    grid = Array(rows) { IntArray(cols) { WALL } }

    // 2. Crear habitaciones aleatorias sin que se toquen
    val generatedRooms = generateRooms()

    // 3. MERGE SORT: ordenar de mayor a menor área
    rooms = MergeSort.sortByArea(generatedRooms)

    // 4. "Tallar" cada habitación en el grid (poner 0s)
    for (room in rooms) carveRoom(room)

    // 5. KRUSKAL: calcular qué habitaciones conectar
    corridors = Kruskal.buildCorridors(rooms)

    // 6. "Tallar" los pasillos en el grid (poner 0s en L)
    for (corridor in corridors) carveCorridor(corridor)

    // 7. Poner jugador en la habitación MÁS GRANDE (rooms[0])
    // 8. Poner salida en la habitación MÁS LEJANA al jugador
    // 9. Poner enemigo en la habitación del MEDIO
}
```

**Función `carveCorridor()` — Cómo se dibujan los pasillos:**

Los pasillos tienen forma de **L**: primero van horizontal desde la habitación A hasta la columna de B, luego van vertical hasta llegar a B.

```
Habitación A (centro en fila=5, col=3)
Habitación B (centro en fila=10, col=12)

Pasillo horizontal: desde col=3 hasta col=12 en fila=5
  . . . . . . . . . .   ← fila 5

Pasillo vertical: desde fila=5 hasta fila=10 en col=12
                  |      ← col 12
                  |
                  |
                  |
                  B
```

**Función `moveEnemy()` — IA en acción:**

```kotlin
fun moveEnemy(): Boolean {
    // 1. Crear copia del grid solo con WALL=1 y LIBRE=0
    //    (sin los marcadores de jugador/enemigo para que Dijkstra funcione limpio)
    val pathGrid = ...

    // 2. DIJKSTRA: ¿cuál es el SIGUIENTE PASO hacia el jugador?
    val next = Dijkstra.nextStep(pathGrid, enemyPos, playerPos)

    // 3. Mover el enemigo una celda
    enemyPos = next

    // 4. ¿El enemigo llegó a donde está el jugador?
    return enemyPos == playerPos  // true = GAME OVER
}
```

---

## 📄 Archivo 6: `MainActivity.kt` — La Pantalla

**¿Qué es?** Todo lo que el usuario ve y toca. Usa Jetpack Compose (el sistema de UI moderno de Android).

**Componentes visuales:**

| Composable | Qué muestra |
|-----------|------------|
| `DungeonGameScreen()` | Pantalla completa. Maneja el estado del juego. |
| `DungeonCanvas()` | Dibuja el mapa celda por celda con colores y círculos. |
| `DPadButton()` | Botón de dirección (▲▼◀▶). |
| `LegendItem()` | Un cuadradito de color con su etiqueta (leyenda). |

**Estados del juego (`gameState`):**

```
"playing" → el juego está activo, el D-Pad funciona
"won"     → el jugador llegó a la salida → texto verde
"lost"    → el enemigo atrapó al jugador → texto rojo
```

**Flujo de un turno (cuando el jugador presiona ▶):**

```
Usuario presiona ▶
        │
        ▼
onPlayerMove(dRow=0, dCol=1)
        │
        ├─ movePlayer(0, 1)
        │       ├─ Calcula nueva posición (misma fila, columna+1)
        │       ├─ ¿Es una pared? → NO hace nada, return false
        │       ├─ ¿Es la salida? → return true (VICTORIA)
        │       └─ Actualiza grid y playerPos
        │
        ├─ ¿won? → gameState = "won", terminar
        │
        ├─ moveEnemy()  ← DIJKSTRA aquí
        │       └─ ¿lost? → gameState = "lost"
        │
        └─ ¿showPath activo? → recalcular ruta Dijkstra jugador→salida
```

**Botón 💡 "Ver ruta (Dijkstra)":**
Llama a `dungeon.getPathToExit()` que ejecuta Dijkstra desde el jugador hasta la salida. Las celdas del camino se pasan a `DungeonCanvas` como `highlightPath` y se pintan de azul semitransparente.

---

## 🔄 Cómo los 3 algoritmos trabajan juntos

```
MERGE SORT          KRUSKAL             DIJKSTRA
─────────           ───────             ────────
Ordena rooms   →    Recibe rooms   →    Recibe el grid
por área            ordenadas           final con paredes
                    y genera            y pasillos
                    los pasillos        y encuentra rutas

Resultado:          Resultado:          Resultado:
Lista ordenada      n-1 pasillos        Camino más corto
                    sin ciclos          en tiempo real
```

---

## ⚡ Dónde mejorar el juego (para la exposición)

### Mejora 1 — Dificultad progresiva
En `DungeonGenerator.kt`, línea de creación del enemigo, agregar más enemigos según el nivel:
```kotlin
// Ahora: 1 enemigo fijo
// Mejorar: numEnemies = level * 2, cada uno con Dijkstra propio
```

### Mejora 2 — Terrenos con diferentes costos para Dijkstra
En `Dijkstra.kt`, cambiar `val newDist = dist[curr.row][curr.col] + 1` por:
```kotlin
val cost = when (grid[nr][nc]) {
    LAVA  -> 5   // Lava cuesta 5 pasos
    MUD   -> 3   // Barro cuesta 3 pasos
    else  -> 1   // Suelo normal = 1 paso
}
val newDist = dist[curr.row][curr.col] + cost
```

### Mejora 3 — Más pasillos (Kruskal extendido)
En `Kruskal.kt`, después del MST agregar algunas aristas extra para crear ciclos opcionales (hace el mapa más interesante):
```kotlin
// Después del MST, agregar 20% de aristas extras aleatoriamente
val extras = edges.drop(mst.size).shuffled().take(rooms.size / 5)
```

### Mejora 4 — Visualización paso a paso de Kruskal
Mostrar en pantalla cómo Kruskal va agregando pasillos de uno en uno (con delay entre cada uno). Es perfecto para la exposición.

---

## 📊 Tabla resumen de algoritmos

| Algoritmo | Dónde en el código | Cuándo se ejecuta | Complejidad |
|-----------|-------------------|-------------------|-------------|
| Merge Sort | `MergeSort.kt` → llamado en `DungeonGenerator.generate()` | Una vez al generar el mapa | O(n log n) |
| Kruskal + Union-Find | `Kruskal.kt` → llamado en `DungeonGenerator.generate()` | Una vez al generar el mapa | O(E log E) |
| Dijkstra | `Dijkstra.kt` → llamado en `moveEnemy()` y `getPathToExit()` | Cada turno del jugador | O(V log V) |

---

## 🗣️ Cómo explicarlo en la exposición (guión rápido)

> **"El juego tiene tres fases que usan los tres algoritmos que aprendimos."**
>
> **Fase 1 — Generación:**
> "Cuando el mapa se genera, primero creamos habitaciones al azar. Luego las **ordenamos con Merge Sort** de mayor a menor para saber cuál es la principal. Después **Kruskal** calcula el Árbol de Expansión Mínima — es decir, cómo conectar todas las habitaciones con la menor cantidad de pasillos posibles, garantizando que el mapa sea completamente navegable. Usamos Union-Find con Path Compression para detectar ciclos casi en tiempo constante."
>
> **Fase 2 — Juego en tiempo real:**
> "Cada vez que el jugador se mueve, el enemigo ejecuta **Dijkstra** para calcular la ruta más corta hacia el jugador. Usamos una Priority Queue para que sea eficiente. El botón de ayuda también usa Dijkstra pero desde el jugador hacia la salida."
>
> **Mejoras implementadas:**
> "En Merge Sort garantizamos O(n log n) siempre. En Kruskal aplicamos Path Compression y Union by Rank que hacen el Union-Find casi O(1). En Dijkstra usamos Early Exit para parar tan pronto lleguemos al destino."

---

## 📂 Ruta completa del proyecto

```
D:\ADA_DungeonAlgorithm_project-Johan\
  ADA_DungeonAlgorithm_project-Johan\     ← Abrir ESTA carpeta en Android Studio
    app\src\main\java\com\example\dungeonalgorithm\
      ├── Room.kt
      ├── MergeSort.kt
      ├── Kruskal.kt
      ├── Dijkstra.kt
      ├── DungeonGenerator.kt
      └── MainActivity.kt
```

> [!IMPORTANT]
> Al abrir en Android Studio, selecciona la carpeta **interior** `ADA_DungeonAlgorithm_project-Johan\ADA_DungeonAlgorithm_project-Johan` (la que contiene `build.gradle.kts`), no la carpeta exterior.

> [!TIP]
> Después del Gradle Sync, si hay error de compilación, asegúrate de que el `minSdk = 24` en `app/build.gradle.kts` y tu emulador tiene Android 7.0 o superior.

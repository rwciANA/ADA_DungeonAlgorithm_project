package com.example.dungeonalgorithm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dungeonalgorithm.ui.theme.DungeonAlgorithmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DungeonAlgorithmTheme {
                DungeonGameScreen()
            }
        }
    }
}

@Composable
fun DungeonGameScreen() {
    // Estado del juego
    var dungeon by remember { mutableStateOf(DungeonGenerator(cols = 35, rows = 22, numRooms = 7)) }
    var gameState by remember { mutableStateOf("playing") } // "playing", "won", "lost"
    var showPath by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("¡Encuentra la salida! 🗝️") }
    var highlightPath by remember { mutableStateOf(listOf<Cell>()) }

    // Generar dungeon inicial
    LaunchedEffect(Unit) { dungeon.generate() }

    fun resetGame() {
        dungeon = DungeonGenerator(cols = 35, rows = 22, numRooms = 7)
        dungeon.generate()
        gameState = "playing"
        showPath = false
        highlightPath = emptyList()
        statusMsg = "¡Encuentra la salida! 🗝️"
    }

    fun onPlayerMove(dRow: Int, dCol: Int) {
        if (gameState != "playing") return
        when (dungeon.movePlayer(dRow, dCol)) {
            DungeonGenerator.MoveResult.WON -> {
                gameState = "won"
                statusMsg = "¡VICTORIA! 🏆 Escapaste de la mazmorra"
                return
            }
            DungeonGenerator.MoveResult.CAUGHT -> {
                gameState = "lost"
                statusMsg = "GAME OVER 💀 Caminaste hacia el monstruo"
                return
            }
            DungeonGenerator.MoveResult.BLOCKED -> return  // chocar con una pared no gasta el turno
            DungeonGenerator.MoveResult.MOVED -> {}
        }
        val lost = dungeon.moveEnemy()
        if (lost) {
            gameState = "lost"
            statusMsg = "GAME OVER 💀 El monstruo te atrapó"
        }
        if (showPath) highlightPath = dungeon.getPathToExit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text(
            "🏰 Dungeon Algorithm",
            color = Color(0xFFE0C080),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        // Mensaje de estado
        Text(
            statusMsg,
            color = when (gameState) {
                "won" -> Color(0xFF80FF80)
                "lost" -> Color(0xFFFF6060)
                else -> Color(0xFFB0B0D0)
            },
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        // === CANVAS DEL MAPA ===
        DungeonCanvas(
            dungeon = dungeon,
            highlightPath = highlightPath,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // === LEYENDA ===
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(Color(0xFF4040C0), "Pared")
            LegendItem(Color(0xFFD0C090), "Suelo")
            LegendItem(Color(0xFF40A0FF), "Jugador")
            LegendItem(Color(0xFFFF8000), "Salida")
            LegendItem(Color(0xFFFF3030), "Enemigo")
        }

        Spacer(Modifier.height(10.dp))

        // === CONTROLES DE MOVIMIENTO (D-PAD) ===
        if (gameState == "playing") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DPadButton("▲") { onPlayerMove(-1, 0) }
                Row {
                    DPadButton("◀") { onPlayerMove(0, -1) }
                    Spacer(Modifier.width(44.dp))
                    DPadButton("▶") { onPlayerMove(0, 1) }
                }
                DPadButton("▼") { onPlayerMove(1, 0) }
            }
        }

        Spacer(Modifier.height(8.dp))

        // === BOTONES INFERIORES ===
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (gameState != "playing") {
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A7C59))
                ) { Text("🔄 Nuevo Juego") }
            } else {
                // Botón ayuda (Dijkstra visual)
                Button(
                    onClick = {
                        showPath = !showPath
                        highlightPath = if (showPath) dungeon.getPathToExit() else emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showPath) Color(0xFF8B4513) else Color(0xFF5050A0)
                    )
                ) { Text(if (showPath) "🔵 Ocultar ruta" else "💡 Ver ruta (Dijkstra)") }

                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A3A3A))
                ) { Text("🔄 Nuevo") }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DungeonCanvas(
    dungeon: DungeonGenerator,
    highlightPath: List<Cell>,
    modifier: Modifier = Modifier
) {
    val pathSet = remember(highlightPath) { highlightPath.toSet() }

    Canvas(modifier = modifier) {
        val cellW = size.width / dungeon.cols
        val cellH = size.height / dungeon.rows

        for (r in 0 until dungeon.rows) {
            for (c in 0 until dungeon.cols) {
                val cell = Cell(r, c)
                val color = when {
                    cell in pathSet -> Color(0xFF60A0FF).copy(alpha = 0.5f) // Camino Dijkstra
                    dungeon.grid[r][c] == DungeonGenerator.WALL   -> Color(0xFF2A2A5A)
                    dungeon.grid[r][c] == DungeonGenerator.PLAYER -> Color(0xFF40A0FF)
                    dungeon.grid[r][c] == DungeonGenerator.EXIT   -> Color(0xFFFF8000)
                    dungeon.grid[r][c] == DungeonGenerator.ENEMY  -> Color(0xFFFF3030)
                    dungeon.grid[r][c] == DungeonGenerator.FLOOR  -> Color(0xFFBBB090)
                    else -> Color(0xFFBBB090)
                }

                drawRect(
                    color = color,
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW - 1f, cellH - 1f)
                )

                // Iconos para jugador, enemigo y salida
                when (dungeon.grid[r][c]) {
                    DungeonGenerator.PLAYER -> drawCircle(
                        Color(0xFFFFFFFF),
                        radius = minOf(cellW, cellH) * 0.3f,
                        center = Offset(c * cellW + cellW / 2, r * cellH + cellH / 2)
                    )
                    DungeonGenerator.ENEMY -> drawCircle(
                        Color(0xFFFFFF00),
                        radius = minOf(cellW, cellH) * 0.3f,
                        center = Offset(c * cellW + cellW / 2, r * cellH + cellH / 2)
                    )
                }
            }
        }
    }
}

@Composable
fun DPadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4A6A))
    ) {
        Text(label, fontSize = 18.sp, color = Color.White)
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(Modifier.width(3.dp))
        Text(label, color = Color(0xFFB0B0B0), fontSize = 10.sp)
    }
}

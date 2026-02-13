package com.example.trickleprototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

// ✅ Smooth fade between colors (same order, loops cleanly)
// ✅ Randomized ONCE per app run (stable)
@Composable
private fun CyclingFadingColorTitle() {
    val colors = remember {
        listOf(Color.Red, Color.White, Color.Yellow, Color.Black, Color.Blue)
    }

    var idx by remember { mutableIntStateOf(0) }

    // Random ONCE per app run (stable)
    val holdMs = remember { Random.nextLong(1333L, 13333L) }          // pause on each color
    val fadeMs = remember { Random.nextLong(1333L, 13333L).toInt() }  // fade duration

    LaunchedEffect(Unit) {
        while (true) {
            delay(holdMs)
            idx = (idx + 1) % colors.size
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = colors[idx],
        animationSpec = tween(durationMillis = fadeMs, easing = LinearEasing),
        label = "titleColorFade"
    )

    Text(
        text = "TR1CKL3",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        color = animatedColor
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TrickleApp() } }
    }
}

@Composable
private fun TrickleApp() {
    val engine = remember { GameEngine() }

    // ✅ safe context for SharedPreferences / persistence
    val appContext = LocalContext.current.applicationContext
    val statsStore = remember { StatsStore(appContext) }
    LaunchedEffect(Unit) {
        engine.attachStatsStore(statsStore)
    }

    var difficulty by remember { mutableStateOf<Difficulty?>(null) }

    var choice by remember { mutableIntStateOf(1) }
    var targetId by remember { mutableStateOf<Int?>(null) }
    var guess by remember { mutableIntStateOf(1) }

    var lastResult by remember { mutableStateOf<RoundResult?>(null) }
    var logText by remember { mutableStateOf("") }

    var showHowToPlay by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    // locks (prevents “mid-submit changing mind”)
    var humanActionLocked by remember { mutableStateOf(false) }
    var startLocked by remember { mutableStateOf(false) }

    val phase = lastResult?.phase ?: engine.getPhase()
    val players = lastResult?.players ?: engine.getPlayersSnapshot()
    val gameOver = (phase == EnginePhase.GAME_OVER)

    val scrollState = rememberScrollState()

    // keep log pinned to top (newest at top)
    LaunchedEffect(logText) {
        scrollState.scrollTo(0)
    }

    // BOT stepping (1 sec default, 0.3 sec for passes)
    LaunchedEffect(phase) {
        if (phase == EnginePhase.BOT_TURN) {
            while (true) {
                val current = lastResult?.phase ?: engine.getPhase()
                if (current != EnginePhase.BOT_TURN) break

                val result = engine.step()
                lastResult = result
                logText = buildLogText(result)

                if (result.phase == EnginePhase.BOT_TURN) {
                    val ms = if (result.lastEventKind == LogEventKind.PASS) 300L else 1000L
                    delay(ms)
                }
            }
        }

        if (phase != EnginePhase.PLAYER_TURN) humanActionLocked = false
        if (phase != EnginePhase.SELECT && phase != EnginePhase.ROUND_END) startLocked = false
    }

    // dialogs
    if (showHowToPlay) {
        SimpleDialog(title = "HOW TO PLAY", onClose = { showHowToPlay = false }) {
            HowToPlayText()
        }
    }
    if (showTips) {
        SimpleDialog(title = "ADVANCED TIPS", onClose = { showTips = false }) {
            AdvancedTipsText() // ✅ General tips first, Hat rule after
        }
    }
    if (showStats) {
        SimpleDialog(title = "STATS", onClose = { showStats = false }) {
            StatsText(statsStore.load())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Title with smooth fade between colors
        CyclingFadingColorTitle()

        Spacer(Modifier.height(10.dp))

        // -------------------------
        // MAIN MENU (difficulty null)
        // -------------------------
        if (difficulty == null) {
            DifficultyDropdownNullable(
                selected = null,
                onSelect = {
                    difficulty = it
                    engine.setDifficulty(it)

                    val snap = engineSnapshot(engine)
                    lastResult = snap
                    logText = buildLogText(snap)

                    humanActionLocked = false
                    startLocked = false

                    choice = 1
                    targetId = null
                    guess = 1
                }
            )

            Spacer(Modifier.height(8.dp))
            Text("(Pick a difficulty to begin.)", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(10.dp))

            Text(
                "HOW TO PLAY",
                modifier = Modifier.clickable { showHowToPlay = true },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "ADVANCED TIPS",
                modifier = Modifier.clickable { showTips = true },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "STATS",
                modifier = Modifier.clickable { showStats = true },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            return@Column
        }

        // -------------------------
        // GAME SCREEN
        // -------------------------
        val playerScore = players.firstOrNull { it.id == GameEngine.HUMAN_ID }?.marbles ?: 0
        Text("Player score: $playerScore", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))

        val isPlayerTurn = (phase == EnginePhase.PLAYER_TURN)
        val inputsEnabled = !gameOver && isPlayerTurn && !humanActionLocked

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {

                Text("Player chooses:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dieEnabled = !gameOver && !startLocked
                    SmallChoiceButton("0", selected = (choice == 0), enabled = dieEnabled) { choice = 0 }
                    SmallChoiceButton("1", selected = (choice == 1), enabled = dieEnabled) { choice = 1 }
                    SmallChoiceButton("3", selected = (choice == 3), enabled = dieEnabled) { choice = 3 }
                }

                Spacer(Modifier.height(10.dp))

                val isPassing = (targetId == null)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { targetId = null },
                        enabled = inputsEnabled && !isPassing
                    ) { Text("Pass") }

                    Button(
                        onClick = {
                            val firstTarget = players.firstOrNull { it.id != GameEngine.HUMAN_ID }?.id
                            targetId = firstTarget
                        },
                        enabled = inputsEnabled && isPassing
                    ) { Text("Target") }
                }

                Spacer(Modifier.height(8.dp))

                val dropdownOptions = if (isPlayerTurn) {
                    val targetable = lastResult?.targetableIdsForHuman ?: emptyList()
                    players.filter { it.id in targetable && it.id != GameEngine.HUMAN_ID }
                } else emptyList()

                TargetDropdown(
                    options = dropdownOptions,
                    selectedTargetId = targetId,
                    enabled = inputsEnabled && targetId != null && dropdownOptions.isNotEmpty(),
                    onSelect = { targetId = it }
                )

                Spacer(Modifier.height(10.dp))

                Text("Player guesses:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallChoiceButton("1", selected = (guess == 1), enabled = inputsEnabled) { guess = 1 }
                    SmallChoiceButton("3", selected = (guess == 3), enabled = inputsEnabled) { guess = 3 }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (phase) {
                        EnginePhase.SELECT, EnginePhase.ROUND_END -> {
                            Button(
                                enabled = !gameOver && !startLocked,
                                onClick = {
                                    startLocked = true
                                    val result = engine.startRound(choice)
                                    lastResult = result
                                    logText = buildLogText(result)
                                    targetId = null
                                }
                            ) { Text("Start Round") }
                        }

                        EnginePhase.PLAYER_TURN -> {
                            Button(
                                enabled = !gameOver && !humanActionLocked,
                                onClick = {
                                    humanActionLocked = true
                                    val frozenTarget = targetId
                                    val frozenGuess = if (frozenTarget == null) null else guess

                                    val result = engine.submitHumanTurn(
                                        targetId = frozenTarget,
                                        guess = frozenGuess
                                    )
                                    lastResult = result
                                    logText = buildLogText(result)

                                    if (result.phase == EnginePhase.PLAYER_TURN) {
                                        humanActionLocked = false
                                    } else {
                                        targetId = null
                                    }
                                }
                            ) { Text("Submit Turn") }
                        }

                        EnginePhase.BOT_TURN -> {
                            Button(enabled = false, onClick = {}) { Text("Bots Acting…") }
                        }

                        EnginePhase.GAME_OVER -> {
                            Button(enabled = false, onClick = {}) { Text("Game Over") }
                        }

                        EnginePhase.SETUP -> {
                            Button(enabled = false, onClick = {}) { Text("Setup…") }
                        }
                    }

                    Button(
                        onClick = {
                            engine.reset()
                            difficulty = null
                            lastResult = null
                            logText = ""

                            choice = 1
                            targetId = null
                            guess = 1

                            humanActionLocked = false
                            startLocked = false
                        }
                    ) { Text(if (gameOver) "New Game" else "Main Menu") }
                }
            }

            if (difficulty == Difficulty.EASY) {
                MarblesBox(players = players, modifier = Modifier.widthIn(min = 140.dp, max = 200.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            val hScroll = rememberScrollState()

            Text(
                text = logText.ifBlank { "(log will appear here)" },
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                modifier = Modifier.horizontalScroll(hScroll)
            )
        }
    }
}

private fun engineSnapshot(engine: GameEngine): RoundResult {
    val players = engine.getPlayersSnapshot()
    return RoundResult(
        phase = engine.getPhase(),
        log = emptyList(),
        players = players,
        winnerIds = emptyList(),
        bannerText = null,
        targetableIdsForHuman = emptyList(),
        currentActorId = null,
        lastEventKind = null
    )
}

private fun buildLogText(result: RoundResult): String {
    return result.log.asReversed().joinToString("\n") { it.text }.ifBlank { "" }
}

// -------------------- Dialog + Text Blocks --------------------

@Composable
private fun SimpleDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = { content() },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        }
    )
}

@Composable
private fun HowToPlayText() {
    Text(
        "Pick a die (0/1/3), then take turns.\n\n" +
                "On your turn you may PASS or TARGET someone and guess 1 or 3.\n" +
                "Correct guess = you gain that many marbles.\n" +
                "Wrong guess:\n" +
                "• If they had 1 or 3: they gain that many.\n" +
                "• If they had 0: you lose 1 and the Hat may move.\n\n" +
                "Anyone not targeted gains their die value at the end of the round.\n" +
                "First to 13+ wins."
    )
}

// ✅ General tips first, hat rules after (as requested)
@Composable
private fun AdvancedTipsText() {
    Text(
        "GENERAL TIPS:\n" +
                "• Passing keeps you from making enemies.\n" +
                "• Targeting exposes info — but paints a target on you.\n" +
                "• If you suspect a Spite-style bot, avoid poking them unless you're ready for heat.\n" +
                "• 0 is the landmine: watch who gets ‘safe’ trickles and who gets punished.\n" +
                "• If you’re ahead, sometimes the best move is to pass and let others fight.\n\n" +
                "HAT RULE (0 trap):\n" +
                "If you guess 1 or 3 on someone who actually chose 0, you lose 1 marble and you take the Jester Hat.\n" +
                "Next round, the Hat-holder starts — BUT only if the Hat ended the round on a different person than it started.\n" +
                "Boomerang edge case: if someone starts with the Hat, loses it, then gets it back that same round, the Hat does NOT override the next starter."
    )
}

@Composable
private fun StatsText(stats: PlayerStats) {
    val acc = if (stats.totalGuesses == 0) "—"
    else "${((stats.correctGuesses * 100.0) / stats.totalGuesses).toInt()}%"

    Text(
        "Total games: ${stats.totalGames}\n" +
                "Wins: ${stats.totalWins}\n\n" +
                "Total marbles gained (end-of-game totals): ${stats.totalMarblesAcrossGames}\n\n" +
                "Accuracy: $acc (${stats.correctGuesses}/${stats.totalGuesses})\n" +
                "Tricked by 0: ${stats.timesTrickedByZero}\n" +
                "Perfect games: ${stats.perfectGames}\n\n" +
                "Easy games: ${stats.easyGames} (wins ${stats.easyWins})\n" +
                "Normal games: ${stats.normalGames} (wins ${stats.normalWins})\n" +
                "Hard games: ${stats.hardGames} (wins ${stats.hardWins})"
    )
}

// -------------------- UI components --------------------

@Composable
private fun MarblesBox(players: List<PlayerState>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Marbles:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        players.forEach { p ->
            Text("${p.baseName}: ${p.marbles}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SmallChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) { Text(label) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetDropdown(
    options: List<PlayerState>,
    selectedTargetId: Int?,
    enabled: Boolean,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedTargetId }?.baseName ?: "—"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Player targets") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .widthIn(min = 160.dp, max = 220.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.baseName) },
                    onClick = {
                        onSelect(p.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyDropdownNullable(
    selected: Difficulty?,
    onSelect: (Difficulty) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val label = when (selected) {
        Difficulty.EASY -> "Easy (show information)"
        Difficulty.NORMAL -> "Normal (hide information)"
        Difficulty.HARD -> "Hard (bots block your win)"
        null -> "Select difficulty…"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Difficulty") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Easy (show information)") },
                onClick = { onSelect(Difficulty.EASY); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Normal (hide information)") },
                onClick = { onSelect(Difficulty.NORMAL); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Hard (bots block your win)") },
                onClick = { onSelect(Difficulty.HARD); expanded = false }
            )
        }
    }
}

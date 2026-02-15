package com.example.trickleprototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import kotlin.math.roundToInt
import kotlin.random.Random
import com.example.trickleprototype.ui.theme.TricklePrototypeTheme


//  Smooth fade between colors (same order, loops cleanly)
//  Randomized ONCE per app run (stable)
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

        // IMPORTANT: Use your app theme here so system dark mode works.
        // If your theme function name differs, adjust it.
        setContent {
            TricklePrototypeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrickleApp()
                }
            }
        }
    }
}

@Composable
private fun TrickleApp() {
    val engine = remember { GameEngine() }

    //  safe context for SharedPreferences / persistence
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
    var showArchetypes by remember { mutableStateOf(false) }
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
            AdvancedTipsText()
        }
    }
    if (showArchetypes) {
        SimpleDialog(title = "ARCHETYPES", onClose = { showArchetypes = false }) {
            ArchetypesText()
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
                "ARCHETYPES",
                modifier = Modifier.clickable { showArchetypes = true },
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
        Text("Your marbles: $playerScore", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))

        val isPlayerTurn = (phase == EnginePhase.PLAYER_TURN)
        val inputsEnabled = !gameOver && isPlayerTurn && !humanActionLocked

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {

                Text("Your choice:", style = MaterialTheme.typography.bodyMedium)
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

                Text("Your guess:", style = MaterialTheme.typography.bodyMedium)
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

        // Log
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = logText.ifBlank { "(log will appear here)" },
                fontFamily = FontFamily.Monospace,
                softWrap = true
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
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)   // <- keeps the dialog sane
                    .verticalScroll(scroll)    // <- lets long stats scroll
                    .padding(end = 6.dp)       // <- avoids scroll clipping
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        }
    )
}

@Composable
private fun HowToPlayText() {
    Text(
        "Pick a number(0/1/3), then wait for your turn. On your turn, you may PASS(do nothing) or TARGET someone and guess their number.\n\n" +
                "If you're correct = you gain that many marbles.\n\n" +
                "If you're wrong...\n" +
                "...and they chose 1 or 3: they gain that many marbles.\n" +
                "...and they chose 0: you lose 1 marble and take the Jester's Hat.\n\n" +
                "Anyone who wasn't targeted gains marbles equal to their choice at the end of the round.\n\n" +
                "First to 13+ marbles wins."
    )
}

@Composable
private fun AdvancedTipsText() {
    Text(
        "GENERAL TIPS:\n" +
                "• Choosing (3) gains marbles fast, but makes you easy to steal from.\n" +
                "• Targeting is a great way to gain extra marbles, and enemies!\n" +
                "• If you're repeatedly targeted, choose a (0) to fight back.\n" +
                "• At the start of the game, a player is randomly chosen to go first, and normally each round begins with the next player on the list.\n\n" +
                "BOTS & ARCHETYPES:\n" +
                "• Each game, bots are randomly assigned an Archetype.\n" +
                "• On Easy mode, bots have their names replaced with their Archetype (and you can see their score totals).\n" +
                "• On Normal mode, bots have their names and scores hidden.\n" +
                "• On Hard mode, bots will also gang up on you as the finish line approaches.\n\n" +
                "JESTER'S HAT RULE:\n" +
                "• If you guess 1 or 3 on someone who actually chose 0, you lose 1 marble and take the Jester's Hat.\n" +
                "• Next round, the Hat-holder goes first — BUT only if the Hat ended the round on a different person than it started.\n"
    )
}

@Composable
private fun ArchetypesText() {
    Text(
        "Accretion:\n" +
                "• Starts slow, but ramps up fast\n" +
                "• Chooses: 1, 1, 3, 3, 3...\n" +
                "• Targeting Behavior: Passes until the very last moment\n\n" +

                "Avenger:\n" +
                "• Retaliation engine. Goes after attackers/agitators.\n" +
                "• Chooses: 1 or 3 (0 only if attacked).\n" +
                "• Targeting Behavior: Passes round 1; targets from round 2.\n\n" +

                "Auditor:\n" +
                "• Hates coasters. Targets anyone who passes too much.\n" +
                "• Chooses: 1 or 3.\n" +
                "• Targeting Behavior: Passes rounds 1–2; targets pass-streak players after.\n\n" +

                "Chaos Grandma:\n" +
                "• Pure chaos. No grudges, no fear.\n" +
                "• Chooses: Random (0/1/3 evenly).\n" +
                "• Targeting Behavior: 50/50 pass vs target.\n\n" +

                "Hat Farmer:\n" +
                "• Hunts chaos: targets players who were involved in guesses last round.\n" +
                "• Chooses: Baseline behavior.\n" +
                "• Targeting Behavior: Passes first; then targets “recently guessed” players.\n\n" +

                "Juliet (Colluder):\n" +
                "• Baseline bot logic, but never targets Romeo.\n" +
                "• Chooses: Baseline behavior.\n" +
                "• Targeting Behavior: Baseline behavior (avoids partner).\n\n" +

                "Kingmaker:\n" +
                "• Picks a ‘king’ and only attacks people who attack that king.\n" +
                "• Chooses: 50/50 between 1 and 3.\n" +
                "• Targeting Behavior: Usually passes; targets only to defend king.\n\n" +

                "Limper:\n" +
                "• Defensive turtle. Refuses to engage.\n" +
                "• Chooses: 1 (0 if attacked and defending).\n" +
                "• Targeting Behavior: Always passes.\n\n" +

                "Opportunist:\n" +
                "• Waits, watches, then punishes repeated 3 behavior.\n" +
                "• Chooses: 1 (unless close to winning).\n" +
                "• Targeting Behavior: Passes for 3 rounds, then targets repeat-3 players.\n\n" +

                "Pacifist Collector:\n" +
                "• Greedy but peaceful. Tries to win by trickle alone.\n" +
                "• Chooses: 3.\n" +
                "• Targeting Behavior: Always passes.\n" +

                "Romeo (Colluder):\n" +
                "• Baseline bot logic, but never targets Juliet.\n" +
                "• Chooses: Baseline behavior.\n" +
                "• Targeting Behavior: Baseline behavior (avoids partner).\n\n" +

                "Scout:\n" +
                "• Always hunting for info. Locks onto last-round 3s.\n" +
                "• Chooses: 1 or 3.\n" +
                "• Targeting Behavior: Always targets.\n\n" +

                "Spite Player:\n" +
                "• Quiet until provoked. Then it tunnels one person forever.\n" +
                "• Chooses: 1 or 3.\n" +
                "• Targeting Behavior: Passes until attacked; then focuses the first attacker.\n\n" +

                "Strobe:\n" +
                "• Alternates like a metronome and attacks in a repeating rhythm.\n" +
                "• Chooses: Alternates 1,3,1,3...\n" +
                "• Targeting Behavior: Alternates pass/target with pattern-based guesses.\n\n" +

                "Teacher:\n" +
                "• Lets others learn the game, then shows them how to lose.\n" +
                "• Chooses: 1, 1, 1, 3, 3...\n" +
                "• Targeting Behavior: Passes for 3 rounds, then targets known 3-choosers.\n\n" +

                "Three-Pusher:\n" +
                "• Greedy hammer. Loves 3 and assumes everyone else does too.\n" +
                "• Chooses: 3.\n" +
                "• Targeting Behavior: Targets from round 2 onward, always guessing 3.\n\n"
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
                "Hard games: ${stats.hardGames} (wins ${stats.hardWins})\n\n"
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
        Difficulty.EASY -> "Easy (show archetypes and scores)"
        Difficulty.NORMAL -> "Normal (hides archetypes and scores)"
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
                text = { Text("Easy (show archetypes and scores)") },
                onClick = { onSelect(Difficulty.EASY); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Normal (hide archetypes and scores)") },
                onClick = { onSelect(Difficulty.NORMAL); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Hard (bots block your win)") },
                onClick = { onSelect(Difficulty.HARD); expanded = false }
            )
        }
    }
}

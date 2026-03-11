package com.example.trickleprototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.example.trickleprototype.ui.theme.TricklePrototypeTheme
import kotlinx.coroutines.delay
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            TricklePrototypeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    TrickleApp()
                }
            }
        }
    }
}

private data class AchievementPopup(
    val title: String,
    val desc: String
)

private enum class AppScreen {
    SPLASH,
    MAIN_MENU,
    PLAY,
    RULES,
    PROFILE,
    CUSTOMIZE,
    SETTINGS,
    GAME
}

private fun parseAchievementPopup(line: String): AchievementPopup? {
    val marker = "Achievement Unlocked:"
    val idx = line.indexOf(marker)
    if (idx == -1) return null

    // Take everything after the marker
    val payload = line.substring(idx + marker.length)
        .trim()
        .trim('*')
        .trim()

    // Split into title + desc
    val parts = payload.split(" - ", limit = 2)

    val rawTitle = parts.getOrNull(0) ?: return null
    val rawDesc  = parts.getOrNull(1) ?: ""

    val title = rawTitle.trim().trim('*').trim()
    val desc  = rawDesc.trim().trim('*').trim()

    if (title.isBlank()) return null

    return AchievementPopup(title = title, desc = desc)
}

@Composable
private fun TrickleApp() {
    val engine = remember { GameEngine() }

    val appContext = LocalContext.current.applicationContext
    val statsStore = remember { StatsStore(appContext) }

    var screen by remember { mutableStateOf(AppScreen.SPLASH) }

    var showResetStatsConfirm by remember { mutableStateOf(false) }

    // Player identity
    var playerName by remember { mutableStateOf(statsStore.getPlayerName()) }

    // Keep engine in sync with saved name
    SideEffect {
        engine.setHumanName(playerName)
    }

    // ✅ FIX: attach synchronously (no async race)
    SideEffect {
        engine.attachStatsStore(statsStore)
    }

    var difficulty by remember { mutableStateOf<Difficulty?>(null) }

    var choice by remember { mutableIntStateOf(1) }
    var targetId by remember { mutableStateOf<Int?>(null) }
    var guess by remember { mutableIntStateOf(3) }

    var dieButtonsEnabled by remember { mutableStateOf(true) }

    var lastResult by remember { mutableStateOf<RoundResult?>(null) }
    var logText by remember { mutableStateOf("") }

    // Achievement popups
    var achievementQueue by remember { mutableStateOf<List<AchievementPopup>>(emptyList()) }
    var activeAchievement by remember { mutableStateOf<AchievementPopup?>(null) }

    // must be above LaunchedEffect or it won't exist yet
    val seenAchievements = remember { mutableSetOf<String>() }

    LaunchedEffect(lastResult) {
        val result = lastResult ?: return@LaunchedEffect

        val newlyParsed = result.log
            .mapNotNull { parseAchievementPopup(it.text) }
            .filter { popup ->
                // Use title as the de-dupe key (simple + works with your current strings)
                if (seenAchievements.contains(popup.title)) false
                else {
                    seenAchievements.add(popup.title)
                    true
                }
            }

        if (newlyParsed.isNotEmpty()) {
            // Append to queue
            achievementQueue = achievementQueue + newlyParsed

            // If nothing is showing, show the next one immediately
            if (activeAchievement == null) {
                activeAchievement = achievementQueue.firstOrNull()
                achievementQueue = achievementQueue.drop(1)
            }
        }
    }

    var showHowToPlay by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf(false) }
    var showArchetypes by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }

    // locks
    var humanActionLocked by remember { mutableStateOf(false) }
    var startLocked by remember { mutableStateOf(false) }

    // TURBO
    var turbo by remember { mutableStateOf(false) }

    // Picked only when switching ON
    var turboOnColor by remember { mutableStateOf(Color(0xFFFFFFFF)) } // white
    val turboPalette = remember {
        listOf(
            Color(0xFFC5DAFF), // light blue
            Color(0xFF159DF8), // blue
            Color(0xFF0D47A1)  // dark blue
        )
    }

    // increments every time we return to main menu
    var menuVisitKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(difficulty) {
        if (difficulty == null) menuVisitKey += 1
    }

    val phase = lastResult?.phase ?: engine.getPhase()
    val players = lastResult?.players ?: engine.getPlayersSnapshot()
    val gameOver = (phase == EnginePhase.GAME_OVER)

    val scrollState = rememberScrollState()
    LaunchedEffect(logText) { scrollState.scrollTo(0) }

    // BOT stepping with turbo
    LaunchedEffect(phase, turbo) {
        if (phase == EnginePhase.BOT_TURN) {
            while (true) {
                val current = lastResult?.phase ?: engine.getPhase()
                if (current != EnginePhase.BOT_TURN) break

                val result = engine.step()
                lastResult = result
                logText = buildLogText(result)

                if (result.phase == EnginePhase.BOT_TURN) {
                    val base = if (result.lastEventKind == LogEventKind.PASS) 300L else 1000L
                    val ms = if (turbo) (base / 4).coerceAtLeast(35L) else base
                    delay(ms)
                }
            }
        }

        if (phase != EnginePhase.PLAYER_TURN) humanActionLocked = false
        if (phase != EnginePhase.SELECT && phase != EnginePhase.ROUND_END) startLocked = false
    }

    // dialogs
    if (showHowToPlay) {
        SimpleDialog(
            title = "HOW TO PLAY",
            onClose = { showHowToPlay = false },
            accentColor = Color(0xFFFFFFFF)
        ) { HowToPlayText() }
    }

    if (showTips) {
        SimpleDialog(
            title = "ADVANCED TIPS",
            onClose = { showTips = false },
            accentColor = Color(0xFF808080)
        ) { AdvancedTipsText() }
    }

    if (showArchetypes) {
        SimpleDialog(
            title = "ARCHETYPES",
            onClose = { showArchetypes = false },
            accentColor = Color(0xFF404040)
        ) { ArchetypesText() }
    }

    if (showStats) {
        SimpleDialog(
            title = "PLAYER STATS",
            onClose = { showStats = false },
            accentColor = Color(0xFF007AFF)
        ) { StatsText(statsStore.load()) }
    }

    if (showAchievements) {
        SimpleDialog(
            title = "ACHIEVEMENTS",
            onClose = { showAchievements = false },
            accentColor = Color(0xFF0ADAFF)
        ) { AchievementsText(statsStore.load()) }
    }

    // ✅ FIX 1: Use a full-screen Box so the background can sit behind ALL screens.
    Box(modifier = Modifier.fillMaxSize()) {

        // ✅ FIX 1 (continued): Draw the background image on every screen EXCEPT the splash.
        if (screen != AppScreen.SPLASH) {
            Image(
                painter = painterResource(R.drawable.mainmenugen),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Optional readability overlay (keeps your UI legible on bright areas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        // ✅ FIX 2: Keep consistent safe padding for status + nav bars so buttons don't cramp.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (screen != AppScreen.SPLASH) {
                // Header: title truly centered, turbo pinned right
                Box(modifier = Modifier.fillMaxWidth()) {

                    // Main Menu moved here (top-left) to avoid accidental taps near the bottom controls
                    if (difficulty != null) {
                        Button(
                            onClick = {
                                engine.reset()
                                engine.attachStatsStore(statsStore)

                                difficulty = null
                                screen = AppScreen.MAIN_MENU
                                lastResult = null
                                logText = ""

                                choice = 1
                                targetId = null
                                guess = 3

                                humanActionLocked = false
                                startLocked = false
                            },
                            modifier = Modifier.align(Alignment.CenterStart),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A6A6A),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = if (gameOver) "New Game" else "Main Menu", maxLines = 1)
                        }
                    }

                    val turboContainerColor = if (turbo) turboOnColor else Color(0xFF6A6A6A)
                    val turboContentColor = if (turbo && turboOnColor == Color.Yellow) Color.Black else Color.White

                    if (screen == AppScreen.GAME) {
                        Button(
                            onClick = {
                                // Only re-roll color when turning ON
                                if (!turbo) {
                                    turboOnColor = turboPalette[Random.nextInt(turboPalette.size)]
                                }
                                turbo = !turbo
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(40.dp)
                                .widthIn(min = 130.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = turboContainerColor,
                                contentColor = turboContentColor
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (turbo) "TURBO: ON" else "TURBO: OFF",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // MENUS
            when (screen) {
                AppScreen.SPLASH -> {
                    LaunchedEffect(Unit) {
                        delay(3000L)
                        screen = AppScreen.MAIN_MENU
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)              // true black full-screen background
                            .clickable { screen = AppScreen.MAIN_MENU }, // tap anywhere
                        contentAlignment = Alignment.Center      // centers the whole splash
                    ) {
                        Column(
                            modifier = Modifier.wrapContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val logoBitmap: ImageBitmap =
                                ImageBitmap.imageResource(id = R.drawable.darksoft_logo)

                            Image(
                                painter = BitmapPainter(image = logoBitmap, filterQuality = FilterQuality.None),
                                contentDescription = "DarkSoft logo",
                                modifier = Modifier
                                    .size(140.dp)
                                    .padding(),
                                contentScale = ContentScale.Fit
                            )

                            Text(
                                "Darksoft Game Studios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    return@Column
                }

                AppScreen.MAIN_MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),   // increase/decrease to taste
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuLinkButton(text = "PLAY") { screen = AppScreen.PLAY }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "RULES") { screen = AppScreen.RULES }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "PROFILE") { screen = AppScreen.PROFILE }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "SETTINGS") { screen = AppScreen.SETTINGS }

                        Spacer(Modifier.height(24.dp))
                    }
                    return@Column
                }

                AppScreen.PLAY -> {
                    val stats = statsStore.load()
                    val normalUnlocked = stats.easyGames > 0
                    val hardUnlocked = stats.normalWins > 0

                    fun startGame(picked: Difficulty) {
                        difficulty = picked
                        engine.setDifficulty(picked)

                        val snap = engineSnapshot(engine)
                        lastResult = snap
                        logText = buildLogText(snap)

                        humanActionLocked = false
                        startLocked = false

                        choice = 1
                        targetId = null
                        guess = 3

                        screen = AppScreen.GAME
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuLinkButton(text = "EASY") { startGame(Difficulty.EASY) }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(
                            text = if (normalUnlocked) "NORMAL" else "NORMAL (LOCKED)",
                            enabled = normalUnlocked
                        ) { startGame(Difficulty.NORMAL) }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(
                            text = if (hardUnlocked) "HARD" else "HARD (LOCKED)",
                            enabled = hardUnlocked
                        ) { startGame(Difficulty.HARD) }

                        Spacer(Modifier.height(16.dp))
                        MenuLinkButton(text = "BACK") { screen = AppScreen.MAIN_MENU }

                        Spacer(Modifier.height(24.dp))
                    }
                    return@Column
                }

                AppScreen.RULES -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuLinkButton(text = "HOW TO PLAY") { showHowToPlay = true }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "ADVANCED TIPS") { showTips = true }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "ARCHETYPES") { showArchetypes = true }

                        Spacer(Modifier.height(16.dp))
                        MenuLinkButton(text = "BACK") { screen = AppScreen.MAIN_MENU }

                        Spacer(Modifier.height(24.dp))
                    }
                    return@Column
                }

                AppScreen.PROFILE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuLinkButton(text = "STATS") { showStats = true }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "ACHIEVEMENTS") { showAchievements = true }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "CUSTOMIZE") { screen = AppScreen.CUSTOMIZE }

                        Spacer(Modifier.height(16.dp))
                        MenuLinkButton(text = "BACK") { screen = AppScreen.MAIN_MENU }

                        Spacer(Modifier.height(24.dp))
                    }
                    return@Column
                }

                AppScreen.CUSTOMIZE -> {
                    var draftName by remember(playerName) { mutableStateOf(playerName) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Change Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            singleLine = true,
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val cleaned = draftName.trim().take(18)
                                statsStore.setPlayerName(cleaned)
                                playerName = statsStore.getPlayerName()
                                engine.setHumanName(playerName)
                                screen = AppScreen.PROFILE
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save")
                        }

                        Spacer(Modifier.height(16.dp))
                        MenuLinkButton(text = "BACK") { screen = AppScreen.PROFILE }

                        Spacer(Modifier.height(24.dp))
                    }
                    return@Column
                }

                AppScreen.SETTINGS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (showResetStatsConfirm) {
                            AlertDialog(
                                onDismissRequest = { showResetStatsConfirm = false },
                                title = { Text("ARE YOU SURE?") },
                                text = { Text("This will reset all stats.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            statsStore.resetAll()
                                            playerName = statsStore.getPlayerName()
                                            engine.setHumanName(playerName)
                                            showResetStatsConfirm = false
                                        }
                                    ) { Text("RESET") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showResetStatsConfirm = false }) {
                                        Text("CANCEL")
                                    }
                                },
                                properties = DialogProperties(dismissOnClickOutside = true)
                            )
                        }

                        MenuLinkButton(text = "SOUND (COMING SOON)") { /* placeholder */ }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "MUSIC (COMING SOON)") { /* placeholder */ }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(text = "RESET STATS") {
                            showResetStatsConfirm = true
                        }

                        Spacer(Modifier.height(16.dp))
                        MenuLinkButton(text = "BACK") { screen = AppScreen.MAIN_MENU }

                        Spacer(Modifier.height(24.dp))
                    }
                    return@Column
                }

                AppScreen.GAME -> {
                    // Fall through to game UI below.
                }
            }

            if (difficulty == null) {
                // Safety: if we somehow got here without a difficulty, send player back.
                screen = AppScreen.MAIN_MENU
                return@Column
            }

            // GAME SCREEN
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
                        val dieEnabled = !gameOver && !startLocked && (phase == EnginePhase.SELECT)
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
                            enabled = inputsEnabled && !isPassing,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { OneLineButtonText("Pass") }

                        Button(
                            onClick = {
                                val firstTarget = players.firstOrNull { it.id != GameEngine.HUMAN_ID }?.id
                                targetId = firstTarget
                            },
                            enabled = inputsEnabled && isPassing,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { OneLineButtonText("Target") }
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

                    val zeroGuessUnlocked = statsStore.load().zeroHeroUnlocked

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (zeroGuessUnlocked) {
                            SmallChoiceButton("0", selected = (guess == 0), enabled = inputsEnabled) { guess = 0 }
                        }
                        SmallChoiceButton("1", selected = (guess == 1), enabled = inputsEnabled) { guess = 1 }
                        SmallChoiceButton("3", selected = (guess == 3), enabled = inputsEnabled) { guess = 3 }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val leftLabel = when (phase) {
                            EnginePhase.SELECT, EnginePhase.ROUND_END -> "Start Round->"
                            EnginePhase.PLAYER_TURN -> "Submit Turn->"
                            EnginePhase.BOT_TURN -> "Bots Acting..."
                            EnginePhase.GAME_OVER -> "Game Over!"
                            EnginePhase.SETUP -> "Setup..."
                        }

                        val leftEnabled = when (phase) {
                            EnginePhase.SELECT, EnginePhase.ROUND_END -> !gameOver && !startLocked
                            EnginePhase.PLAYER_TURN -> !gameOver && !humanActionLocked
                            else -> false
                        }

                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            enabled = leftEnabled,
                            onClick = {
                                when (phase) {
                                    EnginePhase.SELECT, EnginePhase.ROUND_END -> {
                                        startLocked = true
                                        val result = engine.startRound(choice)
                                        lastResult = result
                                        logText = buildLogText(result)
                                        targetId = null
                                    }
                                    EnginePhase.PLAYER_TURN -> {
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
                                    else -> Unit
                                }
                            }
                        ) { OneLineButtonText(leftLabel) }
                    }
                }

                if (difficulty == Difficulty.EASY) {
                    MarblesBox(
                        players = players,
                        modifier = Modifier
                            .wrapContentWidth()
                            .widthIn(max = 200.dp)
                    )
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
                Text(
                    text = logText.ifBlank { "(log will appear here)" },
                    fontFamily = FontFamily.Monospace,
                    softWrap = true
                )
            }
        }
    }
}

/// Single-contour cloud: avoids internal “circle outlines” being stroked over the text.
private val CloudButtonShape: GenericShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height

    // Handy percentages
    val left = 0.04f * w
    val right = 0.96f * w
    val top = 0.10f * h
    val bottom = 0.92f * h

    // Cloud “puff line” control heights
    val puffY1 = 0.28f * h
    val puffY2 = 0.08f * h
    val puffY3 = 0.22f * h

    // Start bottom-left-ish
    moveTo(left + 0.08f * w, bottom)

    // Bottom edge (slight curve)
    cubicTo(
        left + 0.30f * w, bottom + 0.02f * h,
        right - 0.30f * w, bottom + 0.02f * h,
        right - 0.08f * w, bottom
    )

    // Right side up into right puff
    cubicTo(
        right + 0.02f * w, 0.78f * h,
        right + 0.01f * w, 0.46f * h,
        right - 0.14f * w, puffY3
    )

    // Big right puff crest
    cubicTo(
        right - 0.06f * w, puffY2,
        right - 0.22f * w, top,
        right - 0.36f * w, puffY1
    )

    // Center puff (highest)
    cubicTo(
        right - 0.44f * w, top - 0.02f * h,
        left + 0.56f * w, top - 0.02f * h,
        left + 0.46f * w, puffY1
    )

    // Left-center puff
    cubicTo(
        left + 0.40f * w, top + 0.06f * h,
        left + 0.24f * w, top + 0.06f * h,
        left + 0.22f * w, puffY1 + 0.02f * h
    )

    // Small left puff down into left side
    cubicTo(
        left + 0.06f * w, puffY3,
        left - 0.02f * w, 0.52f * h,
        left + 0.06f * w, 0.72f * h
    )

    // Back to start along left-bottom curve
    cubicTo(
        left + 0.01f * w, 0.80f * h,
        left + 0.02f * w, bottom,
        left + 0.08f * w, bottom
    )

    close()
}

@Composable
private fun MenuLinkButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    // Homogenized: one consistent “storm grey” for all menu buttons
    val outlineColor = Color(0xFF9AA3AD)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Cloud fill: light by default, slightly deeper when pressed; keep text readable.
    val baseFill = Color(0xFFF2F7FF)
    val pressedFill = Color(0xFFE4F0FF)

    val containerColor by animateColorAsState(
        targetValue = if (pressed) pressedFill else baseFill,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "cloudMenuButtonBg"
    )

    val borderColor = if (enabled) outlineColor else outlineColor.copy(alpha = 0.35f)
    val contentColor = borderColor

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .heightIn(min = 74.dp)
            .shadow(elevation = 10.dp, shape = CloudButtonShape, clip = false),
        shape = CloudButtonShape,
        border = BorderStroke(3.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = baseFill.copy(alpha = 0.40f),
            disabledContentColor = outlineColor.copy(alpha = 0.35f)
        ),
        contentPadding = PaddingValues(vertical = 18.dp, horizontal = 26.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = contentColor
        )
    }
}

@Composable
private fun OneLineButtonText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
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
private fun SimpleDialog(
    title: String,
    onClose: () -> Unit,
    accentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = title,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scroll)
                    .padding(end = 6.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(
                onClick = onClose,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = accentColor
                )
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Black.copy(alpha = 0.92f),
        textContentColor = Color.White,
        titleContentColor = accentColor,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        modifier = Modifier.border(
            BorderStroke(2.dp, accentColor),
            shape = RoundedCornerShape(16.dp)
        )
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
                "- Choosing (3) gains marbles fast, but makes you easy to steal from.\n" +
                "- Targeting is a great way to gain extra marbles, and enemies!\n" +
                "- If you're repeatedly targeted, choose a (0) to fight back.\n" +
                "- At the start of the game, a player is randomly chosen to go first, and normally each round begins with the next player on the list.\n\n" +
                "BOTS & ARCHETYPES:\n" +
                "- Each game, bots are randomly assigned an Archetype.\n" +
                "- On Easy mode, bots have their names replaced with their Archetype (and you can see their score totals).\n" +
                "- On Normal mode, bots have their names and scores hidden.\n" +
                "- On Hard mode, bots will also gang up on you as the finish line approaches.\n\n" +
                "JESTER'S HAT RULE:\n" +
                "- If you guess 1 or 3 on someone who actually chose 0, you lose 1 marble and take the Jester's Hat.\n" +
                "- If you guess 0 someone who actually chose 0, you lose 0 marbles and take the Jester's Hat.\n" +
                "- Next round, the Hat-holder goes first BUT only if the Hat ended the round on a different person than it started.\n"
    )
}

@Composable
private fun ArchetypesText() {
    Text(
        "Accretion:\n" +
                "- Starts slow, but ramps up fast\n" +
                "- Chooses: 1, 1, 3, 3, 3...\n" +
                "- Targeting Behavior: Passes until the very last moment\n\n" +
                "Avenger:\n" +
                "- Retaliates against attackers, even if they didn't attack him\n" +
                "- Chooses: 1 or 3\n" +
                "- Targeting Behavior: Passes round 1; targets attackers from round 2 on\n\n" +
                "Auditor:\n" +
                "- Hates wallflowers. Targets anyone who passes too much\n" +
                "- Chooses: 1 or 3.\n" +
                "- Targeting Behavior: Passes rounds 1 and 2, targets pass-streak players after.\n\n" +
                "Chaos Grandma:\n" +
                "- The Matriarch of Chaos. All random everything\n" +
                "- Chooses: Random (0/1/3 evenly)\n" +
                "- Targeting Behavior: 50/50 pass vs target\n\n" +
                "Hat Farmer:\n" +
                "- Will gladly pay a marble to snag the Jester's Hat\n" +
                "- Chooses: Baseline behavior.\n" +
                "- Targeting Behavior: Passes first; then targets recently guessed players\n\n" +
                "Juliet (Colluder):\n" +
                "- She's in it to win it, or at least watch Romeo win\n" +
                "- Chooses: 1 or 3\n" +
                "- Targeting Behavior: Anyone but Romeo\n\n" +
                "Kingmaker:\n" +
                "- Picks a 'King' and only attacks people who attack that King\n" +
                "- Chooses: 50/50 between 1 and 3\n" +
                "- Targeting Behavior: Usually passes; targets only to avenge their king\n\n" +
                "Limper:\n" +
                "- Seems like maybe they don't want to play\n" +
                "- Chooses: 1 (0 if attacked and defending).\n" +
                "- Targeting Behavior: Always passes\n\n" +
                "Opportunist:\n" +
                "- Waits, watches, then punishes repeated 3 behavior\n" +
                "- Chooses: 1 (unless close to winning)\n" +
                "- Targeting Behavior: Passes for 3 rounds, then targets repeat-3 players\n\n" +
                "Pacifist Collector:\n" +
                "- Greedy but peaceful. Tries to win by Trickle alone\n" +
                "- Chooses: 3\n" +
                "- Targeting Behavior: Always passes\n\n" +
                "Romeo (Colluder):\n" +
                "- His eyes are on the prize(and on Juliet, of course)\n" +
                "- Chooses: 1 or 3\n" +
                "- Targeting Behavior: Juliet is safe, everyone else is a target\n\n" +
                "Scout:\n" +
                "- Will gladly be the first to act\n" +
                "- Chooses: 1 or 3.\n" +
                "- Targeting Behavior: Always targets, even on round 1\n\n" +
                "Spite Player:\n" +
                "- Cut this guy off and he is tailgating you to your house\n" +
                "- Chooses: 1 or 3.\n" +
                "- Targeting Behavior: Passes until attacked; then relentlessly seeks revenge\n\n" +
                "Strobe:\n" +
                "- Alternates like a metronome and attacks in a repeating rhythm.\n" +
                "- Chooses: Alternates 1,3,1,3...\n" +
                "- Targeting Behavior: Alternates pass/target with pattern-based guesses.\n\n" +
                "Teacher:\n" +
                "- Lets others learn the game, then shows them how to lose.\n" +
                "- Chooses: 1, 1, 1, 3, 3...\n" +
                "- Targeting Behavior: Passes for 3 rounds, then targets known 3-choosers.\n\n" +
                "Three-Pusher:\n" +
                "- Bigger number better number, right?\n" +
                "- Chooses: Always 3.\n" +
                "- Targeting Behavior: Targets from round 2 onward, always guessing 3.\n\n"
    )
}

@Composable
private fun StatsText(stats: PlayerStats) {
    val acc = if (stats.totalGuesses == 0) ""
    else "${((stats.correctGuesses * 100.0) / stats.totalGuesses).toInt()}%"

    Column {
        Text(
            "Total games: ${stats.totalGames}\n" +
                    "Wins: ${stats.totalWins}\n\n" +
                    "Total marbles gained: ${stats.totalMarblesAcrossGames}\n\n" +
                    "Accuracy: $acc (${stats.correctGuesses}/${stats.totalGuesses})\n" +
                    "Tricked by 0: ${stats.timesTrickedByZero}\n" +
                    "Perfect games: ${stats.perfectGames}\n\n" +
                    "Easy games: ${stats.easyGames} (wins ${stats.easyWins})\n" +
                    "Normal games: ${stats.normalGames} (wins ${stats.normalWins})\n" +
                    "Hard games: ${stats.hardGames} (wins ${stats.hardWins})\n\n",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AchievementsText(stats: PlayerStats) {
    Column {
        Spacer(Modifier.height(16.dp))
        Text("Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // --- First milestones ---
        AchievementRow(stats.firstGameCompleted, "Get Your Feet Wet", "Complete a game")
        AchievementRow(stats.firstWin, "TRICKLE!", "Win a game")

        // --- Special wins / events ---
        AchievementRow(stats.firstPerfectWin, "Perfect Puddler", "Win with 0 wrong guesses")
        AchievementRow(stats.reachedRound6, "Idle Hands", "Reach round 6")
        AchievementRow(stats.wonWith18Marbles, "Legal Limit", "Win with exactly 18 marbles")

        // --- Difficulty wins ---
        AchievementRow(stats.wonEasy, "Comp Stomp", "Win on Easy")
        AchievementRow(stats.wonNormal, "Pattern Finder", "Win on Normal")
        AchievementRow(stats.wonHard, "TR1CKL3!", "Win on Hard")

        // --- Tourist (progress row) ---
        AchievementRow(
            unlocked = stats.playedAllDifficulties,
            title = "Tourist",
            desc = "Play on Easy, Normal, and Hard",
            progress = "${stats.easyGames.coerceAtLeast(0)} / ${stats.normalGames.coerceAtLeast(0)} / ${stats.hardGames.coerceAtLeast(0)}"
        )

        AchievementRow(stats.pacifistWin, "Conscientious Objector", "Win without guessing")
        // ✅ NEW: show pacifistGame in UI
        AchievementRow(stats.pacifistGame, "Pacifist", "Complete a game without guessing")

        AchievementRow(
            stats.justPressEverythingWin,
            "Just Press Everything",
            "In one game: choose 0/1/3, pass once, guess 1 and 3"
        )

        AchievementRow(
            stats.shakespeareWin,
            "Wherefore Art Thou",
            "Correctly guess Romeo and Juliet (when both are present)"
        )

        // --- Zero chain ---
        AchievementRow(stats.firstTheFool, "The Fool", "Get tricked by a 0")
        AchievementRow(stats.firstZeroTrap, "Zero Trap", "Trick a bot with your 0")
        AchievementRow(stats.zeroHeroUnlocked, "Zero Hero", "Unlock The ability to Guess 0!")

        // ✅ NEW ACHIEVEMENTS (show them)
        AchievementRow(stats.drySeasonWin, "Dry Season", "Win without ever choosing 3")
        AchievementRow(stats.ghostCupWin, "Ghost Cup", "Win without being targeted")
        AchievementRow(stats.onARoll, "On a Roll", "3 correct guesses in a row")
        AchievementRow(stats.dumbLuck, "Dumb Luck", "Correctly guess a 3 in round 1")
        AchievementRow(stats.hatFinisher, "Hat Trick", "Win after starting because you had the Hat")
        AchievementRow(stats.caughtTheStrobe, "Caught the Strobe", "Correctly guess Strobe's 3 twice in one game")
        AchievementRow(stats.pushover, "Pushover", "Correctly guess Three-Pusher's 3 four times in one game")

        // --- Milestones ---
        AchievementRow(
            unlocked = stats.won13thGame,
            title = "Trickle Champion",
            desc = "Win 13 games",
            progress = "${stats.totalWins}/13"
        )
        AchievementRow(
            unlocked = stats.won113thGame,
            title = "Trickle God",
            desc = "Win 113 games",
            progress = "${stats.totalWins}/113"
        )

        AchievementRow(
            unlocked = stats.played13Games,
            title = "Amateur",
            desc = "Play 13 games",
            progress = "${stats.totalGames}/13"
        )
        AchievementRow(
            unlocked = stats.played113Games,
            title = "Professional",
            desc = "Play 113 games",
            progress = "${stats.totalGames}/113"
        )
        AchievementRow(
            unlocked = stats.played1113Games,
            title = "Expert",
            desc = "Play 1,113 games",
            progress = "${stats.totalGames}/1113"
        )

        AchievementRow(
            unlocked = stats.has113MarblesTotal,
            title = "Bucket Filler",
            desc = "Gain 113 marbles across games",
            progress = "${stats.totalMarblesAcrossGames}/113"
        )
        AchievementRow(
            unlocked = stats.has1113MarblesTotal,
            title = "Tub Filler",
            desc = "Gain 1,113 marbles across games",
            progress = "${stats.totalMarblesAcrossGames}/1113"
        )
        AchievementRow(
            unlocked = stats.has11113MarblesTotal,
            title = "Pool Filler",
            desc = "Gain 11,113 marbles across games",
            progress = "${stats.totalMarblesAcrossGames}/11113"
        )
    }
}

// -------------------- UI components --------------------

@Composable
private fun AchievementRow(
    unlocked: Boolean,
    title: String,
    desc: String,
    progress: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (unlocked) "✓" else "X",
            color = if (unlocked) Color(0xFF0000FF) else Color.Gray,
            fontSize = 24.sp
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }

        if (progress != null) {
            Text(
                progress,
                color = if (unlocked) Color(0xFF0000FF) else Color.LightGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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
    val cs = MaterialTheme.colorScheme
    val container = if (selected) cs.primary else cs.surfaceVariant
    val content = if (selected) cs.onPrimary else cs.onSurfaceVariant

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 44.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = cs.surfaceVariant,
            disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false
        )
    }
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
    val selectedName = options.firstOrNull { it.id == selectedTargetId }?.baseName ?: "-"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("You target") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .widthIn(min = 160.dp, max = 220.dp)
        )

        val menuMaxHeight = (options.size * 56).dp

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = menuMaxHeight)
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
    normalUnlocked: Boolean,
    hardUnlocked: Boolean,
    onSelect: (Difficulty) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val label = when (selected) {
        Difficulty.EASY -> "Easy (show archetypes and scores)"
        Difficulty.NORMAL -> "Normal (hides archetypes and scores)"
        Difficulty.HARD -> "Hard (bots block your win)"
        null -> "Select difficulty..."
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
                enabled = normalUnlocked,
                onClick = { onSelect(Difficulty.NORMAL); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Hard (bots block your win)") },
                enabled = hardUnlocked,
                onClick = { onSelect(Difficulty.HARD); expanded = false }
            )
        }
    }
}
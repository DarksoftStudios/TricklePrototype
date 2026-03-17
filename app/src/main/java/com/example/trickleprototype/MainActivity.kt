package com.example.trickleprototype

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.math.sin
import android.media.MediaPlayer
import androidx.compose.ui.text.style.TextAlign
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.example.trickleprototype.ui.theme.TricklePrototypeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.layout.Box


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

private enum class IndicatorTone {
    NEUTRAL,
    GOOD,
    BAD,
    ALERT
}

@Immutable
private data class FloatingIndicator(
    val token: Long,
    val text: String,
    val tone: IndicatorTone
)


private fun indicatorToneColor(tone: IndicatorTone): Color {
    return when (tone) {
        IndicatorTone.NEUTRAL -> Color(0xFFE3F2FD)
        IndicatorTone.GOOD -> Color(0xFFC8E6C9)
        IndicatorTone.BAD -> Color(0xFFFFCDD2)
        IndicatorTone.ALERT -> Color(0xFFFFF59D)
    }
}

private fun hatStripeColor(index: Int): Color {
    return if (index % 2 == 0) Color.White else Color.Black
}

private fun resolvePlayerIdByName(players: List<PlayerState>, rawName: String): Int? {
    val trimmed = rawName.trim().removeSuffix(".")
    return players.firstOrNull { it.baseName.equals(trimmed, ignoreCase = true) }?.id
}

private fun splitTargetNames(rawNames: String): List<String> {
    return rawNames
        .split(" and ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun extractVisualIndicators(
    players: List<PlayerState>,
    line: String
): List<Pair<Int, Pair<String, IndicatorTone>>> {
    fun pairFor(name: String, text: String, tone: IndicatorTone): Pair<Int, Pair<String, IndicatorTone>>? {
        val playerId = resolvePlayerIdByName(players, name) ?: return null
        return playerId to (text to tone)
    }

    Regex("^(.+?) passes\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[1], "PASS", IndicatorTone.NEUTRAL))
    }

    Regex("^(.+?) targets (.+?) and guesses (\\d+)\\.$").matchEntire(line)?.let { match ->
        val actorName = match.groupValues[1]
        val targetNames = splitTargetNames(match.groupValues[2])
        val indicators = mutableListOf<Pair<Int, Pair<String, IndicatorTone>>>()
        pairFor(actorName, "AIM", IndicatorTone.ALERT)?.let { indicators += it }
        targetNames.forEach { name ->
            pairFor(name, "TARGET", IndicatorTone.ALERT)?.let { indicators += it }
        }
        return indicators
    }

    Regex("^(.+?) nailed a 0 and takes the Hat\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[1], "HAT", IndicatorTone.ALERT))
    }

    Regex("^(.+?) was correct and takes (\\d+) from (.+?)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "+${match.groupValues[2]}", IndicatorTone.GOOD),
            pairFor(match.groupValues[3], "-${match.groupValues[2]}", IndicatorTone.BAD)
        )
    }

    Regex("^(.+?) was correct and gains (\\d+)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[1], "+${match.groupValues[2]}", IndicatorTone.GOOD))
    }

    Regex("^(.+?) was wrong on a 0 and gives (\\d+) to (.+?) \\(HAT moves to .+?\\)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "-${match.groupValues[2]}", IndicatorTone.BAD),
            pairFor(match.groupValues[3], "+${match.groupValues[2]}", IndicatorTone.GOOD)
        )
    }

    Regex("^(.+?) was wrong on a 0, loses (\\d+) \\(HAT moves to .+?\\)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[1], "-${match.groupValues[2]}", IndicatorTone.BAD))
    }

    Regex("^(.+?) was wrong on a 0, gains (\\d+) \\(HAT moves to .+?\\)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[1], "+${match.groupValues[2]}", IndicatorTone.GOOD))
    }

    Regex("^(.+?) was wrong, (.+?) takes (\\d+) from them\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "-${match.groupValues[3]}", IndicatorTone.BAD),
            pairFor(match.groupValues[2], "+${match.groupValues[3]}", IndicatorTone.GOOD)
        )
    }

    Regex("^(.+?) was wrong, (.+?) gains (\\d+)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[2], "+${match.groupValues[3]}", IndicatorTone.GOOD))
    }

    Regex("^(.+?) wasn't targeted, trickles (\\d+)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(pairFor(match.groupValues[1], "+${match.groupValues[2]}", IndicatorTone.GOOD))
    }

    return emptyList()
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

    val context = LocalContext.current
    val appContext = context.applicationContext
    val statsStore = remember { StatsStore(appContext) }
    val settingsPrefs = remember {
        appContext.getSharedPreferences("trickle_settings", ComponentActivity.MODE_PRIVATE)
    }

    var screen by remember { mutableStateOf(AppScreen.SPLASH) }

    var showResetStatsConfirm by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("sound_enabled", true)) }
    var musicEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("music_enabled", true)) }

    // Player identity
    var playerName by remember { mutableStateOf(statsStore.getPlayerName()) }

    // Keep engine in sync with saved name
    SideEffect {
        engine.setHumanName(playerName)
    }

    //  FIX: attach synchronously (no async race)
    SideEffect {
        engine.attachStatsStore(statsStore)
    }

    var difficulty by remember { mutableStateOf<Difficulty?>(null) }
    var weatherEnabled by remember { mutableStateOf(true) }

    var choice by remember { mutableIntStateOf(1) }
    var targetId by remember { mutableStateOf<Int?>(null) }
    var secondTargetId by remember { mutableStateOf<Int?>(null) }
    var guess by remember { mutableIntStateOf(3) }
    var displayedRound by remember { mutableIntStateOf(1) }
    var pendingHumanAction by remember { mutableStateOf(PendingHumanAction.NONE) }
    var showLogOverlay by remember { mutableStateOf(false) }

    var dieButtonsEnabled by remember { mutableStateOf(true) }

    var lastResult by remember { mutableStateOf<RoundResult?>(null) }
    var logText by remember { mutableStateOf("") }
    var floatingIndicators by remember { mutableStateOf<Map<Int, FloatingIndicator>>(emptyMap()) }
    var nextIndicatorToken by remember { mutableStateOf(1L) }

    // Achievement popups
    var achievementQueue by remember { mutableStateOf<List<AchievementPopup>>(emptyList()) }
    var activeAchievement by remember { mutableStateOf<AchievementPopup?>(null) }
    var splashSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val bgmPlayer = remember {
        MediaPlayer.create(context, R.raw.mainthemegen)?.apply {
            isLooping = true
            setVolume(0.35f, 0.35f)
        }
    }

    LaunchedEffect(screen, musicEnabled) {
        if (!musicEnabled) {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer.pause()
            }
        } else {
            if (bgmPlayer?.isPlaying != true) {
                bgmPlayer?.start()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            splashSoundPlayer?.release()
            bgmPlayer?.stop()
            bgmPlayer?.release()
        }
    }

    LaunchedEffect(soundEnabled) {
        settingsPrefs.edit().putBoolean("sound_enabled", soundEnabled).apply()

        if (!soundEnabled) {
            splashSoundPlayer?.stop()
            splashSoundPlayer?.release()
            splashSoundPlayer = null
        }
    }

    LaunchedEffect(musicEnabled) {
        settingsPrefs.edit().putBoolean("music_enabled", musicEnabled).apply()
    }

    // must be above LaunchedEffect or it won't exist yet
    val seenAchievements = remember { mutableSetOf<String>() }

    LaunchedEffect(lastResult) {
        val result = lastResult ?: return@LaunchedEffect

        val newlyParsed = result.log
            .mapNotNull { parseAchievementPopup(it.text) }
            .filter { popup ->
                if (seenAchievements.contains(popup.title)) false
                else {
                    seenAchievements.add(popup.title)
                    true
                }
            }

        if (newlyParsed.isNotEmpty()) {
            achievementQueue = achievementQueue + newlyParsed
        }
    }

    LaunchedEffect(activeAchievement, achievementQueue) {
        if (activeAchievement == null && achievementQueue.isNotEmpty()) {
            activeAchievement = achievementQueue.first()
            achievementQueue = achievementQueue.drop(1)
        }
    }

    LaunchedEffect(activeAchievement?.title) {
        if (activeAchievement == null) return@LaunchedEffect
        delay(2500)
        activeAchievement = null
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
    val currentActorId = lastResult?.currentActorId
    val gameOver = (phase == EnginePhase.GAME_OVER)
    val currentWeatherName = lastResult?.currentWeatherName
    val currentWeatherEffect = lastResult?.currentWeatherEffect
    val forcedGuess = lastResult?.forcedGuessForHuman
    val mustTarget = lastResult?.mustTargetForHuman == true
    val needsSecondTarget = lastResult?.requiresSecondTargetForHuman == true

    val scrollState = rememberScrollState()
    LaunchedEffect(logText) { scrollState.scrollTo(0) }

    LaunchedEffect(lastResult?.log) {
        val latestRound = lastResult
            ?.log
            ?.mapNotNull { event ->
                Regex("=== ROUND (\\d+)").find(event.text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
            ?.lastOrNull()

        if (latestRound != null) {
            displayedRound = latestRound
        }
    }

    LaunchedEffect(lastResult?.log?.size, players) {
        val latestLine = lastResult?.log?.lastOrNull()?.text ?: return@LaunchedEffect
        val parsedIndicators = extractVisualIndicators(players, latestLine)
        if (parsedIndicators.isEmpty()) return@LaunchedEffect

        parsedIndicators.forEach { (playerId, parsed) ->
            val token = nextIndicatorToken
            nextIndicatorToken += 1
            val indicator = FloatingIndicator(
                token = token,
                text = parsed.first,
                tone = parsed.second
            )
            floatingIndicators = floatingIndicators.toMutableMap().apply {
                this[playerId] = indicator
            }

            launch {
                delay(3000)
                if (floatingIndicators[playerId]?.token == token) {
                    floatingIndicators = floatingIndicators.toMutableMap().apply {
                        remove(playerId)
                    }
                }
            }
        }
    }

    LaunchedEffect(forcedGuess) {
        if (forcedGuess != null) {
            guess = forcedGuess
        }
    }

    LaunchedEffect(targetId, needsSecondTarget) {
        if (!needsSecondTarget || targetId == null) {
            secondTargetId = null
        } else if (secondTargetId == targetId) {
            secondTargetId = null
        }
    }

    // BOT stepping with turbo
    LaunchedEffect(phase, turbo) {
        if (phase == EnginePhase.BOT_TURN) {
            while (true) {
                val current = lastResult?.phase ?: engine.getPhase()
                if (current != EnginePhase.BOT_TURN) break

                val result = engine.step()
                lastResult = result
                logText = buildLogText(result, difficulty!!)

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

    //  FIX 1: Use a full-screen Box so the background can sit behind ALL screens.
    Box(modifier = Modifier.fillMaxSize()) {

        //  FIX 1 (continued): Draw the background image on every screen EXCEPT the splash.
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

        //  FIX 2: Keep consistent safe padding for status + nav bars so buttons don't cramp.
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
                                secondTargetId = null
                                guess = 3
                                displayedRound = 1
                                pendingHumanAction = PendingHumanAction.NONE
                                showLogOverlay = false

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
                        Text(
                            text = "Trickle",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

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
                        if (soundEnabled) {
                            splashSoundPlayer?.release()
                            splashSoundPlayer = MediaPlayer.create(context, R.raw.darksoftlogo)?.apply {
                                setOnCompletionListener {
                                    it.release()
                                    if (splashSoundPlayer === it) {
                                        splashSoundPlayer = null
                                    }
                                }
                                start()
                            }
                        }

                        delay(3000L)
                        screen = AppScreen.MAIN_MENU
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable { screen = AppScreen.MAIN_MENU },
                        contentAlignment = Alignment.Center
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
                        engine.setWeatherEnabled(weatherEnabled)

                        val snap = engineSnapshot(engine)
                        lastResult = snap
                        logText = buildLogText(snap, difficulty!!)

                        humanActionLocked = false
                        startLocked = false

                        choice = 1
                        targetId = null
                        secondTargetId = null
                        guess = 3
                        displayedRound = 1
                        pendingHumanAction = PendingHumanAction.NONE
                        showLogOverlay = false

                        screen = AppScreen.GAME
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuLinkButton(
                            text = if (weatherEnabled) "WEATHER: ON" else "WEATHER: OFF"
                        ) { weatherEnabled = !weatherEnabled }
                        Spacer(Modifier.height(10.dp))
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

                        MenuLinkButton(
                            text = if (soundEnabled) "SOUND: ON" else "SOUND: OFF"
                        ) {
                            soundEnabled = !soundEnabled

                            if (!soundEnabled) {
                                splashSoundPlayer?.stop()
                                splashSoundPlayer?.release()
                                splashSoundPlayer = null
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        MenuLinkButton(
                            text = if (musicEnabled) "MUSIC: ON" else "MUSIC: OFF"
                        ) {
                            musicEnabled = !musicEnabled

                            if (!musicEnabled) {
                                bgmPlayer?.pause()
                            } else {
                                if (bgmPlayer?.isPlaying != true) {
                                    bgmPlayer?.start()
                                }
                            }
                        }
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
            val humanPlayer = players.firstOrNull { it.id == GameEngine.HUMAN_ID }
            val playerScore = humanPlayer?.marbles ?: 0
            val playerTitle = humanPlayer?.baseName ?: "You"

            val isPlayerTurn = (phase == EnginePhase.PLAYER_TURN)
            val inputsEnabled = !gameOver && isPlayerTurn && !humanActionLocked

            val dropdownOptions = if (isPlayerTurn) {
                val targetable = lastResult?.targetableIdsForHuman ?: emptyList()
                players.filter { it.id in targetable && it.id != GameEngine.HUMAN_ID }
            } else {
                emptyList()
            }

            val secondDropdownOptions = dropdownOptions.filter { it.id != targetId }

            val zeroGuessUnlocked = statsStore.load().zeroHeroUnlocked

            val playerPhaseBadge = phaseBadgeText(
                roundNumber = displayedRound,
                enginePhase = phase,
                pendingHumanAction = pendingHumanAction,
                humanActionLocked = humanActionLocked,
                targetId = targetId,
                secondTargetId = secondTargetId,
                needsSecondTarget = needsSecondTarget,
                latestLogLine = lastResult?.log?.lastOrNull()?.text
            )
            val tableBots = players.filter { it.id != GameEngine.HUMAN_ID }

            val rightBots = tableBots.take(6)
            val leftBots = tableBots.drop(6).take(6).reversed()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Top
                    ) {
                        WeatherInfoBadge(
                            weatherName = currentWeatherName,
                            weatherEffect = currentWeatherEffect,
                            forcedGuess = forcedGuess,
                            mustTarget = mustTarget,
                            needsSecondTarget = needsSecondTarget,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.widthIn(min = 10.dp))

                        PlayerStatusStack(
                            playerTitle = playerTitle,
                            playerScore = playerScore,
                            isCurrentTurn = currentActorId == GameEngine.HUMAN_ID,
                            indicator = floatingIndicators[GameEngine.HUMAN_ID],
                            hasHat = lastResult?.hatHolderId == GameEngine.HUMAN_ID,
                            modifier = Modifier.weight(1.1f)
                        )

                        Spacer(Modifier.widthIn(min = 10.dp))

                        PhaseBadge(
                            text = playerPhaseBadge,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        GameTableSurface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.64f)
                                .height(660.dp)
                        )

                        BotCupColumn(
                            bots = leftBots,
                            currentActorId = currentActorId,
                            hatHolderId = lastResult?.hatHolderId,
                            indicators = floatingIndicators,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 0.dp, top = 18.dp, bottom = 36.dp)
                        )

                        BotCupColumn(
                            bots = rightBots,
                            currentActorId = currentActorId,
                            hatHolderId = lastResult?.hatHolderId,
                            indicators = floatingIndicators,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 0.dp, top = 12.dp, bottom = 52.dp)
                        )

                        TableActionPanel(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(0.50f)
                                .padding(top = 18.dp),
                            choice = choice,
                            onChoiceSelected = { choice = it },
                            choiceEnabled = !gameOver && !startLocked && (phase == EnginePhase.SELECT || phase == EnginePhase.ROUND_END),
                            inputsEnabled = inputsEnabled,
                            pendingHumanAction = pendingHumanAction,
                            onPassSelected = {
                                targetId = null
                                secondTargetId = null
                                pendingHumanAction = PendingHumanAction.PASS
                            },
                            onTargetSelected = {
                                targetId = null
                                secondTargetId = null
                                pendingHumanAction = PendingHumanAction.TARGET
                            },
                            dropdownOptions = dropdownOptions,
                            selectedTargetId = targetId,
                            onTargetPicked = {
                                targetId = it
                                if (secondTargetId == it) secondTargetId = null
                            },
                            needsSecondTarget = needsSecondTarget,
                            secondDropdownOptions = secondDropdownOptions,
                            selectedSecondTargetId = secondTargetId,
                            onSecondTargetPicked = { secondTargetId = it },
                            guess = guess,
                            onGuessSelected = { guess = it },
                            zeroGuessUnlocked = zeroGuessUnlocked,
                            forcedGuess = forcedGuess,
                            submitLabel = when (phase) {
                                EnginePhase.SELECT, EnginePhase.ROUND_END -> "Start Round->"
                                EnginePhase.PLAYER_TURN -> "Submit Turn->"
                                EnginePhase.BOT_TURN -> "Bots Acting..."
                                EnginePhase.GAME_OVER -> "Game Over!"
                                EnginePhase.SETUP -> "Setup..."
                            },
                            submitEnabled = when (phase) {
                                EnginePhase.SELECT, EnginePhase.ROUND_END -> !gameOver && !startLocked
                                EnginePhase.PLAYER_TURN -> !gameOver && !humanActionLocked
                                else -> false
                            },
                            onSubmit = {
                                when (phase) {
                                    EnginePhase.SELECT, EnginePhase.ROUND_END -> {
                                        startLocked = true
                                        pendingHumanAction = PendingHumanAction.NONE
                                        val result = engine.startRound(choice)
                                        lastResult = result
                                        logText = buildLogText(result, difficulty!!)
                                        targetId = null
                                        secondTargetId = null
                                        humanActionLocked = false
                                    }
                                    EnginePhase.PLAYER_TURN -> {
                                        humanActionLocked = true
                                        val frozenTarget = if (pendingHumanAction == PendingHumanAction.TARGET) targetId else null
                                        val frozenGuess = if (frozenTarget == null) null else guess

                                        val result = engine.submitHumanTurn(
                                            targetId = frozenTarget,
                                            guess = frozenGuess,
                                            secondTargetId = if (frozenTarget == null) null else secondTargetId
                                        )
                                        lastResult = result
                                        logText = buildLogText(result, difficulty!!)

                                        if (result.phase == EnginePhase.PLAYER_TURN) {
                                            humanActionLocked = false
                                        } else {
                                            targetId = null
                                            secondTargetId = null
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        )

                        if (showLogOverlay) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(0.62f)
                                    .padding(bottom = 54.dp)
                            ) {
                                LogPanel(
                                    logText = logText,
                                    scrollState = scrollState
                                )
                            }
                        }

                        Button(
                            onClick = { showLogOverlay = !showLogOverlay },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4E342E),
                                contentColor = Color.White
                            )
                        ) {
                            Text(if (showLogOverlay) "Hide Log" else "Log")
                        }
                    }
                }
            }
        }
    }
    activeAchievement?.let { popup ->
        AchievementUnlockOverlay(
            popup = popup,
            onDismiss = { activeAchievement = null }
        )
    }
}



private enum class PendingHumanAction {
    NONE,
    PASS,
    TARGET
}

private fun phaseBadgeText(
    roundNumber: Int,
    enginePhase: EnginePhase,
    pendingHumanAction: PendingHumanAction,
    humanActionLocked: Boolean,
    targetId: Int?,
    secondTargetId: Int?,
    needsSecondTarget: Boolean,
    latestLogLine: String?
): String {
    val isTrickling = enginePhase == EnginePhase.BOT_TURN && (
            latestLogLine?.startsWith("TRICKLE") == true ||
                    latestLogLine?.contains("trickles", ignoreCase = true) == true ||
                    latestLogLine?.contains("Trickle obscured", ignoreCase = true) == true
            )

    val instruction = when (enginePhase) {
        EnginePhase.SELECT, EnginePhase.ROUND_END -> "Choose your number"
        EnginePhase.BOT_TURN -> {
            if (isTrickling) {
                "Trickling"
            } else if (humanActionLocked || pendingHumanAction != PendingHumanAction.NONE) {
                "Wait for next round"
            } else {
                "Wait your turn"
            }
        }
        EnginePhase.PLAYER_TURN -> {
            when (pendingHumanAction) {
                PendingHumanAction.NONE -> "Pass or target"
                PendingHumanAction.PASS -> "Wait for next round"
                PendingHumanAction.TARGET -> {
                    val targetReady = targetId != null && (!needsSecondTarget || secondTargetId != null)
                    if (targetReady) "Choose their number" else "Choose your target"
                }
            }
        }
        EnginePhase.GAME_OVER -> "Game over"
        EnginePhase.SETUP -> "Choose your number"
    }

    val phaseNumber = when (enginePhase) {
        EnginePhase.SELECT, EnginePhase.ROUND_END, EnginePhase.SETUP -> 1
        EnginePhase.BOT_TURN -> {
            when {
                isTrickling -> 3
                humanActionLocked || pendingHumanAction == PendingHumanAction.PASS || pendingHumanAction == PendingHumanAction.TARGET -> 2
                else -> 1
            }
        }
        EnginePhase.PLAYER_TURN -> 2
        EnginePhase.GAME_OVER -> 3
    }

    return "Round $roundNumber, Phase $phaseNumber: $instruction"
}

@Composable
private fun WeatherInfoBadge(
    weatherName: String?,
    weatherEffect: String?,
    forcedGuess: Int?,
    mustTarget: Boolean,
    needsSecondTarget: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xAA1E1E1E),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (weatherName.isNullOrBlank()) "Weather: Clear" else "Weather: $weatherName",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (!weatherEffect.isNullOrBlank()) {
                Text(
                    text = weatherEffect,
                    color = Color(0xFFD7E3FC),
                    fontSize = 12.sp
                )
            }
            if (forcedGuess != null) {
                Text(
                    text = "Locked guess: $forcedGuess",
                    color = Color(0xFFFFF59D),
                    fontSize = 12.sp
                )
            }
            if (mustTarget) {
                Text(
                    text = "Must target if able.",
                    color = Color(0xFFFFCCBC),
                    fontSize = 12.sp
                )
            }
            if (needsSecondTarget) {
                Text(
                    text = "Targeting needs two picks.",
                    color = Color(0xFFFFCCBC),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PhaseBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xAA1F2A44)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PlayerStatusStack(
    playerTitle: String,
    playerScore: Int,
    isCurrentTurn: Boolean,
    indicator: FloatingIndicator?,
    hasHat: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TableCup(
            label = "YOU",
            highlighted = true,
            isCurrentTurn = isCurrentTurn,
            indicator = indicator,
            hasHat = hasHat
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = playerTitle,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Score: $playerScore",
            color = Color(0xFFFFF59D),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BotCupColumn(
    bots: List<PlayerState>,
    currentActorId: Int?,
    hatHolderId: Int?,
    indicators: Map<Int, FloatingIndicator>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        bots.forEach { bot ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TableCup(
                    label = bot.baseName.take(1).uppercase(),
                    highlighted = false,
                    isCurrentTurn = currentActorId == bot.id,
                    indicator = indicators[bot.id],
                    hasHat = hatHolderId == bot.id
                )

                Spacer(Modifier.height(1.dp))

                Text(
                    text = bot.baseName,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(72.dp)
                )

                Spacer(Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun TableCup(
    label: String,
    highlighted: Boolean,
    isCurrentTurn: Boolean = false,
    indicator: FloatingIndicator? = null,
    hasHat: Boolean = false
) {
    val bucketFill = if (highlighted) Color(0xFFFF5252) else Color(0xFFD32F2F)
    val bucketBorder = if (highlighted) Color(0xFFFFCDD2) else Color(0xFF7F0000)
    val turnGlowColor = Color(0xFFFFEB3B)
    val cupShape = GenericShape { size, _ ->
        val topInset = size.width * 0.04f
        val bottomInset = size.width * 0.13f

        moveTo(topInset, 0f)
        lineTo(size.width - topInset, 0f)
        lineTo(size.width - bottomInset, size.height)
        lineTo(bottomInset, size.height)
        close()
    }

    Box(
        modifier = Modifier.size(width = 72.dp, height = 106.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (hasHat) {
            JesterHatBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp)
            )
        }

        if (indicator != null) {
            val alpha = remember(indicator.token) { Animatable(1f) }
            LaunchedEffect(indicator.token) {
                alpha.snapTo(1f)
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (hasHat) 30.dp else 4.dp)
                    .alpha(alpha.value),
                shape = RoundedCornerShape(12.dp),
                color = indicatorToneColor(indicator.tone).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.35f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = indicator.text,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Box(
            modifier = Modifier.size(width = 52.dp, height = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentTurn) {
                Surface(
                    modifier = Modifier.size(width = 72.dp, height = 86.dp),
                    shape = cupShape,
                    color = turnGlowColor.copy(alpha = 0.20f),
                    border = BorderStroke(4.dp, turnGlowColor.copy(alpha = 0.95f)),
                    shadowElevation = 18.dp
                ) {}
            }

            Surface(
                modifier = Modifier.size(width = 60.dp, height = 74.dp),
                shape = cupShape,
                color = bucketFill,
                border = BorderStroke(3.dp, if (isCurrentTurn) turnGlowColor else bucketBorder),
                shadowElevation = if (isCurrentTurn) 12.dp else 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 5.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(8.dp),
                        shape = RoundedCornerShape(3.dp),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {}

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.76f)
                            .fillMaxHeight(0.60f),
                        shape = GenericShape { size, _ ->
                            val innerTopInset = size.width * 0.03f
                            val innerBottomInset = size.width * 0.14f

                            moveTo(innerTopInset, 0f)
                            lineTo(size.width - innerTopInset, 0f)
                            lineTo(size.width - innerBottomInset, size.height)
                            lineTo(innerBottomInset, size.height)
                            close()
                        },
                        color = Color.Black.copy(alpha = 0.10f)
                    ) {}


                }
            }
        }
    }
}

@Composable
private fun JesterHatBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(3) { index ->
            Canvas(
                modifier = Modifier.size(width = 12.dp, height = if (index == 1) 24.dp else 18.dp)
            ) {
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height * 0.78f)
                        lineTo(0f, size.height * 0.78f)
                        close()
                    },
                    color = hatStripeColor(index)
                )
                drawCircle(
                    color = if (index % 2 == 0) Color.Black else Color.White,
                    radius = size.minDimension * 0.11f,
                    center = Offset(size.width / 2f, size.height * 0.88f)
                )
            }
        }
    }
}

@Composable
private fun GameTableSurface(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        color = Color(0xFF6D4C41),
        border = BorderStroke(4.dp, Color(0xFF3E2723)),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF7B574B),
                            Color(0xFF6D4C41),
                            Color(0xFF5D4037)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            // Content goes here if needed
        }
    }
}

@Composable
private fun TableActionPanel(
    modifier: Modifier = Modifier,
    choice: Int,
    onChoiceSelected: (Int) -> Unit,
    choiceEnabled: Boolean,
    inputsEnabled: Boolean,
    pendingHumanAction: PendingHumanAction,
    onPassSelected: () -> Unit,
    onTargetSelected: () -> Unit,
    dropdownOptions: List<PlayerState>,
    selectedTargetId: Int?,
    onTargetPicked: (Int) -> Unit,
    needsSecondTarget: Boolean,
    secondDropdownOptions: List<PlayerState>,
    selectedSecondTargetId: Int?,
    onSecondTargetPicked: (Int) -> Unit,
    guess: Int,
    onGuessSelected: (Int) -> Unit,
    zeroGuessUnlocked: Boolean,
    forcedGuess: Int?,
    submitLabel: String,
    submitEnabled: Boolean,
    onSubmit: () -> Unit
) {
    val showChoiceButtons = choiceEnabled
    val showActionButtons = inputsEnabled && pendingHumanAction == PendingHumanAction.NONE
    val showTargeting = inputsEnabled && pendingHumanAction == PendingHumanAction.TARGET
    val targetsReady = selectedTargetId != null && (!needsSecondTarget || selectedSecondTargetId != null)
    val showGuessButtons = showTargeting && targetsReady

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xA6191919),
        border = BorderStroke(2.dp, Color(0x66FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showChoiceButtons) {
                Text("Choose your number", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallChoiceButton("0", selected = choice == 0, enabled = choiceEnabled) { onChoiceSelected(0) }
                    SmallChoiceButton("1", selected = choice == 1, enabled = choiceEnabled) { onChoiceSelected(1) }
                    SmallChoiceButton("3", selected = choice == 3, enabled = choiceEnabled) { onChoiceSelected(3) }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (showActionButtons) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPassSelected,
                        enabled = inputsEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF546E7A),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Pass") }

                    Button(
                        onClick = onTargetSelected,
                        enabled = inputsEnabled && dropdownOptions.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8D6E63),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Target") }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (showTargeting) {
                TargetDropdown(
                    options = dropdownOptions,
                    selectedTargetId = selectedTargetId,
                    enabled = dropdownOptions.isNotEmpty(),
                    label = "Target",
                    onSelect = onTargetPicked
                )

                if (needsSecondTarget && selectedTargetId != null) {
                    Spacer(Modifier.height(8.dp))
                    TargetDropdown(
                        options = secondDropdownOptions,
                        selectedTargetId = selectedSecondTargetId,
                        enabled = secondDropdownOptions.isNotEmpty(),
                        label = "Second target",
                        onSelect = onSecondTargetPicked
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            if (showGuessButtons) {
                Text("Choose their number", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (zeroGuessUnlocked) {
                        SmallChoiceButton(
                            "0",
                            selected = guess == 0,
                            enabled = forcedGuess == null
                        ) { onGuessSelected(0) }
                    }
                    SmallChoiceButton(
                        "1",
                        selected = guess == 1,
                        enabled = forcedGuess == null || forcedGuess == 1
                    ) { onGuessSelected(1) }
                    SmallChoiceButton(
                        "3",
                        selected = guess == 3,
                        enabled = forcedGuess == null || forcedGuess == 3
                    ) { onGuessSelected(3) }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onSubmit,
                enabled = submitEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF263238),
                    contentColor = Color.White
                )
            ) {
                OneLineButtonText(submitLabel)
            }
        }
    }
}

@Composable
private fun LogPanel(
    logText: String,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xEE101010),
        border = BorderStroke(2.dp, Color(0xFF795548))
    ) {
        Box(
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            Text(
                text = logText.ifBlank { "(log will appear here)" },
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                softWrap = true
            )
        }
    }
}

/// Single-contour cloud: avoids internal "circle outlines" being stroked over the text.
private val CloudButtonShape: GenericShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height

    // Handy percentages
    val left = 0.04f * w
    val right = 0.96f * w
    val top = 0.10f * h
    val bottom = 0.92f * h

    // Cloud "puff line" control heights
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
    // Homogenized: one consistent "storm grey" for all menu buttons
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
        lastEventKind = null,
        currentWeatherName = null,
        currentWeatherEffect = null,
        forcedGuessForHuman = null,
        mustTargetForHuman = false,
        requiresSecondTargetForHuman = false,
        hatHolderId = null
    )
}

private fun buildLogText(result: RoundResult, difficulty: Difficulty): String {
    val visibleEvents =
        if (difficulty == Difficulty.HARD) result.log.takeLast(6)
        else result.log

    return visibleEvents.asReversed().joinToString("\n") { it.text }.ifBlank { "" }
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
                    "Hard games: ${stats.hardGames} (wins ${stats.hardWins})\n\n"+
                    "(Stats do not track on Easy mode)",
            style = MaterialTheme.typography.bodyMedium
        )
    }

}

@Composable
private fun AchievementUnlockOverlay(
    popup: AchievementPopup,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "achievement_popup")
    val haloScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val sparklePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_phase"
    )
    val innerSparklePhase by transition.animateFloat(
        initialValue = (2f * Math.PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_sparkle_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.88f)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .alpha(0.95f)
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val auraCenter = Offset(centerX, centerY)
                    val base = minOf(size.width, size.height) * 0.50f * haloScale

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF79E8FF).copy(alpha = 0.28f),
                                Color(0xFF4F68FF).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = auraCenter,
                            radius = base * 2.2f
                        ),
                        radius = base * 2.2f,
                        center = auraCenter
                    )

                    drawCircle(
                        color = Color(0xFF7CCBFF).copy(alpha = 0.12f),
                        radius = base * 1.55f,
                        center = auraCenter
                    )

                    drawCircle(
                        color = Color(0xFFB8E6FF).copy(alpha = 0.08f),
                        radius = base * 1.85f,
                        center = auraCenter
                    )

                    val outerDotCount = 18
                    repeat(outerDotCount) { index ->
                        val angle =
                            sparklePhase + ((2f * Math.PI).toFloat() * index / outerDotCount.toFloat())
                        val radius = base * (1.02f + (index % 3) * 0.08f)
                        val x = centerX + cos(angle) * radius
                        val y = centerY + sin(angle) * radius * 0.72f

                        drawCircle(
                            color = Color(0xFF9ED8FF).copy(alpha = 0.72f - (index % 4) * 0.08f),
                            radius = 6.dp.toPx() + (index % 2) * 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    val innerDotCount = 12
                    repeat(innerDotCount) { index ->
                        val angle =
                            innerSparklePhase + ((2f * Math.PI).toFloat() * index / innerDotCount.toFloat())
                        val radius = base * (0.62f + (index % 2) * 0.06f)
                        val x = centerX + cos(angle) * radius
                        val y = centerY + sin(angle) * radius * 0.58f

                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f - (index % 3) * 0.07f),
                            radius = 3.5.dp.toPx() + (index % 2) * 1.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .shadow(28.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF101B38),
                    border = BorderStroke(
                        2.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8EEBFF),
                                Color(0xFF6D83FF),
                                Color(0xFFB2F7FF)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF11224A),
                                        Color(0xFF0B1430)
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ACHIEVEMENT UNLOCKED",
                            color = Color(0xFF9FEFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "*",
                            fontSize = 36.sp,
                            color = Color(0xFFEAFDFF)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = popup.title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = popup.desc,
                            color = Color(0xFFD6E8FF),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Tap anywhere to dismiss",
                            color = Color(0xFF8FB0D9),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsText(stats: PlayerStats) {
    Column {
        Spacer(Modifier.height(16.dp))
        Text("(Unlock on Normal or Hard)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        AchievementSectionHeader("Core")
        AchievementRow(stats.firstGameCompleted, "Get Your Feet Wet", "Complete a game")
        AchievementRow(stats.firstPerfectWin, "Perfect Puddler", "Win with 0 wrong guesses")
        AchievementRow(stats.reachedRound6, "Idle Hands", "Reach round 6")
        AchievementRow(stats.wonWith18Marbles, "Legal Limit", "Win with exactly 18 marbles")

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Difficulty")
        AchievementRow(stats.wonEasy, "Comp Stomp", "Win on Easy")
        AchievementRow(stats.wonNormal, "Pattern Finder", "Win on Normal")
        AchievementRow(stats.wonHard, "TR1CKL3!", "Win on Hard")
        AchievementRow(
            unlocked = stats.playedAllDifficulties,
            title = "Tourist",
            desc = "Play on Easy, Normal, and Hard",
            progress = "${stats.easyGames.coerceAtLeast(0)} / ${stats.normalGames.coerceAtLeast(0)} / ${stats.hardGames.coerceAtLeast(0)}"
        )

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Style")
        AchievementRow(stats.pacifistWin, "Conscientious Objector", "Win without guessing")
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
        AchievementRow(stats.drySeasonWin, "Dry Season", "Win without ever choosing 3")
        AchievementRow(stats.ghostCupWin, "Ghost Cup", "Win without being targeted")
        AchievementRow(stats.onARoll, "On a Roll", "3 correct guesses in a row")
        AchievementRow(stats.dumbLuck, "Dumb Luck", "Correctly guess a 3 in round 1")
        AchievementRow(stats.hatFinisher, "Hat Trick", "Win after starting because you had the Hat")
        AchievementRow(stats.caughtTheStrobe, "Caught the Strobe", "Correctly guess Strobe's 3 twice in one game")
        AchievementRow(stats.pushover, "Pushover", "Correctly guess Three-Pusher's 3 four times in one game")

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Zero")
        AchievementRow(stats.firstTheFool, "The Fool", "Get tricked by a 0")
        AchievementRow(stats.firstZeroTrap, "Zero Trap", "Trick a bot with your 0")
        AchievementRow(stats.zeroHeroUnlocked, "Zero Hero", "Unlock the ability to Guess 0!")

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Milestones")
        AchievementRow(
            unlocked = stats.won13thGame,
            title = "Trickle Pro",
            desc = "Win 13 games",
            progress = "${stats.totalWins}/13"
        )
        AchievementRow(
            unlocked = stats.won113thGame,
            title = "Trickle Champion",
            desc = "Win 113 games",
            progress = "${stats.totalWins}/113"
        )
        AchievementRow(
            unlocked = stats.won1113thGame,
            title = "Trickle God",
            desc = "Win 1113 games",
            progress = "${stats.totalWins}/1113"
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

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Weather")
        val unlockedWeather = stats.unlockedWeatherAchievements
        WeatherAchievements.perCard.forEach { def ->
            AchievementRow(
                unlocked = WeatherAchievements.unlocked(unlockedWeather, def.id),
                title = def.title,
                desc = def.desc
            )
        }
        AchievementRow(
            unlocked = stats.stormChaser,
            title = "Storm Chaser",
            desc = "Experience every weather card across completed games",
            progress = "${stats.seenWeatherIds.intersect(WeatherAchievements.allWeatherIds).size}/${WeatherAchievements.allWeatherIds.size}"
        )
    }
}

@Composable
private fun AchievementSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF0ADAFF),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(6.dp))
}

// -------------------- UI components --------------------
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
            text = if (unlocked) "O" else "X",
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
        Spacer(Modifier.height(6.dp))
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
    label: String = "You target",
    onSelect: (Int) -> Unit
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
            label = { Text(label) },
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
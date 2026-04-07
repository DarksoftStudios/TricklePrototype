package com.example.trickleprototype

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import com.example.trickleprototype.ui.theme.TricklePrototypeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
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

private enum class SplashStage {
    LOGO,
    VIDEO
}

private enum class IndicatorTone {
    NEUTRAL,
    GOOD,
    BAD,
    ALERT
}

private enum class TargetVisualState {
    NORMAL,
    SELECTABLE,
    SELECTED,
    DISABLED
}

@Immutable
private data class FloatingIndicator(
    val token: Long,
    val text: String,
    val tone: IndicatorTone
)

private enum class TableSeatSlot {
    PLAYER_HEAD,
    LEFT_TOP,
    LEFT_MID,
    LEFT_BOTTOM,
    RIGHT_TOP,
    RIGHT_MID,
    RIGHT_BOTTOM
}

@Immutable
private data class TableAnchor(
    val xFraction: Float,
    val yFraction: Float
) {
    fun toOffset(width: Float, height: Float): Offset {
        return Offset(width * xFraction, height * yFraction)
    }

    fun toOffset(size: Size): Offset {
        return toOffset(size.width, size.height)
    }
}

@Immutable
private data class TablePoint(
    val x: Float,
    val y: Float
)

private fun Rect.centerPoint(): TablePoint {
    return TablePoint(
        x = left + (width / 2f),
        y = top + (height / 2f)
    )
}

private fun Rect.cupLandingPoint(): TablePoint {
    return TablePoint(
        x = left + (width / 2f),
        y = top + (height * 0.46f)
    )
}

private fun Rect.bowlSpawnPoint(): TablePoint {
    return TablePoint(
        x = left + (width / 2f),
        y = top + (height * 0.38f)
    )
}

@Immutable
private data class TableLayoutAnchors(
    val bowlCenter: TableAnchor,
    val bowlSpawn: TableAnchor,
    val bowlHatRest: TableAnchor,
    val seatAnchors: Map<TableSeatSlot, TableAnchor>
) {
    fun seatAnchor(slot: TableSeatSlot): TableAnchor {
        return seatAnchors.getValue(slot)
    }
}

private fun defaultTableLayoutAnchors(): TableLayoutAnchors {
    return TableLayoutAnchors(
        bowlCenter = TableAnchor(
            xFraction = 0.50f,
            yFraction = 0.57f
        ),
        bowlSpawn = TableAnchor(
            xFraction = 0.50f,
            yFraction = 0.52f
        ),
        bowlHatRest = TableAnchor(
            xFraction = 0.50f,
            yFraction = 0.46f
        ),
        seatAnchors = mapOf(
            TableSeatSlot.PLAYER_HEAD to TableAnchor(0.50f, 0.16f),
            TableSeatSlot.LEFT_TOP to TableAnchor(0.16f, 0.27f),
            TableSeatSlot.LEFT_MID to TableAnchor(0.14f, 0.50f),
            TableSeatSlot.LEFT_BOTTOM to TableAnchor(0.17f, 0.74f),
            TableSeatSlot.RIGHT_TOP to TableAnchor(0.84f, 0.27f),
            TableSeatSlot.RIGHT_MID to TableAnchor(0.86f, 0.50f),
            TableSeatSlot.RIGHT_BOTTOM to TableAnchor(0.83f, 0.74f)
        )
    )
}

private fun leftSeatSlotForIndex(index: Int): TableSeatSlot {
    return when (index) {
        0 -> TableSeatSlot.LEFT_TOP
        1 -> TableSeatSlot.LEFT_MID
        else -> TableSeatSlot.LEFT_BOTTOM
    }
}

private fun rightSeatSlotForIndex(index: Int): TableSeatSlot {
    return when (index) {
        0 -> TableSeatSlot.RIGHT_TOP
        1 -> TableSeatSlot.RIGHT_MID
        else -> TableSeatSlot.RIGHT_BOTTOM
    }
}

@Immutable
private data class MarbleFlightVisual(
    val id: Long,
    val start: TablePoint,
    val end: TablePoint,
    val launchDelayMs: Int,
    val laneOffsetPx: Float
)

@Immutable
private data class TargetArrowVisual(
    val actorId: Int,
    val targetIds: List<Int>
)


private fun marbleTransferPoint(
    transferType: MarbleTransferEndpointType,
    playerId: Int?,
    bowlSpawnPoint: TablePoint?,
    cupCenters: Map<Int, TablePoint>
): TablePoint? {
    return when (transferType) {
        MarbleTransferEndpointType.BOWL -> bowlSpawnPoint
        MarbleTransferEndpointType.PLAYER -> cupCenters[playerId ?: GameEngine.HUMAN_ID]
    }
}

private fun buildMarbleFlights(
    transfers: List<MarbleTransferEvent>,
    bowlSpawnPoint: TablePoint?,
    cupCenters: Map<Int, TablePoint>,
    nextId: () -> Long
): List<MarbleFlightVisual> {
    if (bowlSpawnPoint == null) return emptyList()

    val flights = mutableListOf<MarbleFlightVisual>()

    transfers.forEach { transfer ->
        val startPoint = marbleTransferPoint(
            transferType = transfer.fromType,
            playerId = transfer.fromPlayerId,
            bowlSpawnPoint = bowlSpawnPoint,
            cupCenters = cupCenters
        ) ?: return@forEach

        val endPoint = marbleTransferPoint(
            transferType = transfer.toType,
            playerId = transfer.toPlayerId,
            bowlSpawnPoint = bowlSpawnPoint,
            cupCenters = cupCenters
        ) ?: return@forEach

        repeat(transfer.amount) { index ->
            flights += MarbleFlightVisual(
                id = nextId(),
                start = startPoint,
                end = endPoint,
                launchDelayMs = index * 170,
                laneOffsetPx = 0f
            )
        }
    }

    return flights
}

private fun indicatorToneColor(tone: IndicatorTone): Color {
    return when (tone) {
        IndicatorTone.NEUTRAL -> Color(0xFFFFEB3B)
        IndicatorTone.GOOD -> Color(0xFF2196F3)
        IndicatorTone.BAD -> Color(0xFFFF0228)
        IndicatorTone.ALERT -> Color(0xFFFF9800)
    }
}

private fun hatStripeColor(index: Int): Color {
    return if (index % 2 == 0) Color.White else Color.Black
}

private fun resolvePlayerIdByName(players: List<PlayerState>, rawName: String): Int? {
    val trimmed = rawName.trim().removeSuffix(".")
    return players.firstOrNull { it.baseName.equals(trimmed, ignoreCase = true) }?.id
}

private fun displayedChoiceFromLog(
    playerName: String,
    log: List<RoundLogEvent>
): Int? {
    val escapedName = Regex.escape(playerName)
    val revealedRegex = Regex("""^$escapedName is revealed: (0|1|3)$""")
    val choseRegex = Regex("""^$escapedName chose (0|1|3)\.$""")

    for (event in log.asReversed()) {
        val line = event.text.trim()

        revealedRegex.matchEntire(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { return it }

        choseRegex.matchEntire(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { return it }
    }

    return null
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
            pairFor(match.groupValues[1], "CORRECT", IndicatorTone.GOOD)
        )
    }

    Regex("^(.+?) was correct and gains (\\d+)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "CORRECT", IndicatorTone.GOOD)
        )
    }

    Regex("^(.+?) was wrong on a 0 and gives (\\d+) to (.+?) \\(HAT moves to .+?\\)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "WRONG", IndicatorTone.BAD)
        )
    }

    Regex("^(.+?) was wrong on a 0, loses (\\d+) \\(HAT moves to .+?\\)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "WRONG", IndicatorTone.BAD)
        )
    }

    Regex("^(.+?) was wrong on a 0, gains (\\d+) \\(HAT moves to .+?\\)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "WRONG", IndicatorTone.BAD)
        )
    }

    Regex("^(.+?) was wrong, (.+?) takes (\\d+) from them\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "WRONG", IndicatorTone.BAD)
        )
    }

    Regex("^(.+?) was wrong, (.+?) gains (\\d+)\\.$").matchEntire(line)?.let { match ->
        return listOfNotNull(
            pairFor(match.groupValues[1], "WRONG", IndicatorTone.BAD)
        )
    }

    return emptyList()
}

private fun parseAchievementPopup(line: String): AchievementPopup? {
    val marker = "Achievement Unlocked:"
    val idx = line.indexOf(marker)
    if (idx == -1) return null

    val payload = line.substring(idx + marker.length)
        .trim()
        .trim('*')
        .trim()

    val parts = payload.split(" - ", limit = 2)

    val rawTitle = parts.getOrNull(0) ?: return null
    val rawDesc = parts.getOrNull(1) ?: ""

    val title = rawTitle.trim().trim('*').trim()
    val desc = rawDesc.trim().trim('*').trim()

    if (title.isBlank()) return null

    return AchievementPopup(title = title, desc = desc)
}

@Composable
private fun TrickleApp() {
    val engine = remember { GameEngine() }

    val context = LocalContext.current
    val activity = context as? Activity
    val appContext = context.applicationContext
    val statsStore = remember { StatsStore(appContext) }
    val settingsPrefs = remember {
        appContext.getSharedPreferences("trickle_settings", ComponentActivity.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(AppScreen.SPLASH) }
    var difficultyEntryTransitionActive by remember { mutableStateOf(false) }
    val difficultyEntryZoom = remember { Animatable(1f) }
    val difficultyEntryFade = remember { Animatable(0f) }
    val uiContentFade = remember { Animatable(1f) }

    var showResetStatsConfirm by remember { mutableStateOf(false) }
    var showQuitConfirm by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("sound_enabled", true)) }
    var musicEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("music_enabled", true)) }
    var passTargetConfirmEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("pass_target_confirm_enabled", false)) }

    var playerName by remember { mutableStateOf(statsStore.getPlayerName()) }

    SideEffect {
        engine.setHumanName(playerName)
    }

    SideEffect {
        engine.attachStatsStore(statsStore)
    }

    var difficulty by remember { mutableStateOf<Difficulty?>(null) }
    var weatherEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("weather_enabled", false)) }

    var choice by remember { mutableIntStateOf(1) }
    var targetId by remember { mutableStateOf<Int?>(null) }
    var secondTargetId by remember { mutableStateOf<Int?>(null) }
    var guess by remember { mutableIntStateOf(3) }
    var pendingHumanAction by remember { mutableStateOf(PendingHumanAction.NONE) }
    var showLogOverlay by remember { mutableStateOf(false) }

    var dieButtonsEnabled by remember { mutableStateOf(true) }

    var lastResult by remember { mutableStateOf<RoundResult?>(null) }
    var logText by remember { mutableStateOf("") }
    var floatingIndicators by remember { mutableStateOf<Map<Int, FloatingIndicator>>(emptyMap()) }
    var nextIndicatorToken by remember { mutableStateOf(1L) }
    var marbleFlights by remember { mutableStateOf<List<MarbleFlightVisual>>(emptyList()) }
    var activeTargetArrow by remember { mutableStateOf<TargetArrowVisual?>(null) }
    var nextMarbleFlightId by remember { mutableLongStateOf(1L) }
    var bowlSpawnPoint by remember { mutableStateOf<TablePoint?>(null) }
    val cupCenters = remember { mutableStateMapOf<Int, TablePoint>() }
    var lastQueuedMarbleTransferSignature by remember { mutableStateOf("") }

    var achievementQueue by remember { mutableStateOf<List<AchievementPopup>>(emptyList()) }
    var activeAchievement by remember { mutableStateOf<AchievementPopup?>(null) }
    var splashSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var introVideoPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var introVideoView by remember { mutableStateOf<VideoView?>(null) }
    var splashStage by remember { mutableStateOf(SplashStage.LOGO) }

    var splashFadeTarget by remember { mutableStateOf(0f) }
    var splashTransitionLocked by remember { mutableStateOf(false) }
    val splashFadeAlpha by animateFloatAsState(
        targetValue = splashFadeTarget,
        animationSpec = tween(durationMillis = 260),
        label = "splashFadeAlpha"
    )

    var weatherBadgeCenter by remember { mutableStateOf<TablePoint?>(null) }
    var gameAreaCenter by remember { mutableStateOf<TablePoint?>(null) }
    var activeWeatherOverlayId by remember { mutableStateOf<String?>(null) }
    var activeWeatherOverlayToken by remember { mutableLongStateOf(0L) }
    var lastAnimatedWeatherName by remember { mutableStateOf<String?>(null) }

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
            splashSoundPlayer = null
            introVideoView?.stopPlayback()
            introVideoView = null
            introVideoPlayer?.release()
            introVideoPlayer = null
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
            introVideoPlayer?.setVolume(0f, 0f)
        } else {
            introVideoPlayer?.setVolume(1f, 1f)
        }
    }

    LaunchedEffect(musicEnabled) {
        settingsPrefs.edit().putBoolean("music_enabled", musicEnabled).apply()
    }

    LaunchedEffect(passTargetConfirmEnabled) {
        settingsPrefs.edit().putBoolean("pass_target_confirm_enabled", passTargetConfirmEnabled).apply()
    }

    LaunchedEffect(weatherEnabled) {
        settingsPrefs.edit().putBoolean("weather_enabled", weatherEnabled).apply()
    }

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

    var humanActionLocked by remember { mutableStateOf(false) }
    var startLocked by remember { mutableStateOf(false) }

    var turbo by remember { mutableStateOf(false) }

    var turboOnColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    val turboPalette = remember {
        listOf(
            Color(0xFFC5DAFF),
            Color(0xFF159DF8),
            Color(0xFF0D47A1)
        )
    }

    var menuVisitKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(difficulty) {
        if (difficulty == null) menuVisitKey += 1
    }

    LaunchedEffect(screen, difficultyEntryTransitionActive) {
        if (screen == AppScreen.SPLASH) {
            uiContentFade.snapTo(1f)
            return@LaunchedEffect
        }

        if (difficultyEntryTransitionActive) {
            uiContentFade.snapTo(0f)
            return@LaunchedEffect
        }

        uiContentFade.snapTo(0f)
        uiContentFade.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = LinearEasing)
        )
    }

    val phase = lastResult?.phase ?: engine.getPhase()
    val displayedRoundFromEngine = lastResult?.roundNumber ?: engine.getRoundNumber()
    val players = lastResult?.players ?: engine.getPlayersSnapshot()
    val currentActorId = lastResult?.currentActorId
    val gameOver = (phase == EnginePhase.GAME_OVER)
    val currentWeatherName = lastResult?.currentWeatherName
    val currentWeatherEffect = lastResult?.currentWeatherEffect
    val currentWeatherId = weatherIdForName(currentWeatherName)
    val forcedGuess = lastResult?.forcedGuessForHuman
    val mustTarget = lastResult?.mustTargetForHuman == true
    val needsSecondTarget = lastResult?.requiresSecondTargetForHuman == true
    val activeTargetArrowFromResult = lastResult?.activeTargetArrowActorId?.let { actorId ->
        val targetIds = lastResult?.activeTargetArrowTargetIds.orEmpty()
        if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(logText) { scrollState.scrollTo(0) }

    LaunchedEffect(currentWeatherName, displayedRoundFromEngine, screen) {
        if (screen != AppScreen.GAME) return@LaunchedEffect

        if (currentWeatherName.isNullOrBlank()) {
            lastAnimatedWeatherName = null
            activeWeatherOverlayId = null
            return@LaunchedEffect
        }

        if (displayedRoundFromEngine <= 1) {
            lastAnimatedWeatherName = currentWeatherName
            activeWeatherOverlayId = null
            return@LaunchedEffect
        }

        if (currentWeatherName != lastAnimatedWeatherName) {
            activeWeatherOverlayId = weatherIdForName(currentWeatherName)
            activeWeatherOverlayToken += 1L
            lastAnimatedWeatherName = currentWeatherName
        }
    }


    fun resetRunUiState(clearDifficulty: Boolean) {
        if (clearDifficulty) {
            difficulty = null
        }

        lastResult = null
        logText = ""
        floatingIndicators = emptyMap()
        marbleFlights = emptyList()
        activeTargetArrow = null
        lastQueuedMarbleTransferSignature = ""
        achievementQueue = emptyList()
        activeAchievement = null
        weatherBadgeCenter = null
        gameAreaCenter = null
        activeWeatherOverlayId = null
        activeWeatherOverlayToken = 0L
        lastAnimatedWeatherName = null

        choice = 1
        targetId = null
        secondTargetId = null
        guess = 3
        pendingHumanAction = PendingHumanAction.NONE
        showLogOverlay = false
        showQuitConfirm = false

        humanActionLocked = false
        startLocked = false
    }

    fun startGameSession(picked: Difficulty, animateEntry: Boolean) {
        engine.reset()
        engine.attachStatsStore(statsStore)
        difficulty = picked
        engine.setDifficulty(picked)
        engine.setWeatherEnabled(weatherEnabled)

        val snap = engineSnapshot(engine)
        lastResult = snap
        logText = buildLogText(snap, picked)

        floatingIndicators = emptyMap()
        marbleFlights = emptyList()
        activeTargetArrow = null
        lastQueuedMarbleTransferSignature = ""
        achievementQueue = emptyList()
        activeAchievement = null

        choice = 1
        targetId = null
        secondTargetId = null
        guess = 3
        pendingHumanAction = PendingHumanAction.NONE
        showLogOverlay = false
        showQuitConfirm = false

        humanActionLocked = false
        startLocked = false
        screen = AppScreen.GAME

        if (!animateEntry) {
            difficultyEntryTransitionActive = false
            return
        }

        scope.launch {
            difficultyEntryTransitionActive = true
            difficultyEntryZoom.snapTo(1f)
            difficultyEntryFade.snapTo(0f)

            launch {
                difficultyEntryZoom.animateTo(
                    targetValue = 1.42f,
                    animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
                )
            }

            delay(300L)

            difficultyEntryFade.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = LinearEasing)
            )

            delay(140L)
            difficultyEntryTransitionActive = false
            difficultyEntryZoom.snapTo(1f)
            difficultyEntryFade.snapTo(0f)
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

    LaunchedEffect(lastResult, bowlSpawnPoint, cupCenters.size) {
        val result = lastResult ?: return@LaunchedEffect
        val transfers = result.marbleTransfers
        if (transfers.isEmpty()) return@LaunchedEffect

        val transferSignature = buildString {
            append(result.log.size)
            append("|")
            append(result.currentActorId ?: -1)
            append("|")
            append(result.phase.name)
            transfers.forEach { transfer ->
                append("|")
                append(transfer.fromType.name)
                append(":")
                append(transfer.fromPlayerId ?: -1)
                append("->")
                append(transfer.toType.name)
                append(":")
                append(transfer.toPlayerId ?: -1)
                append(":")
                append(transfer.amount)
            }
        }

        if (transferSignature == lastQueuedMarbleTransferSignature) {
            return@LaunchedEffect
        }

        val newFlights = buildMarbleFlights(
            transfers = transfers,
            bowlSpawnPoint = bowlSpawnPoint,
            cupCenters = cupCenters,
            nextId = {
                val next = nextMarbleFlightId
                nextMarbleFlightId += 1L
                next
            }
        )

        if (newFlights.isEmpty()) {
            return@LaunchedEffect
        }

        lastQueuedMarbleTransferSignature = transferSignature
        marbleFlights = marbleFlights + newFlights
    }

    LaunchedEffect(phase, turbo) {
        if (phase == EnginePhase.BOT_TURN) {
            while (true) {
                val current = lastResult?.phase ?: engine.getPhase()
                if (current != EnginePhase.BOT_TURN) break

                val result = engine.step()
                lastResult = result
                logText = buildLogText(result, difficulty!!)
                activeTargetArrow = result.activeTargetArrowActorId?.let { actorId ->
                    val targetIds = result.activeTargetArrowTargetIds
                    if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
                }

                if (result.phase == EnginePhase.BOT_TURN) {
                    val base = if (result.lastEventKind == LogEventKind.PASS) 300L else 1000L
                    val baseDelayMs = if (turbo) (base / 4).coerceAtLeast(35L) else base

                    val marbleAnimationDelayMs =
                        result.marbleTransfers.maxOfOrNull { transfer ->
                            ((transfer.amount - 1).coerceAtLeast(0) * 170L) + 520L
                        } ?: 0L

                    val totalDelayMs = maxOf(baseDelayMs, marbleAnimationDelayMs + 80L)
                    delay(totalDelayMs)
                }
            }
        }

        if (phase != EnginePhase.PLAYER_TURN) humanActionLocked = false
        if (phase != EnginePhase.SELECT && phase != EnginePhase.ROUND_END) startLocked = false
    }

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

    fun finishSplashIntro() {
        if (splashTransitionLocked) return
        splashTransitionLocked = true

        scope.launch {
            splashSoundPlayer?.stop()
            splashSoundPlayer?.release()
            splashSoundPlayer = null

            introVideoPlayer?.setOnCompletionListener(null)
            introVideoView?.stopPlayback()
            introVideoView = null

            introVideoPlayer?.release()
            introVideoPlayer = null

            splashFadeTarget = 1f
            delay(280)

            splashStage = SplashStage.LOGO
            screen = AppScreen.MAIN_MENU

            delay(40)

            splashFadeTarget = 0f
            delay(260)

            splashTransitionLocked = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (screen != AppScreen.SPLASH) {
            val backgroundResId = when (screen) {
                AppScreen.GAME -> R.drawable.gamescreengen
                else -> R.drawable.mainmenugen
            }

            Image(
                painter = painterResource(backgroundResId),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (difficultyEntryTransitionActive) {
                DifficultyEntryTransitionOverlay(
                    zoom = difficultyEntryZoom.value,
                    nextImageAlpha = difficultyEntryFade.value,
                    currentImageRes = R.drawable.mainmenugen,
                    nextImageRes = R.drawable.gamescreengen
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp)
                .alpha(uiContentFade.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (difficultyEntryTransitionActive) {
                return@Column
            }

            if (screen != AppScreen.SPLASH) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (difficulty != null) {
                        Button(
                            onClick = {
                                if (gameOver) {
                                    engine.reset()
                                    engine.attachStatsStore(statsStore)
                                    resetRunUiState(clearDifficulty = true)
                                    screen = AppScreen.MAIN_MENU
                                } else {
                                    showQuitConfirm = true
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterStart),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A6A6A),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = if (gameOver) "Main Menu" else "Quit", maxLines = 1)
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

            Spacer(Modifier.height(4.dp))

            when (screen) {
                AppScreen.SPLASH -> {
                    LaunchedEffect(splashStage) {
                        if (splashStage != SplashStage.LOGO) return@LaunchedEffect

                        introVideoView?.stopPlayback()
                        introVideoView = null
                        introVideoPlayer?.release()
                        introVideoPlayer = null

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

                        splashSoundPlayer?.stop()
                        splashSoundPlayer?.release()
                        splashSoundPlayer = null

                        splashStage = SplashStage.VIDEO
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                finishSplashIntro()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (splashStage == SplashStage.LOGO) {
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
                        } else {
                            AndroidView(
                                factory = { viewContext ->
                                    VideoView(viewContext).apply {
                                        introVideoView = this

                                        val videoUri = Uri.parse(
                                            "android.resource://${viewContext.packageName}/${R.raw.introvidgen}"
                                        )

                                        setVideoURI(videoUri)

                                        setOnPreparedListener { mediaPlayer ->
                                            introVideoPlayer?.release()
                                            introVideoPlayer = mediaPlayer

                                            mediaPlayer.isLooping = false
                                            if (soundEnabled) {
                                                mediaPlayer.setVolume(1f, 1f)
                                            } else {
                                                mediaPlayer.setVolume(0f, 0f)
                                            }

                                            start()
                                        }

                                        setOnCompletionListener {
                                            finishSplashIntro()
                                        }

                                        setOnErrorListener { _, _, _ ->
                                            finishSplashIntro()
                                            true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { videoView ->
                                    introVideoView = videoView

                                    if (!videoView.isPlaying && !splashTransitionLocked) {
                                        videoView.start()
                                    }
                                }
                            )
                        }
                    }
                    return@Column
                }

                AppScreen.MAIN_MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MenuLinkButton(text = "PLAY") { screen = AppScreen.PLAY }
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "RULES") { screen = AppScreen.RULES }
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "PROFILE") { screen = AppScreen.PROFILE }
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "SETTINGS") { screen = AppScreen.SETTINGS }
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "QUIT") { activity?.finish() }

                        Spacer(Modifier.height(28.dp))
                    }
                    return@Column
                }

                AppScreen.PLAY -> {
                    val stats = statsStore.load()
                    val normalUnlocked = stats.easyGames > 0
                    val hardUnlocked = stats.normalWins > 0
                    val weatherUnlocked = stats.wonHard

                    fun startGame(picked: Difficulty) {
                        startGameSession(picked = picked, animateEntry = true)
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 72.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(35.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MenuLinkButton(
                                text = when {
                                    !weatherUnlocked -> "WEATHER (LOCKED)"
                                    weatherEnabled -> "WEATHER: ON"
                                    else -> "WEATHER: OFF"
                                }
                            ) {
                                if (weatherUnlocked) {
                                    weatherEnabled = !weatherEnabled
                                } else {
                                    Toast.makeText(context, "Win on HARD to unlock WEATHER", Toast.LENGTH_SHORT).show()
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            MenuLinkButton(text = "EASY") { startGame(Difficulty.EASY) }

                            Spacer(Modifier.height(4.dp))
                            MenuLinkButton(
                                text = if (normalUnlocked) "NORMAL" else "NORMAL (LOCKED)"
                            ) {
                                if (normalUnlocked) {
                                    startGame(Difficulty.NORMAL)
                                } else {
                                    Toast.makeText(context, "Play a game on EASY to unlock NORMAL", Toast.LENGTH_SHORT).show()
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            MenuLinkButton(
                                text = if (hardUnlocked) "HARD" else "HARD (LOCKED)"
                            ) {
                                if (hardUnlocked) {
                                    startGame(Difficulty.HARD)
                                } else {
                                    Toast.makeText(context, "Beat a game on NORMAL to unlock HARD", Toast.LENGTH_SHORT).show()
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            MenuLinkButton(text = "BACK") { screen = AppScreen.MAIN_MENU }

                            Spacer(Modifier.height(16.dp))
                        }
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
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "ADVANCED TIPS") { showTips = true }
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "ARCHETYPES") { showArchetypes = true }

                        Spacer(Modifier.height(16.dp))
                        MenuLinkButton(text = "BACK") { screen = AppScreen.MAIN_MENU }

                        Spacer(Modifier.height(14.dp))
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
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(text = "ACHIEVEMENTS") { showAchievements = true }
                        Spacer(Modifier.height(4.dp))
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
                        Spacer(Modifier.height(4.dp))
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
                        Spacer(Modifier.height(4.dp))
                        MenuLinkButton(
                            text = if (passTargetConfirmEnabled) "PASS CONFIRM: ON" else "PASS CONFIRM: OFF"
                        ) {
                            passTargetConfirmEnabled = !passTargetConfirmEnabled
                        }
                        Spacer(Modifier.height(4.dp))
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
                }
            }

            if (difficulty == null) {
                screen = AppScreen.MAIN_MENU
                return@Column
            }

            val humanPlayer = players.firstOrNull { it.id == GameEngine.HUMAN_ID }
            val playerScore = humanPlayer?.marbles ?: 0
            val playerTitle = humanPlayer?.baseName ?: "Player"

            val isPlayerTurn = (phase == EnginePhase.PLAYER_TURN)
            val inputsEnabled = !gameOver && isPlayerTurn && !humanActionLocked

            val dropdownOptions = if (isPlayerTurn) {
                val targetable = lastResult?.targetableIdsForHuman ?: emptyList()
                players.filter { it.id in targetable && it.id != GameEngine.HUMAN_ID }
            } else {
                emptyList()
            }

            val hasValidTargets = dropdownOptions.isNotEmpty()
            val canPassThisTurn = !mustTarget || !hasValidTargets
            val secondDropdownOptions = dropdownOptions.filter { it.id != targetId }
            val isHumanTargetingSelectionActive =
                inputsEnabled && pendingHumanAction == PendingHumanAction.TARGET
            val primaryTargetableIds = dropdownOptions.map { it.id }.toSet()
            val secondaryTargetableIds = secondDropdownOptions.map { it.id }.toSet()

            fun visualStateForTargetablePlayer(playerId: Int): TargetVisualState {
                if (!isHumanTargetingSelectionActive) return TargetVisualState.NORMAL
                if (playerId == targetId || (needsSecondTarget && playerId == secondTargetId)) {
                    return TargetVisualState.SELECTED
                }

                val legalIds = if (needsSecondTarget && targetId != null) {
                    secondaryTargetableIds
                } else {
                    primaryTargetableIds
                }

                return if (playerId in legalIds) {
                    TargetVisualState.SELECTABLE
                } else {
                    TargetVisualState.DISABLED
                }
            }

            fun handleTargetSeatClick(clickedPlayerId: Int) {
                if (!isHumanTargetingSelectionActive) return

                if (!needsSecondTarget) {
                    if (clickedPlayerId == targetId) {
                        targetId = null
                    } else if (clickedPlayerId in primaryTargetableIds) {
                        targetId = clickedPlayerId
                    }
                    return
                }

                if (targetId == null) {
                    if (clickedPlayerId in primaryTargetableIds) {
                        targetId = clickedPlayerId
                        secondTargetId = null
                    }
                    return
                }

                if (clickedPlayerId == targetId) {
                    targetId = null
                    secondTargetId = null
                    return
                }

                if (clickedPlayerId == secondTargetId) {
                    secondTargetId = null
                    return
                }

                if (clickedPlayerId in secondaryTargetableIds) {
                    secondTargetId = clickedPlayerId
                }
            }

            val previewTargetIds = buildList {
                targetId?.let { add(it) }
                if (needsSecondTarget) {
                    secondTargetId?.let { add(it) }
                }
            }
            val displayTargetArrow = when {
                pendingHumanAction == PendingHumanAction.TARGET && previewTargetIds.isNotEmpty() -> {
                    TargetArrowVisual(actorId = GameEngine.HUMAN_ID, targetIds = previewTargetIds)
                }
                else -> activeTargetArrow ?: activeTargetArrowFromResult
            }

            val zeroGuessUnlocked = statsStore.load().zeroHeroUnlocked

            val winnerBadgeLabel = buildWinnerBadgeLabel(lastResult)

            val playerPhaseBadge = phaseBadgeText(
                roundNumber = displayedRoundFromEngine,
                enginePhase = phase,
                pendingHumanAction = pendingHumanAction,
                humanActionLocked = humanActionLocked,
                targetId = targetId,
                secondTargetId = secondTargetId,
                needsSecondTarget = needsSecondTarget,
                latestLogLine = lastResult?.log?.lastOrNull()?.text,
                passTargetConfirmEnabled = passTargetConfirmEnabled,
                canPass = canPassThisTurn,
                hasTargets = hasValidTargets,
                roundResult = lastResult
            )
            fun submitPlayerTurn(
                selectedTargetId: Int?,
                selectedGuess: Int?,
                selectedSecondTargetId: Int?
            ) {
                if (selectedTargetId != null) {
                    activeTargetArrow = TargetArrowVisual(
                        actorId = GameEngine.HUMAN_ID,
                        targetIds = buildList {
                            add(selectedTargetId)
                            selectedSecondTargetId?.let { add(it) }
                        }
                    )
                } else {
                    activeTargetArrow = null
                }

                humanActionLocked = true
                val result = engine.submitHumanTurn(
                    targetId = selectedTargetId,
                    guess = selectedGuess,
                    secondTargetId = selectedSecondTargetId
                )
                lastResult = result
                logText = buildLogText(result, difficulty!!)
                activeTargetArrow = result.activeTargetArrowActorId?.let { actorId ->
                    val targetIds = result.activeTargetArrowTargetIds
                    if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
                }

                if (result.phase == EnginePhase.PLAYER_TURN) {
                    humanActionLocked = false

                    val stillHasValidTargets = result.targetableIdsForHuman
                        .orEmpty()
                        .any { it != GameEngine.HUMAN_ID }
                    val stillCanPass = result.mustTargetForHuman != true || !stillHasValidTargets

                    if (!passTargetConfirmEnabled || !stillCanPass) {
                        pendingHumanAction = PendingHumanAction.NONE
                    }

                    if (pendingHumanAction != PendingHumanAction.TARGET) {
                        targetId = null
                        secondTargetId = null
                    }
                } else {
                    pendingHumanAction = PendingHumanAction.NONE
                    targetId = null
                    secondTargetId = null
                }
            }
            val tableBots = players.filter { it.id != GameEngine.HUMAN_ID }
            val splitIndex = (tableBots.size + 1) / 2
            val rightBots = tableBots.take(splitIndex)
            val leftBots = tableBots.drop(splitIndex).reversed()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        gameAreaCenter = coordinates.boundsInRoot().centerPoint()
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .wrapContentHeight()
                            .padding(horizontal = 0.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeatherInfoBadge(
                            weatherName = currentWeatherName,
                            weatherEffect = currentWeatherEffect,
                            weatherId = currentWeatherId,
                            forcedGuess = forcedGuess,
                            mustTarget = mustTarget,
                            needsSecondTarget = needsSecondTarget,
                            onBadgeAnchorMeasured = { anchor ->
                                weatherBadgeCenter = anchor
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )

                        PhaseBadge(
                            text = playerPhaseBadge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )

                        PlayerStatusStack(
                            playerTitle = playerTitle,
                            playerScore = playerScore,
                            isCurrentTurn = currentActorId == GameEngine.HUMAN_ID,
                            indicator = floatingIndicators[GameEngine.HUMAN_ID],
                            hasHat = lastResult?.hatHolderId == GameEngine.HUMAN_ID,
                            isStarter = lastResult?.currentStarterId == GameEngine.HUMAN_ID,
                            displayedChoice = lastResult?.players?.firstOrNull { it.id == GameEngine.HUMAN_ID }?.revealedChoice,
                            targetVisualState = TargetVisualState.NORMAL,
                            onTargetClick = null,
                            onCupAnchorMeasured = { playerId, anchor ->
                                cupCenters[playerId] = anchor
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        GameTableSurface(
                            onBowlSpawnMeasured = { measuredAnchor ->
                                bowlSpawnPoint = measuredAnchor
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.64f)
                                .height(660.dp)
                        )

                        BotCupColumn(
                            bots = leftBots,
                            currentActorId = currentActorId,
                            hatHolderId = lastResult?.hatHolderId,
                            starterId = lastResult?.currentStarterId,
                            indicators = floatingIndicators,
                            showMarbleCounts = difficulty == Difficulty.EASY,
                            targetVisualStateForBot = { botId -> visualStateForTargetablePlayer(botId) },
                            onBotClicked = { botId -> handleTargetSeatClick(botId) },
                            onCupAnchorMeasured = { playerId, anchor ->
                                cupCenters[playerId] = anchor
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .padding(start = 0.dp, top = 0.dp, bottom = 8.dp)
                        )

                        BotCupColumn(
                            bots = rightBots,
                            currentActorId = currentActorId,
                            hatHolderId = lastResult?.hatHolderId,
                            starterId = lastResult?.currentStarterId,
                            indicators = floatingIndicators,
                            showMarbleCounts = difficulty == Difficulty.EASY,
                            targetVisualStateForBot = { botId -> visualStateForTargetablePlayer(botId) },
                            onBotClicked = { botId -> handleTargetSeatClick(botId) },
                            onCupAnchorMeasured = { playerId, anchor ->
                                cupCenters[playerId] = anchor
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 0.dp, top = 0.dp, bottom = 8.dp)
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
                            canPass = canPassThisTurn,
                            onPassSelected = {
                                if (!canPassThisTurn) {
                                    pendingHumanAction = PendingHumanAction.NONE
                                    targetId = null
                                    secondTargetId = null
                                } else {
                                    targetId = null
                                    secondTargetId = null
                                    if (passTargetConfirmEnabled) {
                                        pendingHumanAction = PendingHumanAction.PASS
                                    } else {
                                        pendingHumanAction = PendingHumanAction.NONE
                                        submitPlayerTurn(
                                            selectedTargetId = null,
                                            selectedGuess = null,
                                            selectedSecondTargetId = null
                                        )
                                    }
                                }
                            },
                            onTargetSelected = {
                                targetId = null
                                secondTargetId = null
                                pendingHumanAction = PendingHumanAction.TARGET
                            },
                            onConfirmPass = {
                                if (!canPassThisTurn) {
                                    pendingHumanAction = PendingHumanAction.NONE
                                    targetId = null
                                    secondTargetId = null
                                } else {
                                    targetId = null
                                    secondTargetId = null
                                    pendingHumanAction = PendingHumanAction.PASS
                                    submitPlayerTurn(
                                        selectedTargetId = null,
                                        selectedGuess = null,
                                        selectedSecondTargetId = null
                                    )
                                }
                            },
                            onTargetInstead = {
                                targetId = null
                                secondTargetId = null
                                pendingHumanAction = PendingHumanAction.TARGET
                            },
                            onPassInstead = {
                                if (mustTarget) {
                                    pendingHumanAction = PendingHumanAction.TARGET
                                } else {
                                    targetId = null
                                    secondTargetId = null
                                    pendingHumanAction = PendingHumanAction.PASS
                                }
                            },
                            dropdownOptions = dropdownOptions,
                            selectedTargetId = targetId,
                            onTargetPicked = { pickedTargetId ->
                                if (pickedTargetId < 0) {
                                    targetId = null
                                    secondTargetId = null
                                } else {
                                    targetId = pickedTargetId
                                    if (secondTargetId == pickedTargetId) secondTargetId = null
                                }
                            },
                            needsSecondTarget = needsSecondTarget,
                            secondDropdownOptions = secondDropdownOptions,
                            selectedSecondTargetId = secondTargetId,
                            onSecondTargetPicked = { pickedSecondTargetId ->
                                secondTargetId = if (pickedSecondTargetId < 0) null else pickedSecondTargetId
                            },
                            guess = guess,
                            onGuessSelected = { selectedGuess ->
                                guess = selectedGuess
                            },
                            zeroGuessUnlocked = zeroGuessUnlocked,
                            forcedGuess = forcedGuess,
                            showSubmitButton = phase != EnginePhase.PLAYER_TURN || (
                                    phase == EnginePhase.PLAYER_TURN &&
                                            pendingHumanAction == PendingHumanAction.TARGET &&
                                            targetId != null &&
                                            (!needsSecondTarget || secondTargetId != null)
                                    ),
                            submitLabel = when (phase) {
                                EnginePhase.SELECT, EnginePhase.ROUND_END -> "Start Round->"
                                EnginePhase.PLAYER_TURN -> "Submit Guess->"
                                EnginePhase.BOT_TURN -> "Bots Acting..."
                                EnginePhase.GAME_OVER -> "Play Again"
                                EnginePhase.SETUP -> "Setup..."
                            },
                            submitEnabled = when (phase) {
                                EnginePhase.SELECT, EnginePhase.ROUND_END -> !gameOver && !startLocked
                                EnginePhase.PLAYER_TURN -> (
                                        !gameOver &&
                                                !humanActionLocked &&
                                                pendingHumanAction == PendingHumanAction.TARGET &&
                                                targetId != null &&
                                                (!needsSecondTarget || secondTargetId != null)
                                        )
                                EnginePhase.GAME_OVER -> difficulty != null
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
                                        activeTargetArrow = result.activeTargetArrowActorId?.let { actorId ->
                                            val targetIds = result.activeTargetArrowTargetIds
                                            if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
                                        }
                                        targetId = null
                                        secondTargetId = null
                                    }

                                    EnginePhase.PLAYER_TURN -> {
                                        submitPlayerTurn(
                                            selectedTargetId = targetId,
                                            selectedGuess = guess,
                                            selectedSecondTargetId = secondTargetId
                                        )
                                    }

                                    EnginePhase.GAME_OVER -> {
                                        difficulty?.let { picked ->
                                            startGameSession(picked = picked, animateEntry = false)
                                        }
                                    }

                                    else -> Unit
                                }
                            }
                        )

                        if (difficulty != Difficulty.HARD && showLogOverlay) {
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

                        if (difficulty != Difficulty.HARD) {
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

                WeatherTransitionOverlay(
                    weatherId = activeWeatherOverlayId,
                    animationToken = activeWeatherOverlayToken,
                    fromAnchor = gameAreaCenter,
                    toAnchor = weatherBadgeCenter,
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(5f)
                )

                displayTargetArrow?.let { arrow ->
                    TargetArrowOverlay(
                        arrow = arrow,
                        cupCenters = cupCenters,
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(5.5f)
                    )
                }

                MarbleFlightOverlay(
                    flights = marbleFlights,
                    onFlightFinished = { flightId ->
                        marbleFlights = marbleFlights.filterNot { it.id == flightId }
                    },
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(6f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = splashFadeAlpha))
                .zIndex(100f)
        )
    }

    if (showQuitConfirm) {
        AlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            title = {
                Text("Are you sure?")
            },
            text = {
                Text("You will lose all marbles and progress towards achievements.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        engine.reset()
                        engine.attachStatsStore(statsStore)
                        resetRunUiState(clearDifficulty = true)
                        screen = AppScreen.MAIN_MENU
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuitConfirm = false }
                ) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
        )
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
    latestLogLine: String?,
    passTargetConfirmEnabled: Boolean,
    canPass: Boolean,
    hasTargets: Boolean,
    roundResult: RoundResult?
): String {
    val isTrickling = enginePhase == EnginePhase.BOT_TURN && (
            latestLogLine?.startsWith("TRICKLE") == true ||
                    latestLogLine?.contains("trickles", ignoreCase = true) == true ||
                    latestLogLine?.contains("Trickle obscured", ignoreCase = true) == true
            )

    if (enginePhase == EnginePhase.GAME_OVER) {
        val winnerLabel = buildWinnerBadgeLabel(roundResult)
        return if (!winnerLabel.isNullOrBlank()) {
            "Game Over, $winnerLabel wins!"
        } else {
            "Game Over"
        }
    }

    val instruction = when (enginePhase) {
        EnginePhase.SELECT, EnginePhase.ROUND_END -> "Choose 0, 1, or 3."
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
                PendingHumanAction.NONE -> {
                    when {
                        !hasTargets && canPass -> "Pass"
                        canPass -> "Pass or target"
                        else -> "Choose your target"
                    }
                }
                PendingHumanAction.PASS -> {
                    if (!canPass) {
                        "Choose your target"
                    } else if (passTargetConfirmEnabled) {
                        if (hasTargets) "Confirm pass or target instead" else "Confirm pass"
                    } else {
                        "Wait for next round"
                    }
                }
                PendingHumanAction.TARGET -> {
                    val targetReady = targetId != null && (!needsSecondTarget || secondTargetId != null)
                    if (targetReady) "Choose their number" else "Choose your target"
                }
            }
        }
        EnginePhase.SETUP -> "Choose 0, 1, or 3."
        EnginePhase.GAME_OVER -> "Game Over"
    }

    val phaseNumber = when (enginePhase) {
        EnginePhase.SELECT, EnginePhase.ROUND_END, EnginePhase.SETUP -> 1
        EnginePhase.BOT_TURN -> {
            when {
                isTrickling -> 3
                humanActionLocked ||
                        pendingHumanAction == PendingHumanAction.PASS ||
                        pendingHumanAction == PendingHumanAction.TARGET -> 2
                else -> 1
            }
        }
        EnginePhase.PLAYER_TURN -> 2
        EnginePhase.GAME_OVER -> 3
    }

    return "Round $roundNumber | Phase $phaseNumber: $instruction"
}

private fun buildWinnerBadgeLabel(result: RoundResult?): String? {
    if (result == null) return null
    if (result.phase != EnginePhase.GAME_OVER) return null
    if (result.winnerIds.isEmpty()) return null

    val winnerNames = result.winnerIds.mapNotNull { winnerId ->
        result.players.firstOrNull { it.id == winnerId }?.baseName
    }

    if (winnerNames.isEmpty()) return null

    return winnerNames.joinToString(", ")
}

@Composable
private fun WeatherInfoBadge(
    weatherName: String?,
    weatherEffect: String?,
    weatherId: String?,
    forcedGuess: Int?,
    mustTarget: Boolean,
    needsSecondTarget: Boolean,
    onBadgeAnchorMeasured: (TablePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .onGloballyPositioned { coordinates ->
                onBadgeAnchorMeasured(coordinates.boundsInRoot().centerPoint())
            }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xAA1E1E1E),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!weatherId.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(72.dp)
                        .alpha(0.16f)
                ) {
                    WeatherIconCanvas(
                        weatherId = weatherId,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (weatherName.isNullOrBlank()) "Weather: Clear" else "Weather: $weatherName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )

                if (!weatherEffect.isNullOrBlank()) {
                    Text(
                        text = weatherEffect,
                        color = Color(0xFFD7E3FC),
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                }

                if (forcedGuess != null) {
                    Text(
                        text = "Locked guess: $forcedGuess",
                        color = Color(0xFFFFF59D),
                        fontSize = 13.sp
                    )
                }

                if (mustTarget) {
                    Text(
                        text = "Must target if able.",
                        color = Color(0xFFFFCCBC),
                        fontSize = 13.sp
                    )
                }

                if (needsSecondTarget) {
                    Text(
                        text = "Targeting needs two picks.",
                        color = Color(0xFFFFCCBC),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun weatherIdForName(weatherName: String?): String? {
    if (weatherName.isNullOrBlank()) return null
    return Weather.allCards.firstOrNull { it.displayName == weatherName }?.id
}

@Composable
private fun WeatherTransitionOverlay(
    weatherId: String?,
    animationToken: Long,
    fromAnchor: TablePoint?,
    toAnchor: TablePoint?,
    modifier: Modifier = Modifier
) {
    if (weatherId.isNullOrBlank() || animationToken <= 0L) return

    val progress = remember(animationToken) { Animatable(0f) }
    val overlayAlphaAnim = remember(animationToken) { Animatable(0f) }

    LaunchedEffect(animationToken, weatherId, fromAnchor, toAnchor) {
        progress.snapTo(0f)
        overlayAlphaAnim.snapTo(0f)
        overlayAlphaAnim.animateTo(
            targetValue = 0.82f,
            animationSpec = tween(durationMillis = 180, easing = LinearEasing)
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        overlayAlphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = LinearEasing)
        )
    }

    val overlayTranslationX = if (fromAnchor != null && toAnchor != null) {
        (toAnchor.x - fromAnchor.x) * progress.value
    } else {
        0f
    }
    val overlayTranslationY = if (fromAnchor != null && toAnchor != null) {
        (toAnchor.y - fromAnchor.y) * progress.value
    } else {
        (-220f) * progress.value
    }
    val overlayScale = 1.50f - (1.18f * progress.value)
    val overlayAlpha = overlayAlphaAnim.value * (1f - (0.35f * progress.value))

    if (overlayAlpha <= 0.01f) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    translationX = overlayTranslationX
                    translationY = overlayTranslationY
                    scaleX = overlayScale
                    scaleY = overlayScale
                    alpha = overlayAlpha
                }
        ) {
            WeatherIconCanvas(
                weatherId = weatherId,
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun WeatherIconCanvas(
    weatherId: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawWeatherIcon(weatherId = weatherId, tint = tint)
    }
}

private fun DrawScope.drawWeatherIcon(
    weatherId: String,
    tint: Color
) {
    val unit = size.minDimension
    val stroke = unit * 0.072f
    val cloudTop = size.height * 0.34f
    val cloudRect = Rect(
        left = size.width * 0.20f,
        top = cloudTop,
        right = size.width * 0.80f,
        bottom = size.height * 0.62f
    )

    fun line(startX: Float, startY: Float, endX: Float, endY: Float, width: Float = stroke) {
        drawLine(
            color = tint,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }

    fun circle(cx: Float, cy: Float, radius: Float) {
        drawCircle(
            color = tint,
            radius = radius,
            center = Offset(cx, cy)
        )
    }

    fun ring(cx: Float, cy: Float, radius: Float, width: Float = stroke) {
        drawCircle(
            color = tint,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = width)
        )
    }

    fun drop(cx: Float, cy: Float, scale: Float = 1f) {
        val width = unit * 0.09f * scale
        val height = unit * 0.17f * scale
        val path = Path().apply {
            moveTo(cx, cy - height * 0.62f)
            quadraticBezierTo(cx + width * 0.66f, cy - height * 0.10f, cx, cy + height * 0.56f)
            quadraticBezierTo(cx - width * 0.66f, cy - height * 0.10f, cx, cy - height * 0.62f)
            close()
        }
        drawPath(path = path, color = tint)
    }

    fun hailStone(cx: Float, cy: Float, scale: Float = 1f) {
        drawCircle(
            color = tint,
            radius = unit * 0.045f * scale,
            center = Offset(cx, cy)
        )
    }

    fun snowFlake(cx: Float, cy: Float, scale: Float = 1f) {
        val length = unit * 0.12f * scale
        for (angle in listOf(0f, 60f, 120f)) {
            rotate(angle, Offset(cx, cy)) {
                line(cx - length, cy, cx + length, cy, width = stroke * 0.52f)
                line(cx + length * 0.30f, cy, cx + length * 0.54f, cy - length * 0.18f, width = stroke * 0.38f)
                line(cx + length * 0.30f, cy, cx + length * 0.54f, cy + length * 0.18f, width = stroke * 0.38f)
                line(cx - length * 0.30f, cy, cx - length * 0.54f, cy - length * 0.18f, width = stroke * 0.38f)
                line(cx - length * 0.30f, cy, cx - length * 0.54f, cy + length * 0.18f, width = stroke * 0.38f)
            }
        }
    }

    fun bolt(centerX: Float, topY: Float, scale: Float = 1f) {
        val path = Path().apply {
            moveTo(centerX - unit * 0.05f * scale, topY)
            lineTo(centerX + unit * 0.02f * scale, topY)
            lineTo(centerX - unit * 0.03f * scale, topY + unit * 0.16f * scale)
            lineTo(centerX + unit * 0.10f * scale, topY + unit * 0.16f * scale)
            lineTo(centerX - unit * 0.08f * scale, topY + unit * 0.40f * scale)
            lineTo(centerX - unit * 0.01f * scale, topY + unit * 0.22f * scale)
            lineTo(centerX - unit * 0.14f * scale, topY + unit * 0.22f * scale)
            close()
        }
        drawPath(path = path, color = tint)
    }

    fun cloud(scale: Float = 1f, offsetY: Float = 0f) {
        val left = size.width * 0.22f
        val top = size.height * 0.32f + offsetY
        drawCircle(tint, radius = unit * 0.12f * scale, center = Offset(left + unit * 0.14f, top + unit * 0.10f))
        drawCircle(tint, radius = unit * 0.15f * scale, center = Offset(left + unit * 0.30f, top + unit * 0.04f))
        drawCircle(tint, radius = unit * 0.12f * scale, center = Offset(left + unit * 0.47f, top + unit * 0.10f))
        drawRoundRect(
            color = tint,
            topLeft = Offset(left + unit * 0.08f, top + unit * 0.10f),
            size = Size(unit * 0.48f, unit * 0.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.08f, unit * 0.08f)
        )
    }

    fun sun(rayCount: Int = 8, rayLength: Float = unit * 0.13f, ringOnly: Boolean = false) {
        val center = Offset(size.width * 0.50f, size.height * 0.38f)
        if (ringOnly) ring(center.x, center.y, unit * 0.13f, width = stroke * 0.74f)
        else circle(center.x, center.y, unit * 0.12f)
        repeat(rayCount) { index ->
            val angle = (360f / rayCount) * index
            rotate(angle, center) {
                line(center.x, center.y - unit * 0.22f, center.x, center.y - unit * 0.34f, width = stroke * 0.72f)
            }
        }
    }

    fun windBand(y: Float, startX: Float = size.width * 0.18f, endX: Float = size.width * 0.82f, curve: Float = unit * 0.05f) {
        val path = Path().apply {
            moveTo(startX, y)
            cubicTo(
                startX + unit * 0.14f, y - curve,
                endX - unit * 0.18f, y + curve,
                endX, y
            )
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke * 0.62f, cap = StrokeCap.Round)
        )
    }

    fun pressureSpiral(turns: Int = 2, inward: Boolean = true) {
        val center = Offset(size.width * 0.50f, size.height * 0.46f)
        val path = Path()
        val maxRadius = unit * 0.24f
        val minRadius = unit * 0.05f
        val steps = 42
        for (step in 0..steps) {
            val fraction = step / steps.toFloat()
            val radius = if (inward) {
                maxRadius - ((maxRadius - minRadius) * fraction)
            } else {
                minRadius + ((maxRadius - minRadius) * fraction)
            }
            val angle = fraction * turns * 360f
            val radians = Math.toRadians(angle.toDouble())
            val x = center.x + (kotlin.math.cos(radians).toFloat() * radius)
            val y = center.y + (kotlin.math.sin(radians).toFloat() * radius)
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke * 0.70f, cap = StrokeCap.Round)
        )
        if (inward) {
            line(center.x + unit * 0.19f, center.y - unit * 0.02f, center.x + unit * 0.27f, center.y - unit * 0.09f, width = stroke * 0.60f)
            line(center.x + unit * 0.19f, center.y - unit * 0.02f, center.x + unit * 0.28f, center.y + unit * 0.02f, width = stroke * 0.60f)
        } else {
            line(center.x + unit * 0.18f, center.y + unit * 0.09f, center.x + unit * 0.30f, center.y + unit * 0.09f, width = stroke * 0.60f)
            line(center.x + unit * 0.30f, center.y + unit * 0.09f, center.x + unit * 0.24f, center.y + unit * 0.03f, width = stroke * 0.60f)
            line(center.x + unit * 0.30f, center.y + unit * 0.09f, center.x + unit * 0.24f, center.y + unit * 0.15f, width = stroke * 0.60f)
        }
    }

    when (weatherId) {
        "drizzle" -> {
            cloud()
            drop(size.width * 0.38f, size.height * 0.72f, 0.92f)
            drop(size.width * 0.50f, size.height * 0.76f, 0.92f)
            drop(size.width * 0.62f, size.height * 0.72f, 0.92f)
        }

        "downpour" -> {
            cloud()
            repeat(5) { index ->
                drop(size.width * (0.28f + (index * 0.11f)), size.height * 0.74f, 1f)
            }
        }

        "fog" -> {
            windBand(size.height * 0.34f, curve = unit * 0.02f)
            windBand(size.height * 0.48f, curve = unit * 0.02f)
            windBand(size.height * 0.62f, curve = unit * 0.02f)
            drawOval(
                color = tint.copy(alpha = 0.72f),
                topLeft = Offset(size.width * 0.22f, size.height * 0.28f),
                size = Size(size.width * 0.56f, size.height * 0.18f)
            )
        }

        "sunny_day" -> {
            sun()
        }

        "low_pressure" -> {
            pressureSpiral(inward = true)
        }

        "windshear" -> {
            windBand(size.height * 0.34f, startX = size.width * 0.12f, endX = size.width * 0.88f, curve = unit * 0.08f)
            windBand(size.height * 0.50f, startX = size.width * 0.18f, endX = size.width * 0.86f, curve = unit * 0.10f)
            line(size.width * 0.60f, size.height * 0.26f, size.width * 0.78f, size.height * 0.18f, width = stroke * 0.58f)
            line(size.width * 0.56f, size.height * 0.64f, size.width * 0.78f, size.height * 0.78f, width = stroke * 0.58f)
        }

        "static_charge" -> {
            ring(size.width * 0.50f, size.height * 0.46f, unit * 0.18f, width = stroke * 0.54f)
            bolt(size.width * 0.50f, size.height * 0.24f, 1f)
            circle(size.width * 0.26f, size.height * 0.48f, unit * 0.03f)
            circle(size.width * 0.74f, size.height * 0.44f, unit * 0.03f)
            circle(size.width * 0.36f, size.height * 0.72f, unit * 0.03f)
            circle(size.width * 0.66f, size.height * 0.70f, unit * 0.03f)
        }

        "crosswinds" -> {
            windBand(size.height * 0.34f, curve = unit * 0.07f)
            windBand(size.height * 0.58f, curve = unit * 0.07f)
            line(size.width * 0.26f, size.height * 0.22f, size.width * 0.74f, size.height * 0.78f, width = stroke * 0.60f)
            line(size.width * 0.74f, size.height * 0.22f, size.width * 0.26f, size.height * 0.78f, width = stroke * 0.60f)
        }

        "sleet" -> {
            cloud()
            drop(size.width * 0.38f, size.height * 0.72f, 0.88f)
            snowFlake(size.width * 0.62f, size.height * 0.73f, 0.52f)
            line(size.width * 0.50f, size.height * 0.64f, size.width * 0.54f, size.height * 0.80f, width = stroke * 0.48f)
        }

        "thunderstorm" -> {
            cloud()
            bolt(size.width * 0.50f, size.height * 0.50f, 0.92f)
        }

        "drought" -> {
            sun(rayCount = 10, rayLength = unit * 0.10f)
            line(size.width * 0.26f, size.height * 0.74f, size.width * 0.74f, size.height * 0.74f, width = stroke * 0.54f)
            line(size.width * 0.38f, size.height * 0.74f, size.width * 0.32f, size.height * 0.84f, width = stroke * 0.44f)
            line(size.width * 0.46f, size.height * 0.74f, size.width * 0.52f, size.height * 0.86f, width = stroke * 0.44f)
            line(size.width * 0.60f, size.height * 0.74f, size.width * 0.66f, size.height * 0.82f, width = stroke * 0.44f)
        }

        "lightning_storm" -> {
            cloud()
            bolt(size.width * 0.44f, size.height * 0.48f, 0.88f)
            bolt(size.width * 0.60f, size.height * 0.52f, 0.72f)
        }

        "tornado" -> {
            val path = Path().apply {
                moveTo(size.width * 0.26f, size.height * 0.28f)
                cubicTo(size.width * 0.66f, size.height * 0.28f, size.width * 0.72f, size.height * 0.40f, size.width * 0.44f, size.height * 0.52f)
                cubicTo(size.width * 0.52f, size.height * 0.60f, size.width * 0.48f, size.height * 0.72f, size.width * 0.54f, size.height * 0.84f)
                lineTo(size.width * 0.42f, size.height * 0.84f)
                cubicTo(size.width * 0.50f, size.height * 0.70f, size.width * 0.44f, size.height * 0.60f, size.width * 0.34f, size.height * 0.50f)
                cubicTo(size.width * 0.20f, size.height * 0.40f, size.width * 0.18f, size.height * 0.30f, size.width * 0.26f, size.height * 0.28f)
                close()
            }
            drawPath(path = path, color = tint)
        }

        "hail" -> {
            cloud()
            repeat(4) { index ->
                hailStone(size.width * (0.30f + (index * 0.13f)), size.height * 0.74f)
            }
        }

        "hurricane" -> {
            pressureSpiral(turns = 3, inward = true)
            ring(size.width * 0.50f, size.height * 0.46f, unit * 0.05f, width = stroke * 0.50f)
        }

        "rainbow" -> {
            repeat(4) { index ->
                drawArc(
                    color = tint.copy(alpha = 0.40f + (index * 0.15f)),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * (0.16f + (index * 0.04f)), size.height * (0.24f + (index * 0.04f))),
                    size = Size(size.width * (0.68f - (index * 0.08f)), size.height * (0.46f - (index * 0.08f))),
                    style = Stroke(width = stroke * 0.50f)
                )
            }
        }

        "perfect_storm" -> {
            cloud()
            bolt(size.width * 0.48f, size.height * 0.50f, 0.84f)
            drop(size.width * 0.64f, size.height * 0.72f, 0.92f)
            hailStone(size.width * 0.34f, size.height * 0.74f)
            windBand(size.height * 0.86f, startX = size.width * 0.24f, endX = size.width * 0.76f, curve = unit * 0.03f)
        }

        "heat_mirage" -> {
            sun(rayCount = 6, ringOnly = true)
            windBand(size.height * 0.62f, curve = unit * 0.08f)
            windBand(size.height * 0.74f, curve = unit * 0.08f)
            windBand(size.height * 0.86f, curve = unit * 0.08f)
        }

        "high_pressure" -> {
            pressureSpiral(inward = false)
        }

        "stormfront" -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(size.width * 0.18f, size.height * 0.36f),
                size = Size(size.width * 0.18f, size.height * 0.30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.06f, unit * 0.06f)
            )
            cloud(offsetY = unit * 0.04f)
            line(size.width * 0.36f, size.height * 0.36f, size.width * 0.36f, size.height * 0.74f, width = stroke * 0.52f)
        }

        "cold_rain" -> {
            cloud()
            drop(size.width * 0.38f, size.height * 0.72f, 0.92f)
            drop(size.width * 0.52f, size.height * 0.76f, 0.92f)
            snowFlake(size.width * 0.68f, size.height * 0.76f, 0.42f)
        }

        "thunderhead" -> {
            cloud(scale = 1.12f, offsetY = unit * 0.02f)
            drawOval(
                color = tint.copy(alpha = 0.20f),
                topLeft = Offset(size.width * 0.22f, size.height * 0.56f),
                size = Size(size.width * 0.56f, size.height * 0.16f)
            )
        }

        "cool_breeze" -> {
            windBand(size.height * 0.34f, curve = unit * 0.04f)
            windBand(size.height * 0.52f, curve = unit * 0.04f)
            windBand(size.height * 0.70f, curve = unit * 0.04f)
            snowFlake(size.width * 0.72f, size.height * 0.28f, 0.32f)
        }

        "smog" -> {
            cloud(offsetY = unit * 0.08f)
            windBand(size.height * 0.62f, curve = unit * 0.01f)
            windBand(size.height * 0.74f, curve = unit * 0.01f)
            drawOval(
                color = tint.copy(alpha = 0.18f),
                topLeft = Offset(size.width * 0.18f, size.height * 0.52f),
                size = Size(size.width * 0.64f, size.height * 0.22f)
            )
        }

        else -> {
            cloud()
        }
    }
}

@Composable
private fun PhaseBadge(

    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xAA1F2A44)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                color = Color.White,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
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
    isStarter: Boolean,
    displayedChoice: Int?,
    targetVisualState: TargetVisualState,
    onTargetClick: (() -> Unit)?,
    onCupAnchorMeasured: (Int, TablePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = when (targetVisualState) {
        TargetVisualState.DISABLED -> 0.38f
        else -> 1f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .then(
                if (onTargetClick != null) {
                    Modifier.clickable(onClick = onTargetClick)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playerTitle,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(10.dp))

        TableCup(
            label = "Player (You)",
            highlighted = true,
            isCurrentTurn = isCurrentTurn,
            indicator = indicator,
            hasHat = hasHat,
            isStarter = isStarter,
            displayedChoice = displayedChoice,
            targetVisualState = targetVisualState,
            onCupAnchorMeasured = { measuredAnchor ->
                onCupAnchorMeasured(GameEngine.HUMAN_ID, measuredAnchor)
            }
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = "Score: $playerScore/${GameEngine.WIN_SCORE}",
            color = Color(0xFFFFF59D),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun BotCupColumn(
    bots: List<PlayerState>,
    currentActorId: Int?,
    hatHolderId: Int?,
    starterId: Int?,
    indicators: Map<Int, FloatingIndicator>,
    showMarbleCounts: Boolean,
    targetVisualStateForBot: (Int) -> TargetVisualState,
    onBotClicked: (Int) -> Unit,
    onCupAnchorMeasured: (Int, TablePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        bots.forEach { bot ->
            val targetVisualState = targetVisualStateForBot(bot.id)
            val contentAlpha = when (targetVisualState) {
                TargetVisualState.DISABLED -> 0.38f
                else -> 1f
            }
            val nameColor = when (targetVisualState) {
                TargetVisualState.SELECTED -> Color(0xFFFFF59D)
                TargetVisualState.SELECTABLE -> Color(0xFFFFF59D)
                else -> Color.White
            }

            Column(
                modifier = Modifier
                    .alpha(contentAlpha)
                    .clickable(
                        enabled = targetVisualState == TargetVisualState.SELECTABLE ||
                                targetVisualState == TargetVisualState.SELECTED,
                        onClick = { onBotClicked(bot.id) }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TableCup(
                    label = bot.baseName.take(1).uppercase(),
                    highlighted = false,
                    isCurrentTurn = currentActorId == bot.id,
                    indicator = indicators[bot.id],
                    hasHat = hatHolderId == bot.id,
                    isStarter = starterId == bot.id,
                    displayedChoice = bot.revealedChoice,
                    marbleCountText = if (showMarbleCounts) bot.marbles.toString() else null,
                    targetVisualState = targetVisualState,
                    onCupAnchorMeasured = { measuredAnchor ->
                        onCupAnchorMeasured(bot.id, measuredAnchor)
                    }
                )

                Text(
                    text = bot.baseName,
                    color = nameColor,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(54.dp)
                )
                Spacer(Modifier.height(4.dp))
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
    hasHat: Boolean = false,
    isStarter: Boolean = false,
    displayedChoice: Int? = null,
    marbleCountText: String? = null,
    targetVisualState: TargetVisualState = TargetVisualState.NORMAL,
    onCupAnchorMeasured: ((TablePoint) -> Unit)? = null
) {
    val bucketFill = when (targetVisualState) {
        TargetVisualState.SELECTED -> Color(0xFFFFB300)
        TargetVisualState.SELECTABLE -> Color(0xFFD84315)
        TargetVisualState.DISABLED -> Color(0xFF5D4037)
        TargetVisualState.NORMAL -> if (highlighted) Color(0xFFFF5252) else Color(0xFFD32F2F)
    }
    val bucketBorder = when (targetVisualState) {
        TargetVisualState.SELECTED -> Color(0xFFFFF176)
        TargetVisualState.SELECTABLE -> Color(0xFFFFCC80)
        TargetVisualState.DISABLED -> Color(0xFF3E2723)
        TargetVisualState.NORMAL -> if (highlighted) Color(0xFFFFCDD2) else Color(0xFF7F0000)
    }
    val cupContentAlpha = when (targetVisualState) {
        TargetVisualState.DISABLED -> 0.78f
        else -> 1f
    }
    val turnGlowTransition = rememberInfiniteTransition(label = "turnGlowTransition")
    val turnGlowColor by turnGlowTransition.animateColor(
        initialValue = Color.White,
        targetValue = Color(0xFF464646),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turnGlowColor"
    )
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
        modifier = Modifier.size(width = 56.dp, height = 68.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SeatIndicatorLane(
            isStarter = isStarter,
            hasHat = hasHat,
            displayedChoice = displayedChoice,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(24.dp)
        )

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 40.dp, height = 46.dp)
                .onGloballyPositioned { coordinates ->
                    onCupAnchorMeasured?.invoke(coordinates.boundsInRoot().cupLandingPoint())
                },
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentTurn) {
                Surface(
                    modifier = Modifier.size(width = 50.dp, height = 58.dp),
                    shape = cupShape,
                    color = turnGlowColor.copy(alpha = 0.20f),
                    border = BorderStroke(3.dp, turnGlowColor.copy(alpha = 0.95f)),
                    shadowElevation = 14.dp
                ) {}
            }

            Surface(
                modifier = Modifier.size(width = 44.dp, height = 52.dp),
                shape = cupShape,
                color = bucketFill,
                border = BorderStroke(2.dp, if (isCurrentTurn) turnGlowColor else bucketBorder),
                shadowElevation = if (isCurrentTurn) 10.dp else 5.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(6.dp),
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

                    if (marbleCountText != null) {
                        Text(
                            text = marbleCountText,
                            color = Color.White.copy(alpha = cupContentAlpha),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                        )
                    }
                }
            }
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
                    .align(Alignment.Center)
                    .zIndex(3f)
                    .padding(top = 18.dp)
                    .alpha(alpha.value),
                shape = RoundedCornerShape(10.dp),
                color = indicatorToneColor(indicator.tone).copy(alpha = 1f),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.35f)),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = indicator.text,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SeatIndicatorLane(
    isStarter: Boolean,
    hasHat: Boolean,
    displayedChoice: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IndicatorSprite(
            drawableRes = if (isStarter) R.drawable.starter else null,
            contentDescription = if (isStarter) "Starter" else null,
            size = 18.dp,
            visualScale = 1.7f
        )

        Spacer(Modifier.width(1.dp))

        IndicatorSprite(
            drawableRes = if (hasHat) R.drawable.thehat else null,
            contentDescription = if (hasHat) "Hat" else null,
            size = 14.dp,
            visualScale = 1.5f
        )

        Spacer(Modifier.width(1.dp))

        IndicatorSprite(
            drawableRes = when (displayedChoice) {
                0 -> R.drawable.choicezero
                1 -> R.drawable.choiceone
                3 -> R.drawable.choicethree
                else -> R.drawable.choicenone
            },
            contentDescription = "Choice",
            size = 16.dp,
            visualScale = 1.25f
        )
    }
}

@Composable
private fun IndicatorSprite(
    drawableRes: Int?,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    visualScale: Float = 1f
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = visualScale
                        scaleY = visualScale
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun GameTableSurface(
    onBowlSpawnMeasured: (TablePoint) -> Unit,
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
            TableCenterBowl(
                onBowlSpawnMeasured = onBowlSpawnMeasured,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.26f)
                    .fillMaxHeight(0.15f)
                    .offset(y = 34.dp)
            )
        }
    }
}

@Composable
private fun TableCenterBowl(
    onBowlSpawnMeasured: (TablePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            onBowlSpawnMeasured(coordinates.boundsInRoot().bowlSpawnPoint())
        }
    ) {
        val shadowHeight = size.height * 0.22f
        val bowlWidth = size.width
        val bowlHeight = size.height * 0.68f
        val bowlTop = size.height * 0.18f

        drawOval(
            color = Color(0x3A000000),
            topLeft = Offset(
                x = size.width * 0.08f,
                y = size.height * 0.72f
            ),
            size = Size(
                width = size.width * 0.84f,
                height = shadowHeight
            )
        )

        drawOval(
            color = Color(0xFF6E6E6E),
            topLeft = Offset(
                x = 0f,
                y = bowlTop
            ),
            size = Size(
                width = bowlWidth,
                height = bowlHeight
            )
        )

        drawOval(
            color = Color(0xFF4F4F4F),
            topLeft = Offset(
                x = size.width * 0.06f,
                y = bowlTop + size.height * 0.12f
            ),
            size = Size(
                width = size.width * 0.88f,
                height = bowlHeight * 0.72f
            )
        )

        drawOval(
            color = Color(0xFF9A9A9A),
            topLeft = Offset(
                x = size.width * 0.02f,
                y = bowlTop
            ),
            size = Size(
                width = size.width * 0.96f,
                height = bowlHeight * 0.34f
            )
        )

        drawOval(
            color = Color(0x55FFFFFF),
            topLeft = Offset(
                x = size.width * 0.18f,
                y = bowlTop + size.height * 0.05f
            ),
            size = Size(
                width = size.width * 0.30f,
                height = bowlHeight * 0.12f
            )
        )
    }
}

@Composable
private fun TargetArrowOverlay(
    arrow: TargetArrowVisual,
    cupCenters: Map<Int, TablePoint>,
    modifier: Modifier = Modifier
) {
    var overlayOrigin by remember { mutableStateOf(TablePoint(0f, 0f)) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInRoot()
            overlayOrigin = TablePoint(bounds.left, bounds.top)
        }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val actorCenter = cupCenters[arrow.actorId] ?: return@Canvas
            val actorLocal = Offset(
                x = actorCenter.x - overlayOrigin.x,
                y = actorCenter.y - overlayOrigin.y
            )

            arrow.targetIds.distinct().forEach { targetId ->
                val targetCenter = cupCenters[targetId] ?: return@forEach
                val targetLocal = Offset(
                    x = targetCenter.x - overlayOrigin.x,
                    y = targetCenter.y - overlayOrigin.y
                )
                drawTargetArrow(
                    start = actorLocal,
                    end = targetLocal
                )
            }
        }
    }
}

private fun DrawScope.drawTargetArrow(
    start: Offset,
    end: Offset
) {
    val deltaX = end.x - start.x
    val deltaY = end.y - start.y
    val distance = kotlin.math.sqrt((deltaX * deltaX) + (deltaY * deltaY))
    if (distance < 8f) return

    val directionX = deltaX / distance
    val directionY = deltaY / distance
    val startInset = 28f
    val endInset = 20f

    val shaftStart = Offset(
        x = start.x + (directionX * startInset),
        y = start.y + (directionY * startInset)
    )
    val shaftEnd = Offset(
        x = end.x - (directionX * endInset),
        y = end.y - (directionY * endInset)
    )

    val angle = atan2(shaftEnd.y - shaftStart.y, shaftEnd.x - shaftStart.x)
    val arrowHeadLength = 24f
    val arrowHeadSpread = (PI / 7.5f).toFloat()
    val leftHead = Offset(
        x = shaftEnd.x - (arrowHeadLength * cos(angle - arrowHeadSpread)),
        y = shaftEnd.y - (arrowHeadLength * sin(angle - arrowHeadSpread))
    )
    val rightHead = Offset(
        x = shaftEnd.x - (arrowHeadLength * cos(angle + arrowHeadSpread)),
        y = shaftEnd.y - (arrowHeadLength * sin(angle + arrowHeadSpread))
    )

    drawLine(
        color = Color.White.copy(alpha = 0.24f),
        start = shaftStart,
        end = shaftEnd,
        strokeWidth = 12f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFF1B8),
        start = shaftStart,
        end = shaftEnd,
        strokeWidth = 6f,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f), 0f)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = shaftStart,
        end = shaftEnd,
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = 0.24f),
        start = shaftEnd,
        end = leftHead,
        strokeWidth = 10f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = 0.24f),
        start = shaftEnd,
        end = rightHead,
        strokeWidth = 10f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFF1B8),
        start = shaftEnd,
        end = leftHead,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFF1B8),
        start = shaftEnd,
        end = rightHead,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
}

@Composable
private fun MarbleFlightOverlay(
    flights: List<MarbleFlightVisual>,
    onFlightFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var overlayBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            overlayBounds = coordinates.boundsInRoot()
        }
    ) {
        val currentBounds = overlayBounds ?: return@Box

        flights.forEach { flight ->
            key(flight.id) {
                AnimatedMarbleFlight(
                    flight = flight,
                    overlayBounds = currentBounds,
                    onFinished = { onFlightFinished(flight.id) }
                )
            }
        }
    }
}

@Composable
private fun AnimatedMarbleFlight(
    flight: MarbleFlightVisual,
    overlayBounds: Rect,
    onFinished: () -> Unit
) {
    val progress = remember(flight.id) { Animatable(0f) }

    LaunchedEffect(flight.id) {
        if (flight.launchDelayMs > 0) {
            delay(flight.launchDelayMs.toLong())
        }

        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 520,
                easing = FastOutSlowInEasing
            )
        )

        onFinished()
    }

    val start = Offset(
        x = flight.start.x - overlayBounds.left,
        y = flight.start.y - overlayBounds.top
    )
    val end = Offset(
        x = flight.end.x - overlayBounds.left,
        y = flight.end.y - overlayBounds.top
    )

    val eased = FastOutSlowInEasing.transform(progress.value)
    val travelX = start.x + ((end.x - start.x) * eased) + flight.laneOffsetPx
    val travelY = start.y + ((end.y - start.y) * eased)
    val arcLift = sin(progress.value * Math.PI).toFloat() * 26f
    val marbleSizePx = 16f

    val marbleOffset = IntOffset(
        x = (travelX - marbleSizePx / 2f).toInt(),
        y = (travelY - arcLift - marbleSizePx / 2f).toInt()
    )

    Canvas(
        modifier = Modifier
            .offset { marbleOffset }
            .size(16.dp)
    ) {
        drawCircle(color = Color(0xFF4FC3F7))
        drawCircle(
            color = Color(0x88FFFFFF),
            radius = size.minDimension * 0.24f,
            center = Offset(
                x = size.width * 0.34f,
                y = size.height * 0.30f
            )
        )
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
    canPass: Boolean,
    onPassSelected: () -> Unit,
    onTargetSelected: () -> Unit,
    onConfirmPass: () -> Unit,
    onTargetInstead: () -> Unit,
    onPassInstead: () -> Unit,
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
    showSubmitButton: Boolean,
    submitLabel: String,
    submitEnabled: Boolean,
    onSubmit: () -> Unit
) {
    val showChoiceButtons = choiceEnabled
    val showActionButtons = inputsEnabled && pendingHumanAction == PendingHumanAction.NONE
    val showPassConfirmButtons = inputsEnabled && canPass && pendingHumanAction == PendingHumanAction.PASS
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
                Text("Choose 0, 1, or 3.", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallChoiceButton("0", selected = choice == 0, enabled = choiceEnabled) { onChoiceSelected(0) }
                    SmallChoiceButton("1", selected = choice == 1, enabled = choiceEnabled) { onChoiceSelected(1) }
                    SmallChoiceButton("3", selected = choice == 3, enabled = choiceEnabled) { onChoiceSelected(3) }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (showActionButtons) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        canPass && dropdownOptions.isNotEmpty() -> {
                            Button(
                                onClick = onPassSelected,
                                enabled = inputsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF546E7A),
                                    contentColor = Color.White
                                )
                            ) { OneLineButtonText("Pass") }

                            Button(
                                onClick = onTargetSelected,
                                enabled = inputsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8D6E63),
                                    contentColor = Color.White
                                )
                            ) { OneLineButtonText("Target") }
                        }

                        canPass -> {
                            Button(
                                onClick = onPassSelected,
                                enabled = inputsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF546E7A),
                                    contentColor = Color.White
                                )
                            ) { OneLineButtonText("Pass") }
                        }

                        dropdownOptions.isNotEmpty() -> {
                            Button(
                                onClick = onTargetSelected,
                                enabled = inputsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8D6E63),
                                    contentColor = Color.White
                                )
                            ) { OneLineButtonText("Target") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (showPassConfirmButtons) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirmPass,
                        enabled = inputsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF546E7A),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Confirm Pass") }

                    Button(
                        onClick = onTargetInstead,
                        enabled = inputsEnabled && dropdownOptions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8D6E63),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Target Instead") }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (showTargeting) {
                if (canPass) {
                    Button(
                        onClick = onPassInstead,
                        enabled = inputsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF546E7A),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Pass Instead") }

                    Spacer(Modifier.height(8.dp))
                }

                val targetPrompt = when {
                    needsSecondTarget && selectedTargetId == null ->
                        "Click a cup or name to pick your first target."
                    needsSecondTarget && selectedSecondTargetId == null ->
                        "Click a second cup or name."
                    needsSecondTarget ->
                        ""
                    selectedTargetId == null ->
                        "Click a cup or name to pick your target."
                    else ->
                        ""
                }

                Text(
                    text = targetPrompt,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedTargetId != null) {
                    Spacer(Modifier.height(8.dp))

                    fun selectedTargetName(playerId: Int?): String {
                        return (dropdownOptions + secondDropdownOptions)
                            .firstOrNull { it.id == playerId }
                            ?.baseName
                            ?: "-"
                    }

                    val targetSummary = buildString {
                        append("Target: ")
                        append(selectedTargetName(selectedTargetId))
                        if (needsSecondTarget && selectedSecondTargetId != null) {
                            append(" | Second: ")
                            append(selectedTargetName(selectedSecondTargetId))
                        }
                    }

                    Text(
                        text = targetSummary,
                        color = Color(0xFFFFF59D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
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
                            enabled = true
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

            if (showSubmitButton) {
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

private val CloudButtonShape: GenericShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height

    val left = 0.04f * w
    val right = 0.96f * w
    val top = 0.10f * h
    val bottom = 0.92f * h

    val puffY1 = 0.28f * h
    val puffY2 = 0.08f * h
    val puffY3 = 0.22f * h

    moveTo(left + 0.08f * w, bottom)

    cubicTo(
        left + 0.30f * w, bottom + 0.02f * h,
        right - 0.30f * w, bottom + 0.02f * h,
        right - 0.08f * w, bottom
    )

    cubicTo(
        right + 0.02f * w, 0.78f * h,
        right + 0.01f * w, 0.46f * h,
        right - 0.14f * w, puffY3
    )

    cubicTo(
        right - 0.06f * w, puffY2,
        right - 0.22f * w, top,
        right - 0.36f * w, puffY1
    )

    cubicTo(
        right - 0.44f * w, top - 0.02f * h,
        left + 0.56f * w, top - 0.02f * h,
        left + 0.46f * w, puffY1
    )

    cubicTo(
        left + 0.40f * w, top + 0.06f * h,
        left + 0.24f * w, top + 0.06f * h,
        left + 0.22f * w, puffY1 + 0.02f * h
    )

    cubicTo(
        left + 0.06f * w, puffY3,
        left - 0.02f * w, 0.52f * h,
        left + 0.06f * w, 0.72f * h
    )

    cubicTo(
        left + 0.01f * w, 0.80f * h,
        left + 0.02f * w, bottom,
        left + 0.08f * w, bottom
    )

    close()
}

@Composable
private fun MenuLinkButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val outlineColor = Color(0xFF9AA3AD)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

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
            .fillMaxWidth(0.82f)
            .height(86.dp)
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
        roundNumber = engine.getRoundNumber(),
        log = emptyList(),
        players = players,
        winnerIds = emptyList(),
        bannerText = null,
        targetableIdsForHuman = emptyList(),
        currentActorId = null,
        currentStarterId = null,
        lastEventKind = null,
        currentWeatherName = null,
        currentWeatherEffect = null,
        forcedGuessForHuman = null,
        mustTargetForHuman = false,
        requiresSecondTargetForHuman = false,
        hatHolderId = null,
        activeTargetArrowActorId = null,
        activeTargetArrowTargetIds = emptyList(),
        marbleTransfers = emptyList()
    )
}


@Composable
private fun DifficultyEntryTransitionOverlay(
    zoom: Float,
    nextImageAlpha: Float,
    currentImageRes: Int,
    nextImageRes: Int
) {
    val doorTransformOrigin = TransformOrigin(pivotFractionX = 0.52f, pivotFractionY = 0.56f)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = currentImageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = doorTransformOrigin
                    scaleX = zoom
                    scaleY = zoom
                    alpha = 1f - nextImageAlpha
                }
        )

        Image(
            painter = painterResource(id = nextImageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = nextImageAlpha
                }
        )
    }
}

private fun buildLogText(result: RoundResult, difficulty: Difficulty): String {
    if (difficulty == Difficulty.HARD) return ""

    return result.log.asReversed().joinToString("\n") { it.text }.ifBlank { "" }
}

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
                "- On Hard mode, there is no Log to review, you must rely on your memory.\n\n" +
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
                    "Hard games: ${stats.hardGames} (wins ${stats.hardWins})\n\n" +
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
                        Spacer(Modifier.height(4.dp))
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
        Spacer(Modifier.height(4.dp))

        AchievementSectionHeader("Core")
        AchievementRow(stats.firstPerfectWin, "Perfect Puddler", "Win with 0 wrong guesses")
        AchievementRow(stats.reachedRound6, "Idle Hands", "Reach round 6")
        AchievementRow(stats.wonWith18Marbles, "Is That Legal?", "Win with 18+ marbles")

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Difficulty")
        AchievementRow(stats.wonEasy, "Comp Stomp", "Win on Easy")
        AchievementRow(stats.wonNormal, "Pattern Finder", "Win on Normal")
        AchievementRow(stats.wonHard, "TR1CKL3!", "Win on Hard to unlock Weather")
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
            stats.justPressEverything,
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

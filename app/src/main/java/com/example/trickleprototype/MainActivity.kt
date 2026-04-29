package com.example.trickleprototype

import android.app.Activity
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.unit.TextUnit
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
import kotlin.math.max

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

data class AchievementPopup(
    val title: String,
    val desc: String
)

enum class AppScreen {
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



private val TAGGABLE_ARCHETYPE_NAMES = listOf(
    "Glutton",
    "Pacifist",
    "Chaos",
    "Limper",
    "Scout",
    "Strobe",
    "Nemesis",
    "Bully",
    "Cynic",
    "Pitfall"
)

private fun cleanArchetypeNameForAvatar(name: String?): String? {
    val cleaned = name
        ?.replace("(?)", "")
        ?.trim()
        ?.lowercase()
        ?: return null

    return cleaned.ifBlank { null }
}

private fun botAvatarResourceNameForArchetype(name: String?): String? {
    return when (cleanArchetypeNameForAvatar(name)) {
        "auditor" -> "auditor"
        "avenger" -> "avenger"
        "bully" -> "bully"
        "chaos" -> "chaos"
        "cynic" -> "cynic"
        "glutton" -> "glutton"
        "jester" -> "jester"
        "juliet" -> "juliet"
        "cabal" -> "cabal"
        "limper" -> "limper"
        "lurker" -> "lurker"
        "nemesis" -> "nemesis"
        "pacifist" -> "pacifist"
        "pitfall" -> "pitfall"
        "romeo" -> "romeo"
        "scout" -> "scout"
        "strobe" -> "strobe"
        "hunter" -> "hunter"
        "seer" -> "seer"
        "mirror" -> "mirror"
        else -> null
    }
}

fun botAvatarStaticDrawableResourceId(resourceName: String?): Int {
    return when (resourceName) {
        "auditor" -> R.drawable.auditor
        "avenger" -> R.drawable.avenger
        "bully" -> R.drawable.bully
        "chaos" -> R.drawable.chaos
        "cynic" -> R.drawable.cynic
        "glutton" -> R.drawable.glutton
        "jester" -> R.drawable.jester
        "juliet" -> R.drawable.juliet
        "cabal" -> R.drawable.cabal
        "limper" -> R.drawable.limper
        "lurker" -> R.drawable.lurker
        "nemesis" -> R.drawable.nemesis
        "pacifist" -> R.drawable.pacifist
        "pitfall" -> R.drawable.pitfall
        "romeo" -> R.drawable.romeo
        "scout" -> R.drawable.scout
        "strobe" -> R.drawable.strobe
        else -> 0
    }
}

fun botAvatarResourceNameForBotName(name: String?): String? {
    val cleaned = name
        ?.trim()
        ?.lowercase()
        ?.replace(Regex("[^a-z0-9]+"), "_")
        ?.trim('_')
        ?: return null

    return cleaned.ifBlank { null }
}


@Immutable data class FloatingIndicator(
    val token: Long,
    val text: String,
    val tone: IndicatorTone
)

data class BonusMarbleRow(
    val label: String,
    val amount: Int
)

data class BonusMarblePayout(
    val rows: List<BonusMarbleRow>,
    val startingVaultMarbles: Long
) {
    val total: Int = rows.sumOf { it.amount }
}



private fun newlyUnlockedAchievementCount(result: RoundResult): Int {
    return result.log.mapNotNull { parseAchievementPopup(it.text)?.title }.distinct().size
}

private fun uniqueCorrectArchetypeTagCount(
    botTags: Map<Int, String>,
    actualArchetypes: Map<Int, String>
): Int {
    val tagCounts = botTags.values.groupingBy { it }.eachCount()
    return botTags.count { (botId, tag) ->
        tagCounts[tag] == 1 && actualArchetypes[botId] == tag
    }
}

private fun buildBonusMarblePayout(
    result: RoundResult,
    botTags: Map<Int, String>,
    difficulty: Difficulty?,
    startingVaultMarbles: Long
): BonusMarblePayout {
    val humanScore = result.players.firstOrNull { it.id == GameEngine.HUMAN_ID }?.marbles ?: 0
    val rows = mutableListOf(BonusMarbleRow("Game Score", humanScore))

    if (result.humanHeldHatThisGame) rows += BonusMarbleRow("Hat Holder", 1)
    if (result.humanCorrectGuessesThisGame > 0) {
        rows += BonusMarbleRow("Accuracy", result.humanCorrectGuessesThisGame)
    }

    val detectiveCount = uniqueCorrectArchetypeTagCount(botTags, result.botArchetypeNamesByPlayerId)
    if (detectiveCount > 0) rows += BonusMarbleRow("Detective", detectiveCount)

    if (!result.humanStartedAnyRoundThisGame) rows += BonusMarbleRow("Drafting", 2)
    if (!result.humanSubmittedTargetThisGame) rows += BonusMarbleRow("Peacekeeper", 3)

    val patienceAmount = maxOf(0, result.roundNumber - 5) * 4
    if (patienceAmount > 0) rows += BonusMarbleRow("Patience", patienceAmount)

    if (result.humanPerfectBonusIntact) rows += BonusMarbleRow("Perfection", 5)
    if (difficulty == Difficulty.HARD) rows += BonusMarbleRow("Endeavor", 6)
    if (result.winnerIds.contains(GameEngine.HUMAN_ID)) rows += BonusMarbleRow("Victory", 7)

    val achievementAmount = newlyUnlockedAchievementCount(result) * 10
    if (achievementAmount > 0) rows += BonusMarbleRow("Achievement", achievementAmount)

    return BonusMarblePayout(
        rows = rows,
        startingVaultMarbles = startingVaultMarbles
    )
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
    var revealArchetypesPostGame by remember { mutableStateOf(false) }
    val botTags = remember { mutableStateMapOf<Int, String>() }
    var tagMenuBotId by remember { mutableStateOf<Int?>(null) }

    var dieButtonsEnabled by remember { mutableStateOf(true) }

    var lastResult by remember { mutableStateOf<RoundResult?>(null) }
    var logText by remember { mutableStateOf("") }
    var floatingIndicators by remember { mutableStateOf<Map<Int, FloatingIndicator>>(emptyMap()) }
    var nextIndicatorToken by remember { mutableStateOf(1L) }
    var marbleFlights by remember { mutableStateOf<List<MarbleFlightVisual>>(emptyList()) }
    var activeTargetArrow by remember { mutableStateOf<TargetArrowVisual?>(null) }
    var fadingTargetArrow by remember { mutableStateOf<TargetArrowVisual?>(null) }
    var nextMarbleFlightId by remember { mutableLongStateOf(1L) }
    var bowlSpawnPoint by remember { mutableStateOf<TablePoint?>(null) }
    val cupCenters = remember { mutableStateMapOf<Int, TablePoint>() }
    var lastQueuedMarbleTransferSignature by remember { mutableStateOf("") }

    var achievementQueue by remember { mutableStateOf<List<AchievementPopup>>(emptyList()) }
    var activeAchievement by remember { mutableStateOf<AchievementPopup?>(null) }
    var bonusPayout by remember { mutableStateOf<BonusMarblePayout?>(null) }
    var bonusPayoutAppliedKey by remember { mutableStateOf<String?>(null) }
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
    val basePlayers = lastResult?.players ?: engine.getPlayersSnapshot()
    val gameOver = (phase == EnginePhase.GAME_OVER)
    val revealArchetypesActive = revealArchetypesPostGame && gameOver
    val botTagSnapshot = botTags.toMap()

    LaunchedEffect(lastResult, activeAchievement, achievementQueue, difficulty, botTagSnapshot) {
        val result = lastResult ?: return@LaunchedEffect
        if (result.phase != EnginePhase.GAME_OVER) return@LaunchedEffect
        if (activeAchievement != null || achievementQueue.isNotEmpty()) return@LaunchedEffect

        val gameKey = "${result.roundNumber}|${result.log.size}|${result.players.firstOrNull { it.id == GameEngine.HUMAN_ID }?.marbles ?: 0}"
        if (bonusPayoutAppliedKey == gameKey) return@LaunchedEffect

        delay(400)
        val startingVaultMarbles = statsStore.load().vaultMarbles
        val payout = buildBonusMarblePayout(
            result = result,
            botTags = botTagSnapshot,
            difficulty = difficulty,
            startingVaultMarbles = startingVaultMarbles
        )
        val totalProgressAmount = if (difficulty == Difficulty.EASY) {
            0L
        } else {
            (payout.total - (result.players.firstOrNull { it.id == GameEngine.HUMAN_ID }?.marbles ?: 0))
                .coerceAtLeast(0)
                .toLong()
        }
        statsStore.addEarnedMarbles(
            amount = payout.total.toLong(),
            totalMarblesAcrossGamesAmount = totalProgressAmount
        )
        bonusPayout = payout
        bonusPayoutAppliedKey = gameKey
    }

    val revealedPlayers = applyArchetypeRevealToPlayers(
        players = basePlayers,
        result = lastResult,
        revealArchetypes = revealArchetypesActive
    )
    val players = applyBotTagsToPlayers(
        players = revealedPlayers,
        difficulty = difficulty,
        revealArchetypes = revealArchetypesActive,
        botTags = botTagSnapshot
    )
    val botAvatarResourceNamesByPlayerId = buildBotAvatarResourceNamesByPlayerId(
        players = basePlayers,
        result = lastResult,
        difficulty = difficulty,
        revealArchetypes = revealArchetypesActive,
        botTags = botTagSnapshot
    )
    val botTaggedAvatarIds = if (difficulty == Difficulty.EASY || revealArchetypesActive) {
        emptySet()
    } else {
        botTagSnapshot.keys
    }
    val currentActorId = lastResult?.currentActorId
    val currentWeatherName = lastResult?.currentWeatherName
    val currentWeatherEffect = lastResult?.currentWeatherEffect
    val currentWeatherId = weatherIdForName(currentWeatherName)
    val forcedGuess = lastResult?.forcedGuessForHuman
    val mustTarget = lastResult?.mustTargetForHuman == true
    val needsSecondTarget = lastResult?.requiresSecondTargetForHuman == true
    val activeTargetArrowFromResult =
        if (phase == EnginePhase.BOT_TURN || phase == EnginePhase.PLAYER_TURN) {
            lastResult?.activeTargetArrowActorId?.let { actorId ->
                val targetIds = lastResult?.activeTargetArrowTargetIds.orEmpty()
                if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
            }
        } else {
            null
        }

    val scrollState = rememberScrollState()
    LaunchedEffect(logText) { scrollState.scrollTo(0) }

    LaunchedEffect(lastResult, difficulty, revealArchetypesActive, botTagSnapshot) {
        val result = lastResult ?: return@LaunchedEffect
        val pickedDifficulty = difficulty ?: return@LaunchedEffect
        logText = buildLogText(
            result = result,
            difficulty = pickedDifficulty,
            revealArchetypes = revealArchetypesActive,
            botTags = botTagSnapshot
        )
    }

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
        revealArchetypesPostGame = false
        floatingIndicators = emptyMap()
        marbleFlights = emptyList()
        activeTargetArrow = null
        lastQueuedMarbleTransferSignature = ""
        achievementQueue = emptyList()
        activeAchievement = null
        bonusPayout = null
        bonusPayoutAppliedKey = null
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
        botTags.clear()
        tagMenuBotId = null

        humanActionLocked = false
        startLocked = false
    }

    fun startGameSession(picked: Difficulty, animateEntry: Boolean) {
        engine.reset()
        engine.attachStatsStore(statsStore)
        difficulty = picked
        engine.setDifficulty(picked)

        val stats = statsStore.load()
        val weatherControlUnlocked = stats.wonHard
        val effectiveWeatherEnabled = if (weatherControlUnlocked) weatherEnabled else false
        engine.setWeatherEnabled(effectiveWeatherEnabled)
        botTags.clear()
        tagMenuBotId = null

        val snap = engineSnapshot(engine)
        lastResult = snap
        revealArchetypesPostGame = false
        logText = buildLogText(
            result = snap,
            difficulty = picked,
            revealArchetypes = false,
            botTags = botTags.toMap()
        )

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
        botTags.clear()
        tagMenuBotId = null

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
                logText = buildLogText(
                    result = result,
                    difficulty = difficulty!!,
                    revealArchetypes = revealArchetypesPostGame && result.phase == EnginePhase.GAME_OVER,
                    botTags = botTags.toMap()
                )
                activeTargetArrow =
                    if (result.phase == EnginePhase.BOT_TURN || result.phase == EnginePhase.PLAYER_TURN) {
                        result.activeTargetArrowActorId?.let { actorId ->
                            val targetIds = result.activeTargetArrowTargetIds
                            if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
                        }
                    } else {
                        null
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

        if (phase == EnginePhase.ROUND_END || phase == EnginePhase.GAME_OVER) {
            activeTargetArrow = null
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

    bonusPayout?.let { payout ->
        BonusMarblesAnimationOverlay(
            payout = payout,
            onFinished = { bonusPayout = null }
        )
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

            Spacer(Modifier.height(2.dp))

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
                    MainMenuScreen(
                        activity = activity,
                        onNavigate = { screen = it }
                    )
                    return@Column
                }

                AppScreen.PLAY -> {
                    PlayMenuScreen(
                        stats = statsStore.load(),
                        weatherEnabled = weatherEnabled,
                        onWeatherEnabledChange = { weatherEnabled = it },
                        onStartGame = { picked -> startGameSession(picked = picked, animateEntry = true) },
                        onBack = { screen = AppScreen.MAIN_MENU }
                    )
                    return@Column
                }

                AppScreen.RULES -> {
                    RulesMenuScreen(
                        onHowToPlay = { showHowToPlay = true },
                        onAdvancedTips = { showTips = true },
                        onArchetypes = { showArchetypes = true },
                        onBack = { screen = AppScreen.MAIN_MENU }
                    )
                    return@Column
                }

                AppScreen.PROFILE -> {
                    ProfileMenuScreen(
                        onStats = { showStats = true },
                        onAchievements = { showAchievements = true },
                        onCustomize = { screen = AppScreen.CUSTOMIZE },
                        onBack = { screen = AppScreen.MAIN_MENU }
                    )
                    return@Column
                }

                AppScreen.CUSTOMIZE -> {
                    CustomizeMenuScreen(
                        playerName = playerName,
                        onSaveName = { cleaned ->
                            statsStore.setPlayerName(cleaned)
                            playerName = statsStore.getPlayerName()
                            engine.setHumanName(playerName)
                            screen = AppScreen.PROFILE
                        },
                        onBack = { screen = AppScreen.PROFILE }
                    )
                    return@Column
                }

                AppScreen.SETTINGS -> {
                    SettingsMenuScreen(
                        showResetStatsConfirm = showResetStatsConfirm,
                        soundEnabled = soundEnabled,
                        musicEnabled = musicEnabled,
                        passTargetConfirmEnabled = passTargetConfirmEnabled,
                        onDismissResetStatsConfirm = { showResetStatsConfirm = false },
                        onConfirmResetStats = {
                            statsStore.resetAll()
                            playerName = statsStore.getPlayerName()
                            engine.setHumanName(playerName)
                            showResetStatsConfirm = false
                        },
                        onToggleSound = {
                            soundEnabled = !soundEnabled

                            if (!soundEnabled) {
                                splashSoundPlayer?.stop()
                                splashSoundPlayer?.release()
                                splashSoundPlayer = null
                            }
                        },
                        onToggleMusic = {
                            musicEnabled = !musicEnabled

                            if (!musicEnabled) {
                                bgmPlayer?.pause()
                            } else {
                                if (bgmPlayer?.isPlaying != true) {
                                    bgmPlayer?.start()
                                }
                            }
                        },
                        onTogglePassConfirm = {
                            passTargetConfirmEnabled = !passTargetConfirmEnabled
                        },
                        onResetStats = { showResetStatsConfirm = true },
                        onBack = { screen = AppScreen.MAIN_MENU }
                    )
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
            val targetArrowCanShow = phase == EnginePhase.BOT_TURN || phase == EnginePhase.PLAYER_TURN
            val targetArrowShouldShow = displayTargetArrow != null && targetArrowCanShow
            LaunchedEffect(displayTargetArrow, targetArrowShouldShow) {
                if (targetArrowShouldShow && displayTargetArrow != null) {
                    fadingTargetArrow = displayTargetArrow
                }
            }
            val targetArrowAlpha by animateFloatAsState(
                targetValue = if (targetArrowShouldShow) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                finishedListener = { alpha ->
                    if (alpha == 0f) {
                        fadingTargetArrow = null
                    }
                },
                label = "targetArrowAlpha"
            )

            val zeroGuessUnlocked = statsStore.load().zeroHeroUnlocked

            val winnerBadgeLabel = buildWinnerBadgeLabel(lastResult)
            val canShowLogPanel = when (difficulty) {
                Difficulty.HARD -> gameOver
                else -> showLogOverlay
            }
            val canRevealArchetypes = gameOver && (
                    difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD
                    )

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
                logText = buildLogText(
                    result = result,
                    difficulty = difficulty!!,
                    revealArchetypes = revealArchetypesPostGame && result.phase == EnginePhase.GAME_OVER,
                    botTags = botTags.toMap()
                )
                activeTargetArrow =
                    if (result.phase == EnginePhase.BOT_TURN || result.phase == EnginePhase.PLAYER_TURN) {
                        result.activeTargetArrowActorId?.let { actorId ->
                            val targetIds = result.activeTargetArrowTargetIds
                            if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
                        }
                    } else {
                        null
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

                        val smogVisibleBotIds = if (
                            difficulty != Difficulty.EASY &&
                            (lastResult?.smogRevealedPlayerIds?.isNotEmpty() == true)
                        ) {
                            lastResult?.smogRevealedPlayerIds ?: emptySet()
                        } else {
                            emptySet()
                        }

                        BotCupColumn(
                            bots = leftBots,
                            currentActorId = currentActorId,
                            hatHolderId = lastResult?.hatHolderId,
                            starterId = lastResult?.currentStarterId,
                            indicators = floatingIndicators,
                            showMarbleCounts = difficulty == Difficulty.EASY,
                            playersWithForcedMarbleCounts = smogVisibleBotIds,
                            taggingEnabled = difficulty != Difficulty.EASY && !revealArchetypesActive,
                            avatarResourceNameForBot = { botId -> botAvatarResourceNamesByPlayerId[botId] },
                            avatarGreyedOutForBot = { botId -> botId in botTaggedAvatarIds },
                            targetVisualStateForBot = { botId -> visualStateForTargetablePlayer(botId) },
                            onBotClicked = { botId -> handleTargetSeatClick(botId) },
                            onBotNameClicked = { botId -> tagMenuBotId = botId },
                            onCupAnchorMeasured = { playerId, anchor ->
                                cupCenters[playerId] = anchor
                            },
                            indicatorPlacement = SeatIndicatorPlacement.INSIDE_RIGHT,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .padding(start = 0.dp, top = 0.dp, bottom = 0.dp)
                        )

                        BotCupColumn(
                            bots = rightBots,
                            currentActorId = currentActorId,
                            hatHolderId = lastResult?.hatHolderId,
                            starterId = lastResult?.currentStarterId,
                            indicators = floatingIndicators,
                            showMarbleCounts = difficulty == Difficulty.EASY,
                            playersWithForcedMarbleCounts = smogVisibleBotIds,
                            taggingEnabled = difficulty != Difficulty.EASY && !revealArchetypesActive,
                            avatarResourceNameForBot = { botId -> botAvatarResourceNamesByPlayerId[botId] },
                            avatarGreyedOutForBot = { botId -> botId in botTaggedAvatarIds },
                            targetVisualStateForBot = { botId -> visualStateForTargetablePlayer(botId) },
                            onBotClicked = { botId -> handleTargetSeatClick(botId) },
                            onBotNameClicked = { botId -> tagMenuBotId = botId },
                            onCupAnchorMeasured = { playerId, anchor ->
                                cupCenters[playerId] = anchor
                            },
                            indicatorPlacement = SeatIndicatorPlacement.INSIDE_LEFT,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 0.dp, top = 0.dp, bottom = 0.dp)
                        )

                        TableActionPanel(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(148.dp)
                                .wrapContentHeight()
                                .padding(top = 96.dp),
                            choice = choice,
                            onChoiceSelected = { choice = it },
                            choiceOptions = if (phase == EnginePhase.TIEBREAKER_CHOICE) listOf(1, 3) else listOf(0, 1, 3),
                            choicePrompt = if (phase == EnginePhase.TIEBREAKER_CHOICE) "Choose the number" else "Choose 0, 1, or 3.",
                            choiceEnabled = !gameOver && !startLocked && (phase == EnginePhase.SELECT || phase == EnginePhase.ROUND_END || phase == EnginePhase.TIEBREAKER_CHOICE),
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
                                EnginePhase.TIEBREAKER_CHOICE -> "Submit Choice->"
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
                                EnginePhase.TIEBREAKER_CHOICE -> !gameOver && !startLocked && (choice == 1 || choice == 3)
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
                                        logText = buildLogText(
                                            result = result,
                                            difficulty = difficulty!!,
                                            revealArchetypes = revealArchetypesPostGame && result.phase == EnginePhase.GAME_OVER,
                                            botTags = botTags.toMap()
                                        )
                                        activeTargetArrow =
                                            if (result.phase == EnginePhase.BOT_TURN || result.phase == EnginePhase.PLAYER_TURN) {
                                                result.activeTargetArrowActorId?.let { actorId ->
                                                    val targetIds = result.activeTargetArrowTargetIds
                                                    if (targetIds.isEmpty()) null else TargetArrowVisual(actorId = actorId, targetIds = targetIds)
                                                }
                                            } else {
                                                null
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

                                    EnginePhase.TIEBREAKER_CHOICE -> {
                                        startLocked = true
                                        val result = engine.submitTiebreakerChoice(choice)
                                        lastResult = result
                                        logText = buildLogText(
                                            result = result,
                                            difficulty = difficulty!!,
                                            revealArchetypes = revealArchetypesPostGame && result.phase == EnginePhase.GAME_OVER,
                                            botTags = botTags.toMap()
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

                        if (canShowLogPanel && logText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(0.62f)
                                    .padding(bottom = if (canRevealArchetypes) 96.dp else 54.dp)
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

                        if (canRevealArchetypes) {
                            Button(
                                onClick = { revealArchetypesPostGame = !revealArchetypesPostGame },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (difficulty == Difficulty.HARD) 6.dp else 42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF263238),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    if (revealArchetypesActive) {
                                        "Show Bot Names"
                                    } else {
                                        "Reveal Archetypes"
                                    }
                                )
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

                fadingTargetArrow?.let { arrow ->
                    TargetArrowOverlay(
                        arrow = arrow,
                        cupCenters = cupCenters,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(targetArrowAlpha)
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

    tagMenuBotId?.let { selectedBotId ->
        val selectedBot = players.firstOrNull { it.id == selectedBotId }
        if (selectedBot != null) {
            SimpleDialog(
                title = "Tag ${selectedBot.baseName}",
                onClose = { tagMenuBotId = null },
                accentColor = Color(0xFFFFF59D)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TAGGABLE_ARCHETYPE_NAMES.forEach { archetypeName ->
                        Button(
                            onClick = {
                                botTags[selectedBotId] = "$archetypeName(?)"
                                tagMenuBotId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF263238),
                                contentColor = Color.White
                            )
                        ) {
                            OneLineButtonText(archetypeName)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            botTags.remove(selectedBotId)
                            tagMenuBotId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFFFF59D)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFF59D)
                        )
                    ) {
                        OneLineButtonText("Clear Tag")
                    }
                }
            }
        }
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

enum class PendingHumanAction {
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
        EnginePhase.TIEBREAKER_CHOICE -> "Choose 1 or 3."
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
        EnginePhase.TIEBREAKER_CHOICE -> 3
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

private fun buildBotAvatarResourceNamesByPlayerId(
    players: List<PlayerState>,
    result: RoundResult?,
    difficulty: Difficulty?,
    revealArchetypes: Boolean,
    botTags: Map<Int, String>
): Map<Int, String> {
    val visibleArchetypeNames = result?.botArchetypeNamesByPlayerId.orEmpty()
    val shouldShowArchetypeAvatars = difficulty == Difficulty.EASY || revealArchetypes

    return players
        .filter { player -> player.id != GameEngine.HUMAN_ID }
        .mapNotNull { player ->
            val visibleArchetypeResourceName = if (shouldShowArchetypeAvatars) {
                botAvatarResourceNameForArchetype(visibleArchetypeNames[player.id])
            } else {
                null
            }
            val taggedArchetypeResourceName = botAvatarResourceNameForArchetype(botTags[player.id])
            val botNameResourceName = botAvatarResourceNameForBotName(player.baseName)
            val resourceName = visibleArchetypeResourceName
                ?: taggedArchetypeResourceName
                ?: botNameResourceName
                ?: return@mapNotNull null

            player.id to resourceName
        }
        .toMap()
}

private fun applyArchetypeRevealToPlayers(
    players: List<PlayerState>,
    result: RoundResult?,
    revealArchetypes: Boolean
): List<PlayerState> {
    if (!revealArchetypes || result == null) return players

    return players.map { player ->
        val archetypeName = result.botArchetypeNamesByPlayerId[player.id]
        if (archetypeName == null) {
            player
        } else {
            player.copy(baseName = archetypeName)
        }
    }
}


private fun applyBotTagsToPlayers(
    players: List<PlayerState>,
    difficulty: Difficulty?,
    revealArchetypes: Boolean,
    botTags: Map<Int, String>
): List<PlayerState> {
    if (difficulty == null || difficulty == Difficulty.EASY || revealArchetypes) return players

    return players.map { player ->
        val tag = botTags[player.id]
        if (player.id == GameEngine.HUMAN_ID || tag == null) {
            player
        } else {
            player.copy(baseName = tag)
        }
    }
}


private fun applyArchetypeRevealToLogText(
    logText: String,
    result: RoundResult,
    revealArchetypes: Boolean
): String {
    if (!revealArchetypes) return logText

    val playerNamesById = result.players.associate { player -> player.id to player.baseName }

    return result.botArchetypeNamesByPlayerId
        .entries
        .sortedByDescending { entry -> playerNamesById[entry.key]?.length ?: 0 }
        .fold(logText) { currentText, entry ->
            val botName = playerNamesById[entry.key] ?: return@fold currentText
            val replacement = entry.value
            currentText.replace(
                Regex("\\b${Regex.escape(botName)}\\b"),
                replacement
            )
        }
}

private fun applyBotTagsToLogText(
    logText: String,
    result: RoundResult,
    difficulty: Difficulty,
    revealArchetypes: Boolean,
    botTags: Map<Int, String>
): String {
    if (difficulty == Difficulty.EASY || revealArchetypes || botTags.isEmpty()) return logText

    val playerNamesById = result.players.associate { player -> player.id to player.baseName }

    return botTags
        .entries
        .sortedByDescending { entry -> playerNamesById[entry.key]?.length ?: 0 }
        .fold(logText) { currentText, entry ->
            val botName = playerNamesById[entry.key] ?: return@fold currentText
            currentText.replace(
                Regex("\\b${Regex.escape(botName)}\\b"),
                entry.value
            )
        }
}

private fun buildLogText(
    result: RoundResult,
    difficulty: Difficulty,
    revealArchetypes: Boolean,
    botTags: Map<Int, String> = emptyMap()
): String {
    if (difficulty == Difficulty.HARD && result.phase != EnginePhase.GAME_OVER) return ""

    val baseLogText = result.log.asReversed().joinToString("\n") { it.text }.ifBlank { "" }
    val revealAdjustedLogText = applyArchetypeRevealToLogText(
        logText = baseLogText,
        result = result,
        revealArchetypes = revealArchetypes
    )

    return applyBotTagsToLogText(
        logText = revealAdjustedLogText,
        result = result,
        difficulty = difficulty,
        revealArchetypes = revealArchetypes,
        botTags = botTags
    )
}

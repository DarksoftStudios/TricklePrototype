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

enum class IndicatorTone {
    NEUTRAL,
    GOOD,
    BAD,
    ALERT
}

enum class SeatIndicatorPlacement {
    BELOW,
    INSIDE_LEFT,
    INSIDE_RIGHT
}

enum class TargetVisualState {
    NORMAL,
    SELECTABLE,
    SELECTED,
    DISABLED
}

enum class TableSeatSlot {
    PLAYER_HEAD,
    LEFT_TOP,
    LEFT_MID,
    LEFT_BOTTOM,
    RIGHT_TOP,
    RIGHT_MID,
    RIGHT_BOTTOM
}

@Immutable
data class TableAnchor(
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
data class TablePoint(
    val x: Float,
    val y: Float
)

fun Rect.centerPoint(): TablePoint {
    return TablePoint(
        x = left + (width / 2f),
        y = top + (height / 2f)
    )
}

fun Rect.cupLandingPoint(): TablePoint {
    return TablePoint(
        x = left + (width / 2f),
        y = top + (height * 0.46f)
    )
}

fun Rect.bowlSpawnPoint(): TablePoint {
    return TablePoint(
        x = left + (width / 2f),
        y = top + (height * 0.38f)
    )
}

@Immutable
data class TableLayoutAnchors(
    val bowlCenter: TableAnchor,
    val bowlSpawn: TableAnchor,
    val bowlHatRest: TableAnchor,
    val seatAnchors: Map<TableSeatSlot, TableAnchor>
) {
    fun seatAnchor(slot: TableSeatSlot): TableAnchor {
        return seatAnchors.getValue(slot)
    }
}

fun defaultTableLayoutAnchors(): TableLayoutAnchors {
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

fun leftSeatSlotForIndex(index: Int): TableSeatSlot {
    return when (index) {
        0 -> TableSeatSlot.LEFT_TOP
        1 -> TableSeatSlot.LEFT_MID
        else -> TableSeatSlot.LEFT_BOTTOM
    }
}

fun rightSeatSlotForIndex(index: Int): TableSeatSlot {
    return when (index) {
        0 -> TableSeatSlot.RIGHT_TOP
        1 -> TableSeatSlot.RIGHT_MID
        else -> TableSeatSlot.RIGHT_BOTTOM
    }
}

@Immutable
data class MarbleFlightVisual(
    val id: Long,
    val start: TablePoint,
    val end: TablePoint,
    val launchDelayMs: Int,
    val laneOffsetPx: Float
)

@Immutable
data class TargetArrowVisual(
    val actorId: Int,
    val targetIds: List<Int>
)


fun marbleTransferPoint(
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

fun buildMarbleFlights(
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

fun indicatorToneColor(tone: IndicatorTone): Color {
    return when (tone) {
        IndicatorTone.NEUTRAL -> Color(0xFFFFEB3B)
        IndicatorTone.GOOD -> Color(0xFF2196F3)
        IndicatorTone.BAD -> Color(0xFFFF0228)
        IndicatorTone.ALERT -> Color(0xFFFF9800)
    }
}

fun hatStripeColor(index: Int): Color {
    return if (index % 2 == 0) Color.White else Color.Black
}

fun resolvePlayerIdByName(players: List<PlayerState>, rawName: String): Int? {
    val trimmed = rawName.trim().removeSuffix(".")
    return players.firstOrNull { it.baseName.equals(trimmed, ignoreCase = true) }?.id
}

fun displayedChoiceFromLog(
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

fun splitTargetNames(rawNames: String): List<String> {
    return rawNames
        .split(" and ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

fun extractVisualIndicators(
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


@Composable
fun PhaseBadge(

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
fun PlayerStatusStack(
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
            indicatorPlacement = SeatIndicatorPlacement.BELOW,
            onCupAnchorMeasured = { measuredAnchor ->
                onCupAnchorMeasured(GameEngine.HUMAN_ID, measuredAnchor)
            }
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = "Score: $playerScore/${GameEngine.WIN_SCORE}",
            color = Color(0xFF2F38CE),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1
        )
    }
}

fun formatBotCupLabel(label: String): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return trimmed

    val hyphenIndex = trimmed.indexOf('-')
    if (hyphenIndex in 1 until trimmed.lastIndex) {
        return trimmed.substring(0, hyphenIndex + 1) + "\n" + trimmed.substring(hyphenIndex + 1)
    }

    val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size <= 1) return trimmed

    var bestSplitIndex = 1
    var smallestDifference = Int.MAX_VALUE

    for (index in 1 until words.size) {
        val firstLine = words.take(index).joinToString(" ")
        val secondLine = words.drop(index).joinToString(" ")
        val difference = kotlin.math.abs(firstLine.length - secondLine.length)
        if (difference < smallestDifference) {
            smallestDifference = difference
            bestSplitIndex = index
        }
    }

    return words.take(bestSplitIndex).joinToString(" ") + "\n" + words.drop(bestSplitIndex).joinToString(" ")
}

fun botCupLabelFontSize(label: String): TextUnit {
    val longestLineLength = formatBotCupLabel(label)
        .split("\n")
        .maxOfOrNull { it.length }
        ?: 0

    return when {
        longestLineLength <= 8 -> 13.sp
        longestLineLength <= 10 -> 12.sp
        longestLineLength <= 11 -> 11.sp
        else -> 10.sp
    }
}

fun botCupLabelLineHeight(fontSize: TextUnit): TextUnit {
    return when (fontSize) {
        13.sp -> 12.sp
        12.sp -> 11.sp
        11.sp -> 10.sp
        else -> 10.sp
    }
}

@Composable
fun BotCupColumn(
    bots: List<PlayerState>,
    currentActorId: Int?,
    hatHolderId: Int?,
    starterId: Int?,
    indicators: Map<Int, FloatingIndicator>,
    showMarbleCounts: Boolean,
    playersWithForcedMarbleCounts: Set<Int> = emptySet(),
    taggingEnabled: Boolean,
    avatarResourceNameForBot: (Int) -> String?,
    avatarGreyedOutForBot: (Int) -> Boolean,
    targetVisualStateForBot: (Int) -> TargetVisualState,
    onBotClicked: (Int) -> Unit,
    onBotNameClicked: (Int) -> Unit,
    onCupAnchorMeasured: (Int, TablePoint) -> Unit,
    indicatorPlacement: SeatIndicatorPlacement,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
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
                modifier = Modifier.alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarResourceName = avatarResourceNameForBot(bot.id)
                val avatarGreyedOut = avatarGreyedOutForBot(bot.id)
                val formattedLabel = formatBotCupLabel(bot.baseName)
                val labelFontSize = botCupLabelFontSize(bot.baseName)
                val cupClick = if (
                    targetVisualState == TargetVisualState.SELECTABLE ||
                    targetVisualState == TargetVisualState.SELECTED
                ) {
                    { onBotClicked(bot.id) }
                } else {
                    null
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (indicatorPlacement == SeatIndicatorPlacement.INSIDE_RIGHT) {
                        Column(
                            modifier = Modifier
                                .width(78.dp)
                                .padding(end = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BotAvatarIcon(
                                resourceName = avatarResourceName,
                                greyedOut = avatarGreyedOut
                            )

                            Text(
                                text = formattedLabel,
                                color = nameColor,
                                fontSize = labelFontSize,
                                lineHeight = botCupLabelLineHeight(labelFontSize),
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 24.dp)
                                    .then(
                                        if (taggingEnabled) {
                                            Modifier.clickable { onBotNameClicked(bot.id) }
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }

                    TableCup(
                        label = bot.baseName.take(1).uppercase(),
                        onClick = cupClick,
                        highlighted = false,
                        isCurrentTurn = currentActorId == bot.id,
                        indicator = indicators[bot.id],
                        hasHat = hatHolderId == bot.id,
                        isStarter = starterId == bot.id,
                        displayedChoice = bot.revealedChoice,
                        marbleCountText = if (showMarbleCounts || bot.id in playersWithForcedMarbleCounts) bot.marbles.toString() else null,
                        targetVisualState = targetVisualState,
                        indicatorPlacement = indicatorPlacement,
                        onCupAnchorMeasured = { measuredAnchor ->
                            onCupAnchorMeasured(bot.id, measuredAnchor)
                        }
                    )

                    if (indicatorPlacement == SeatIndicatorPlacement.INSIDE_LEFT) {
                        Column(
                            modifier = Modifier
                                .width(78.dp)
                                .padding(start = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BotAvatarIcon(
                                resourceName = avatarResourceName,
                                greyedOut = avatarGreyedOut
                            )

                            Text(
                                text = formattedLabel,
                                color = nameColor,
                                fontSize = labelFontSize,
                                lineHeight = botCupLabelLineHeight(labelFontSize),
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 24.dp)
                                    .then(
                                        if (taggingEnabled) {
                                            Modifier.clickable { onBotNameClicked(bot.id) }
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

fun botAvatarDrawableResourceId(
    context: android.content.Context,
    resourceName: String?
): Int {
    if (resourceName.isNullOrBlank()) return 0

    val staticResourceId = botAvatarStaticDrawableResourceId(resourceName)
    if (staticResourceId != 0) return staticResourceId

    val candidateNames = listOf(
        resourceName,
        "${resourceName}_avatar",
        "avatar_$resourceName",
        "bot_$resourceName",
        "icon_$resourceName"
    )

    return candidateNames
        .asSequence()
        .map { candidateName ->
            context.resources.getIdentifier(candidateName, "drawable", context.packageName)
        }
        .firstOrNull { resourceId -> resourceId != 0 }
        ?: 0
}

fun loadBotAvatarAsset(
    context: android.content.Context,
    resourceName: String?
): ImageBitmap? {
    if (resourceName.isNullOrBlank()) return null

    val candidatePaths = listOf(
        "icons/$resourceName.png",
        "icons/$resourceName.jpg",
        "icons/$resourceName.webp",
        "$resourceName.png",
        "$resourceName.jpg",
        "$resourceName.webp"
    )

    return candidatePaths.firstNotNullOfOrNull { candidatePath ->
        runCatching {
            context.assets.open(candidatePath).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

fun greyedOutAvatarResourceNames(resourceName: String?): List<String> {
    if (resourceName.isNullOrBlank()) return emptyList()

    return listOf(
        "${resourceName}_grey",
        "${resourceName}_gray",
        "${resourceName}_greyed",
        "${resourceName}_grayed",
        "${resourceName}_greyed_out",
        "${resourceName}_grayed_out",
        "grey_$resourceName",
        "gray_$resourceName"
    )
}

fun greyedOutAvatarFilter(): ColorFilter {
    val matrix = ColorMatrix()
    matrix.setToSaturation(0f)
    return ColorFilter.colorMatrix(matrix)
}

@Composable
fun BotAvatarIcon(
    resourceName: String?,
    greyedOut: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayResourceName = remember(resourceName, greyedOut) {
        if (greyedOut) {
            greyedOutAvatarResourceNames(resourceName)
        } else {
            emptyList()
        }
    }
    val displayResourceId = remember(resourceName, greyedOut) {
        displayResourceName
            .asSequence()
            .map { candidateName -> botAvatarDrawableResourceId(context, candidateName) }
            .firstOrNull { resourceId -> resourceId != 0 }
            ?: 0
    }
    val normalResourceId = remember(resourceName) {
        botAvatarDrawableResourceId(context, resourceName)
    }
    val resourceId = if (displayResourceId != 0) displayResourceId else normalResourceId
    val assetBitmap = remember(resourceName, displayResourceName, resourceId) {
        if (resourceId == 0) {
            val greyedAsset = if (greyedOut) {
                displayResourceName.firstNotNullOfOrNull { candidateName ->
                    loadBotAvatarAsset(context, candidateName)
                }
            } else {
                null
            }

            greyedAsset ?: loadBotAvatarAsset(context, resourceName)
        } else {
            null
        }
    }
    val useGeneratedGreyFilter = greyedOut && displayResourceId == 0
    val greyFilter = remember(useGeneratedGreyFilter) {
        if (useGeneratedGreyFilter) greyedOutAvatarFilter() else null
    }

    Box(
        modifier = modifier.size(66.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            resourceId != 0 -> {
                Image(
                    painter = painterResource(id = resourceId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = greyFilter,
                    modifier = Modifier
                        .size(110.dp)
                        .alpha(if (useGeneratedGreyFilter) 0.72f else 1f)
                )
            }

            assetBitmap != null -> {
                Image(
                    bitmap = assetBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = greyFilter,
                    modifier = Modifier
                        .size(110.dp)
                        .alpha(if (useGeneratedGreyFilter) 0.72f else 1f)
                )
            }
        }
    }
}

@Composable
fun TableCup(
    label: String,
    highlighted: Boolean,
    isCurrentTurn: Boolean = false,
    indicator: FloatingIndicator? = null,
    hasHat: Boolean = false,
    isStarter: Boolean = false,
    displayedChoice: Int? = null,
    marbleCountText: String? = null,
    targetVisualState: TargetVisualState = TargetVisualState.NORMAL,
    indicatorPlacement: SeatIndicatorPlacement = SeatIndicatorPlacement.BELOW,
    scale: Float = 1f,
    onClick: (() -> Unit)? = null,
    onCupAnchorMeasured: ((TablePoint) -> Unit)? = null
) {
    val cupContentAlpha = when (targetVisualState) {
        TargetVisualState.DISABLED -> 0.78f
        else -> 1f
    }
    val holderWidth = (58.dp * scale).coerceAtLeast(38.dp)
    val cupBodyWidth = (40.dp * scale).coerceAtLeast(28.dp)
    val cupBodyHeight = (46.dp * scale).coerceAtLeast(32.dp)
    val holderHeight = when (indicatorPlacement) {
        SeatIndicatorPlacement.BELOW -> (78.dp * scale).coerceAtLeast(58.dp)
        SeatIndicatorPlacement.INSIDE_LEFT,
        SeatIndicatorPlacement.INSIDE_RIGHT -> cupBodyHeight
    }
    val cupImageWidth = (72.dp * scale).coerceAtLeast(48.dp)
    val cupImageHeight = (84.dp * scale).coerceAtLeast(56.dp)
    val cupImageOffsetY = 8.dp * scale
    val cupBodyTopPadding = when (indicatorPlacement) {
        SeatIndicatorPlacement.BELOW -> 4.dp * scale
        SeatIndicatorPlacement.INSIDE_LEFT,
        SeatIndicatorPlacement.INSIDE_RIGHT -> 0.dp
    }
    val floatingIndicatorTopPadding = when (indicatorPlacement) {
        SeatIndicatorPlacement.BELOW -> 12.dp * scale
        SeatIndicatorPlacement.INSIDE_LEFT,
        SeatIndicatorPlacement.INSIDE_RIGHT -> 8.dp * scale
    }

    Box(
        modifier = Modifier
            .size(width = holderWidth, height = holderHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = cupBodyTopPadding)
                .size(width = cupBodyWidth, height = cupBodyHeight)
                .onGloballyPositioned { coordinates ->
                    onCupAnchorMeasured?.invoke(coordinates.boundsInRoot().cupLandingPoint())
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.redbucket),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = cupImageWidth, height = cupImageHeight)
                    .offset(y = cupImageOffsetY)
                    .graphicsLayer {
                        scaleX = 1.4f
                        scaleY = 1.4f
                    }
                    .alpha(cupContentAlpha)
            )

            if (marbleCountText != null) {
                Text(
                    text = marbleCountText,
                    color = Color.White.copy(alpha = cupContentAlpha),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = max(9f, 14f * scale).sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = cupImageOffsetY)
                        .fillMaxWidth()
                )
            }

            if (indicatorPlacement == SeatIndicatorPlacement.INSIDE_LEFT || indicatorPlacement == SeatIndicatorPlacement.INSIDE_RIGHT) {
                SideSeatIndicatorStack(
                    isStarter = isStarter,
                    hasHat = hasHat,
                    displayedChoice = displayedChoice,
                    scale = scale,
                    modifier = Modifier
                        .align(
                            if (indicatorPlacement == SeatIndicatorPlacement.INSIDE_LEFT) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            }
                        )
                        .zIndex(2f)
                        .padding(
                            start = if (indicatorPlacement == SeatIndicatorPlacement.INSIDE_LEFT) (4.dp * scale).coerceAtLeast(2.dp) else 0.dp,
                            end = if (indicatorPlacement == SeatIndicatorPlacement.INSIDE_RIGHT) (4.dp * scale).coerceAtLeast(2.dp) else 0.dp
                        )
                )
            }
        }

        if (indicatorPlacement == SeatIndicatorPlacement.BELOW) {
            SeatIndicatorLane(
                isStarter = isStarter,
                hasHat = hasHat,
                displayedChoice = displayedChoice,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
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
                    .align(Alignment.Center)
                    .zIndex(3f)
                    .padding(top = floatingIndicatorTopPadding)
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
                    fontSize = max(7f, 9f * scale).sp,
                    modifier = Modifier.padding(
                        horizontal = (6.dp * scale).coerceAtLeast(3.dp),
                        vertical = (2.dp * scale).coerceAtLeast(1.dp)
                    )
                )
            }
        }
    }
}

@Composable
fun SeatIndicatorLane(
    isStarter: Boolean,
    hasHat: Boolean,
    displayedChoice: Int?,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val starterSize = (18.dp * scale).coerceAtLeast(11.dp)
    val hatSize = (14.dp * scale).coerceAtLeast(9.dp)
    val choiceSize = (16.dp * scale).coerceAtLeast(10.dp)
    val spacing = (1.dp * scale).coerceAtLeast(0.5.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IndicatorSprite(
            drawableRes = if (isStarter) R.drawable.starter else null,
            contentDescription = if (isStarter) "Starter" else null,
            size = starterSize,
            visualScale = 2.8f
        )

        Spacer(Modifier.width(spacing))

        IndicatorSprite(
            drawableRes = if (hasHat) R.drawable.thehat else null,
            contentDescription = if (hasHat) "Hat" else null,
            size = hatSize,
            visualScale = 2.5f
        )

        Spacer(Modifier.width(spacing))

        IndicatorSprite(
            drawableRes = when (displayedChoice) {
                0 -> R.drawable.choicezero
                1 -> R.drawable.choiceone
                3 -> R.drawable.choicethree
                else -> R.drawable.choicenone
            },
            contentDescription = "Choice",
            size = choiceSize,
            visualScale = 1.25f
        )
    }
}

@Composable
fun SideSeatIndicatorStack(
    isStarter: Boolean,
    hasHat: Boolean,
    displayedChoice: Int?,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val starterSize = (14.dp * scale).coerceAtLeast(11.dp)
    val hatSize = (12.dp * scale).coerceAtLeast(10.dp)
    val choiceSize = (14.dp * scale).coerceAtLeast(11.dp)
    val spacing = (0.5.dp * scale).coerceAtLeast(0.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IndicatorSprite(
            drawableRes = if (isStarter) R.drawable.starter else null,
            contentDescription = if (isStarter) "Starter" else null,
            size = starterSize,
            visualScale = 2.6f
        )

        IndicatorSprite(
            drawableRes = if (hasHat) R.drawable.thehat else null,
            contentDescription = if (hasHat) "Hat" else null,
            size = hatSize,
            visualScale = 2.3f
        )

        IndicatorSprite(
            drawableRes = when (displayedChoice) {
                0 -> R.drawable.choicezero
                1 -> R.drawable.choiceone
                3 -> R.drawable.choicethree
                else -> R.drawable.choicenone
            },
            contentDescription = "Choice",
            size = choiceSize,
            visualScale = 1.2f
        )
    }
}

@Composable
fun IndicatorSprite(
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
fun GameTableSurface(
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
fun TableCenterBowl(
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
fun TargetArrowOverlay(
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

fun DrawScope.drawTargetArrow(
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
fun MarbleFlightOverlay(
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
fun AnimatedMarbleFlight(
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


fun weatherIdForName(weatherName: String?): String? {
    if (weatherName.isNullOrBlank()) return null
    return Weather.allCards.firstOrNull { it.displayName == weatherName }?.id
}

@Composable
fun WeatherTransitionOverlay(
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
fun WeatherIconCanvas(
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

        "snow" -> {
            cloud()
            snowFlake(size.width * 0.34f, size.height * 0.72f, 0.34f)
            snowFlake(size.width * 0.50f, size.height * 0.78f, 0.28f)
            snowFlake(size.width * 0.66f, size.height * 0.70f, 0.36f)
        }

        "whiteout" -> {
            cloud(offsetY = unit * 0.08f)
            windBand(size.height * 0.62f, curve = unit * 0.01f)
            windBand(size.height * 0.74f, curve = unit * 0.01f)
            drawOval(
                color = tint.copy(alpha = 0.18f),
                topLeft = Offset(size.width * 0.18f, size.height * 0.52f),
                size = Size(size.width * 0.64f, size.height * 0.22f)
            )
        }

        "smog" -> {
            cloud(offsetY = unit * 0.02f)
            drawOval(
                color = tint.copy(alpha = 0.16f),
                topLeft = Offset(size.width * 0.18f, size.height * 0.22f),
                size = Size(size.width * 0.64f, size.height * 0.20f)
            )
            ring(size.width * 0.50f, size.height * 0.68f, unit * 0.12f, width = stroke * 0.46f)
        }

        else -> {
            cloud()
        }
    }
}



@Composable
fun LogPanel(
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

fun engineSnapshot(engine: GameEngine): RoundResult {
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
        marbleTransfers = emptyList(),
        botArchetypeNamesByPlayerId = emptyMap(),
        smogRevealedPlayerIds = emptySet(),
        humanHeldHatThisGame = false,
        humanCorrectGuessesThisGame = 0,
        humanSubmittedTargetThisGame = false,
        humanStartedAnyRoundThisGame = false,
        humanPerfectBonusIntact = true
    )
}

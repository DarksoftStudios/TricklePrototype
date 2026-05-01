package com.example.trickleprototype

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun SimpleDialog(
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
fun HowToPlayText() {
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
fun AdvancedTipsText() {
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
                "- On Hard mode, there is no Log to review, and Boss archetypes appear.\n" +
                "- Click on a bot's name to 'tag' them as the archetype you think they are.\n\n" +
                "JESTER'S HAT RULE:\n" +
                "- If you guess 1 or 3 on someone who actually chose 0, you lose 1 marble and take the Jester's Hat.\n" +
                "- If you guess 0 on someone who chose 0, you lose 0 marbles and take the Jester's Hat.\n" +
                "- Next round, the Hat-holder goes first BUT only if the Hat ended the round on a different person than it started.\n"
    )
}

@Composable
fun ArchetypesText() {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ArchetypeRuleEntry(
            resourceName = "auditor",
            text = "Auditor:\n" +
                    "- Hates wallflowers. Targets anyone who passes too much\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Passes rounds 1 and 2, targets pass-streak players after\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "avenger",
            text = "Avenger:\n" +
                    "- Retaliates against attackers, even if they didn't attack him\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Passes round 1; targets attackers from round 2 on\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "bully",
            text = "Bully:\n" +
                    "- Picks on the meek\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: Targets players who chose 1 last round\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "chaos",
            text = "Chaos:\n" +
                    "- The Matriarch of RNG. All random everything\n" +
                    "- Chooses: 33% chance of 0/1/3\n" +
                    "- Targeting Behavior: 50% pass, 50% target\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "cynic",
            text = "Cynic:\n" +
                    "- Assumes everyone is greedy and punishes it\n" +
                    "- Chooses: 1\n" +
                    "- Targeting Behavior: Targets players who chose 3 last round\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "echo",
            text = "Echo:\n" +
                    "- Stop copying me! StOp CoPyInG mE!\n" +
                    "- Chooses: 3 on round 1, then copies the Player's choice last round\n" +
                    "- Targeting Behavior: Copies whether the Player's action last round\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "jester",
            text = "Jester:\n" +
                    "- Will gladly pay a marble to snag the Jester's Hat\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Passes first; then targets recently guessed players\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "juliet",
            text = "Juliet:\n" +
                    "- She's in it to win it, or at least watch Romeo win\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Anyone but Romeo\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "cabal",
            text = "Cabal:\n" +
                    "- Picks a 'King' and only attacks people who attack that King\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Usually passes; targets only to avenge their King\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "limper",
            text = "Limper:\n" +
                    "- Seems like maybe they don't want to play\n" +
                    "- Chooses: 1 (0 if attacked and defending)\n" +
                    "- Targeting Behavior: Always passes\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "lurker",
            text = "Lurker:\n" +
                    "- Waits, watches, then punishes repeated 3 behavior\n" +
                    "- Chooses: 1 (unless close to winning)\n" +
                    "- Targeting Behavior: Passes for 2 rounds, then targets repeat-3 players\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "nemesis",
            text = "Nemesis:\n" +
                    "- Cut this guy off and he is tailgating you to your house\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Passes until attacked; then relentlessly seeks revenge\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "pacifist",
            text = "Pacifist:\n" +
                    "- Greedy but peaceful. Tries to win by Trickle alone\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: Always passes\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "pitfall",
            text = "Pitfall:\n" +
                    "- Seems like a treasure chest, actually a beartrap\n" +
                    "- Chooses: 3 on round 1, 0 on round 2, then random between 0 and 3\n" +
                    "- Targeting Behavior: Always passes\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "romeo",
            text = "Romeo:\n" +
                    "- His eyes are on the prize(and on Juliet, of course)\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Juliet is safe, everyone else is a target\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "scout",
            text = "Scout:\n" +
                    "- Will gladly be the first to act\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Always targets, even on round 1\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "strobe",
            text = "Strobe:\n" +
                    "- Moody is an understatement\n" +
                    "- Chooses: Alternates 1,3,1,3...\n" +
                    "- Targeting Behavior: Alternates pass/target\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "hunter",
            text = "Hunter:\n" +
                    "- [Hard-mode boss] Fixates on the Player whenever possible\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: Targets the Player and guesses 3; if the Player cannot be targeted, hunts known 3-choosers\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "seer",
            text = "Seer:\n" +
                    "- [Hard-mode boss] Sees hidden choices and score totals\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: Targets the highest-scoring player who chose 3\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "mirror",
            text = "Mirror:\n" +
                    "- [Hard-mode boss] Copies the Player at the exact same time\n" +
                    "- Chooses: Whatever the Player chose this round\n" +
                    "- Targeting Behavior: Copies the Player's pass or target choice and looks for the same guess type\n\n"
        )
    }
}

@Composable
fun ArchetypeRuleEntry(
    resourceName: String,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotAvatarIcon(
            resourceName = resourceName,
            greyedOut = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 6.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StatsText(stats: PlayerStats) {
    val acc = if (stats.totalGuesses == 0) ""
    else "${((stats.correctGuesses * 100.0) / stats.totalGuesses).toInt()}%"

    Column {
        Text(
            "Total games: ${stats.totalGames}\n" +
                    "Wins: ${stats.totalWins}\n\n" +
                    "Lifetime earned: ${stats.lifetimeMarblesEarned}\n" +
                    "Vault: ${stats.vaultMarbles}\n\n" +
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

private data class BonusMarbleVisual(
    val id: Int,
    val rowIndex: Int,
    val amountIndex: Int,
    val rowAmount: Int,
    val rowColumn: Int,
    val rowStack: Int,
    val rowDelayMs: Int,
    val fallDelayMs: Int,
    val wobble: Float
)

@Composable
fun BonusMarblesText(payout: BonusMarblePayout) {
    BonusMarblesAnimationOverlay(
        payout = payout,
        onFinished = {}
    )
}

@Composable
fun BonusMarblesAnimationOverlay(
    payout: BonusMarblePayout,
    onFinished: () -> Unit
) {
    val total = payout.total.coerceAtLeast(0)
    val titleBeatMs = 520
    val rowBeatMs = 420
    val marbleStepMs = 70
    val marbleSlideMs = 360
    val finalBeatMs = 620
    val textFadeMs = 420
    val fallPauseMs = 180

    val marbleVisuals = remember(payout) {
        val visuals = mutableListOf<BonusMarbleVisual>()
        var nextId = 0
        payout.rows.forEachIndexed { rowIndex, row ->
            repeat(row.amount.coerceAtLeast(0)) { amountIndex ->
                val id = nextId
                visuals += BonusMarbleVisual(
                    id = id,
                    rowIndex = rowIndex,
                    amountIndex = amountIndex,
                    rowAmount = row.amount.coerceAtLeast(0),
                    rowColumn = amountIndex,
                    rowStack = 0,
                    rowDelayMs = amountIndex * marbleStepMs,
                    fallDelayMs = (id * 22) + ((amountIndex % 5) * 18),
                    wobble = ((id % 7) - 3) * 0.06f
                )
                nextId += 1
            }
        }
        visuals
    }

    var visibleRowCount by remember(payout) { mutableIntStateOf(0) }
    var activeCountingRow by remember(payout) { mutableIntStateOf(-1) }
    var countedMarbleCount by remember(payout) { mutableIntStateOf(0) }
    var collapseStarted by remember(payout) { mutableStateOf(false) }
    var rowsFaded by remember(payout) { mutableStateOf(false) }
    var finalFlash by remember(payout) { mutableStateOf(false) }

    val rowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (rowsFaded) 0f else 1f,
        animationSpec = tween(durationMillis = textFadeMs, easing = FastOutSlowInEasing),
        label = "bonusRowsAlpha"
    )
    val collectorAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (rowsFaded || total <= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "bonusCollectorAlpha"
    )
    val totalColor by animateColorAsState(
        targetValue = if (finalFlash) Color(0xFFFFFFFF) else Color(0xFF80D8FF),
        animationSpec = tween(durationMillis = 120),
        label = "bonusMarbleTotalFlash"
    )

    LaunchedEffect(payout) {
        visibleRowCount = 0
        activeCountingRow = -1
        countedMarbleCount = 0
        collapseStarted = false
        rowsFaded = false
        finalFlash = false

        delay(titleBeatMs.toLong())

        if (payout.rows.isEmpty() || total <= 0) {
            delay(finalBeatMs.toLong())
            rowsFaded = true
            delay(textFadeMs.toLong())
            finalFlash = true
            delay(900L)
            onFinished()
            return@LaunchedEffect
        }

        payout.rows.forEachIndexed { rowIndex, row ->
            visibleRowCount = rowIndex + 1
            activeCountingRow = -1
            delay(rowBeatMs.toLong())

            activeCountingRow = rowIndex
            val rowCountTime = (row.amount.coerceAtLeast(0) * marbleStepMs) + marbleSlideMs
            delay(rowCountTime.toLong())

            activeCountingRow = -1
            delay(rowBeatMs.toLong())
        }

        delay(finalBeatMs.toLong())
        rowsFaded = true
        delay((textFadeMs + fallPauseMs).toLong())

        collapseStarted = true

        val longestFallMs = marbleVisuals.maxOfOrNull {
            it.fallDelayMs + 960 + ((it.id % 5) * 40)
        } ?: 960
        delay(longestFallMs.toLong())

        finalFlash = true
        delay(1000L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(Color.Black.copy(alpha = 0.46f)),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(560.dp)
                .border(
                    BorderStroke(2.dp, Color(0xFF80D8FF)),
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    Color.Black.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            val panelWidth = maxWidth
            val jugX = panelWidth * 0.50f
            val jugY = maxHeight * 0.70f
            val bonusTitleHeight = 32.dp
            val bonusTitleGap = 12.dp
            val bonusRowHeight = 38.dp
            val bonusRowGap = 12.dp
            val rowUnderlineStartY = bonusTitleHeight + bonusTitleGap + bonusRowHeight
            val rowSpacing = bonusRowHeight + bonusRowGap

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(rowAlpha)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(bonusRowGap)
            ) {
                Text(
                    text = "Marbles Gained",
                    color = Color(0xFF80D8FF),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bonusTitleHeight)
                )

                payout.rows.forEachIndexed { index, row ->
                    val rowVisible = index < visibleRowCount
                    val displayLabel = if (index == 0) "Score" else row.label

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bonusRowHeight)
                            .alpha(if (rowVisible) 1f else 0f)
                    ) {
                        val amountText = if (index == 0) "${row.amount}" else "+${row.amount}"

                        Text(
                            text = if (rowVisible) "$displayLabel: $amountText" else "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.White.copy(alpha = if (rowVisible) 0.75f else 0f))
                        )
                    }
                }
            }

            marbleVisuals
                .filter { it.rowIndex < visibleRowCount }
                .forEach { marble ->
                    AnimatedBonusMarble(
                        marble = marble,
                        panelWidth = panelWidth,
                        rowUnderlineStartY = rowUnderlineStartY,
                        rowSpacing = rowSpacing,
                        jugX = jugX,
                        jugY = jugY,
                        countingStarted = activeCountingRow >= marble.rowIndex,
                        collapseStarted = collapseStarted,
                        onEnteredJug = {
                            countedMarbleCount += 1
                        }
                    )
                }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .graphicsLayer(alpha = collectorAlpha),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.jug),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(296.dp)
                )

                Text(
                    text = "${payout.startingVaultMarbles + countedMarbleCount}",
                    color = totalColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .graphicsLayer(
                            scaleX = if (finalFlash) 1.14f else 1f,
                            scaleY = if (finalFlash) 1.14f else 1f
                        )
                )
            }

            Text(
                text = "+${payout.total} Marbles",
                color = totalColor,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .graphicsLayer(
                        alpha = collectorAlpha,
                        scaleX = if (finalFlash) 1.10f else 1f,
                        scaleY = if (finalFlash) 1.10f else 1f
                    )
            )
        }
    }
}

@Composable
private fun AnimatedBonusMarble(
    marble: BonusMarbleVisual,
    panelWidth: androidx.compose.ui.unit.Dp,
    rowUnderlineStartY: androidx.compose.ui.unit.Dp,
    rowSpacing: androidx.compose.ui.unit.Dp,
    jugX: androidx.compose.ui.unit.Dp,
    jugY: androidx.compose.ui.unit.Dp,
    countingStarted: Boolean,
    collapseStarted: Boolean,
    onEnteredJug: () -> Unit
) {
    val rowProgress = remember(marble.id) { Animatable(0f) }
    val fallProgress = remember(marble.id) { Animatable(0f) }
    var counted by remember(marble.id) { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val launchDurationMs = 360 + ((marble.id % 4) * 35)
    val fallDurationMs = 960 + ((marble.id % 5) * 40)

    LaunchedEffect(countingStarted) {
        if (!countingStarted) return@LaunchedEffect
        delay(marble.rowDelayMs.toLong())
        rowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = launchDurationMs, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(collapseStarted) {
        if (!collapseStarted) return@LaunchedEffect
        delay(marble.fallDelayMs.toLong())
        fallProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = fallDurationMs, easing = FastOutSlowInEasing)
        )
    }

    val marbleImageSize = 28.dp
    val visibleMarbleDiameter = marbleImageSize
    val visibleMarbleCenterCorrectionX = 0.dp
    val visibleMarbleCenterCorrectionY = 0.dp
    val marbleLineGap = 5.dp

    val lineRightVisibleCenterX = panelWidth - 42.dp
    val lineLeftVisibleCenterX = panelWidth * 0.36f
    val usableLineWidth = lineRightVisibleCenterX - lineLeftVisibleCenterX

    val tightVisibleCenterSpacing = visibleMarbleDiameter * 0.70f
    val compactVisibleCenterSpacing = usableLineWidth / (marble.rowAmount - 1).coerceAtLeast(1).toFloat()
    val visibleCenterSpacing = if (compactVisibleCenterSpacing < tightVisibleCenterSpacing) {
        compactVisibleCenterSpacing
    } else {
        tightVisibleCenterSpacing
    }

    val settledVisibleCenterX = lineRightVisibleCenterX - (marble.amountIndex * visibleCenterSpacing.value).dp
    val settledVisibleCenterY = rowUnderlineStartY + (rowSpacing * marble.rowIndex.toFloat()) - (visibleMarbleDiameter / 2f) - marbleLineGap
    val settledX = settledVisibleCenterX - (marbleImageSize / 2f) - visibleMarbleCenterCorrectionX
    val settledY = settledVisibleCenterY - (marbleImageSize / 2f) - visibleMarbleCenterCorrectionY

    val spawnX = panelWidth + 42.dp
    val spawnY = settledY

    val rowP = rowProgress.value
    val fallP = fallProgress.value

    LaunchedEffect(fallP >= 0.82f) {
        if (fallP >= 0.82f && !counted) {
            counted = true
            onEnteredJug()
        }
    }

    val slideLift = 10.dp * (1f - kotlin.math.abs((rowP * 2f) - 1f))
    val lineX = spawnX + (settledX - spawnX) * rowP
    val lineY = spawnY + (settledY - spawnY) * rowP - slideLift

    val wobbleX = (kotlin.math.sin((fallP * 3.14f) + marble.wobble) * 10f).dp
    val jugTargetX = jugX - 16.dp + ((marble.id % 7) * 5).dp
    val jugTargetY = jugY + 64.dp + ((marble.id % 4) * 5).dp

    val fallLift = 42.dp * (1f - kotlin.math.abs((fallP * 2f) - 1f))
    val x = lineX + (jugTargetX - lineX) * fallP + wobbleX
    val y = lineY + (jugTargetY - lineY) * fallP - fallLift
    val hitFadeProgress = ((fallP - 0.78f) / 0.22f).coerceIn(0f, 1f)
    val alpha = rowP * (1f - hitFadeProgress)
    val scale = 1f - (hitFadeProgress * 0.45f)

    Image(
        painter = painterResource(R.drawable.marble),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(marbleImageSize)
            .graphicsLayer(
                translationX = with(density) { x.toPx() },
                translationY = with(density) { y.toPx() },
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
            .zIndex(3f + (marble.amountIndex * 0.001f))
    )
}

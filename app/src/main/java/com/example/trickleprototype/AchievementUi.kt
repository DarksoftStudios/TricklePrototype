package com.example.trickleprototype

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AchievementUnlockOverlay(
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
                        Spacer(Modifier.height(2.dp))
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
fun AchievementsText(stats: PlayerStats) {
    Column {
        Spacer(Modifier.height(16.dp))
        Text("(Unlock on Normal or Hard)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))

        AchievementSectionHeader("Core")
        AchievementRow(stats.firstPerfectWin, "Perfect Puddler", "Win with 0 wrong guesses")
        AchievementRow(stats.reachedRound6, "Idle Hands", "Reach round 6")
        AchievementRow(stats.wonWith18Marbles, "Is That Legal?", "Win with 18+ marbles")

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Difficulty")
        AchievementRow(stats.wonEasy, "Comp Stomp", "Won on Easy")
        AchievementRow(stats.wonNormal, "Pattern Finder", "Won on Normal")
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
        AchievementRow(stats.dieting, "Dieting", "Correctly guess Glutton's 3 four times in one game")
        AchievementRow(stats.copycat, "Copycat", "Lose when Echo wins")

        Spacer(Modifier.height(14.dp))
        AchievementSectionHeader("Bosses")
        AchievementRow(stats.beatHunter, "Marked Prey", "Win against Hunter")
        AchievementRow(stats.beatSeer, "Blind Prophet", "Win against Seer")
        AchievementRow(stats.beatMirror, "Broken Reflection", "Win against Mirror")
        AchievementRow(stats.bossSlayer, "Boss Slayer", "Win against every boss")

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
            desc = "Experience every unique weather across completed games",
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


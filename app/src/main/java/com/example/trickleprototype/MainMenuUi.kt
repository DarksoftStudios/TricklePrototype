package com.example.trickleprototype

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun MainMenuScreen(
    activity: Activity?,
    onNavigate: (AppScreen) -> Unit,
    onDevBonusMarbles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuLinkButton(text = "PLAY") { onNavigate(AppScreen.PLAY) }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "RULES") { onNavigate(AppScreen.RULES) }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "PROFILE") { onNavigate(AppScreen.PROFILE) }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "SETTINGS") { onNavigate(AppScreen.SETTINGS) }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "QUIT") { activity?.finish() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "DEV BONUS MARBLES") { onDevBonusMarbles() }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun PlayMenuScreen(
    stats: PlayerStats,
    weatherEnabled: Boolean,
    onWeatherEnabledChange: (Boolean) -> Unit,
    onStartGame: (Difficulty) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val normalUnlocked = stats.easyGames > 0
    val hardUnlocked = stats.normalWins > 0
    val weatherUnlocked = stats.wonHard

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
                    !weatherUnlocked -> "WEATHER:(LOCKED)"
                    weatherEnabled -> "WEATHER: ON"
                    else -> "WEATHER: OFF"
                }
            ) {
                if (weatherUnlocked) {
                    onWeatherEnabledChange(!weatherEnabled)
                } else {
                    onWeatherEnabledChange(true)
                    Toast.makeText(context, "Win on HARD to unlock WEATHER", Toast.LENGTH_SHORT).show()
                }
            }

            Spacer(Modifier.height(2.dp))
            MenuLinkButton(text = "EASY") { onStartGame(Difficulty.EASY) }

            Spacer(Modifier.height(2.dp))
            MenuLinkButton(
                text = if (normalUnlocked) "NORMAL" else "NORMAL (LOCKED)"
            ) {
                if (normalUnlocked) {
                    onStartGame(Difficulty.NORMAL)
                } else {
                    Toast.makeText(context, "Finish a game to unlock NORMAL", Toast.LENGTH_SHORT).show()
                }
            }

            Spacer(Modifier.height(2.dp))
            MenuLinkButton(
                text = if (hardUnlocked) "HARD" else "HARD (LOCKED)"
            ) {
                if (hardUnlocked) {
                    onStartGame(Difficulty.HARD)
                } else {
                    Toast.makeText(context, "Win a game on NORMAL to unlock HARD", Toast.LENGTH_SHORT).show()
                }
            }

            Spacer(Modifier.height(2.dp))
            MenuLinkButton(text = "BACK") { onBack() }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun RulesMenuScreen(
    onHowToPlay: () -> Unit,
    onAdvancedTips: () -> Unit,
    onArchetypes: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuLinkButton(text = "HOW TO PLAY") { onHowToPlay() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "ADVANCED TIPS") { onAdvancedTips() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "ARCHETYPES") { onArchetypes() }

        Spacer(Modifier.height(16.dp))
        MenuLinkButton(text = "BACK") { onBack() }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
fun ProfileMenuScreen(
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    onCustomize: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuLinkButton(text = "STATS") { onStats() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "ACHIEVEMENTS") { onAchievements() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "CUSTOMIZE") { onCustomize() }

        Spacer(Modifier.height(16.dp))
        MenuLinkButton(text = "BACK") { onBack() }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun CustomizeMenuScreen(
    playerName: String,
    onSaveName: (String) -> Unit,
    onBack: () -> Unit
) {
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
                onSaveName(cleaned)
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Text("Save")
        }

        Spacer(Modifier.height(16.dp))
        MenuLinkButton(text = "BACK") { onBack() }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsMenuScreen(
    showResetStatsConfirm: Boolean,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    passTargetConfirmEnabled: Boolean,
    onDismissResetStatsConfirm: () -> Unit,
    onConfirmResetStats: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onTogglePassConfirm: () -> Unit,
    onResetStats: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showResetStatsConfirm) {
            AlertDialog(
                onDismissRequest = { onDismissResetStatsConfirm() },
                title = { Text("ARE YOU SURE?") },
                text = { Text("This will reset all stats and unlocks.") },
                confirmButton = {
                    TextButton(
                        onClick = { onConfirmResetStats() }
                    ) { Text("RESET") }
                },
                dismissButton = {
                    TextButton(onClick = { onDismissResetStatsConfirm() }) {
                        Text("CANCEL")
                    }
                },
                properties = DialogProperties(dismissOnClickOutside = true)
            )
        }

        MenuLinkButton(
            text = if (soundEnabled) "SOUND: ON" else "SOUND: OFF"
        ) {
            onToggleSound()
        }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(
            text = if (musicEnabled) "MUSIC: ON" else "MUSIC: OFF"
        ) {
            onToggleMusic()
        }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(
            text = if (passTargetConfirmEnabled) "PASS CONFIRM: ON" else "PASS CONFIRM: OFF"
        ) {
            onTogglePassConfirm()
        }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "RESET STATS") {
            onResetStats()
        }

        Spacer(Modifier.height(16.dp))
        MenuLinkButton(text = "BACK") { onBack() }

        Spacer(Modifier.height(24.dp))
    }
}


@Composable
fun DifficultyEntryTransitionOverlay(
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
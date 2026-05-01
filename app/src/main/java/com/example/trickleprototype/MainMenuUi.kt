package com.example.trickleprototype

import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun MainMenuScreen(
    activity: Activity?,
    quickplayGameLabel: String,
    onNavigate: (AppScreen) -> Unit,
    onQuickplay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LargePlayButton(
            quickplayGameLabel = quickplayGameLabel,
            onPlay = { onNavigate(AppScreen.PLAY) },
            onQuickplay = onQuickplay
        )

        Spacer(Modifier.height(10.dp))
        MenuLinkButton(text = "MORE") { onNavigate(AppScreen.MORE) }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun LargePlayButton(
    quickplayGameLabel: String,
    onPlay: () -> Unit,
    onQuickplay: () -> Unit
) {
    OutlinedButton(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth(0.90f)
            .height(258.dp),
        shape = CloudButtonShape,
        border = androidx.compose.foundation.BorderStroke(3.dp, androidx.compose.ui.graphics.Color(0xFF9AA3AD)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFF2F7FF),
            contentColor = androidx.compose.ui.graphics.Color(0xFF9AA3AD)
        ),
        contentPadding = PaddingValues(vertical = 22.dp, horizontal = 26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PLAY",
                modifier = Modifier.padding(top = 36.dp),
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                fontSize = 38.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onQuickplay,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFE4F0FF),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF65707C)
                ),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "QUICKPLAY:",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontSize = 16.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = quickplayGameLabel.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MoreMenuScreen(
    onRules: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuLinkButton(text = "RULES") { onRules() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "PROFILE") { onProfile() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "SETTINGS") { onSettings() }
        Spacer(Modifier.height(2.dp))
        MenuLinkButton(text = "SHOP") { onShop() }

        Spacer(Modifier.height(16.dp))
        MenuLinkButton(text = "BACK") { onBack() }

        Spacer(Modifier.height(24.dp))
    }
}

data class ShopColorItem(
    val id: String,
    val label: String,
    val color: Color
)

private data class PendingShopPurchase(
    val id: String,
    val label: String,
    val category: String,
    val cost: Long,
    val onConfirm: (String) -> Unit
)

private data class ShopUpgradePlaceholder(
    val name: String,
    val cost: Long,
    val description: String
)

private val ShopUpgradePlaceholders = listOf(
    ShopUpgradePlaceholder(
        name = "Faucet",
        cost = 11L,
        description = "At the end of the game, earn 1 extra marble if you had a 1 trickle through untargeted."
    ),
    ShopUpgradePlaceholder(
        name = "Hose",
        cost = 333L,
        description = "At the end of the game, earn 3 extra marbles if you had a 1 trickle through untargeted."
    ),
    ShopUpgradePlaceholder(
        name = "Rooster",
        cost = 113L,
        description = "Earn 13 extra marbles on the first game you play every 24 hours."
    ),
    ShopUpgradePlaceholder(
        name = "Weather Vane",
        cost = 333L,
        description = "Earn 33 extra marbles on the first game you win every 24 hours."
    ),
    ShopUpgradePlaceholder(
        name = "Community Board",
        cost = 1111L,
        description = "Unlocks daily missions. Missions refresh daily and will award marbles for future tasks."
    ),
    ShopUpgradePlaceholder(
        name = "Fountain",
        cost = 11111L,
        description = "At the end of any game, earn 1 extra marble."
    ),
    ShopUpgradePlaceholder(
        name = "Waterslide",
        cost = 33333L,
        description = "At the end of any game, earn 3 extra marbles."
    ),
    ShopUpgradePlaceholder(
        name = "One-Pound Weight",
        cost = 111L,
        description = "Auto-play placeholder. Later, this will automatically choose 1 when combined with a coin."
    ),
    ShopUpgradePlaceholder(
        name = "Three-Pound Weight",
        cost = 333L,
        description = "Auto-play placeholder. Later, this will automatically choose 3 when combined with a coin."
    ),
    ShopUpgradePlaceholder(
        name = "Patinad Coin",
        cost = 111L,
        description = "Auto-play placeholder. Later, this will combine with a weight and autopass every turn."
    ),
    ShopUpgradePlaceholder(
        name = "Normal Coin",
        cost = 333L,
        description = "Auto-play placeholder. Later, this will combine with a weight and randomly autopass or auto-target."
    ),
    ShopUpgradePlaceholder(
        name = "Polished Coin",
        cost = 1113L,
        description = "Auto-play placeholder. Later, this will combine with a weight and auto-target every turn."
    ),
    ShopUpgradePlaceholder(
        name = "Sealed Coin",
        cost = 3333L,
        description = "Auto-play placeholder. Later, this will combine with a weight, auto-target every turn, and guess 3."
    )
)

val PlayerShopColors = listOf(
    ShopColorItem("red", "Red", Color(0xFFD32F2F)),
    ShopColorItem("orange", "Orange", Color(0xFFF57C00)),
    ShopColorItem("yellow", "Yellow", Color(0xFFFBC02D)),
    ShopColorItem("green", "Green", Color(0xFF388E3C)),
    ShopColorItem("blue", "Blue", Color(0xFF1976D2)),
    ShopColorItem("purple", "Purple", Color(0xFF7B1FA2)),
    ShopColorItem("pink", "Pink", Color(0xFFC2185B)),
    ShopColorItem("brown", "Brown", Color(0xFF795548)),
    ShopColorItem("black", "Black", Color(0xFF111111)),
    ShopColorItem("grey", "Grey", Color(0xFF757575)),
    ShopColorItem("white", "White", Color.White)
)

fun colorForShopColorId(colorId: String): Color? {
    return PlayerShopColors.firstOrNull { it.id == colorId }?.color
}

@Composable
fun ShopMenuScreen(
    vaultMarbles: Long,
    unlockedNameColorIds: Set<String>,
    unlockedAvatarOutlineColorIds: Set<String>,
    selectedNameColorId: String,
    selectedAvatarOutlineColorId: String,
    onBuyNameColor: (String) -> Unit,
    onSelectNameColor: (String) -> Unit,
    onBuyAvatarOutlineColor: (String) -> Unit,
    onSelectAvatarOutlineColor: (String) -> Unit,
    onBack: () -> Unit
) {
    var pendingPurchase by remember { mutableStateOf<PendingShopPurchase?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 34.dp, start = 18.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shop",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Vault: $vaultMarbles marbles",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        ShopColorSection(
            title = "Avatar Outline",
            cost = 13L,
            colors = PlayerShopColors,
            unlockedIds = unlockedAvatarOutlineColorIds,
            selectedId = selectedAvatarOutlineColorId,
            defaultId = "",
            defaultLabel = "None",
            onBuy = { item ->
                pendingPurchase = PendingShopPurchase(
                    id = item.id,
                    label = item.label,
                    category = "Avatar Outline",
                    cost = 13L,
                    onConfirm = onBuyAvatarOutlineColor
                )
            },
            onSelect = onSelectAvatarOutlineColor
        )

        ShopColorSection(
            title = "Name Color",
            cost = 113L,
            colors = PlayerShopColors.filterNot { it.id == "white" },
            unlockedIds = unlockedNameColorIds,
            selectedId = selectedNameColorId,
            defaultId = "",
            defaultLabel = "White",
            onBuy = { item ->
                pendingPurchase = PendingShopPurchase(
                    id = item.id,
                    label = item.label,
                    category = "Name Color",
                    cost = 113L,
                    onConfirm = onBuyNameColor
                )
            },
            onSelect = onSelectNameColor
        )

        ShopUpgradePlaceholderSection()

        Spacer(Modifier.height(8.dp))
        MenuLinkButton(text = "BACK") { onBack() }
    }

    pendingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = { Text("Confirm Purchase") },
            text = {
                Text(
                    text = "Buy ${purchase.label} ${purchase.category} for ${purchase.cost} marbles?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchase.onConfirm(purchase.id)
                        pendingPurchase = null
                    }
                ) {
                    Text("BUY")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun ShopColorSection(
    title: String,
    cost: Long,
    colors: List<ShopColorItem>,
    unlockedIds: Set<String>,
    selectedId: String,
    defaultId: String,
    defaultLabel: String,
    onBuy: (ShopColorItem) -> Unit,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$title - $cost marbles each",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        ShopColorButton(
            label = defaultLabel,
            color = Color.White,
            selected = selectedId == defaultId,
            unlocked = true,
            onClick = { onSelect(defaultId) }
        )

        colors.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { item ->
                    val unlocked = item.id in unlockedIds
                    ShopColorButton(
                        label = if (unlocked) item.label else "${item.label} Locked",
                        color = item.color,
                        selected = selectedId == item.id,
                        unlocked = unlocked,
                        onClick = {
                            if (unlocked) {
                                onSelect(item.id)
                            } else {
                                onBuy(item)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShopUpgradePlaceholderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Upgrades - Coming Soon",
            color = Color(0xFF333333),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        ShopUpgradePlaceholders.forEach { upgrade ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .border(2.dp, Color(0xFF9AA3AD), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${upgrade.name} - ${upgrade.cost} marbles",
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = upgrade.description,
                    color = Color(0xFF666666),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ShopColorButton(
    label: String,
    color: Color,
    selected: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) Color(0xFF2F38CE) else Color(0xFF9AA3AD)
    val buttonShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .background(Color.White, buttonShape)
            .border(2.dp, borderColor, buttonShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(color)
                .border(1.dp, Color.Black)
        )

        Text(
            text = label,
            color = if (unlocked) Color(0xFF333333) else Color(0xFF666666),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
    playerAvatarResourceName: String,
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    onCustomize: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val playerAvatarOptions = listOf(
        AvatarMenuItem("Player", "player", locked = false),
        AvatarMenuItem("Fem Player", "playerf", locked = false),
        AvatarMenuItem("Male Player", "playerm", locked = false),
        AvatarMenuItem("Dog Player", "playerd", locked = false),
        AvatarMenuItem("Cat Player", "playerc", locked = false),
        AvatarMenuItem("Al", "al", locked = false),
        AvatarMenuItem("Barbara", "barbara", locked = false),
        AvatarMenuItem("Clark", "clark", locked = false),
        AvatarMenuItem("David", "david", locked = false),
        AvatarMenuItem("Erika", "erika", locked = false),
        AvatarMenuItem("Fred", "fred", locked = false),
        AvatarMenuItem("Graham", "graham", locked = false),
        AvatarMenuItem("Harry", "harry", locked = false),
        AvatarMenuItem("Ian", "ian", locked = false),
        AvatarMenuItem("Josh", "josh", locked = false),
        AvatarMenuItem("Kelly", "kelly", locked = false),
        AvatarMenuItem("Lois", "lois", locked = false),
        AvatarMenuItem("Auditor", "auditor", locked = true),
        AvatarMenuItem("Avenger", "avenger", locked = true),
        AvatarMenuItem("Bully", "bully", locked = true),
        AvatarMenuItem("Cabal", "cabal", locked = true),
        AvatarMenuItem("Chaos", "chaos", locked = true),
        AvatarMenuItem("Cynic", "cynic", locked = true),
        AvatarMenuItem("Echo", "echo", locked = true),
        AvatarMenuItem("Glutton", "glutton", locked = true),
        AvatarMenuItem("Hunter", "hunter", locked = true),
        AvatarMenuItem("Jester", "jester", locked = true),
        AvatarMenuItem("Juliet", "juliet", locked = true),
        AvatarMenuItem("Limper", "limper", locked = true),
        AvatarMenuItem("Lurker", "lurker", locked = true),
        AvatarMenuItem("Mirror", "mirror", locked = true),
        AvatarMenuItem("Nemesis", "nemesis", locked = true),
        AvatarMenuItem("Pacifist", "pacifist", locked = true),
        AvatarMenuItem("Pitfall", "pitfall", locked = true),
        AvatarMenuItem("Romeo", "romeo", locked = true),
        AvatarMenuItem("Scout", "scout", locked = true),
        AvatarMenuItem("Seer", "seer", locked = true),
        AvatarMenuItem("Strobe", "strobe", locked = true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 34.dp, start = 18.dp, end = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuLinkButton(text = "STATS") { onStats() }
        MenuLinkButton(text = "ACHIEVEMENTS") { onAchievements() }
        MenuLinkButton(text = "CUSTOMIZE") { onCustomize() }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Player Avatar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        playerAvatarOptions.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                rowItems.forEach { item ->
                    AvatarMenuItemButton(
                        item = item,
                        selected = playerAvatarResourceName == item.resourceName,
                        onSelected = { onAvatarSelected(item.resourceName) },
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        MenuLinkButton(text = "BACK") { onBack() }

        Spacer(Modifier.height(24.dp))
    }
}

private data class AvatarMenuItem(
    val label: String,
    val resourceName: String,
    val locked: Boolean
)

@Composable
private fun AvatarMenuItemButton(
    item: AvatarMenuItem,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) Color(0xFF2F38CE) else Color(0xFF9AA3AD)
    val textColor = if (item.locked) Color(0xFF777777) else Color(0xFF111111)

    Column(
        modifier = modifier
            .border(2.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .alpha(if (item.locked) 0.55f else 1f)
            .then(
                if (!item.locked) {
                    Modifier.clickable(onClick = onSelected)
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotAvatarIcon(
            resourceName = item.resourceName,
            greyedOut = item.locked,
            modifier = Modifier.size(54.dp)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (item.locked) "${item.label} Locked" else item.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
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
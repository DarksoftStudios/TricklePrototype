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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val description: String,
    val spriteName: String
)

private val ShopUpgradePlaceholders = listOf(
    ShopUpgradePlaceholder(
        name = "Faucet",
        cost = 11L,
        description = "At the end of the game, earn 1 extra marble if you had a 1 trickle through untargeted.",
        spriteName = "faucet"
    ),
    ShopUpgradePlaceholder(
        name = "Hose",
        cost = 333L,
        description = "At the end of the game, earn 3 extra marbles if you had a 1 trickle through untargeted.",
        spriteName = "hose"
    ),
    ShopUpgradePlaceholder(
        name = "Rooster",
        cost = 113L,
        description = "Earn 13 extra marbles on the first game you play every 24 hours.",
        spriteName = "rooster"
    ),
    ShopUpgradePlaceholder(
        name = "Weather Vane",
        cost = 333L,
        description = "Earn 33 extra marbles on the first game you win every 24 hours.",
        spriteName = "vane"
    ),
    ShopUpgradePlaceholder(
        name = "Community Board",
        cost = 1111L,
        description = "Unlocks daily missions. Missions refresh daily and will award marbles for future tasks.",
        spriteName = "corkboard"
    ),
    ShopUpgradePlaceholder(
        name = "Fountain",
        cost = 11111L,
        description = "At the end of any game, earn 1 extra marble.",
        spriteName = "fountain"
    ),
    ShopUpgradePlaceholder(
        name = "Waterslide",
        cost = 33333L,
        description = "At the end of any game, earn 3 extra marbles.",
        spriteName = "waterslide"
    ),
    ShopUpgradePlaceholder(
        name = "One-Pound Weight",
        cost = 111L,
        description = "Auto-play placeholder. Later, this will automatically choose 1 when combined with a coin.",
        spriteName = "weight1"
    ),
    ShopUpgradePlaceholder(
        name = "Three-Pound Weight",
        cost = 333L,
        description = "Auto-play placeholder. Later, this will automatically choose 3 when combined with a coin.",
        spriteName = "weight3"
    ),
    ShopUpgradePlaceholder(
        name = "Patinad Coin",
        cost = 111L,
        description = "Auto-play placeholder. Later, this will combine with a weight and autopass every turn.",
        spriteName = "coina"
    ),
    ShopUpgradePlaceholder(
        name = "Normal Coin",
        cost = 333L,
        description = "Auto-play placeholder. Later, this will combine with a weight and randomly autopass or auto-target.",
        spriteName = "coinn"
    ),
    ShopUpgradePlaceholder(
        name = "Polished Coin",
        cost = 1113L,
        description = "Auto-play placeholder. Later, this will combine with a weight and auto-target every turn.",
        spriteName = "coino"
    ),
    ShopUpgradePlaceholder(
        name = "Sealed Coin",
        cost = 3333L,
        description = "Auto-play placeholder. Later, this will combine with a weight, auto-target every turn, and guess 3.",
        spriteName = "coins"
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
    stats: PlayerStats,
    vaultMarbles: Long,
    unlockedNameColorIds: Set<String>,
    unlockedAvatarOutlineColorIds: Set<String>,
    unlockedArchetypeAvatarResourceNames: Set<String>,
    onBuyNameColor: (String) -> Unit,
    onBuyAvatarOutlineColor: (String) -> Unit,
    onBuyArchetypeAvatar: (String) -> Unit,
    onBack: () -> Unit
) {
    var pendingPurchase by remember { mutableStateOf<PendingShopPurchase?>(null) }
    val shopTitleAccentColors = remember {
        PlayerShopColors.map { it.color }.shuffled()
    }
    val shopNameTitleAccentColors = remember {
        PlayerShopColors.filterNot { it.id == "white" }.map { it.color }.shuffled()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 34.dp, start = 18.dp, end = 18.dp, bottom = 24.dp)
            .background(
                Color.White.copy(alpha = 0.78f),
                androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Shop",
                color = Color(0xFF333333),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Jug-Vault: $vaultMarbles marbles",
                color = Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            ShopColorPurchaseDropdown(
                title = "Avatar Outline",
                cost = 13L,
                colors = PlayerShopColors,
                titleAccentColors = shopTitleAccentColors,
                unlockedIds = unlockedAvatarOutlineColorIds,
                onBuy = { item ->
                    pendingPurchase = PendingShopPurchase(
                        id = item.id,
                        label = item.label,
                        category = "Avatar Outline",
                        cost = 13L,
                        onConfirm = onBuyAvatarOutlineColor
                    )
                }
            )

            ShopColorPurchaseDropdown(
                title = "Name Color",
                cost = 113L,
                colors = PlayerShopColors.filterNot { it.id == "white" },
                titleAccentColors = shopNameTitleAccentColors,
                unlockedIds = unlockedNameColorIds,
                onBuy = { item ->
                    pendingPurchase = PendingShopPurchase(
                        id = item.id,
                        label = item.label,
                        category = "Name Color",
                        cost = 113L,
                        onConfirm = onBuyNameColor
                    )
                }
            )

            ShopArchetypeAvatarPurchaseSection(
                stats = stats,
                unlockedResourceNames = unlockedArchetypeAvatarResourceNames,
                onBuy = { item ->
                    pendingPurchase = PendingShopPurchase(
                        id = item.resourceName,
                        label = item.label,
                        category = "Avatar",
                        cost = ArchetypeAvatarUnlocks.COST,
                        onConfirm = onBuyArchetypeAvatar
                    )
                }
            )

            ShopUpgradePlaceholderSection()

            Spacer(Modifier.height(8.dp))
            MenuLinkButton(text = "BACK") { onBack() }
        }
    }

    pendingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = { Text("Confirm Purchase", color = Color.White) },
            text = {
                Text(
                    text = "Buy ${purchase.label} ${purchase.category} for ${purchase.cost} marbles?",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchase.onConfirm(purchase.id)
                        pendingPurchase = null
                    }
                ) {
                    Text("BUY", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun ShopColorPurchaseDropdown(
    title: String,
    cost: Long,
    colors: List<ShopColorItem>,
    titleAccentColors: List<Color>,
    unlockedIds: Set<String>,
    onBuy: (ShopColorItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShopColorPurchaseTitle(
            title = title,
            cost = cost,
            accentColors = titleAccentColors
        )

        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFFFFFFF)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Text("Choose Color", color = Color(0xFF1C1C1C))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                colors.forEach { item ->
                    val unlocked = item.id in unlockedIds
                    DropdownMenuItem(
                        text = {
                            ShopColorDropdownRow(
                                label = if (unlocked) "${item.label} Owned" else "${item.label} - $cost",
                                color = item.color,
                                textColor = Color(0xFFFFFFFF)
                            )
                        },
                        onClick = {
                            if (!unlocked) {
                                onBuy(item)
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopColorPurchaseTitle(
    title: String,
    cost: Long,
    accentColors: List<Color>
) {
    val safeAccentColors = accentColors.ifEmpty { listOf(Color(0xFF333333)) }
    val outlineAccentColor = safeAccentColors.first()
    val colorWordColors = safeAccentColors.take(5).let { selectedColors ->
        if (selectedColors.size >= 5) {
            selectedColors
        } else {
            selectedColors + List(5 - selectedColors.size) { Color(0xFF333333) }
        }
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (title) {
            "Avatar Outline" -> {
                Text(
                    text = "Avatar ",
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .border(
                            2.dp,
                            outlineAccentColor,
                            androidx.compose.foundation.shape.RoundedCornerShape(5.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Outline",
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = " - $cost marbles each",
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            "Name Color" -> {
                Text(
                    text = "Name ",
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = buildAnnotatedString {
                        "Color".forEachIndexed { index, letter ->
                            pushStyle(SpanStyle(color = colorWordColors[index]))
                            append(letter)
                            pop()
                        }
                    },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = " - $cost marbles each",
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                Text(
                    text = "$title - $cost marbles each",
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ShopColorDropdownRow(
    label: String,
    color: Color,
    textColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(color, androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                .border(1.dp, Color(0xFF9AA3AD), androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
        )

        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun ShopArchetypeAvatarPurchaseSection(
    stats: PlayerStats,
    unlockedResourceNames: Set<String>,
    onBuy: (ArchetypeAvatarUnlockDef) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Archetype Avatars - ${ArchetypeAvatarUnlocks.COST} marbles each",
            color = Color(0xFF333333),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        ArchetypeAvatarUnlocks.all.forEach { item ->
            val achievementReady = item.achievementUnlocked(stats)
            val owned = item.resourceName in unlockedResourceNames
            val available = achievementReady && !owned

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .border(2.dp, Color(0xFF9AA3AD), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .then(
                        if (available) {
                            Modifier.clickable { onBuy(item) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .alpha(if (achievementReady || owned) 1f else 0.55f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BotAvatarIcon(
                    resourceName = item.resourceName,
                    greyedOut = !owned,
                    modifier = Modifier.size(44.dp),
                    flipHorizontally = true
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.label,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Text(
                        text = when {
                            owned -> "Owned"
                            achievementReady -> "Achievement unlocked - tap to buy"
                            else -> "Locked until achievement"
                        },
                        color = Color(0xFF666666),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun shopUpgradeSpriteResourceId(spriteName: String): Int {
    return when (spriteName) {
        "faucet" -> R.drawable.faucet
        "hose" -> R.drawable.hose
        "rooster" -> R.drawable.rooster
        "vane" -> R.drawable.vane
        "corkboard" -> R.drawable.corkboard
        "fountain" -> R.drawable.fountain
        "waterslide" -> R.drawable.waterslide
        "weight1" -> R.drawable.weight1
        "weight3" -> R.drawable.weight3
        "coina" -> R.drawable.coina
        "coinn" -> R.drawable.coinn
        "coino" -> R.drawable.coino
        "coins" -> R.drawable.coins
        else -> 0
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
            color = Color(0xFF000000),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        ShopUpgradePlaceholders.forEach { upgrade ->
            val spriteResourceId = shopUpgradeSpriteResourceId(upgrade.spriteName)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .border(2.dp, Color(0xFF9AA3AD), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (spriteResourceId != 0) {
                    Image(
                        painter = painterResource(id = spriteResourceId),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${upgrade.name} - ${upgrade.cost} marbles",
                        color = Color(0xFF000000),
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
    unlockedNameColorIds: Set<String>,
    unlockedAvatarOutlineColorIds: Set<String>,
    unlockedArchetypeAvatarResourceNames: Set<String>,
    selectedNameColorId: String,
    selectedAvatarOutlineColorId: String,
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    onCustomize: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onNameColorSelected: (String) -> Unit,
    onAvatarOutlineColorSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val basePlayerAvatarOptions = listOf(
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
        AvatarMenuItem("Lois", "lois", locked = false)
    )
    val archetypeAvatarOptions = ArchetypeAvatarUnlocks.all.map { item ->
        AvatarMenuItem(
            label = item.label,
            resourceName = item.resourceName,
            locked = item.resourceName !in unlockedArchetypeAvatarResourceNames
        )
    }
    val playerAvatarOptions = basePlayerAvatarOptions + archetypeAvatarOptions

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

        ProfileColorDropdown(
            title = "Name Color",
            colors = PlayerShopColors.filterNot { it.id == "white" },
            unlockedIds = unlockedNameColorIds,
            selectedId = selectedNameColorId,
            emptyLabel = "No name colors unlocked",
            onSelect = onNameColorSelected
        )

        ProfileColorDropdown(
            title = "Avatar Outline",
            colors = PlayerShopColors,
            unlockedIds = unlockedAvatarOutlineColorIds,
            selectedId = selectedAvatarOutlineColorId,
            emptyLabel = "No outline colors unlocked",
            onSelect = onAvatarOutlineColorSelected
        )

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

@Composable
private fun ProfileColorDropdown(
    title: String,
    colors: List<ShopColorItem>,
    unlockedIds: Set<String>,
    selectedId: String,
    emptyLabel: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val unlockedColors = colors.filter { it.id in unlockedIds }
    val selectedLabel = unlockedColors.firstOrNull { it.id == selectedId }?.label.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .border(2.dp, Color(0xFF9AA3AD), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { if (unlockedColors.isNotEmpty()) expanded = true },
                enabled = unlockedColors.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF6F7D8C)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF2F7FF),
                    contentColor = Color(0xFF333333),
                    disabledContainerColor = Color(0xFFE8EDF3),
                    disabledContentColor = Color(0xFF6F7D8C)
                )
            ) {
                Text(
                    text = selectedLabel.ifBlank { emptyLabel },
                    color = if (unlockedColors.isNotEmpty()) Color(0xFF333333) else Color(0xFF6F7D8C),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                unlockedColors.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            ShopColorDropdownRow(
                                label = item.label,
                                color = item.color,
                                textColor = Color(0xFFFFFFFF)
                            )
                        },
                        onClick = {
                            onSelect(item.id)
                            expanded = false
                        }
                    )
                }
            }
        }
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
            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
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
            modifier = Modifier.size(54.dp),
            flipHorizontally = true
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
package com.example.trickleprototype

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
                    "- Pressures the table by always playing high and hunting the cautious\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: 50% chance to target on round 1 (guessing 1); from round 2 on, targets players who chose 1 last round (guessing 1)\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "chaos",
            text = "Chaos:\n" +
                    "- The Matriarch of RNG. All random everything\n" +
                    "- Chooses: Random (0/1/3 evenly)\n" +
                    "- Targeting Behavior: 50/50 pass vs target\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "cynic",
            text = "Cynic:\n" +
                    "- Assumes everyone is greedy and punishes it\n" +
                    "- Chooses: 1\n" +
                    "- Targeting Behavior: 50% chance to target on round 1 (guessing 3); from round 2 on, targets players who chose 3 last round (guessing 3)\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "echo",
            text = "Echo:\n" +
                    "- Mirrors the Player one beat behind\n" +
                    "- Chooses: 3 on round 1, then copies the Player's previous choice\n" +
                    "- Targeting Behavior: Copies whether the Player passed or targeted, then looks for the same guessed number\n\n"
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
            text = "cabal:\n" +
                    "- Picks a 'King' and only attacks people who attack that King\n" +
                    "- Chooses: 1 or 3\n" +
                    "- Targeting Behavior: Usually passes; targets only to avenge their king\n\n"
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
                    "- Targeting Behavior: Passes for 3 rounds, then targets repeat-3 players\n\n"
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
                    "- Starts strong, stumbles, then becomes unpredictable\n" +
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
                    "- Alternates like a metronome and attacks in a repeating rhythm.\n" +
                    "- Chooses: Alternates 1,3,1,3...\n" +
                    "- Targeting Behavior: Alternates pass/target with pattern-based guesses.\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "hunter",
            text = "Hunter:\n" +
                    "- Hard-mode boss. Fixates on the Player whenever possible\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: Targets the Player and guesses 3; if the Player cannot be targeted, hunts known 3-choosers\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "seer",
            text = "Seer:\n" +
                    "- Hard-mode boss. Sees hidden choices and score totals\n" +
                    "- Chooses: 3\n" +
                    "- Targeting Behavior: Targets the highest-scoring player who chose 3, then the highest-scoring player who chose 1; never targets a 0\n\n"
        )

        ArchetypeRuleEntry(
            resourceName = "mirror",
            text = "Mirror:\n" +
                    "- Hard-mode boss. Copies the Player at the exact same time\n" +
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
fun BonusMarblesText(payout: BonusMarblePayout) {
    Column {
        payout.rows.forEach { row ->
            Text(
                text = "${row.label}: +${row.amount}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Total: +${payout.total}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

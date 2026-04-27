package com.example.trickleprototype

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TableActionPanel(

    modifier: Modifier = Modifier,
    choice: Int,
    onChoiceSelected: (Int) -> Unit,
    choiceOptions: List<Int>,
    choicePrompt: String,
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
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showChoiceButtons) {
                Text(choicePrompt, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (0 in choiceOptions) {
                        SmallChoiceButton("0", selected = choice == 0, enabled = choiceEnabled) { onChoiceSelected(0) }
                    }
                    if (1 in choiceOptions) {
                        SmallChoiceButton("1", selected = choice == 1, enabled = choiceEnabled) { onChoiceSelected(1) }
                    }
                    if (3 in choiceOptions) {
                        SmallChoiceButton("3", selected = choice == 3, enabled = choiceEnabled) { onChoiceSelected(3) }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }

            if (showActionButtons) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    when {
                        canPass && dropdownOptions.isNotEmpty() -> {
                            Button(
                                onClick = onPassSelected,
                                enabled = inputsEnabled,
                                modifier = Modifier
                                    .widthIn(min = 96.dp)
                                    .heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF546E7A),
                                    contentColor = Color.White
                                )
                            ) { OneLineButtonText("Pass") }

                            Button(
                                onClick = onTargetSelected,
                                enabled = inputsEnabled,
                                modifier = Modifier
                                    .widthIn(min = 96.dp)
                                    .heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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
                                modifier = Modifier
                                    .widthIn(min = 96.dp)
                                    .heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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
                                modifier = Modifier
                                    .widthIn(min = 96.dp)
                                    .heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8D6E63),
                                    contentColor = Color.White
                                )
                            ) { OneLineButtonText("Target") }
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }

            if (showPassConfirmButtons) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Button(
                        onClick = onConfirmPass,
                        enabled = inputsEnabled,
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF546E7A),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Confirm Pass") }

                    Button(
                        onClick = onTargetInstead,
                        enabled = inputsEnabled && dropdownOptions.isNotEmpty(),
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8D6E63),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Target Instead") }
                }
                Spacer(Modifier.height(7.dp))
            }

            if (showTargeting) {
                if (canPass) {
                    Button(
                        onClick = onPassInstead,
                        enabled = inputsEnabled,
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF546E7A),
                            contentColor = Color.White
                        )
                    ) { OneLineButtonText("Pass Instead") }

                    Spacer(Modifier.height(5.dp))
                }

                val targetPrompt = when {
                    needsSecondTarget && selectedTargetId == null ->
                        "Click a cup to pick your first target."
                    needsSecondTarget && selectedSecondTargetId == null ->
                        "Click a second cup."
                    needsSecondTarget ->
                        ""
                    selectedTargetId == null ->
                        "Click a cup to pick your target."
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
                    Spacer(Modifier.height(5.dp))

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

                Spacer(Modifier.height(7.dp))
            }

            if (showGuessButtons) {
                Text(
                    text = "Choose their number",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                Spacer(Modifier.height(7.dp))
            }

            if (showSubmitButton) {
                Button(
                    onClick = onSubmit,
                    enabled = submitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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


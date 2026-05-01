package com.example.trickleprototype

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CloudButtonShape: GenericShape = GenericShape { size, _ ->
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
fun MenuLinkButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
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
fun OneLineButtonText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}


@Composable
fun SmallChoiceButton(
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
        modifier = Modifier
            .heightIn(min = 36.dp)
            .widthIn(min = 40.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
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
            softWrap = false,
            fontSize = 13.sp
        )
    }
}

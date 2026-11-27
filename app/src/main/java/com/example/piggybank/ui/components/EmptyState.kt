package com.example.piggybank.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piggybank.ui.theme.PrimaryTeal
import com.example.piggybank.ui.theme.TextSecondary

/**
 * Shared borderless empty state with an icon, centered message, and optional action button.
 */
@Composable
fun BorderlessEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    iconSize: Dp = 36.dp,
    iconTint: Color = PrimaryTeal,
    messageColor: Color = TextSecondary
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = messageColor,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )
            }
        }
    }
}

/**
 * Computes the remaining vertical space below an anchor item inside a LazyColumn
 * so an empty state centers cleanly between the header content and the bottom bar.
 */
@Composable
fun rememberRemainingLazyListHeight(
    listState: LazyListState,
    anchorItemIndex: Int,
    itemSpacing: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    fallbackHeight: Dp = 240.dp
): Dp {
    val density = LocalDensity.current
    val spacingPx = with(density) { itemSpacing.roundToPx() }
    val bottomPaddingPx = with(density) { bottomContentPadding.roundToPx() }
    var cachedRemainingPx by remember(anchorItemIndex, spacingPx, bottomPaddingPx) {
        mutableIntStateOf(-1)
    }

    val measuredPx by remember(listState, anchorItemIndex, spacingPx, bottomPaddingPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            if (viewportHeight <= 0) {
                return@derivedStateOf -1
            }
            if (listState.firstVisibleItemIndex == 0) {
                val anchorItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == anchorItemIndex }
                    ?: return@derivedStateOf -1
                val anchorBottomAtRest =
                    anchorItem.offset + listState.firstVisibleItemScrollOffset + anchorItem.size + spacingPx
                // Leave a 1px margin so fit-to-viewport empty content does not add micro-scroll
                (layoutInfo.viewportEndOffset - anchorBottomAtRest - bottomPaddingPx - 1)
                    .coerceAtLeast(0)
            } else {
                -1
            }
        }
    }

    if (measuredPx >= 0) {
        cachedRemainingPx = measuredPx
    }

    return if (cachedRemainingPx >= 0) {
        with(density) { cachedRemainingPx.toDp() }
    } else {
        fallbackHeight
    }
}

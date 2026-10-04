package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

/**
 * Android 10 compatible overscroll stretch.
 *
 * It deliberately does not use Android 12's EdgeEffect stretch API. Unconsumed
 * scroll distance at either edge is converted into a small scale transform and
 * springs back when the gesture releases.
 */
@Composable
fun Modifier.android10Stretch(state: LazyListState): Modifier {
    val scope = rememberCoroutineScope()
    val stretch = remember { Animatable(0f) }
    var lastStretch by remember { mutableFloatStateOf(0f) }

    val connection = remember(state) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y == 0f) return Offset.Zero
                val atTop = state.firstVisibleItemIndex == 0 &&
                    state.firstVisibleItemScrollOffset == 0
                val atBottom = !state.canScrollForward

                val pullingTop = atTop && available.y > 0f
                val pullingBottom = atBottom && available.y < 0f
                if (pullingTop || pullingBottom) {
                    val next = (stretch.value + available.y / 900f)
                        .coerceIn(-0.045f, 0.045f)
                    scope.launch {
                        stretch.snapTo(next)
                        lastStretch = next
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (stretch.value != 0f) {
                    stretch.animateTo(0f, spring())
                }
                return Velocity.Zero
            }
        }
    }

    return nestedScroll(connection).graphicsLayer {
        val amount = stretch.value
        scaleY = 1f - kotlin.math.abs(amount) * 0.55f
        scaleX = 1f + kotlin.math.abs(amount) * 0.06f
        translationY = amount * 24f
    }
}

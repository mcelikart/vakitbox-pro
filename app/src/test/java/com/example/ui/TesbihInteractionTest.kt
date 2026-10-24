package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class TesbihInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun verticalSwipeInLeftGutterScrollsThePage() {
        lateinit var listState: LazyListState
        compose.setContent {
            MyApplicationTheme {
                listState = rememberLazyListState()
                Box(Modifier.size(width = 320.dp, height = 480.dp)) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        item {
                            TesbihBeadSwiper(
                                count = 0,
                                currentTarget = 33,
                                currentDhikrName = "Sübhanallah",
                                onIncrement = {},
                                onDecrement = {}
                            )
                        }
                        item { Box(Modifier.height(220.dp)) }
                    }
                }
            }
        }

        val density = compose.density
        val x = with(density) { 32.dp.toPx() }
        val startY = with(density) { 300.dp.toPx() }
        val endY = with(density) { 120.dp.toPx() }
        compose.onNodeWithTag("tesbih-pull-area").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(x, startY))
            moveTo(androidx.compose.ui.geometry.Offset(x, endY), 120)
            up()
        }

        compose.runOnIdle {
            assertTrue("a swipe beside the beads should reach the page scroll", listState.firstVisibleItemScrollOffset > 0)
        }
    }

    @Test
    fun verticalSwipeOnTheBeadsStillCountsOneDhikr() {
        val count = mutableIntStateOf(0)
        compose.setContent {
            MyApplicationTheme {
                Box(Modifier.size(width = 320.dp, height = 480.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            TesbihBeadSwiper(
                                count = count.intValue,
                                currentTarget = 33,
                                currentDhikrName = "Sübhanallah",
                                onIncrement = { count.intValue++ },
                                onDecrement = { count.intValue = (count.intValue - 1).coerceAtLeast(0) }
                            )
                        }
                        item { Box(Modifier.height(220.dp)) }
                    }
                }
            }
        }

        val density = compose.density
        val x = with(density) { 180.dp.toPx() }
        val startY = with(density) { 120.dp.toPx() }
        val endY = with(density) { 240.dp.toPx() }
        compose.onNodeWithTag("tesbih-pull-area").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(x, startY))
            moveTo(androidx.compose.ui.geometry.Offset(x, endY), 120)
            up()
        }

        compose.runOnIdle { assertEquals(1, count.intValue) }
    }
}

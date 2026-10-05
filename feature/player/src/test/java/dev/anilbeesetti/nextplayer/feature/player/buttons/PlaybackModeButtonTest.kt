package dev.anilbeesetti.nextplayer.feature.player.buttons

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackModeButtonTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pressingButtonCyclesThroughTheFourPlaybackModes() {
        val player = composeRule.runOnIdle { TestPlayer() }
        try {
            composeRule.setContent {
                NextPlayerTheme {
                    PlaybackModeButton(player = player)
                }
            }
            val button = composeRule.onNodeWithTag(PLAYBACK_MODE_TEST_TAG)
            val expectedModes = listOf(
                true to Player.REPEAT_MODE_OFF,
                false to Player.REPEAT_MODE_ALL,
                false to Player.REPEAT_MODE_ONE,
                false to Player.REPEAT_MODE_OFF,
            )

            for ((shuffleOn, repeatMode) in expectedModes) {
                button.performClick()
                composeRule.runOnIdle {
                    assertEquals(shuffleOn, player.shuffleModeEnabled)
                    assertEquals(repeatMode, player.repeatMode)
                }
            }
        } finally {
            composeRule.runOnIdle { player.release() }
        }
    }

    @Test
    fun labelFollowsTheModeAfterEachClick() {
        val player = composeRule.runOnIdle { TestPlayer() }
        try {
            composeRule.setContent {
                NextPlayerTheme {
                    PlaybackModeButton(player = player)
                }
            }
            val button = composeRule.onNodeWithTag(PLAYBACK_MODE_TEST_TAG)
            val expectedLabels = listOf(R.string.shuffle_on, R.string.loop_mode_all, R.string.loop_mode_one, R.string.loop_mode_off)

            for (label in expectedLabels) {
                button.performClick()
                button.assertContentDescriptionEquals(composeRule.activity.getString(label))
            }
        } finally {
            composeRule.runOnIdle { player.release() }
        }
    }

    @Test
    fun shuffleTakesPrecedenceOverLooping() {
        val player = composeRule.runOnIdle { TestPlayer(shuffleOn = true, repeatMode = Player.REPEAT_MODE_ALL) }
        try {
            composeRule.setContent {
                NextPlayerTheme {
                    PlaybackModeButton(player = player)
                }
            }
            composeRule.onNodeWithTag(PLAYBACK_MODE_TEST_TAG)
                .assertContentDescriptionEquals(composeRule.activity.getString(R.string.shuffle_on))
                .performClick()
            composeRule.runOnIdle {
                assertEquals(false, player.shuffleModeEnabled)
                assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
            }
        } finally {
            composeRule.runOnIdle { player.release() }
        }
    }

    private class TestPlayer(
        shuffleOn: Boolean = false,
        @Player.RepeatMode repeatMode: Int = Player.REPEAT_MODE_OFF,
    ) : SimpleBasePlayer(Looper.getMainLooper()) {
        private var state = State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SET_SHUFFLE_MODE,
                        Player.COMMAND_SET_REPEAT_MODE,
                    )
                    .build(),
            )
            .setPlaylist(listOf(MediaItemData.Builder("item").setDurationUs(60_000_000).build()))
            .setPlaybackState(Player.STATE_READY)
            .setShuffleModeEnabled(shuffleOn)
            .setRepeatMode(repeatMode)
            .build()

        override fun getState(): State = state

        override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
            state = state.buildUpon().setRepeatMode(repeatMode).build()
            return Futures.immediateVoidFuture()
        }

        override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
            state = state.buildUpon().setShuffleModeEnabled(shuffleModeEnabled).build()
            return Futures.immediateVoidFuture()
        }
    }
}

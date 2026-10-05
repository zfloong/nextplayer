package dev.anilbeesetti.nextplayer.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR
import dev.anilbeesetti.nextplayer.feature.player.LocalControlsVisibilityState

const val PLAYBACK_MODE_TEST_TAG = "playbackMode"

/** Single button cycling through the four playback orders: sequential, shuffle, loop all, loop one. */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackModeButton(
    modifier: Modifier = Modifier,
    player: Player?,
) {
    val repeatState = rememberRepeatButtonState(player)
    val shuffleState = rememberShuffleButtonState(player)
    val controlsVisibilityState = LocalControlsVisibilityState.current
    val playbackMode = playbackModeOf(shuffleState.shuffleOn, repeatState.repeatModeState)

    PlayerButton(
        modifier = modifier.testTag(PLAYBACK_MODE_TEST_TAG),
        enabled = repeatState.isEnabled && shuffleState.isEnabled,
        onClick = {
            // Read the live player state: a stale recomposition would replay the previous transition on a fast double tap.
            player?.run {
                val nextMode = playbackModeOf(shuffleModeEnabled, repeatMode).next()
                shuffleModeEnabled = nextMode == PlaybackMode.SHUFFLE
                repeatMode = nextMode.toRepeatMode()
            }
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            painter = playbackMode.iconPainter(),
            contentDescription = playbackMode.contentDescription(),
        )
    }
}

enum class PlaybackMode {
    SEQUENTIAL,
    SHUFFLE,
    LOOP_ALL,
    LOOP_ONE,
    ;

    fun next(): PlaybackMode = when (this) {
        SEQUENTIAL -> SHUFFLE
        SHUFFLE -> LOOP_ALL
        LOOP_ALL -> LOOP_ONE
        LOOP_ONE -> SEQUENTIAL
    }
}

private fun playbackModeOf(shuffleOn: Boolean, @Player.RepeatMode repeatMode: Int): PlaybackMode = when {
    shuffleOn -> PlaybackMode.SHUFFLE
    repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.LOOP_ONE
    repeatMode == Player.REPEAT_MODE_ALL -> PlaybackMode.LOOP_ALL
    else -> PlaybackMode.SEQUENTIAL
}

private fun PlaybackMode.toRepeatMode(): @Player.RepeatMode Int = when (this) {
    PlaybackMode.LOOP_ONE -> Player.REPEAT_MODE_ONE
    PlaybackMode.LOOP_ALL -> Player.REPEAT_MODE_ALL
    PlaybackMode.SEQUENTIAL, PlaybackMode.SHUFFLE -> Player.REPEAT_MODE_OFF
}

@Composable
private fun PlaybackMode.iconPainter(): Painter = when (this) {
    PlaybackMode.SEQUENTIAL -> painterResource(coreUiR.drawable.ic_loop_off)
    PlaybackMode.SHUFFLE -> painterResource(coreUiR.drawable.ic_shuffle_on)
    PlaybackMode.LOOP_ALL -> painterResource(coreUiR.drawable.ic_loop_all)
    PlaybackMode.LOOP_ONE -> painterResource(coreUiR.drawable.ic_loop_one)
}

@Composable
private fun PlaybackMode.contentDescription(): String = when (this) {
    PlaybackMode.SEQUENTIAL -> stringResource(coreUiR.string.loop_mode_off)
    PlaybackMode.SHUFFLE -> stringResource(coreUiR.string.shuffle_on)
    PlaybackMode.LOOP_ALL -> stringResource(coreUiR.string.loop_mode_all)
    PlaybackMode.LOOP_ONE -> stringResource(coreUiR.string.loop_mode_one)
}

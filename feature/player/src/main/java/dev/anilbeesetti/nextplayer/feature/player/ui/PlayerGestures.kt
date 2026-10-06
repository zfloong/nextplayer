package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.Player
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomHorizontalDragGestures
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomTransformGestures
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomVerticalDragGestures
import dev.anilbeesetti.nextplayer.feature.player.state.ControlBarHeights
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.PictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.SeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.TapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoTransformState
import dev.anilbeesetti.nextplayer.feature.player.state.VolumeAndBrightnessGestureState

private const val SWIPE_TO_CHANGE_ITEM_THRESHOLD = 0.15f

internal enum class ItemSwipeDirection {
    NEXT,
    PREVIOUS,
}

/** A portrait swipe past the threshold changes the video, like the short video feeds do. */
internal fun swipeToChangeItemDirection(
    verticalDragDistance: Float,
    screenHeight: Float,
): ItemSwipeDirection? {
    val threshold = screenHeight * SWIPE_TO_CHANGE_ITEM_THRESHOLD
    return when {
        verticalDragDistance <= -threshold -> ItemSwipeDirection.NEXT
        verticalDragDistance >= threshold -> ItemSwipeDirection.PREVIOUS
        else -> null
    }
}

/**
 * Portrait taps are zoned by the measured control bars, not by fixed screen fractions: the title row
 * and the seekbar row sit at the edges, and their heights change with the font scale, the system bar
 * insets and whether the chapter chip is shown.
 */
internal fun isControlBarTapRegion(
    tapY: Float,
    screenHeight: Int,
    controlBarHeights: ControlBarHeights,
): Boolean {
    if (tapY < controlBarHeights.top) return true
    return tapY >= screenHeight - controlBarHeights.bottom
}

/**
 * A tap over a control bar only folds the controls away; a tap on the video strip between them is
 * pause/resume. Pausing must also show the controls: while paused they never auto-hide, and they hold
 * the only seekbar. Landscape and hidden-controls taps keep a single behaviour.
 */
internal fun handleSingleTap(
    isPortrait: Boolean,
    tapY: Float,
    screenHeight: Int,
    controlBarHeights: ControlBarHeights,
    player: Player,
    controlsVisibilityState: ControlsVisibilityState,
) {
    if (!isPortrait) {
        controlsVisibilityState.toggleControlsVisibility()
        return
    }
    val overControlBar = controlsVisibilityState.controlsVisible &&
        isControlBarTapRegion(tapY, screenHeight, controlBarHeights)
    if (overControlBar) {
        controlsVisibilityState.hideControls()
        return
    }
    when (player.isPlaying) {
        true -> {
            player.pause()
            controlsVisibilityState.showControls()
        }

        false -> player.play()
    }
}

@Composable
fun PlayerGestures(
    modifier: Modifier = Modifier,
    player: Player,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    pictureInPictureState: PictureInPictureState,
    seekGestureState: SeekGestureState,
    videoTransformState: VideoTransformState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
    isPortrait: Boolean,
    controlBarHeights: ControlBarHeights,
    onSwipeToPreviousItem: () -> Unit,
    onSwipeToNextItem: () -> Unit,
) {
    BoxWithConstraints {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(
                    isPortrait,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectTapGestures(
                        onTap = { offset ->
                            if (tapGestureState.seekMillis != 0L) return@detectTapGestures
                            handleSingleTap(
                                isPortrait = isPortrait,
                                tapY = offset.y,
                                screenHeight = size.height,
                                controlBarHeights = controlBarHeights,
                                player = player,
                                controlsVisibilityState = controlsVisibilityState,
                            )
                        },
                        onDoubleTap = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleDoubleTap(offset = it, size = size)
                        },
                        onPress = {
                            tryAwaitRelease()
                            tapGestureState.handleOnLongPressRelease()
                        },
                        onLongPress = {
                            if (controlsVisibilityState.controlsLocked) return@detectTapGestures
                            tapGestureState.handleLongPress(offset = it)
                        },
                    )
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomHorizontalDragGestures(
                        onDragStart = seekGestureState::onDragStart,
                        onHorizontalDrag = seekGestureState::onDrag,
                        onDragCancel = seekGestureState::onDragEnd,
                        onDragEnd = seekGestureState::onDragEnd,
                    )
                }
                .pointerInput(
                    isPortrait,
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    if (isPortrait) {
                        var verticalDragDistance = 0f
                        detectCustomVerticalDragGestures(
                            onDragStart = { verticalDragDistance = 0f },
                            onVerticalDrag = { _, dragAmount -> verticalDragDistance += dragAmount },
                            onDragCancel = { verticalDragDistance = 0f },
                            onDragEnd = {
                                when (swipeToChangeItemDirection(verticalDragDistance, size.height.toFloat())) {
                                    ItemSwipeDirection.NEXT -> onSwipeToNextItem()
                                    ItemSwipeDirection.PREVIOUS -> onSwipeToPreviousItem()
                                    null -> Unit
                                }
                                verticalDragDistance = 0f
                            },
                        )
                    } else {
                        detectCustomVerticalDragGestures(
                            onDragStart = { volumeAndBrightnessGestureState.onDragStart(it, size) },
                            onVerticalDrag = volumeAndBrightnessGestureState::onDrag,
                            onDragCancel = volumeAndBrightnessGestureState::onDragEnd,
                            onDragEnd = volumeAndBrightnessGestureState::onDragEnd,
                        )
                    }
                }
                .pointerInput(
                    controlsVisibilityState.controlsLocked,
                    pictureInPictureState.isInPictureInPictureMode,
                ) {
                    if (controlsVisibilityState.controlsLocked) return@pointerInput
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectCustomTransformGestures(
                        onGesture = { _, panChange, zoomChange, _ ->
                            if (tapGestureState.isLongPressGestureInAction) return@detectCustomTransformGestures
                            videoTransformState.onZoomPanGesture(
                                constraints = this@BoxWithConstraints.constraints,
                                panChange = panChange,
                                zoomChange = zoomChange,
                            )
                        },
                        onGestureEnd = {
                            videoTransformState.onZoomPanGestureEnd()
                        },
                    )
                },
        )
    }
}

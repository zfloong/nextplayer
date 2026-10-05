package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomHorizontalDragGestures
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomTransformGestures
import dev.anilbeesetti.nextplayer.feature.player.extensions.detectCustomVerticalDragGestures
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

@Composable
fun PlayerGestures(
    modifier: Modifier = Modifier,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    pictureInPictureState: PictureInPictureState,
    seekGestureState: SeekGestureState,
    videoTransformState: VideoTransformState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
    isPortrait: Boolean,
    onSwipeToPreviousItem: () -> Unit,
    onSwipeToNextItem: () -> Unit,
) {
    BoxWithConstraints {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(pictureInPictureState.isInPictureInPictureMode) {
                    if (pictureInPictureState.isInPictureInPictureMode) return@pointerInput

                    detectTapGestures(
                        onTap = {
                            if (tapGestureState.seekMillis != 0L) return@detectTapGestures
                            controlsVisibilityState.toggleControlsVisibility()
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

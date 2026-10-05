package dev.anilbeesetti.nextplayer.feature.player

import android.graphics.Rect
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.anilbeesetti.nextplayer.feature.player.extensions.forEachTextureView
import dev.anilbeesetti.nextplayer.feature.player.state.PictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoTransformState
import dev.anilbeesetti.nextplayer.feature.player.ui.ShutterView
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleView

@OptIn(UnstableApi::class)
@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: Player,
    pictureInPictureState: PictureInPictureState,
    videoTransformState: VideoTransformState,
    subtitleConfiguration: SubtitleConfiguration,
) {
    val presentationState = rememberPresentationState(player)
    val rotationDegrees = videoTransformState.rotationDegrees
    val isCanvasRotated = rotationDegrees != 0
    val rootView = LocalView.current

    // The half-turn cannot ride on a graphicsLayer: interop views only receive its scale and translation, and a
    // SurfaceView layer is composited outside the window buffer, so it ignores view transforms altogether.
    // Rotating therefore needs a TextureView plus the transform set on the view itself.
    SideEffect(rotationDegrees) {
        if (isCanvasRotated) {
            rootView.post { rootView.applyCanvasRotation(rotationDegrees) }
        }
    }

    Box(modifier.fillMaxSize()) {
        key(isCanvasRotated) {
            PlayerSurface(
                player = player,
                surfaceType = if (isCanvasRotated) SURFACE_TYPE_TEXTURE_VIEW else SURFACE_TYPE_SURFACE_VIEW,
                modifier = Modifier
                    .resizeWithContentScale(
                        contentScale = ContentScale.Fit,
                        sourceSizeDp = presentationState.videoSizeDp?.let { size ->
                            size.copy(
                                width = with(LocalDensity.current) { size.width.toDp().value },
                                height = with(LocalDensity.current) { size.height.toDp().value },
                            )
                        },
                    )
                    .onGloballyPositioned {
                        val bounds = it.boundsInWindow()
                        val rect = Rect(
                            bounds.left.toInt(),
                            bounds.top.toInt(),
                            bounds.right.toInt(),
                            bounds.bottom.toInt(),
                        )
                        pictureInPictureState.setVideoViewRect(rect)
                        // The surface is recreated when the canvas is rotated, so re-apply on every layout pass.
                        rootView.applyCanvasRotation(rotationDegrees)
                    }
                    .graphicsLayer {
                        scaleX = videoTransformState.zoom
                        scaleY = videoTransformState.zoom
                        translationX = videoTransformState.offset.x
                        translationY = videoTransformState.offset.y
                    },
            )
        }

        SubtitleView(
            player = player,
            isInPictureInPictureMode = pictureInPictureState.isInPictureInPictureMode,
            configuration = subtitleConfiguration,
        )

        if (presentationState.coverSurface) {
            ShutterView()
        }
    }
}

private fun View.applyCanvasRotation(degrees: Int) {
    forEachTextureView { textureView ->
        textureView.rotation = degrees.toFloat()
    }
}

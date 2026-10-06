package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistDisplayTitleTest {
    @Test
    fun escapedNetworkSegmentBecomesReadableName() {
        val item = item(uri = "smb://192.0.2.1/Media/%E6%97%A5%E6%9C%AC%E8%AA%9E.mkv?cid=3")

        assertEquals("日本語", item.displayTitle)
        assertEquals("A 1080", item(uri = "smb://192.0.2.1/Media/A%201080.mkv?cid=3").displayTitle)
    }

    @Test
    fun plusSignInANameIsNotTurnedIntoASpace() {
        assertEquals("C++ 1080", item(uri = "smb://192.0.2.1/Media/C%2B%2B%201080.mkv?cid=3").displayTitle)
        assertEquals("A+B", item(uri = "smb://192.0.2.1/Media/A+B.mkv?cid=3").displayTitle)
    }

    @Test
    fun escapesThatAreNotUtf8StayAsTheyWereStored() {
        assertEquals("%FF%FE", item(uri = "smb://192.0.2.1/Media/%FF%FE.mkv?cid=3").displayTitle)
        assertEquals("%E6", item(uri = "smb://192.0.2.1/Media/%E6?cid=3").displayTitle)
        assertEquals("half%2", item(uri = "smb://192.0.2.1/Media/half%2.mkv?cid=3").displayTitle)
    }

    @Test
    fun storedTitleAndKnownVideoWinOverTheUri() {
        assertEquals(
            "Channel One",
            item(uri = "smb://192.0.2.1/Media/%E6%97%A5.mkv?cid=3", title = "Channel One").displayTitle,
        )
        assertEquals(
            "Renamed",
            item(uri = "content://media/external/video/9", video = video(name = "Renamed.mkv")).displayTitle,
        )
    }

    @Test
    fun supportingTextDecodesTheWholeNetworkPath() {
        val item = item(uri = "smb://192.0.2.1/%E5%8A%87/%E6%97%A5.mkv?cid=3")

        assertEquals("smb://192.0.2.1/劇/日.mkv?cid=3", item.supportingText)
        assertEquals(
            "/Movies",
            item(uri = "content://media/external/video/9", video = video()).supportingText,
        )
    }

    private fun item(
        uri: String,
        title: String? = null,
        video: Video? = null,
    ) = PlaylistItem(
        position = 0,
        uri = uri,
        title = title,
        tvgLogo = null,
        duration = M3UPlaylistItem.UNKNOWN_DURATION,
        groupTitle = null,
        video = video,
    )

    private fun video(name: String = "Sample.mkv") = Video(
        id = 1L,
        path = "/Movies/$name",
        parentPath = "/Movies",
        duration = 1_000,
        uriString = "content://media/external/video/1",
        nameWithExtension = name,
        width = 1920,
        height = 1080,
        size = 1_000,
    )
}

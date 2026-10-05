package dev.anilbeesetti.nextplayer.core.media.network.discovery

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmbShareEnumeratorTest {

    @Test
    fun `request stub encodes server name then the empty level-1 container`() {
        val stub = DefaultSmbShareEnumerator.ndrShareEnumRequest("192.168.99.99")

        // "\\192.168.99.99" = 15 chars + NUL = 16 => 3 count u32s + 32 data bytes
        assertEquals(80, stub.size)
        assertEquals(0x00020000L, le32(stub, 0))
        assertEquals(16L, le32(stub, 4))
        assertEquals(0L, le32(stub, 8))
        assertEquals(16L, le32(stub, 12))
        assertEquals(
            "\\\\192.168.99.99\u0000",
            String(stub, 16, 32, StandardCharsets.UTF_16LE),
        )
        assertEquals(1L, le32(stub, 48)) // level
        assertEquals(1L, le32(stub, 52)) // union switch
        assertEquals(0x00020004L, le32(stub, 56)) // container referent
        assertEquals(0L, le32(stub, 60)) // EntriesRead
        assertEquals(0L, le32(stub, 64)) // Buffer NULL
        assertEquals(0xFFFFFFFFL, le32(stub, 68)) // PreferedMaximumLength
        assertEquals(0x00020008L, le32(stub, 72)) // ResumeHandle referent
        assertEquals(0L, le32(stub, 76))
    }

    private fun syntheticReply(vararg entries: Pair<String, Long>): ByteArray {
        val w = NdrWriter()
        w.u32(1) // level
        w.u32(1) // union switch
        w.u32(0x00020000) // container referent
        w.u32(entries.size.toLong())
        w.u32(0x00000002) // buffer referent
        w.u32(entries.size.toLong()) // conformant max count, before the elements
        entries.forEach { (_, type) ->
            w.u32(0x00000001) // name referent
            w.u32(type)
            w.u32(0x00000001) // remark referent
        }
        entries.forEach { (name, _) ->
            w.conformantVaryingString(name)
            w.conformantVaryingString("remark of $name")
        }
        w.u32(entries.size.toLong()) // TotalEntries
        w.u32(0) // ResumeHandle: NULL
        w.u32(0) // return code
        return w.toByteArray()
    }

    @Test
    fun `reply round trip decodes names types and remarks`() {
        val reply = syntheticReply("Data" to 0L, "USBData" to 0L)

        val entries = DefaultSmbShareEnumerator.parseShareEnumReply(reply)

        assertEquals(
            listOf(
                SmbShareEntry("Data", 0, "remark of Data"),
                SmbShareEntry("USBData", 0, "remark of USBData"),
            ),
            entries,
        )
    }

    @Test
    fun `special and ipc shares are filtered out`() {
        val reply = syntheticReply(
            "Data" to 0L,
            "ADMIN$" to 0x80000000L,
            "IPC$" to 0x80000003L,
            "print" to 0x00000001L,
        )

        val visible = DefaultSmbShareEnumerator.parseShareEnumReply(reply).filter { it.isDisk }

        assertEquals(listOf("Data"), visible.map { it.name })
        assertFalse(SmbShareEntry("ADMIN$", 0x80000000L, "").isDisk)
        assertTrue(SmbShareEntry("Disk", 0L, "").isDisk)
    }

    @Test
    fun `null buffer yields no entries`() {
        val w = NdrWriter()
        w.u32(1)
        w.u32(1)
        w.u32(0) // container referent NULL
        w.u32(0) // TotalEntries
        w.u32(0) // ResumeHandle NULL
        w.u32(0)
        assertEquals(emptyList<SmbShareEntry>(), DefaultSmbShareEnumerator.parseShareEnumReply(w.toByteArray()))
    }

    @Test(expected = SmbEnumerationException::class)
    fun `non zero return code throws`() {
        val w = NdrWriter()
        w.u32(1)
        w.u32(1)
        w.u32(0)
        w.u32(0)
        w.u32(0)
        w.u32(5) // ACCESS_DENIED
        DefaultSmbShareEnumerator.parseShareEnumReply(w.toByteArray())
    }

    @Test(expected = SmbEnumerationException::class)
    fun `unexpected info level throws`() {
        val w = NdrWriter()
        w.u32(0) // level 0 response to a level 1 request
        w.u32(0)
        w.u32(0)
        w.u32(0)
        w.u32(0)
        w.u32(0)
        DefaultSmbShareEnumerator.parseShareEnumReply(w.toByteArray())
    }
}

package dev.iimuz.capturehub.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class UlidTest {
    @Test
    fun `generates 26 character crockford base32 string`() {
        val id = Ulid.generate()
        assertEquals(26, id.length)
        assertTrue(id.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" })
    }

    @Test
    fun `encodes known timestamp into first 10 characters`() {
        // ULID 仕様リポジトリの既知のテストベクタ
        val id = Ulid.generate(timeMillis = 1469918176385L)
        assertEquals("01ARZ3NDEK", id.substring(0, 10))
    }

    @Test
    fun `sorts by timestamp`() {
        val earlier = Ulid.generate(timeMillis = 1_000L)
        val later = Ulid.generate(timeMillis = 2_000L)
        assertTrue(earlier < later)
    }

    @Test
    fun `uses injected random for suffix`() {
        val a = Ulid.generate(timeMillis = 0L, random = Random(42))
        val b = Ulid.generate(timeMillis = 0L, random = Random(42))
        assertEquals(a, b)
    }
}

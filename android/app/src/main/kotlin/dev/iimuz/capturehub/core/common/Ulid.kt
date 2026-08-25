package dev.iimuz.capturehub.core.common

import java.security.SecureRandom
import java.util.Random

object Ulid {
    private const val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val defaultRandom = SecureRandom()

    fun generate(
        timeMillis: Long = System.currentTimeMillis(),
        random: Random = defaultRandom,
    ): String {
        require(timeMillis >= 0) { "timeMillis must be non-negative" }
        val chars = CharArray(26)
        var time = timeMillis
        for (i in 9 downTo 0) {
            chars[i] = ENCODING[(time and 0x1F).toInt()]
            time = time ushr 5
        }
        for (i in 10 until 26) {
            chars[i] = ENCODING[random.nextInt(32)]
        }
        return String(chars)
    }
}

package dev.iimuz.capturehub.sync

import java.io.IOException

class InMemoryVaultFiles : VaultFiles {
    val files = mutableMapOf<String, String>()

    override fun readOrNull(fileName: String): String? = files[fileName]

    override fun create(fileName: String) {
        files.putIfAbsent(fileName, "")
    }

    override fun append(
        fileName: String,
        content: String,
    ) {
        files[fileName] = files.getValue(fileName) + content
    }
}

class FailingVaultFiles : VaultFiles {
    override fun readOrNull(fileName: String): String? = throw IOException("vault unreachable")

    override fun create(fileName: String): Unit = throw IOException("vault unreachable")

    override fun append(
        fileName: String,
        content: String,
    ): Unit = throw IOException("vault unreachable")
}

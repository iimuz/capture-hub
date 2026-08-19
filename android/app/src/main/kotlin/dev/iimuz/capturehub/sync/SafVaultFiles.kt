package dev.iimuz.capturehub.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

class SafVaultFiles(
    private val context: Context,
    treeUri: Uri,
) : VaultFiles {
    private val root =
        DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Cannot access vault tree: $treeUri")

    // provider は要求した displayName を調整して作成することがあり、名前による
    // 再解決は失敗しうる。作成・取得したハンドルを保持して再解決を避ける
    private val handles = mutableMapOf<String, DocumentFile>()

    override fun readOrNull(fileName: String): String? {
        val file = root.findFile(fileName) ?: return null
        handles[fileName] = file
        val input =
            context.contentResolver.openInputStream(file.uri)
                ?: throw IOException("Cannot open $fileName for reading")
        return input.reader(Charsets.UTF_8).use { it.readText() }
    }

    override fun create(fileName: String) {
        handles[fileName] =
            root.createFile("text/markdown", fileName)
                ?: throw IOException("Cannot create $fileName")
    }

    override fun append(
        fileName: String,
        content: String,
    ) {
        val file =
            handles[fileName]
                ?: root.findFile(fileName)
                ?: throw IOException("$fileName not found")
        val output =
            context.contentResolver.openOutputStream(file.uri, "wa")
                ?: throw IOException("Cannot open $fileName for appending")
        output.writer(Charsets.UTF_8).use { it.write(content) }
    }
}

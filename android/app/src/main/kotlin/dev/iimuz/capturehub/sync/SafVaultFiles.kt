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

    override fun readOrNull(fileName: String): String? {
        val file = root.findFile(fileName) ?: return null
        val input =
            context.contentResolver.openInputStream(file.uri)
                ?: throw IOException("Cannot open $fileName for reading")
        return input.reader(Charsets.UTF_8).use { it.readText() }
    }

    override fun create(fileName: String) {
        // displayName が mime に対応する拡張子を含む場合、provider は拡張子を
        // 二重付与しない
        root.createFile("text/markdown", fileName)
            ?: throw IOException("Cannot create $fileName")
    }

    override fun append(
        fileName: String,
        content: String,
    ) {
        val file =
            root.findFile(fileName)
                ?: throw IOException("$fileName not found")
        val output =
            context.contentResolver.openOutputStream(file.uri, "wa")
                ?: throw IOException("Cannot open $fileName for appending")
        output.writer(Charsets.UTF_8).use { it.write(content) }
    }
}

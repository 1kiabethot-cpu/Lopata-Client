package com.lopata.launcher

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.ZipInputStream

enum class Kind(val sub: String, val ext: String, val marker: String, val label: String) {
    MOD("mods", ".jar", "fabric.mod.json", "Fabric-мод"),
    PACK("resourcepacks", ".zip", "pack.mcmeta", "ресурспак")
}

object GameFiles {
    fun root(ctx: Context, uri: String?): DocumentFile? =
        uri?.let { DocumentFile.fromTreeUri(ctx, Uri.parse(it)) }?.takeIf { it.exists() && it.canWrite() }

    private fun dir(root: DocumentFile, name: String): DocumentFile? =
        root.findFile(name)?.takeIf { it.isDirectory } ?: root.createDirectory(name)

    private fun displayName(ctx: Context, uri: Uri): String {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return uri.lastPathSegment ?: "file"
    }

    private fun hasEntry(ctx: Context, uri: Uri, entry: String): Boolean {
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            ZipInputStream(ins).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    if (e.name == entry) return true
                    e = z.nextEntry
                }
            }
        }
        return false
    }

    fun install(ctx: Context, treeUri: String?, kind: Kind, src: Uri): String {
        val root = root(ctx, treeUri) ?: error("Спочатку обери папку гри (.minecraft)")
        val name = displayName(ctx, src)
        require(name.lowercase().endsWith(kind.ext)) { "$name: потрібен файл ${kind.ext}" }
        require(hasEntry(ctx, src, kind.marker)) { "$name: не схоже на ${kind.label} (нема ${kind.marker} у корені архіву)" }
        val d = dir(root, kind.sub) ?: error("Не вдалося створити папку ${kind.sub}")
        d.findFile(name)?.delete()
        val out = d.createFile("application/octet-stream", name) ?: error("Не вдалося створити $name")
        ctx.contentResolver.openInputStream(src)!!.use { i ->
            ctx.contentResolver.openOutputStream(out.uri, "wt")!!.use { o -> i.copyTo(o) }
        }
        return name
    }

    fun list(ctx: Context, treeUri: String?, kind: Kind): List<String> =
        root(ctx, treeUri)?.findFile(kind.sub)?.listFiles()
            ?.filter { it.isFile }?.mapNotNull { it.name }?.sorted() ?: emptyList()

    fun remove(ctx: Context, treeUri: String?, kind: Kind, name: String): Boolean =
        root(ctx, treeUri)?.findFile(kind.sub)?.findFile(name)?.delete() ?: false

    private fun backup(ctx: Context, root: DocumentFile, f: DocumentFile, newName: String) {
        root.findFile(newName)?.delete()
        val b = root.createFile("application/octet-stream", newName) ?: return
        ctx.contentResolver.openInputStream(f.uri)?.use { i ->
            ctx.contentResolver.openOutputStream(b.uri, "wt")?.use { o -> i.copyTo(o) }
        }
    }

    fun applyOptions(ctx: Context, treeUri: String?, preset: Preset): Int {
        val root = root(ctx, treeUri) ?: error("Спочатку обери папку гри (.minecraft)")
        val f = root.findFile("options.txt")
        val map = LinkedHashMap<String, String>()
        if (f != null) {
            backup(ctx, root, f, "options.lopata-backup.txt")
            ctx.contentResolver.openInputStream(f.uri)?.bufferedReader()?.useLines { lines ->
                lines.forEach { l -> val i = l.indexOf(':'); if (i > 0) map[l.substring(0, i)] = l.substring(i + 1) }
            }
        }
        map.putAll(preset.values)
        val target = f ?: root.createFile("application/octet-stream", "options.txt") ?: error("Не вдалося створити options.txt")
        ctx.contentResolver.openOutputStream(target.uri, "wt")!!.bufferedWriter().use { w ->
            map.forEach { (k, v) -> w.write("$k:$v\n") }
        }
        return preset.values.size
    }

    fun writeServers(ctx: Context, treeUri: String?, list: List<Server>) {
        val root = root(ctx, treeUri) ?: error("Спочатку обери папку гри (.minecraft)")
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).apply {
            writeByte(10); writeUTF("")
            writeByte(9); writeUTF("servers"); writeByte(10); writeInt(list.size)
            list.forEach {
                writeByte(8); writeUTF("ip"); writeUTF(it.ip)
                writeByte(8); writeUTF("name"); writeUTF(it.name)
                writeByte(0)
            }
            writeByte(0)
        }
        val f = root.findFile("servers.dat")
        if (f != null) backup(ctx, root, f, "servers.lopata-backup.dat")
        val target = f ?: root.createFile("application/octet-stream", "servers.dat") ?: error("Не вдалося створити servers.dat")
        ctx.contentResolver.openOutputStream(target.uri, "wt")!!.use { it.write(bos.toByteArray()) }
    }
}

package com.lopata.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AppModel(private val ctx: Context) {
    private val store = Store(ctx)
    private val scope: CoroutineScope = MainScope()

    var message by mutableStateOf<String?>(null)
    var treeUri by mutableStateOf(store.treeUri)
    var pickFolder: () -> Unit = {}

    val servers = mutableStateListOf<Server>().apply { addAll(store.servers()) }
    val profiles = mutableStateListOf<Profile>()
    var activeId by mutableStateOf(store.activeProfile)
    var mods by mutableStateOf<List<String>>(emptyList())
    var packs by mutableStateOf<List<String>>(emptyList())

    init {
        profiles.addAll(store.profiles())
        if (profiles.isEmpty()) {
            val p = Profile(UUID.randomUUID().toString(), "Основний", "Player", Preset.BALANCED.name)
            profiles.add(p); store.saveProfiles(profiles); activeId = p.id; store.activeProfile = p.id
        }
        if (profiles.none { it.id == activeId }) { activeId = profiles[0].id; store.activeProfile = activeId }
        refresh()
    }

    val active: Profile get() = profiles.first { it.id == activeId }
    val folderOk: Boolean get() = GameFiles.root(ctx, treeUri) != null

    fun setTree(uri: Uri) {
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        treeUri = uri.toString(); store.treeUri = treeUri
        val r = GameFiles.root(ctx, treeUri)
        val looks = r?.listFiles()?.any { it.name in setOf("mods", "versions", "options.txt", "resourcepacks", "saves") } == true
        message = if (r == null) "Немає доступу на запис до цієї папки"
        else if (looks) "Папку гри збережено" else "Збережено, але це не схоже на .minecraft (нема mods/versions/options.txt)"
        refresh()
    }

    private fun io(okMsg: (() -> String)? = null, work: () -> Unit) {
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching(work) }
            r.onSuccess { okMsg?.let { m -> message = m() } }
            r.onFailure { message = "Помилка: ${it.message ?: it.javaClass.simpleName}" }
            refresh()
        }
    }

    fun refresh() {
        val t = treeUri
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { GameFiles.list(ctx, t, Kind.MOD) to GameFiles.list(ctx, t, Kind.PACK) }
                    .getOrDefault(emptyList<String>() to emptyList<String>())
            }
            mods = res.first; packs = res.second
        }
    }

    fun install(kind: Kind, uris: List<Uri>) {
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                uris.map { u -> runCatching { GameFiles.install(ctx, treeUri, kind, u) } }
            }
            val ok = results.count { it.isSuccess }
            val err = results.firstOrNull { it.isFailure }?.exceptionOrNull()?.message
            message = "Встановлено: $ok з ${results.size}" + (if (err != null) ". $err" else "")
            refresh()
        }
    }

    fun remove(kind: Kind, name: String) = io({ "Видалено $name" }) {
        check(GameFiles.remove(ctx, treeUri, kind, name)) { "не вдалося видалити" }
    }

    fun applyPreset() {
        val p = Preset.valueOf(active.preset)
        io({ "Застосовано «${p.title}» до options.txt (копія: options.lopata-backup.txt)" }) {
            GameFiles.applyOptions(ctx, treeUri, p)
        }
    }

    fun exportServers() {
        val list = servers.toList()
        io({ "Записано ${list.size} серв. у servers.dat (копія: servers.lopata-backup.dat)" }) {
            GameFiles.writeServers(ctx, treeUri, list)
        }
    }

    private val ipRe = Regex("^[A-Za-z0-9.-]{1,253}(:\\d{1,5})?$")
    fun addServer(name: String, ip: String) {
        val i = ip.trim(); val n = name.trim().ifEmpty { i }
        if (!ipRe.matches(i)) { message = "Невірна адреса. Приклад: play.example.com:25565"; return }
        if (servers.any { it.ip.equals(i, true) }) { message = "Така адреса вже є"; return }
        servers.add(Server(n, i)); store.saveServers(servers)
    }
    fun removeServer(s: Server) { servers.remove(s); store.saveServers(servers) }

    private val nickRe = Regex("^[A-Za-z0-9_]{3,16}$")
    private fun update(p: Profile) {
        val i = profiles.indexOfFirst { it.id == p.id }; if (i >= 0) profiles[i] = p; store.saveProfiles(profiles)
    }
    fun setNick(n: String) {
        if (!nickRe.matches(n)) { message = "Нік: 3–16 символів, лише A–Z, 0–9, _"; return }
        update(active.copy(nick = n)); message = "Нік збережено локально"
    }
    fun setPreset(p: Preset) = update(active.copy(preset = p.name))
    fun addProfile(name: String) {
        val n = name.trim(); if (n.isEmpty()) { message = "Вкажи назву профілю"; return }
        val p = Profile(UUID.randomUUID().toString(), n, active.nick, active.preset)
        profiles.add(p); store.saveProfiles(profiles); select(p.id)
    }
    fun select(id: String) { activeId = id; store.activeProfile = id }
    fun removeProfile(p: Profile) {
        if (profiles.size <= 1) { message = "Не можна видалити останній профіль"; return }
        profiles.remove(p); store.saveProfiles(profiles)
        if (activeId == p.id) select(profiles[0].id)
    }

    private val launchers = listOf("net.kdt.pojavlaunch", "com.movtery.zalithlauncher")
    fun launcherPackage(): String? =
        launchers.firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }

    fun openLauncher() {
        val i = launcherPackage()?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
        if (i == null) { message = "PojavLauncher не знайдено на телефоні"; return }
        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { message = "Не вдалося відкрити лаунчер: ${it.message}" }
    }
}

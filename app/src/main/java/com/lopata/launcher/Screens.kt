package com.lopata.launcher

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }
}

@Composable
private fun field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) =
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Pink, unfocusedBorderColor = PinkDim,
            focusedLabelColor = Pink, cursorColor = Pink)
    )

@Composable
fun HomeScreen(m: AppModel) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val pkg = m.launcherPackage()
    Page {
        Spacer(Modifier.height(12.dp))
        Text("LOPATA", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Pink.copy(alpha = glow))
        Text("CLIENT · Minecraft 1.21.4 · Fabric", color = Soft)
        NeonCard {
            H("Профіль: ${m.active.name}")
            Text("Локальний нік: ${m.active.nick}", color = Soft)
            Text("Нік не є авторизацією Microsoft. На online-mode серверах увійти з ним не вийде.",
                color = Soft.copy(alpha = 0.7f), fontSize = 13.sp)
        }
        NeonCard {
            H("Стан")
            Text(if (pkg != null) "PojavLauncher: знайдено ($pkg)" else "PojavLauncher: не знайдено", color = Soft)
            Text(if (m.folderOk) "Папка гри: підключена" else "Папка гри: не вибрана", color = Soft)
            Text("Модів: ${m.mods.size} · Ресурспаків: ${m.packs.size}", color = Soft)
        }
        if (!m.folderOk) NeonButton("Обрати папку .minecraft", Modifier.fillMaxWidth()) { m.pickFolder() }
        NeonButton("Відкрити PojavLauncher", Modifier.fillMaxWidth(), enabled = pkg != null) { m.openLauncher() }
        Text("Лаунчер лише відкриває Pojav. Вибрати профіль 1.21.4 Fabric і натиснути Play потрібно в ньому.",
            color = Soft.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
fun ServersScreen(m: AppModel) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    Page {
        NeonCard {
            H("Додати сервер")
            field(name, { name = it }, "Назва")
            Spacer(Modifier.height(8.dp))
            field(ip, { ip = it }, "IP або домен (:порт)")
            Spacer(Modifier.height(10.dp))
            NeonButton("Додати", Modifier.fillMaxWidth()) {
                m.addServer(name, ip)
                if (m.servers.any { it.ip.equals(ip.trim(), true) }) { name = ""; ip = "" }
            }
        }
        m.servers.forEach { s ->
            NeonCard {
                Text(s.name, fontWeight = FontWeight.Bold, color = Soft)
                Text(s.ip, color = Pink)
                Row {
                    TextButton(onClick = { clip.setText(AnnotatedString(s.ip)); m.message = "IP скопійовано" }) { Text("Копіювати", color = Pink) }
                    TextButton(onClick = { m.removeServer(s) }) { Text("Видалити", color = Soft) }
                }
            }
        }
        if (m.servers.isEmpty()) Text("Список порожній", color = Soft.copy(alpha = 0.7f))
        NeonCard {
            H("Експорт у гру")
            Text("Записує список у servers.dat. Поточний файл буде збережено як servers.lopata-backup.dat.",
                color = Soft.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            NeonButton("Записати в servers.dat", Modifier.fillMaxWidth(), enabled = m.folderOk && m.servers.isNotEmpty()) { m.exportServers() }
        }
    }
}

@Composable
fun ModsScreen(m: AppModel) {
    val pickMods = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { l: List<Uri> ->
        if (l.isNotEmpty()) m.install(Kind.MOD, l)
    }
    val pickPacks = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { l: List<Uri> ->
        if (l.isNotEmpty()) m.install(Kind.PACK, l)
    }
    Page {
        if (!m.folderOk) {
            NeonCard {
                Text("Спочатку обери папку .minecraft, яку використовує PojavLauncher.", color = Soft)
                Spacer(Modifier.height(8.dp))
                NeonButton("Обрати папку", Modifier.fillMaxWidth()) { m.pickFolder() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton("+ Мод .jar", Modifier.weight(1f), enabled = m.folderOk) { pickMods.launch(arrayOf("*/*")) }
            NeonButton("+ Ресурспак .zip", Modifier.weight(1f), enabled = m.folderOk) { pickPacks.launch(arrayOf("*/*")) }
        }
        listOf(Kind.MOD to m.mods, Kind.PACK to m.packs).forEach { (kind, files) ->
            NeonCard {
                H("${kind.sub} (${files.size})")
                if (files.isEmpty()) Text("Порожньо", color = Soft.copy(alpha = 0.7f))
                files.forEach { f ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(f, color = Soft, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = { m.remove(kind, f) }) { Text("Видалити", color = Pink) }
                    }
                }
            }
        }
        Text("Перевіряється, що .jar містить fabric.mod.json, а .zip — pack.mcmeta. Сумісність мода з 1.21.4 не перевіряється.",
            color = Soft.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
fun ProfileScreen(m: AppModel) {
    var nick by remember(m.activeId, m.active.nick) { mutableStateOf(m.active.nick) }
    var newName by remember { mutableStateOf("") }
    Page {
        NeonCard {
            H("Локальний нікнейм")
            field(nick, { nick = it }, "Нік (3–16: A–Z, 0–9, _)")
            Spacer(Modifier.height(8.dp))
            NeonButton("Зберегти", Modifier.fillMaxWidth()) { m.setNick(nick) }
        }
        NeonCard {
            H("Важливо")
            Text("Це лише збережена назва. Microsoft-авторизації тут немає, тому на серверах з online-mode такий нік не пустить. Нік у самій грі задається в PojavLauncher.",
                color = Soft, fontSize = 14.sp)
        }
        NeonCard {
            H("Профілі")
            m.profiles.forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = p.id == m.activeId, onClick = { m.select(p.id) },
                        colors = RadioButtonDefaults.colors(selectedColor = Pink))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = Soft, fontWeight = FontWeight.Bold)
                        Text("${p.nick} · ${Preset.valueOf(p.preset).title}", color = Soft.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    TextButton(onClick = { m.removeProfile(p) }) { Text("Видалити", color = Pink) }
                }
            }
            Spacer(Modifier.height(8.dp))
            field(newName, { newName = it }, "Назва нового профілю")
            Spacer(Modifier.height(8.dp))
            GhostButton("Створити профіль", Modifier.fillMaxWidth()) { m.addProfile(newName); newName = "" }
        }
    }
}

@Composable
fun SettingsScreen(m: AppModel) {
    Page {
        NeonCard {
            H("Папка гри")
            Text(if (m.folderOk) "Підключена" else "Не вибрана", color = Soft)
            Text("Обери ту .minecraft, яку відкриває PojavLauncher (зазвичай у games/PojavLauncher). Папки Android/data Android 12 для вибору не дозволяє.",
                color = Soft.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            GhostButton("Обрати папку", Modifier.fillMaxWidth()) { m.pickFolder() }
        }
        NeonCard {
            H("Оптимізація Minecraft")
            Preset.values().forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = m.active.preset == p.name, onClick = { m.setPreset(p) },
                        colors = RadioButtonDefaults.colors(selectedColor = Pink))
                    Column {
                        Text(p.title, color = Soft, fontWeight = FontWeight.Bold)
                        Text(p.desc, color = Soft.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            NeonButton("Застосувати до options.txt", Modifier.fillMaxWidth(), enabled = m.folderOk) { m.applyPreset() }
            Text("Змінюються лише ключі пресету, решта options.txt лишається. Копія: options.lopata-backup.txt. Закрий гру перед застосуванням.",
                color = Soft.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        NeonCard {
            H("Поради для Pojav")
            Text("Пам'ять (RAM), рендерер і Java налаштовуються лише в самому PojavLauncher — цей застосунок до них доступу не має. Для Redmi Note 9 Pro орієнтуйся на 2–3 ГБ RAM для гри.",
                color = Soft, fontSize = 13.sp)
        }
        Text("LOPATA CLIENT launcher 0.2.0", color = Soft.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

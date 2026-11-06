package com.lopata.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = AppModel(applicationContext)
        setContent {
            LopataTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val snack = remember { SnackbarHostState() }
                val pickTree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    if (uri != null) model.setTree(uri)
                }
                model.pickFolder = { pickTree.launch(null) }
                LaunchedEffect(model.message) {
                    model.message?.let { snack.showSnackbar(it); model.message = null }
                }
                val items = listOf(
                    "Головна" to Icons.Default.Home, "Сервери" to Icons.Default.Star,
                    "Моди" to Icons.Default.Add, "Профіль" to Icons.Default.Person,
                    "Налашт." to Icons.Default.Settings
                )
                Scaffold(
                    containerColor = Color.Black,
                    snackbarHost = { SnackbarHost(snack) },
                    bottomBar = {
                        NavigationBar(containerColor = Panel) {
                            items.forEachIndexed { i, (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == i, onClick = { tab = i },
                                    icon = { Icon(icon, label) }, label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black, selectedTextColor = Pink,
                                        indicatorColor = Pink, unselectedIconColor = Soft, unselectedTextColor = Soft)
                                )
                            }
                        }
                    }
                ) { pad ->
                    Crossfade(tab, animationSpec = tween(300), modifier = androidx.compose.ui.Modifier.padding(pad), label = "tabs") { t ->
                        when (t) {
                            0 -> HomeScreen(model)
                            1 -> ServersScreen(model)
                            2 -> ModsScreen(model)
                            3 -> ProfileScreen(model)
                            else -> SettingsScreen(model)
                        }
                    }
                }
            }
        }
    }
}

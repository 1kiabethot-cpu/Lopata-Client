package com.lopata.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("lopata", Context.MODE_PRIVATE)

    var treeUri: String?
        get() = sp.getString("tree", null)
        set(v) { sp.edit().putString("tree", v).apply() }

    var activeProfile: String
        get() = sp.getString("active", "") ?: ""
        set(v) { sp.edit().putString("active", v).apply() }

    fun servers(): List<Server> = runCatching {
        val a = JSONArray(sp.getString("servers", "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Server(o.getString("name"), o.getString("ip")) } }
    }.getOrDefault(emptyList())

    fun saveServers(l: List<Server>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("name", it.name).put("ip", it.ip)) }
        sp.edit().putString("servers", a.toString()).apply()
    }

    fun profiles(): List<Profile> = runCatching {
        val a = JSONArray(sp.getString("profiles", "[]"))
        (0 until a.length()).map {
            a.getJSONObject(it).let { o -> Profile(o.getString("id"), o.getString("name"), o.getString("nick"), o.getString("preset")) }
        }
    }.getOrDefault(emptyList())

    fun saveProfiles(l: List<Profile>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("nick", it.nick).put("preset", it.preset)) }
        sp.edit().putString("profiles", a.toString()).apply()
    }
}

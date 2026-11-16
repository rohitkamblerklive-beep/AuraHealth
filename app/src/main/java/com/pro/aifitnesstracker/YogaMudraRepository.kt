package com.pro.aifitnesstracker

import android.content.Context
import org.json.JSONArray

data class YogaMudra(
    val id: String,
    val name: String,
    val translation: String,
    val category: String,
    val benefits: String,
    val steps: List<String>,
    val duration: String,
    val icon: String
)

class YogaMudraRepository(private val context: Context) {
    private var mudras: List<YogaMudra> = emptyList()

    init {
        loadMudras()
    }

    private fun loadMudras() {
        try {
            val prefs = SharedPrefsManager(context)
            val lang = prefs.getLanguage()
            val fileName = if (lang == "en" || lang.isBlank()) "yoga_mudras.json" else "yoga_mudras_$lang.json"
            val jsonString = try {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                context.assets.open("yoga_mudras.json").bufferedReader().use { it.readText() }
            }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<YogaMudra>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val stepsArray = obj.getJSONArray("steps")
                val stepsList = mutableListOf<String>()
                for (j in 0 until stepsArray.length()) {
                    stepsList.add(stepsArray.getString(j))
                }
                list.add(
                    YogaMudra(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        translation = obj.getString("translation"),
                        category = obj.getString("category"),
                        benefits = obj.getString("benefits"),
                        steps = stepsList,
                        duration = obj.getString("duration"),
                        icon = obj.getString("icon")
                    )
                )
            }
            mudras = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllMudras(): List<YogaMudra> = mudras

    fun getMudrasByCategory(category: String): List<YogaMudra> {
        return mudras.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun searchMudras(query: String): List<YogaMudra> {
        if (query.isBlank()) return mudras
        val trimmed = query.trim().lowercase()
        return mudras.filter {
            it.name.lowercase().contains(trimmed) ||
            it.benefits.lowercase().contains(trimmed) ||
            it.translation.lowercase().contains(trimmed)
        }
    }
}

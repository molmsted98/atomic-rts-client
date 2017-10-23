package com.atomicobject.rts.models

import org.json.simple.JSONObject
import kotlin.collections.Map

data class Game(val mapWidth: Int, val mapHeight: Int, val gameDuration: Int, val turnDuration: Int,
                val unitInfo: Map<String, String>, val map: HashMap<Pair<Int, Int>, Tile>, var time: Int, var turn: Int) {
    fun updateTile(key: Pair<Int, Int>, tile: Tile) {
        map.put(key, tile)
    }

    companion object {
        @JvmStatic fun newInstance(gameInfo: JSONObject): Game {
            val height = (gameInfo["map_height"] as Long).toInt()
            val width = (gameInfo["map_width"] as Long).toInt()
            val gameDuration = (gameInfo["game_duration"] as Long).toInt()
            val turnDuration = (gameInfo["turn_duration"] as Long).toInt()
            val unitInfo = gameInfo["unit_info"] as Map<String, String>

            return Game(height, width, gameDuration, turnDuration, unitInfo, HashMap(), 0, 0)
        }
    }
}
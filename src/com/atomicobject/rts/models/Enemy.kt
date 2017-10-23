package com.atomicobject.rts.models

import org.json.simple.JSONObject

data class Enemy(val id: Long, val type: String, val status: String, val player_id: Int, val health: Int) {
    companion object {
        @JvmStatic fun newInstance(enemyUnit: JSONObject): Enemy {
            val id = enemyUnit["id"] as Long
            val type = enemyUnit["type"] as String
            val status = enemyUnit["status"] as String
            val playerId = (enemyUnit["player_id"] as Long).toInt()
            val health = (enemyUnit["health"] as Long).toInt()

            return Enemy(id, type, status, playerId, health)
        }
    }
}

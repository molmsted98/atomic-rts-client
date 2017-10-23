package com.atomicobject.rts.models

import org.json.simple.JSONObject

data class Tile(var visible: Boolean, var x: Long, var y: Long, var blocked: Boolean, var resource: Resource?,
                var enemies: ArrayList<Enemy>) {
    fun update(json: JSONObject, resource: Resource?, enemies: ArrayList<Enemy>) {
        val visible = json["visible"] as Boolean
        var blocked = true
        if(json["blocked"] != null) {
            blocked = json["blocked"] as Boolean
        }
        this.visible = visible
        this.blocked = blocked

        if(this.resource != null && resource != null) {
            this.resource!!.update(resource)
        }
        else {
            this.resource = resource
        }

        this.enemies = enemies
    }

    companion object {
        @JvmStatic fun newInstance(json: JSONObject, resources: Resource?, enemies: ArrayList<Enemy>): Tile {
            val x = json["x"] as Long
            val y = json["y"] as Long
            val visible = json["visible"] as Boolean
            val blocked = json["blocked"] as Boolean

            return Tile(visible, x, y, blocked, resources, enemies)
        }
    }
}
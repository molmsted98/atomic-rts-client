package com.atomicobject.rts.models

data class Resource(val id: Long, var type: String, val total: Long, var value: Long, var assignedWorkersIds: ArrayList<Long>,
                    val x: Long, val y: Long) {
    fun update(resource: Resource) {
        this.type = resource.type
        this.value = resource.value
    }
}
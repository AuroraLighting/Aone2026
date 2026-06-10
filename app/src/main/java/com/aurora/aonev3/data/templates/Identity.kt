package com.aurora.aonev3.data.templates

import androidx.room.Entity

@Entity(tableName = "identities", primaryKeys = ["deviceClass", "defaultName"])
data class Identity(
        val deviceClass: String,
        val defaultName: String,
        val date: Int,
        val version: String
)
package com.soundicons.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Feature 8: Category/Folder entity */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

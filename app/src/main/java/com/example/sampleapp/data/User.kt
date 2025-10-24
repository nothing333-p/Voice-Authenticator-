package com.example.sampleapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

// We'll use a converter for the list of audio file paths later
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val gender: String,
    val audioPaths: List<String> // store paths to recorded audio files
)

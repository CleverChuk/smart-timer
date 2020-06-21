package com.cleverchuk.smarttimer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_state")
data class TimerState(
        @PrimaryKey
        val id: Int,
        @ColumnInfo(name = "current_screen")
        val currentScreen: Int,
        val time: Int,
        val fullTime: Int,
        val delay: Int,
        val state: Int)
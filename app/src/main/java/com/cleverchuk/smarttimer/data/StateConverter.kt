package com.cleverchuk.smarttimer.data

import androidx.room.TypeConverter
import com.cleverchuk.smarttimer.ui.Timer

class StateConverter {
    companion object {
        @TypeConverter
        fun stateToInt(state: Timer.State?): Int {
            return state!!.ordinal
        }

        @TypeConverter
        fun intToState(state: Int): Timer.State {
            return when (state) {
                1 -> Timer.State.STARTED
                2 -> Timer.State.COUNTING
                3 -> Timer.State.STOPPED
                4 -> Timer.State.PAUSED
                5 -> Timer.State.DONE
                else -> Timer.State.IDLE
            }
        }
    }
}
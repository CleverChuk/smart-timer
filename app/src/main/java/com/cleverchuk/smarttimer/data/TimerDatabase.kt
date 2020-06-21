package com.cleverchuk.smarttimer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TimerState::class], version = 1, exportSchema = false)
//@TypeConverters(StateConverter::class)
abstract class TimerDatabase : RoomDatabase() {

    abstract fun timerStateDao(): TimerStateDao

    companion object {
        const val DATABASE_NAME = "timer_database"

        @Volatile
        private var INSTANCE: TimerDatabase? = null

        fun getDatabase(context: Context): TimerDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TimerDatabase::class.java,
                        DATABASE_NAME
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
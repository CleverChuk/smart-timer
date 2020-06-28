package com.cleverchuk.smarttimer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TimerStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timerState: TimerState)

    @Update
    suspend fun update(timerState: TimerState)

    @Query("SELECT * FROM timer_state WHERE id=:id")
    suspend fun findById(id: Int): TimerState?

    @Query("SELECT * FROM timer_state WHERE id=:id")
    fun observableFindById(id: Int): LiveData<TimerState?>

    @Query("SELECT * FROM timer_state")
    suspend fun findAll(): List<TimerState>

    @Query("DELETE FROM timer_state")
    suspend fun delete()
}
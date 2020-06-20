package com.cleverchuk.smarttimer

import com.cleverchuk.smarttimer.ui.TimeFragment
import com.cleverchuk.smarttimer.ui.Timer
import org.junit.Test
import java.util.concurrent.Executors


class TimerTest {
    @Test
    fun testCountDown() {
        val timer = Timer(Executors.newSingleThreadExecutor())

//            System.out.println(String.format("%d = %d : %d : %d", time, extractHour(time), extractMinute(time), extractSecond(time)))
        timer.start(TimeFragment())
    }
}
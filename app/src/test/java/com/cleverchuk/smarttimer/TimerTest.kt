package com.cleverchuk.smarttimer

import com.cleverchuk.smarttimer.ui.TimeFragment
import com.cleverchuk.smarttimer.ui.Timer
import org.junit.Test
import java.util.concurrent.Executors


class TimerTest {
    @ExperimentalUnsignedTypes
    @Test
    fun testCountDown() {
        val timer = Timer(Executors.newSingleThreadExecutor())

        var time = timer.combineTime(19, 56, 20)

        while (time > 0) {
            time--
            if (timer.extractMinute(time) > 59 && timer.extractSecond(time) > 59)
                time = timer.combineTime(timer.extractHour(time), 59, 59)
            else if (timer.extractSecond(time) > 59)
                time = timer.combineTime(timer.extractHour(time), timer.extractMinute(time), 59)
            else if (timer.extractMinute(time) > 0 && timer.extractSecond(time) == 0)
                time = timer.combineTime(timer.extractHour(time), timer.extractMinute(time) - 1, 59)

            System.out.println(String.format("%s = %d : %d : %d", time.toUInt().toString(2), timer.extractHour(time), timer.extractMinute(time), timer
                    .extractSecond(time)))
        }
    }
}
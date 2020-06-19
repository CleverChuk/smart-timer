package com.cleverchuk.smarttimer

import com.cleverchuk.smarttimer.ui.Timer
import org.junit.Before
import org.junit.Test


class TimerTest {
    @Test
    fun testCountDown(){
      val  timer = Timer(hr=0, min =59, sec = 0)
        timer.countDown()
    }
}
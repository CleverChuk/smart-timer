package com.cleverchuk.smarttimer.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.cleverchuk.smarttimer.MainActivity
import com.cleverchuk.smarttimer.R
import com.cleverchuk.smarttimer.data.TimerDatabase
import com.cleverchuk.smarttimer.data.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class CountDownService : Service() {
    private var timer: Timer? = null
    lateinit var timeFragment: TimeFragment
    private var wakeLock: PowerManager.WakeLock? = null
    private val countDownBinder: CountDownBinder = CountDownBinder()
    private val timerDatabase: TimerDatabase = TimerDatabase.getDatabase(this)
    private var state: Timer.State? = null
    private var time: Int? = null
    private val uri: Uri = Uri.parse("android.resource://com.cleverchuk.smarttimer/" + R.raw.faded_chords);

    private val stateObserver: Observer<Timer.State> = Observer { state ->
        if ((state == Timer.State.DONE) && timeFragment.repeat) {
            val notificationManager: NotificationManager = this@CountDownService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            notificationManager.notify(VIBRATE_NOTIFICATION_ID, createNotificationWithVibration(timeFragment.hr, timeFragment.min, timeFragment.sec))
            timer?.start(timeFragment)
        }
    }

    private val timerObserver: Observer<TimeFragment> = Observer { timeFragment ->
        val notificationManager: NotificationManager = this@CountDownService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotificationWithoutVibration(timeFragment.hr, timeFragment.min, timeFragment.sec))
        GlobalScope.launch(Dispatchers.IO) {
            timerDatabase.timerStateDao()
                    .update(TimerState(1,
                            1,
                            timer?.time!!,
                            timer!!.combineTime(
                                    this@CountDownService.timeFragment.hr,
                                    this@CountDownService.timeFragment.min,
                                    this@CountDownService.timeFragment.sec
                            ),
                            this@CountDownService.timeFragment.delay,
                            state?.ordinal ?: 2))
        }
    }

    override fun onCreate() {
        timer = Timer()
        startForeground(NOTIFICATION_ID, createNotificationWithoutVibration(timer?.hr, timer?.min, timer?.sec))
        Log.i("onCreate", "The service has been created".toUpperCase(Locale.ROOT))

    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("Someone tried to bind: ", intent.toString())
        return countDownBinder
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("onStartCommand", "executed with startId: $startId")
        if (intent != null) {
            state = Timer.State.valueOf(intent.action!!)
            when (intent.action) {
                Timer.State.COUNTING.name -> {
                    timer?.state?.observeForever(stateObserver)
                    timer?.timeFragment?.observeForever(timerObserver)

                    timeFragment = intent.getParcelableExtra("fulltime")!!
                    time = intent.getIntExtra("time", 0)
                    timer?.start(time!!)
                    // we need this lock so our service gets not affected by Doze Mode
                    wakeLock =
                            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CountDownService::lock").apply {
                                    acquire(2000)
                                }
                            }
                }
                else -> stopService()
            }
        } else {
            Log.i("null intent",
                    "with a null intent. It has been probably restarted by the system."
            )
        }

        return START_STICKY
    }


    private fun stopService() {
        try {
            timer?.state?.removeObserver(stateObserver)
            timer?.timeFragment?.removeObserver(timerObserver)
            timer?.cancel()
            timer = null

            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e("Error stopping service", e?.localizedMessage)
        } finally {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createNotificationWithVibration(hr: Int?, min: Int?, sec: Int?): Notification {
        val notificationChannelId = "SMART TIMER SERVICE CHANNEL - HIGH"
        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(notificationChannelId, "Smart Timer notifications channel - High", NotificationManager.IMPORTANCE_HIGH)
                    .let {
                        it.description = "Smart Timer channel"
                        it.enableLights(true)
                        it.lightColor = Color.RED
                        it.enableVibration(true)
                        it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                        it
                    }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, notificationChannelId)
        return builder
                .setContentTitle("Timer")
                .setContentText(String.format("%02d : %02d : %02d", hr, min, sec))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(uri)
                .setTicker("Timer")
                .setPriority(Notification.PRIORITY_HIGH)
                .build()
    }

    private fun createNotificationWithoutVibration(hr: Int?, min: Int?, sec: Int?): Notification {
        val notificationChannelId = "SMART TIMER SERVICE CHANNEL - LOW"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(notificationChannelId, "Smart Timer notifications channel - Low", NotificationManager.IMPORTANCE_LOW)
                    .let {
                        it.description = "Smart Timer channel"
                        it.enableLights(false)
                        it.enableVibration(false)
                        it
                    }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, notificationChannelId)
        return builder
                .setContentTitle("Timer")
                .setContentText(String.format("%02d : %02d : %02d", hr, min, sec))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Timer")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build()
    }

    fun getTimer() = timer?.time

    companion object {
        const val VIBRATE_NOTIFICATION_ID = 1200
        const val NOTIFICATION_ID = 1201
    }

    inner class CountDownBinder : Binder() {
        fun getService(): CountDownService = this@CountDownService
    }

}
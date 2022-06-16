package com.aa.pacer

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.aa.pacer.PacerUI.Companion.LOG_TAG

class PacerService : Service() {

    private var mInterval: Long = 0
    private var mMaxCount: Long = 0
    private var mLastTickTime: Long = 0
    private var mCount: Long = 0
    private var mState = Const.STATE_IDLE
    private var mCallback: IPacerCallback? = null
    private var mPlayVoice: Boolean = false
    private var mPlayRingtone: Boolean = false
    private var mPlayVibrate: Boolean = false
    private var mPauseOnCall: Boolean = false
    private var mRingtonePosition: Int = 0
    private var mRingtone: Ringtone? = null
    private var mRingtoneDuration: Int = 0
    private var mWakeLock: PowerManager.WakeLock? = null
    private val mSync = Any()
    private var mVibrator: Vibrator? = null
    private var mVibrationEffect: VibrationEffect? = null
    private var mTts: TextToSpeech? = null
    private  val mService = this
    private val mHandler = object : Handler(mService.mainLooper) {}

    private var mTelephonyManager: TelephonyManager? = null

    internal var mTickRun: Runnable = object : Runnable {

        override fun run() {
            if (mState == Const.STATE_PAUSED) return

            var stop = false

            mLastTickTime = SystemClock.elapsedRealtime()
            mCount++

            // We need to post next
            // one immediately,
            // before sounds so that
            // time is right
            if (mCount < mMaxCount && mState == Const.STATE_TICKING) {
                mHandler.postDelayed(this, mInterval)
            } else {
                stop = true
            }

            try {
                if (mCallback != null) {
                    mCallback!!.tick(mLastTickTime, mCount)
                }
            } catch (e1: RemoteException) {
                e1.printStackTrace()
            }

            if (mPlayVibrate) {
                if (mVibrator == null) {
                    mVibrator = (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                    mVibrationEffect = VibrationEffect.createOneShot(200, 10)
                }
                mVibrator!!.vibrate(mVibrationEffect)
            }
            if (mPlayRingtone) {
                synchronized(mSync) {
                    mRingtone!!.play()
                    try {
                        Thread.sleep(mRingtoneDuration.toLong())
                    } catch (e: InterruptedException) {
                    }

                    if (mRingtone!!.isPlaying) {
                        mRingtone!!.stop()
                    }
                }
            }
            if (mPlayVoice) {
                sayText(java.lang.Long.toString(mCount))
            }

            createChannel(this@PacerService)
            showNotification(this@PacerService)

            if (stop) {
                stopService()
            }
        }
    }

    internal var mTelephonListener: TelephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> resumeCountingAndNotifyUI()
                TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> pauseCountingAndNotifyUI()
            }
        }
    }

    private val mBinder = object : IPacerService.Stub() {

        @Synchronized
        @Throws(RemoteException::class)
        override fun getLastCount(): Long {
            Log.i(LOG_TAG, "Service: Binder getLastCount")

            return mCount
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun getLastTickTime(): Long {
            return mLastTickTime
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun setCallback(
                callback: IPacerCallback) {
            mCallback = callback
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun setRingtoneSound(
                ringtone: Int,
                duration: Int) {
            mRingtonePosition = ringtone
            synchronized(mSync) {
                val rm = RingtoneManager(this@PacerService)
                rm.setType(RingtoneManager.TYPE_ALL)
                rm.cursor
                mRingtone = rm.getRingtone(mRingtonePosition)
            }
            mRingtoneDuration = duration
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun start(
                interval: Long,
                maxCount: Long) {
            if (mState != Const.STATE_IDLE) {
                return
            }

            showNotification(this@PacerService)

            mTts = TextToSpeech(this@PacerService, OnInitListener {
                // mTTSPresent = true;
            })


            mState = Const.STATE_TICKING

            mInterval = interval
            mMaxCount = maxCount
            mLastTickTime = SystemClock.elapsedRealtime()
            mCount = 0
            mHandler.postDelayed(mTickRun, mInterval)

            if (mPauseOnCall) {
                mTelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (mTelephonyManager != null) mTelephonyManager!!.registerTelephonyCallback(
                    mService.mainExecutor,
                    mTelephonListener)
            }
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun stop(): Boolean {
            if (mTts != null) mTts!!.shutdown()

            if (mState == Const.STATE_TICKING) {
                removeNotification(this@PacerService)

                mState = Const.STATE_IDLE
                mHandler.removeCallbacks(mTickRun)
                return true
            } else {
                return false
            }
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun setRingtone(
                value: Boolean) {
            mPlayRingtone = value
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun setVibrate(
                value: Boolean) {
            mPlayVibrate = value
        }

        @Synchronized
        @Throws(RemoteException::class)
        override fun setVoice(
                value: Boolean) {
            mPlayVoice = value
        }

        @Throws(RemoteException::class)
        override fun pause(): Boolean {
            return pauseCountingAndNotifyUI()
        }

        @Throws(RemoteException::class)
        override fun resume(): Boolean {
            return resumeCountingAndNotifyUI()
        }

        @Throws(RemoteException::class)
        override fun getState(): Int {
            return mState
        }

        @Throws(RemoteException::class)
        override fun setPauseOnCall(
                value: Boolean) {
            mPauseOnCall = value
        }

    }

    private fun stopService() {
        Log.i(LOG_TAG, "Service: onStopService")

        mHandler.removeCallbacks(mTickRun)
        try {
            mState = Const.STATE_IDLE
            if (mCallback != null) {
                mCallback!!.finish()
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        removeNotification(this@PacerService)

        // Give time to TTS to finish saying last number
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        if (mTts != null) mTts!!.shutdown()
        stopSelf()
    }

    override fun onCreate() {
        Log.i(LOG_TAG, "Service: onCreate")
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pacer:myWakeLockTag")
        mWakeLock!!.acquire()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "Service: onStartCommand")
        when (intent.getIntExtra(NOTIFICATION_EXTRA_ID, -1)) {
            NOTIFICATION_PAUSE -> pauseCountingAndNotifyUI()
            NOTIFICATION_RESUME -> resumeCountingAndNotifyUI()
            NOTIFICATION_STOP -> stopService()
            else -> mIsRunning = true
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Service: Destroy")

        if (mWakeLock!!.isHeld) {
            mWakeLock!!.release()
        }

        if (mTelephonyManager != null) mTelephonyManager!!.unregisterTelephonyCallback(mTelephonListener)

        removeNotification(this@PacerService)

        super.onDestroy()
    }

    override fun onBind(arg0: Intent): IBinder? {
        Log.i(LOG_TAG, "Service: onBind")

        return mBinder
    }

    private fun pauseCounting(): Boolean {
        if (mState != Const.STATE_TICKING) return false

        mHandler.removeCallbacks(mTickRun)
        mState = Const.STATE_PAUSED
        showNotification(this)
        return true

    }

    private fun pauseCountingAndNotifyUI(): Boolean {
        if (mCallback != null)
            try {
                mCallback!!.pause()
            } catch (e: RemoteException) {
            }

        return pauseCounting()
    }

    private fun resumeCountingAndNotifyUI(): Boolean {
        if (mCallback != null)
            try {
                mCallback!!.resume()
            } catch (e: RemoteException) {
            }

        return resumeCounting()
    }

    private fun resumeCounting(): Boolean {
        if (mState != Const.STATE_PAUSED) return false

        mState = Const.STATE_TICKING
        mHandler.postDelayed(mTickRun, mInterval)
        showNotification(this)
        return true
    }


    private fun sayText(text: String) {
        if (mTts != null) mTts!!.speak(text, TextToSpeech.QUEUE_ADD, null)
    }


    val CHANNEL_ID = "LiveEvents"

    fun createChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val name = "Live event" // getString(R.string.channel_name)
        val description = "Counter control"
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
        channel.description = description
        channel.setShowBadge(false)

        notificationManager.createNotificationChannel(channel)
    }


    internal fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = Notification.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.drawable.notification_icon)
        builder.setContentTitle("Pacer is " + (if (mState == Const.STATE_PAUSED) "paused at   " else "counting:   ") + mCount)
        builder.setContentText("Pacer is running")
        builder.setContentText("Touch to bring up.")
        val intent = Intent(context, PacerUI::class.java)
        val i = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(i)
        builder.setOngoing(true)


        val intentStop = Intent(context, PacerService::class.java)
        intentStop.putExtra(NOTIFICATION_EXTRA_ID, NOTIFICATION_STOP)
        intentStop.action = "stop"
        val iStop = PendingIntent.getService(context, 1, intentStop, PendingIntent.FLAG_IMMUTABLE)
        val actionStop = Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_media_stop),
                "End", iStop)
                .build()
        builder.addAction(actionStop)

        if (mState == Const.STATE_TICKING || mState == Const.STATE_IDLE) {
            val intentPause = Intent(context, PacerService::class.java)
            intentPause.putExtra(NOTIFICATION_EXTRA_ID, NOTIFICATION_PAUSE)
            intentPause.action = "pause"
            val iPause = PendingIntent.getService(context, 1, intentPause, PendingIntent.FLAG_IMMUTABLE)
            val actionPause = Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_media_pause),
                    "Pause", iPause)
                    .build()
            builder.addAction(actionPause)
        } else if (mState == Const.STATE_PAUSED) {
            val intentResume = Intent(context, PacerService::class.java)
            intentResume.putExtra(NOTIFICATION_EXTRA_ID, NOTIFICATION_RESUME)
            intentResume.action = "resume"
            val iResume = PendingIntent.getService(context, 1, intentResume, PendingIntent.FLAG_IMMUTABLE)
            val actionResume = Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_media_play),
                    "Resume", iResume)
                    .build()
            builder.addAction(actionResume)
        }
        nm.notify(1, builder.build())
    }

    companion object {
        val NOTIFICATION_EXTRA_ID = "com.aa.pacer.action"
        val NOTIFICATION_PAUSE = 991
        val NOTIFICATION_RESUME = 992
        val NOTIFICATION_STOP = 993

        var mIsRunning: Boolean = false

        internal fun removeNotification(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancelAll()
        }
    }
}

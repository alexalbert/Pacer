package com.aa.pacer

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.aa.pacer.databinding.MainBinding


@SuppressLint("ClickableViewAccessibility")
class PacerUI : AppCompatActivity()  {

    private var mResuming: Boolean = false

    private var hideKeyboardTS: Long = 0

    lateinit var mChronometer: Chronometer
    lateinit var mCountText: TextView
    lateinit var mBtnStart: Button
    lateinit var mBtnResume: Button

    lateinit var mServiceIntent: Intent

    private var mInterval: Long = 0
    private var mRingthonePosition: Int = 0
    private var mRingthoneDuration = 1000
    private var mTimes: Int = 0
    private var mKeepAwake: Boolean = false

    private var mPlayVoice = true
    private var mPlayRingtone: Boolean = false
    private var mPlayVibrate: Boolean = false
    private var mPauseOnCall: Boolean = false

    lateinit var mBottom: View
    lateinit var mCounters: View
    lateinit var mMain: View

    lateinit var mMinutes: EditText
    lateinit var mSeconds: EditText
    lateinit var mRepeat: EditText
    lateinit var mCurrentEdit: EditText
    lateinit var mAdd: Button
    lateinit var mSpinner: Spinner
    lateinit var mSpinnerAdapter: MySpinnerAdapter

    val mSchemas = Schemas()

    private var mSchemaIndex: Int = 0

    private var mKeyboardFragment = KeyboardFragment()

    private var mService: IPacerService? = null

    lateinit var toast: Toast

    @SuppressLint("HandlerLeak")
    private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: android.os.Message) {
            when (msg.what) {
                H_ACTION_SERVICE_CALLBACK_TICK -> if (mService != null) {
                    try {
                        if (mService!!.state != Const.STATE_IDLE) {
                            mCountText.text = java.lang.Long.toString(mService!!.lastCount)
                            mChronometer.base = mService!!.lastTickTime
                            mChronometer.start()
                            mBtnStart.text = "Stop"
                        } else {
                            mChronometer.stop()
                            mBtnStart.text = "Start"
                        }

                        mBtnResume.visibility = if (mService!!.state != Const.STATE_IDLE) View.VISIBLE else View.GONE

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                H_ACTION_SERVICE_CALLBACK_PAUSE -> {
                    mChronometer.stop()
                    resetControlsOnPause()
                }

                H_ACTION_SERVICE_CALLBACK_RESUME -> {
                    mChronometer.start()
                    resetControlsOnStartOrResume()
                }

                H_ACTION_SERVICE_CALLBACK_FINISH -> {
                    resetControlsOnStop()
                    releaseScreenLock()
                }

                H_ACTION_MENU_SETTINGS -> startActivity(Intent(this@PacerUI, SettingsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))

                H_ON_SERVICE_CONNECTED -> handleServiceConnect()
            }
        }
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(LOG_TAG, "onServiceConnected")

            try {
                mService = IPacerService.Stub.asInterface(service)
                Log.i(LOG_TAG, "onServiceConnected1")
                mService!!.setCallback(mBinder)
                Log.i(LOG_TAG, "onServiceConnected2")


                mHandler.sendEmptyMessage(H_ON_SERVICE_CONNECTED)
                Log.i(LOG_TAG, "onServiceConnected3")
            } catch (e: RemoteException) {
                Log.i(LOG_TAG, "onServiceConnected ex")
                e.printStackTrace()
            }

        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(LOG_TAG, "onServiceDisconnected")
            mService = null
        }
    }

    private val mBinder = object : IPacerCallback.Stub() {
        @Throws(RemoteException::class)
        override fun tick(lastTickTime: Long, lastCount: Long) {
            mHandler.sendEmptyMessage(H_ACTION_SERVICE_CALLBACK_TICK)
        }

        @Throws(RemoteException::class)
        override fun finish() {
            mHandler.sendEmptyMessage(H_ACTION_SERVICE_CALLBACK_FINISH)
        }

        @Throws(RemoteException::class)
        override fun pause() {
            mHandler.sendEmptyMessage(H_ACTION_SERVICE_CALLBACK_PAUSE)
        }

        @Throws(RemoteException::class)
        override fun resume() {
            mHandler.sendEmptyMessage(H_ACTION_SERVICE_CALLBACK_RESUME)
        }
    }

    private val isPortrait: Boolean
        get() {
            val rotation = display?.rotation
            return rotation == null || rotation == 0 || rotation == 180
        }

    private fun handleServiceConnect() {
        val state: Int
        try {
            state = mService!!.state
            Log.i(LOG_TAG, "In handleServiceConnect mService " + mService)
            Log.i(LOG_TAG, "In handleServiceConnect mService state " + mService?.state)
            Log.i(LOG_TAG, "In handleServiceConnect resuming " + mResuming)

            if (mResuming) {
                mResuming = false
                when (state) {
                    Const.STATE_TICKING -> {
                        acquireScreenLock()
                        resetControlsOnStartOrResume()
                        mHandler.sendEmptyMessage(H_ACTION_SERVICE_CALLBACK_TICK)
                    }
                    Const.STATE_IDLE -> {
                        // While client has been away, service finished and died. Or it was never started.
                        resetControlsOnStop()
                        unbindService(mConnection)
                        stopService(mServiceIntent)
                    }
                    Const.STATE_PAUSED -> {
                        acquireScreenLock()
                        mCountText.text = mService!!.lastCount.toString()
                        resetControlsOnPause()
                    }
                }
            } else {
                when (state) {
                    Const.STATE_TICKING -> {
                        acquireScreenLock()
                        mHandler.sendEmptyMessage(H_ACTION_SERVICE_CALLBACK_TICK)
                    }
                    Const.STATE_IDLE -> {
                        setService()
                        mService!!.start(mInterval, mTimes.toLong())
                    }
                    Const.STATE_PAUSED -> {
                        acquireScreenLock()
                        resetControlsOnPause()
                    }
                }
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    private fun setService() {
        Log.i(LOG_TAG, "setService")

        updateFromSettings()

        checkTelephonyPermission()

         if (mService != null) {
            try {
                mService!!.setRingtoneSound(mRingthonePosition, mRingthoneDuration)
                mService!!.setVoice(mPlayVoice)
                mService!!.setRingtone(mPlayRingtone)
                mService!!.setVibrate(mPlayVibrate)
                mService!!.setPauseOnCall(mPauseOnCall)

            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    private fun checkTelephonyPermission() {
        if (mPauseOnCall) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_PHONE_STATE
                )
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
               mPauseOnCall = false
            }
        }

    private lateinit var binding: MainBinding

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "onCreate")
        super.onCreate(savedInstanceState)

        binding = MainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val toolbar = binding.toolbar
        toolbar.background = ColorDrawable(Color.BLACK)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.spinner.setAdapter(MySpinnerAdapter(this, R.layout.spinner_row, R.id.spnItemName, mSchemas.get(), mSchemas.get()))


        mMinutes = binding.minutes
        mSeconds = binding.seconds
        mRepeat = binding.repeat
        mBottom = binding.bottom
        mCounters = binding.counters
        mMain = binding.main
        mAdd = binding.add
        mSpinner = binding.spinner
        mSpinnerAdapter = mSpinner.adapter as MySpinnerAdapter

        mMinutes.inputType = 0
        mSeconds.inputType = 0
        mRepeat.inputType = 0

        val prefs = getPreferences(Context.MODE_PRIVATE)
        if (prefs != null) {
            mInterval = prefs.getLong(INTERVAL, 0)
            mTimes = prefs.getInt(TIMES, 1000)
            mSchemas.set(prefs.getStringSet(SCHEMAS, null))
            mSchemaIndex = prefs.getInt(SCHEMA_INDEX, 0)
            mSpinnerAdapter.refresh()
            mSpinner.setSelection(mSchemaIndex)
        }

        if (mInterval != 0L) {
            val min = mInterval / 1000 / 60
            val sec = mInterval / 1000 % 60
            mMinutes.setText(java.lang.Long.toString(min))
            mSeconds.setText(java.lang.Long.toString(sec))
        }

        mRepeat.setText(Integer.toString(mTimes))

        mMain.setOnTouchListener { v, event ->
            if (mKeyboardFragment.view != null) {
                val fragmentTop = (mKeyboardFragment.requireView().parent as View).top

                if (v != mMinutes && v != mSeconds && v != mRepeat && event.y < fragmentTop) {
                    removeKeyboard()
                }
            }
            false
        }

        mAdd.setOnClickListener { v ->
            if (validateInput()) {
                mSchemas.add(mMinutes.text.toString(), mSeconds.text.toString(), mRepeat.text.toString())
                mSpinnerAdapter.refresh()
            }
        }


        mSpinner.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val set = mSchemas.get()
                if (set != null && set.size > position && position >= 0) {
                    val s = mSchemas.get()[position]
                    var (minutes, seconds, repeat) = mSchemas.parse(s)
                    mMinutes.setText(minutes)
                    mSeconds.setText(seconds)
                    mRepeat.setText(repeat)
                }
            }
        }

        val mEditTouchListener = OnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) return@OnTouchListener false

            if (isPortrait) {
                mCurrentEdit = v as EditText

                val ft =  supportFragmentManager.beginTransaction()
                val f = supportFragmentManager.findFragmentById(R.id.keyboardFragment)
                if (f == null || !f.isAdded && !f.isVisible) {
                    ft.add(R.id.keyboardFragment, mKeyboardFragment)
                    ft.commit()
                }

                mBottom.visibility = View.GONE
            }
            false
        }

        mMinutes.setOnTouchListener(mEditTouchListener)
        mSeconds.setOnTouchListener(mEditTouchListener)
        mRepeat.setOnTouchListener(mEditTouchListener)

        mServiceIntent = Intent(this, PacerService::class.java)

        mBtnStart = binding.btnStart
        mBtnResume = binding.btnResume
        mCountText = binding.count

        mBtnResume.visibility = View.GONE

        mChronometer =binding.chronometer
        mChronometer.setTextColor(Color.BLUE)
        mChronometer.text = ""
        mCountText.setTextColor(Color.RED)
        //        mBtnStart.setBackgroundColor(Color.GRAY);

        mBottom.visibility = View.VISIBLE
        mBtnStart.requestFocus()


        if (isPortrait) {
            mCounters.setPadding(0, 30, 0, 30)
            mMinutes.isCursorVisible = false
            mSeconds.isCursorVisible = false
            mRepeat.isCursorVisible = false
        } else {
            mCounters.setPadding(0, 5, 0, 5)
            mMinutes.isCursorVisible = true
            mSeconds.isCursorVisible = true
            mRepeat.isCursorVisible = true
        }

        mBtnStart.setOnClickListener{
                if (System.currentTimeMillis() - hideKeyboardTS < 500) {
                    return@setOnClickListener
                }
                if (mBtnStart.text == resources.getString(R.string.start)) {
                    updateFromSettings()

                    if (!validateInput()) {
                        return@setOnClickListener
                    }

                    resetControlsOnStartOrResume()

                    startService(mServiceIntent)

                    mResuming = false

                    bindService(mServiceIntent, mConnection, 0)
                    acquireScreenLock()
                } else {
                    try {
                        if (mService != null) {
                            mService!!.stop()
                        }
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }

                    unbindService(mConnection)

                    mService = null
                    stopService(mServiceIntent)

                    resetControlsOnStop()
                    releaseScreenLock()
                }
        }

        mBtnResume.setOnClickListener{
                if (System.currentTimeMillis() - hideKeyboardTS < 500) {
                    return@setOnClickListener
                }

                try {
                    val state = mService!!.state
                    if (state == Const.STATE_TICKING) {
                        if (mService != null) {
                            mChronometer.text = ""
                            mService!!.pause()
                        }
                    } else if (state == Const.STATE_PAUSED) {
                        if (mService != null) {
                            mService!!.resume()
                        }
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
        }
    }


    private fun validateInput(): Boolean {
        val min = mMinutes.text.toString()
        val sec = mSeconds.text.toString()
        val repeat = mRepeat.text.toString()

        mInterval = 0
        if (min.trim { it <= ' ' }.length > 0) {
            mInterval += (Integer.parseInt(min) * 60).toLong()
        }
        if (sec.trim { it <= ' ' }.length > 0) {
            mInterval += Integer.parseInt(sec).toLong()
        }
        mInterval *= 1000

        if (mInterval < 3000) {
            toast = Toast.makeText(this@PacerUI, "Please set at least 3 seconds interval", Toast.LENGTH_LONG)
            toast.show()
            return false
        }
        mTimes = Integer.parseInt(repeat)

        return true
    }
    private fun resetControlsOnStop() {
        mBtnStart.setText(R.string.start)
        mBtnResume.visibility = View.GONE

        mChronometer.stop()
        mCountText.text = ""
        mChronometer.text = ""
    }

    private fun resetControlsOnStartOrResume() {
        mBtnStart.setText(R.string.stop)
        mBtnResume.setText(R.string.pause)
        mBtnResume.visibility = View.VISIBLE

        mChronometer.base = SystemClock.elapsedRealtime()
        mChronometer.start()
    }

    private fun resetControlsOnPause() {
        mBtnStart.setText(R.string.stop)
        mBtnResume.setText(R.string.resume)
        mBtnResume.visibility = View.VISIBLE

        mChronometer.stop()
    }

    private fun resetControlsOnAdd() {
        mBtnStart.setText("Save")
        mBtnResume.setText("Cancel")
        mBtnResume.visibility = View.VISIBLE

        mChronometer.stop()
    }

    private fun clearData() {
        mMinutes.text.clear()
        mSeconds.text.clear()
        mRepeat.text.clear()
    }

    private fun acquireScreenLock() {
        if (mKeepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun releaseScreenLock(): Boolean {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        return false
    }

    public override fun onResume() {
        super.onResume()
        Log.i(LOG_TAG, "onResume")

        updateFromSettings()

        mResuming = true
        mServiceIntent = Intent(this, PacerService::class.java)

        startService(mServiceIntent)
        bindService(mServiceIntent, mConnection, 0)
    }

    public override fun onRestart() {
        Log.i(LOG_TAG, "onRestart")
        super.onRestart()
    }


    public override fun onPause() {
        Log.i(LOG_TAG, "onPause")

        super.onPause()
        if (mService != null) {
            try {
                mService!!.setCallback(null)
                unbindService(mConnection)
                Log.i(LOG_TAG, "unbindConnection")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mService = null
        }
    }

    override fun onStop() {
        Log.i(LOG_TAG, "onStop")

        try {
            unbindService(mConnection);
        } catch(e: Exception) {}

        savePrefs()

        super.onStop()
    }

    private fun savePrefs() {
        val prefs = getPreferences(Context.MODE_PRIVATE)
        if (prefs != null) {
            val editor = prefs.edit()
            editor.putLong(INTERVAL, mInterval)
            editor.putInt(TIMES, mTimes)
            editor.putStringSet(SCHEMAS, mSchemas.getAsMutableSet())
            editor.putInt(SCHEMA_INDEX, mSpinner.selectedItemPosition)
            editor.commit()
        }
    }

    public override fun onDestroy() {
        Log.i(LOG_TAG, "onDestroy")

        super.onDestroy()
    }


    // Called only the first time the options menu is displayed.
    // Create the menu entries.
    // Menu adds items in the order shown.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val settings = menu.add(Menu.NONE, H_ACTION_MENU_SETTINGS, Menu.NONE, "Settings")
        settings.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        settings.setIcon(android.R.drawable.ic_menu_preferences)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mHandler.sendEmptyMessage(item.itemId)
        return false
    }

    private fun updateFromSettings() {
        // Get  preferences from the Settings list
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        if (settings != null) {
            mPlayVoice = settings.getBoolean(VOICE, true)
            mPlayRingtone = settings.getBoolean(RINGTONE, false)
            mPlayVibrate = settings.getBoolean(VIBRATE, false)
            mKeepAwake = settings.getBoolean(SCREEN, false)
            mPauseOnCall = settings.getBoolean(CALL, false)
        }
    }

    fun removeKeyboard() {
        val ft = supportFragmentManager.beginTransaction()
        ft.remove(mKeyboardFragment)
        ft.commit()

        mBottom.visibility = View.VISIBLE
        mBtnStart.requestFocus()
        hideKeyboardTS = System.currentTimeMillis()
    }

    companion object {
        val LOG_TAG = "Pacer1"

        val H_ACTION_SERVICE_CALLBACK_TICK = 11
        val H_ACTION_SERVICE_CALLBACK_FINISH = 12
        val H_ACTION_SERVICE_CALLBACK_PAUSE = 13
        val H_ACTION_SERVICE_CALLBACK_RESUME = 14

        val H_ON_SERVICE_CONNECTED = 21

        val H_ACTION_MENU_SETTINGS = 211

        // Saved state ids
        const val INTERVAL = "interval"
        const val TIMES = "times"
        const val SCHEMAS = "sets"
        const val SCHEMA_INDEX = "schema_index"

        // Prefernces ids
        const val RINGTONE = "ringtone"
        const val SCREEN = "screen"
        const val CALL = "call"
        const val VOICE = "voice"
        const val VIBRATE = "vibrate"
    }
}
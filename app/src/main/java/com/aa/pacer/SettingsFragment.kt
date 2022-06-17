package com.aa.pacer

import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle
import com.aa.pacer.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferencesFromResource(R.xml.prefs, null)

//        this.preferenceManager.sharedPreferences

//        registerForActivityResult()

//        https://issuetracker.google.com/issues/37057453#comment2
//        https://stackoverflow.com/questions/62671106/onactivityresult-method-is-deprecated-what-is-the-alternative
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}
}
package com.aa.pacer

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    private val RINGTONE_NAME = "ringtoneName"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferencesFromResource(R.xml.prefs, null)
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}
}
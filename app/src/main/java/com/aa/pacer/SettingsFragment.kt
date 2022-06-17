package com.aa.pacer

import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle
import com.aa.pacer.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferencesFromResource(R.xml.prefs, null)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}
}
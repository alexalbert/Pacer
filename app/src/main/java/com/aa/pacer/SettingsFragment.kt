package com.aa.pacer

import android.R.attr
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class SettingsFragment : PreferenceFragmentCompat() {

    private val RINGTONE_NAME = "ringtoneName"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPreferencesFromResource(R.xml.prefs, null)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return if (preference.key.equals(RINGTONE_NAME)) {

            val currentRingtonePath = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString(RINGTONE_NAME, Settings.System.DEFAULT_NOTIFICATION_URI.encodedPath)

            Log.e("aaaa_bbb", currentRingtonePath.toString())


            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentRingtonePath))
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
//            intent.putExtra(
//                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
//                Settings.System.DEFAULT_NOTIFICATION_URI
//            )
            val existingValue: String? = null // getRingtonePreferenceValue() TODO
            if (existingValue != null) {
                if (existingValue.isEmpty()) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        Uri.parse(existingValue)
                    )
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI
                )
            }
            activityResultLauncher.launch(intent)
            true
        } else {
            super.onPreferenceTreeClick(preference)
        }
    }

    var activityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.getResultCode() === Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data

            val ringtoneUri: Uri? =
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

            if (ringtoneUri != null) {
                val editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                editor.putString(RINGTONE_NAME, ringtoneUri.encodedPath)
                Log.e("aaaa", ringtoneUri.encodedPath.toString())
                editor.commit()

                RingtoneManager.setActualDefaultRingtoneUri(
                    requireContext(),
                    RingtoneManager.TYPE_NOTIFICATION,
                    ringtoneUri
                );
            }
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}
}
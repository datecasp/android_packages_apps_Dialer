/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.app.settings;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import com.android.dialer.app.R;
import com.android.dialer.callrecord.impl.CallRecorderService;
import com.android.dialer.util.SettingsUtil;

public class SoundSettingsFragment extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener {

  private static final int NO_VIBRATION_FOR_CALLS = 0;
  private static final int DO_VIBRATION_FOR_CALLS = 1;
  
  private SwitchPreference enableDndInCall;

  private NotificationManager notificationManager;

  @Override
  public Context getContext() {
    return getActivity();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.sound_settings);

    Context context = getActivity();
    
    final Preference systemSoundsAndVibrationSettings = (Preference) findPreference("system_sounds_and_vibration_settings");
    systemSoundsAndVibrationSettings.setTitle(String.format(
         context.getString(R.string.system_sounds_and_vibration_title), 
         context.getString(R.string.sounds_and_vibration_title).toLowerCase()));
	  systemSoundsAndVibrationSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference preference) {
			Intent soundSettingIntent = new Intent(Settings.ACTION_SOUND_SETTINGS);
			                    soundSettingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			                    soundSettingIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			                    soundSettingIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
	        startActivity(soundSettingIntent);

	        return true;
	     }
	  });
    enableDndInCall = (SwitchPreference) findPreference("incall_enable_dnd");

    if (!hasVibrator()){
      PreferenceScreen ps = getPreferenceScreen();
      Preference inCallVibrateOutgoing = findPreference(
          context.getString(R.string.incall_vibrate_outgoing_key));
      Preference inCallVibrateCallWaiting = findPreference(
          context.getString(R.string.incall_vibrate_call_waiting_key));
      Preference inCallVibrateHangup = findPreference(
          context.getString(R.string.incall_vibrate_hangup_key));
      Preference inCallVibrate45Secs = findPreference(
          context.getString(R.string.incall_vibrate_45_key));
      ps.removePreference(inCallVibrateOutgoing);
      ps.removePreference(inCallVibrateCallWaiting);
      ps.removePreference(inCallVibrateHangup);
      ps.removePreference(inCallVibrate45Secs);
    }


    enableDndInCall.setOnPreferenceChangeListener(this);

    TelephonyManager telephonyManager =
        (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

    if (!CallRecorderService.isEnabled(getActivity())) {
      getPreferenceScreen().removePreference(
          findPreference(context.getString(R.string.call_recording_category_key)));
    }
    notificationManager = context.getSystemService(NotificationManager.class);
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!Settings.System.canWrite(getContext())) {
      // If the user launches this setting fragment, then toggles the WRITE_SYSTEM_SETTINGS
      // AppOp, then close the fragment since there is nothing useful to do.
      getActivity().onBackPressed();
      return;
    }
  }

  /**
   * Supports onPreferenceChangeListener to look for preference changes.
   *
   * @param preference The preference to be changed
   * @param objValue The value of the selection, NOT its localized display value.
   */
  @Override
  public boolean onPreferenceChange(Preference preference, Object objValue) {
    if (!Settings.System.canWrite(getContext())) {
      // A user shouldn't be able to get here, but this protects against monkey crashes.
      Toast.makeText(
              getContext(),
              getResources().getString(R.string.toast_cannot_write_system_settings),
              Toast.LENGTH_SHORT)
          .show();
      return true;
    }
    if (preference == enableDndInCall) {
      boolean newValue = (Boolean) objValue;
      if (newValue && !notificationManager.isNotificationPolicyAccessGranted()) {
        new AlertDialog.Builder(getContext())
            .setMessage(R.string.incall_dnd_dialog_message)
            .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
              }
            })
            .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            })
            .show();

        // At this time, it is unknown whether the user granted the permission
        return false;
      }
    }
    return true;
  }

  /** Click listener for toggle events. */
  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    if (!Settings.System.canWrite(getContext())) {
      Toast.makeText(
              getContext(),
              getResources().getString(R.string.toast_cannot_write_system_settings),
              Toast.LENGTH_SHORT)
          .show();
      return true;
    }
    return true;
  }

  /** Whether the device hardware has a vibrator. */
  private boolean hasVibrator() {
    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
    return vibrator != null && vibrator.hasVibrator();
  }

  private boolean shouldHideCarrierSettings() {
    CarrierConfigManager configManager =
        (CarrierConfigManager) getActivity().getSystemService(Context.CARRIER_CONFIG_SERVICE);
    return configManager
        .getConfig()
        .getBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL);
  }
}

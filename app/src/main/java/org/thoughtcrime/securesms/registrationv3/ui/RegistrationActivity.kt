/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registrationv3.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.ActivityNavigator
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.lock.v2.CreateSvrPinActivity
import org.thoughtcrime.securesms.pin.PinRestoreActivity
import org.thoughtcrime.securesms.profiles.AvatarHelper
import org.thoughtcrime.securesms.profiles.edit.CreateProfileActivity
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.registration.sms.SmsRetrieverReceiver
import org.thoughtcrime.securesms.registrationv3.ui.restore.RemoteRestoreActivity
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme
import org.thoughtcrime.securesms.util.RemoteConfig

/**
 * Activity to hold the entire registration process.
 */
class RegistrationActivity : BaseActivity() {

  private val TAG = Log.tag(RegistrationActivity::class.java)

  private val dynamicTheme = DynamicNoActionBarTheme()
  val sharedViewModel: RegistrationViewModel by viewModels()

  private var smsRetrieverReceiver: SmsRetrieverReceiver? = null

  init {
    lifecycle.addObserver(SmsRetrieverObserver())
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    dynamicTheme.onCreate(this)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_registration_navigation_v3)

    sharedViewModel.isReregister = intent.getBooleanExtra(RE_REGISTRATION_EXTRA, false)

    sharedViewModel.checkpoint.observe(this) {
      if (it >= RegistrationCheckpoint.LOCAL_REGISTRATION_COMPLETE) {
        handleSuccessfulVerify()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }

  private fun handleSuccessfulVerify() {
    if (SignalStore.account.hasLinkedDevices) {
      SignalStore.misc.shouldShowLinkedDevicesReminder = sharedViewModel.isReregister
    }

    if (SignalStore.storageService.needsAccountRestore) {
      Log.i(TAG, "Performing pin restore.")
      startActivity(Intent(this, PinRestoreActivity::class.java))
      finish()
    } else {
      val isProfileNameEmpty = Recipient.self().profileName.isEmpty
      val isAvatarEmpty = !AvatarHelper.hasAvatar(this, Recipient.self().id)
      val needsProfile = isProfileNameEmpty || isAvatarEmpty
      val needsPin = !SignalStore.svr.hasOptedInWithAccess()

      Log.i(TAG, "Pin restore flow not required. Profile name empty: $isProfileNameEmpty | Profile avatar empty: $isAvatarEmpty | Needs PIN: $needsPin")

      if (!needsProfile && !needsPin) {
        sharedViewModel.completeRegistration()
      }

      val startIntent = MainActivity.clearTop(this)

      val nextIntent: Intent? = when {
        needsPin -> CreateSvrPinActivity.getIntentForPinCreate(this@RegistrationActivity)
        !SignalStore.registration.hasSkippedTransferOrRestore() && RemoteConfig.messageBackups -> RemoteRestoreActivity.getIntent(this@RegistrationActivity)
        needsProfile -> CreateProfileActivity.getIntentForUserProfile(this@RegistrationActivity)
        else -> null
      }

      if (nextIntent != null) {
        startIntent.putExtra("next_intent", nextIntent)
      }

      Log.d(TAG, "Launching ${startIntent.component} with next_intent: ${nextIntent?.component}")
      startActivity(startIntent)
      finish()
      ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
  }

  private inner class SmsRetrieverObserver : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
      smsRetrieverReceiver = SmsRetrieverReceiver(application)
      smsRetrieverReceiver?.registerReceiver()
    }

    override fun onDestroy(owner: LifecycleOwner) {
      smsRetrieverReceiver?.unregisterReceiver()
      smsRetrieverReceiver = null
    }
  }

  companion object {
    const val RE_REGISTRATION_EXTRA: String = "re_registration"

    @JvmStatic
    fun newIntentForNewRegistration(context: Context, originalIntent: Intent): Intent {
      return Intent(context, RegistrationActivity::class.java).apply {
        putExtra(RE_REGISTRATION_EXTRA, false)
        setData(originalIntent.data)
      }
    }

    @JvmStatic
    fun newIntentForReRegistration(context: Context): Intent {
      return Intent(context, RegistrationActivity::class.java).apply {
        putExtra(RE_REGISTRATION_EXTRA, true)
      }
    }
  }
}

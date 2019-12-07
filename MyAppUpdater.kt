package com.cop.mus.reactiontimer.utils

import android.app.Activity
import android.util.Log
import com.github.javiersantos.appupdater.AppUpdater
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MyAppUpdater(private val context: Activity) {
    private lateinit var appUpdateManager: AppUpdateManager
    val updateRequestCode = 95


    private fun checkUpdatesLowerApi() {
        val appUpdater = AppUpdater(context)
                .setTitleOnUpdateAvailable("Update available")
                .setContentOnUpdateAvailable("Check out the latest version available of my app!")
                .setTitleOnUpdateNotAvailable("Update not available")
                .setContentOnUpdateNotAvailable("No update available. Check for updates again later!")
                .setButtonUpdate("Update now?")
                .setCancelable(false)
        appUpdater.start()
    }

    fun checkUpdates() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            checkUpdatesApi21()
        } else {
            checkUpdatesLowerApi()
        }
    }

    private fun checkUpdatesApi21() {
        appUpdateManager = AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                requestUpdate(appUpdateInfo)
            }
        }
        appUpdateInfoTask.addOnFailureListener {
            Log.d("appUpdate", it.message!!)
        }
    }

    private fun requestUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,
                context,
                updateRequestCode)
    }

    fun onResumeAppUpdate() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            appUpdateManager
                    .appUpdateInfo
                    .addOnSuccessListener { appUpdateInfo ->
                        if (appUpdateInfo.updateAvailability()
                                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                        ) {
                            requestUpdate(appUpdateInfo)
                        }
                    }
        }
    }
}
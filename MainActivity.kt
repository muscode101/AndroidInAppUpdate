package com.cop.mus.reactiontimer.ui.views.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.view.MenuItemCompat
import com.cop.mus.reactiontimer.R
import com.cop.mus.reactiontimer.model.ServiceData
import com.cop.mus.reactiontimer.repositories.AdmobRepository
import com.cop.mus.reactiontimer.services.PlayActionsService
import com.cop.mus.reactiontimer.services.PlayFinishService
import com.cop.mus.reactiontimer.services.PlayStartService
import com.cop.mus.reactiontimer.services.RecordingService
import com.cop.mus.reactiontimer.services.playItem.PlayItemService
import com.cop.mus.reactiontimer.ui.views.about.AboutActivity
import com.cop.mus.reactiontimer.ui.views.custom.FeedbackDialog
import com.cop.mus.reactiontimer.ui.views.settings.SettingsActivity
import com.cop.mus.reactiontimer.utils.MyAppUpdater
import com.cop.mus.reactiontimer.utils.MyIntentHandler
import com.cop.mus.reactiontimer.utils.MyServiceState
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.kobakei.ratethisapp.RateThisApp
import dagger.android.support.DaggerAppCompatActivity
import io.fabric.sdk.android.Fabric
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import java.io.File
import javax.inject.Inject


class MainActivity : DaggerAppCompatActivity() {

    private var backPressed: Long = 0
    private val compositeDisposable = CompositeDisposable()
    private var shareActionProvider: ShareActionProvider? = null
    private lateinit var myAppUpdater: MyAppUpdater
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    @Inject
    lateinit var rootFolder: File
    @Inject
    lateinit var viewModel: MainViewModel
    @Inject
    lateinit var feedbackDialog: FeedbackDialog
    @Inject
    lateinit var myIntentHandler: MyIntentHandler
    @Inject
    lateinit var admobRepository: AdmobRepository
    @Inject
    lateinit var serviceListener: io.reactivex.Observable<ServiceData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarMain)
        initTabs()
        initRxListener()
        initBannerAds()
        initFabric()
        initAppRater()
        initMyAppUpdater()
    }

    private fun initMyAppUpdater() {
        myAppUpdater = MyAppUpdater(this)
        myAppUpdater.checkUpdates()
    }

    private fun initAppRater() {
        RateThisApp.onCreate(this)
        RateThisApp.showRateDialogIfNeeded(this)
    }

    private fun initFabric() {
        Fabric.with(this, Answers())
        Fabric.with(this, Crashlytics())
    }

    private fun initBannerAds() {
        admobRepository.setAdView(ad_view_main)
        admobRepository.loadBannerAds()
    }

    private fun initTabs() {
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.adapter = sectionsPagerAdapter
        container.offscreenPageLimit = 3
        sliding_tabs.setupWithViewPager(container)
    }

    private fun initRxListener() {
        compositeDisposable.add(
                serviceListener
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            startRequiredService(it)
                        }
        )
    }

    private fun startRequiredService(serviceData: ServiceData) {
        when (serviceData.serviceState) {
            MyServiceState.PLAY_PREPARE_ACTION -> startService(Intent(this, PlayStartService::class.java))
            MyServiceState.RESUME_RUN_ACTION -> startPlayActionService(serviceData)
            MyServiceState.PLAY_RUN_ACTION -> startService(Intent(this, PlayActionsService::class.java))
            MyServiceState.PLAY_FINISHED_ACTION -> startService(Intent(this, PlayFinishService::class.java))
            else -> {
                stopAllServices()
            }
        }
    }

    private fun startPlayActionService(serviceData: ServiceData) {
        val i = Intent(this, PlayActionsService::class.java)
        i.putExtra("IntValue", serviceData.ActionCount)
        startService(i)
    }

    override fun onBackPressed() {
        if (backPressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finish()
            overridePendingTransition(android.R.anim.fade_in,
                    android.R.anim.fade_out)
        } else
            toast("Press once again to exit!").duration = Toast.LENGTH_SHORT
        backPressed = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllServices()
        rxDispose()
        admobRepository.onDestroyAds()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateListChanges()
        admobRepository.onResumeAds()
        myAppUpdater.onResumeAppUpdate()
    }

    override fun onStart() {
        super.onStart()
        admobRepository.onStartAds()
    }

    override fun onPause() {
        super.onPause()
        admobRepository.onPauseAds()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                finish()
                startActivity(intentFor<SettingsActivity>())
                overridePendingTransition(android.R.anim.fade_in,
                        android.R.anim.fade_out)
            }
            R.id.action_about -> {
                finish()
                startActivity(intentFor<AboutActivity>())
                overridePendingTransition(android.R.anim.fade_in,
                        android.R.anim.fade_out)
            }
            R.id.action_rate_Us -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.playstore_address))))
            }
            R.id.action_feedback -> {
                feedbackDialog.show(supportFragmentManager, getString(R.string.tag_dialog_feedback))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun stopAllServices() {
        stopService(Intent(this, PlayItemService::class.java))
        stopService(Intent(this, RecordingService::class.java))
        stopService(Intent(this, PlayActionsService::class.java))
        stopService(Intent(this, PlayStartService::class.java))
        stopService(Intent(this, PlayFinishService::class.java))
    }

    private fun rxDispose() {
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_share).also { menuItem ->
            initShareActionProvider(menuItem)
        }
        return true
    }

    private fun initShareActionProvider(menuItem: MenuItem) {
        // Fetch and store ShareActionProvider
        shareActionProvider = MenuItemCompat.getActionProvider(menuItem) as ShareActionProvider?
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, R.string.app_title)
        i.putExtra(Intent.EXTRA_TEXT, R.string.app_link)
        shareActionProvider?.setShareIntent(i)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                2 -> {
                    myIntentHandler.getData(data)
                }
                myAppUpdater.updateRequestCode -> {
                    myAppUpdater.checkUpdates()
                }
            }
        }
    }
}
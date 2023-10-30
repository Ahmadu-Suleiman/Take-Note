package com.meta4projects.takenote.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.meta4projects.takenote.R
import com.meta4projects.takenote.fragments.CategoriesFragment
import com.meta4projects.takenote.fragments.MainFragment
import com.meta4projects.takenote.fragments.NoteTrashFragment
import com.meta4projects.takenote.fragments.NotesFragment
import com.meta4projects.takenote.others.Utils
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : FullscreenActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var reviewInfo: ReviewInfo? = null
    private lateinit var drawerLayout: DrawerLayout
    private var interstitialAdTrashNote: InterstitialAd? = null
    private var fragment: Fragment? = null
    private lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        consentForAds()
        if (Utils.isFirstTime(this)) {
            startActivity(Intent(this, TutorialActivity::class.java))
            finish()
        }
        drawerLayout = findViewById(R.id.drawer_layout)
        val layout = findViewById<ConstraintLayout>(R.id.main_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.menu.getItem(4).subMenu!!.getItem(0).setActionView(R.layout.menu_image_ad)
        drawerLayout.setScrimColor(Color.TRANSPARENT)
        drawerLayout.addDrawerListener(object : ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                val scaleFactor = 6f
                val slideX = drawerView.width * slideOffset
                layout.translationX = slideX
                layout.scaleX = 1 - slideOffset / scaleFactor
                layout.scaleY = 1 - slideOffset / scaleFactor
            }
        })
        val mainFragment: Fragment = MainFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, mainFragment, mainFragment.toString()).commit()
        loadTrashNoteInterstitial()
        updateAndReview()
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START, true)
                else if (supportFragmentManager.fragments.first().tag != mainFragment.toString()) supportFragmentManager.beginTransaction().replace(R.id.fragment_container, mainFragment, mainFragment.toString()).commit()
                else showLeaveDialog()
            }
        }
        onBackPressedDispatcher.addCallback(this@MainActivity, onBackPressedCallback)
    }

    private fun consentForAds() {
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(this, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                if (loadAndShowError != null) {
                    Log.w(Utils.TAG, String.format("%s: %s", loadAndShowError.errorCode, loadAndShowError.message))
                }

                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk()
                }
            }
        }, { requestConsentError ->
            Log.w(Utils.TAG, String.format("%s: %s", requestConsentError.errorCode, requestConsentError.message))
        })

        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this)
    }

    private fun updateAndReview() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val reviewManager = ReviewManagerFactory.create(this)

        //update
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo -> if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).setAllowAssetPackDeletion(true).build()) else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).setAllowAssetPackDeletion(true).build()) }

        //review
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo?> -> if (task.isSuccessful) reviewInfo = task.result }
        if (reviewInfo != null) reviewManager.launchReviewFlow(this, reviewInfo!!)
    }

    fun hamburgerClick() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START, true) else drawerLayout.openDrawer(GravityCompat.START, true)
    }

    fun searchClick() {
        startActivity(Intent(this@MainActivity, SearchActivity::class.java))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        fragment = null
        when (id) {
            R.id.all_notes -> fragment = NotesFragment()
            R.id.all_categories -> fragment = CategoriesFragment()
            R.id.notes_in_trash -> fragment = NoteTrashFragment()
            R.id.apps -> showApps()
            R.id.night_mode -> setMode()
            R.id.show_tutorial -> {
                startActivity(Intent(this, TutorialActivity::class.java))
                finish()
            }

            R.id.share_app -> shareTakeNote()
            R.id.rate -> rate()
            R.id.about -> showAboutDialog()
            else -> fragment = MainFragment()
        }
        if (fragment != null) {

            if (fragment is NoteTrashFragment && interstitialAdTrashNote != null) interstitialAdTrashNote!!.show(this@MainActivity)
            else supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment!!, fragment.toString()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).setReorderingAllowed(true).commit()
        }
        drawerLayout.closeDrawer(GravityCompat.START, true)
        return true
    }

    private fun openNoteTrashFragment() {
        if (fragment is NoteTrashFragment) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment as NoteTrashFragment, fragment.toString()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).setReorderingAllowed(true).commit()
            loadTrashNoteInterstitial()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTrashNoteInterstitial()
    }

    private fun loadTrashNoteInterstitial() {
        InterstitialAd.load(this, getString(R.string.interstitial_note_trash_unit_id), AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAdTrashNote = interstitialAd
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        openNoteTrashFragment()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialAdTrashNote = null
                    }

                    override fun onAdDismissedFullScreenContent() {
                        openNoteTrashFragment()
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialAdTrashNote = null
            }
        })
    }

    private fun setMode() {
        if (Utils.isNightMode(this)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    private fun showApps() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=5382562347439530585")))
    }

    private fun showAboutDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_about, findViewById(R.id.about_dialog), false)
        val dialogAbout = Utils.getDialogView(this, view)
        dialogAbout.show()
    }

    private fun rate() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun shareTakeNote() {
        val message = """
            
            I'm recommending this note taking app to you, you can easily create subsections in it!
            
            https//play.google.com/store/apps/details?id=$packageName
            
            """.trimIndent()
        IntentBuilder(this).setType("text/plain").setSubject(getString(R.string.app_name)).setChooserTitle("share using...").setText(message).startChooser()
    }

    private fun showLeaveDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_leave_app, findViewById(R.id.layout_leave_app_dialog), false)
        val dialogLeaveApp = Utils.getDialogView(this, view)
        view.findViewById<View>(R.id.text_leave_app).setOnClickListener {
            dialogLeaveApp.dismiss()
            finish()
        }
        view.findViewById<View>(R.id.text_cancel_leave).setOnClickListener { dialogLeaveApp.dismiss() }
        dialogLeaveApp.show()
    }
}
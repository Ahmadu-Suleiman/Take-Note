package com.meta4projects.takenote.activities;

import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;
import static com.meta4projects.takenote.others.Utils.getDialogView;
import static com.meta4projects.takenote.others.Utils.isFirstTime;
import static com.meta4projects.takenote.others.Utils.isNightMode;
import static com.meta4projects.takenote.others.Utils.setFirstTime;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ShareCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.fragments.CategoriesFragment;
import com.meta4projects.takenote.fragments.MainFragment;
import com.meta4projects.takenote.fragments.NoteTrashFragment;
import com.meta4projects.takenote.fragments.NotesFragment;

public class MainActivity extends FullscreenActivity implements NavigationView.OnNavigationItemSelectedListener {


    private ReviewInfo reviewInfo;
    private DrawerLayout drawerLayout;
    private InterstitialAd interstitialAdTrashNote;
    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this);

        if (isFirstTime(this)) {
            setFirstTime(this);
            AsyncTask.execute(() -> NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().insertCategory(new Category("Home"), new Category("Work"), new Category("Study"), new Category("Ideas")));
            startActivity(new Intent(this, TutorialActivity.class));
            finish();
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        ConstraintLayout layout = findViewById(R.id.main_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(4).getSubMenu().getItem(0).setActionView(R.layout.menu_image_ad);

        drawerLayout.setScrimColor(Color.TRANSPARENT);
        drawerLayout.addDrawerListener(new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                float scaleFactor = 6f;
                float slideX = drawerView.getWidth() * slideOffset;
                layout.setTranslationX(slideX);
                layout.setScaleX(1 - slideOffset / scaleFactor);
                layout.setScaleY(1 - slideOffset / scaleFactor);
            }
        });

        Fragment fragment = new MainFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
        loadTrashNoteInterstitial();
        updateAndReview();
    }

    private void updateAndReview() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        ReviewManager reviewManager = ReviewManagerFactory.create(this);

        //update
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE))
                appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(IMMEDIATE).setAllowAssetPackDeletion(true).build());
            else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(IMMEDIATE).setAllowAssetPackDeletion(true).build());
        });

        //review
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) reviewInfo = task.getResult();
        });
        if (reviewInfo != null) reviewManager.launchReviewFlow(this, reviewInfo);
    }

    public void hamburgerClick() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START, true);
        else
            drawerLayout.openDrawer(GravityCompat.START, true);
    }

    public void searchClick() {
        startActivity(new Intent(MainActivity.this, SearchActivity.class));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        fragment = null;

        if (id == R.id.all_notes) fragment = new NotesFragment();
        else if (id == R.id.all_categories) fragment = new CategoriesFragment();
        else if (id == R.id.notes_in_trash) fragment = new NoteTrashFragment();
        else if (id == R.id.apps) showApps();
        else if (id == R.id.night_mode) setMode();
        else if (id == R.id.show_tutorial) {
            startActivity(new Intent(this, TutorialActivity.class));
            finish();
        } else if (id == R.id.share_app) shareTakeNote();
        else if (id == R.id.rate) rate();
        else if (id == R.id.about) showAboutDialog();
        else fragment = new MainFragment();

        if (fragment != null) {
            if (fragment instanceof NoteTrashFragment && interstitialAdTrashNote != null) {
                interstitialAdTrashNote.show(MainActivity.this);
            } else {
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).setReorderingAllowed(true).commit();
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START, true);
        return true;
    }

    private void openNoteTrashFragment() {
        if (fragment instanceof NoteTrashFragment) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null).setReorderingAllowed(true).commit();
            loadTrashNoteInterstitial();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrashNoteInterstitial();
    }

    private void loadTrashNoteInterstitial() {
        InterstitialAd.load(this, getString(R.string.interstitial_note_trash_unit_id), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                interstitialAdTrashNote = interstitialAd;

                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        openNoteTrashFragment();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        interstitialAdTrashNote = null;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        openNoteTrashFragment();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAdTrashNote = null;
            }
        });
    }

    private void setMode() {
        if (isNightMode(this))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    private void showApps() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=5382562347439530585")));
    }

    private void showAboutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_about, findViewById(R.id.about_dialog), false);
        final AlertDialog dialogAbout = getDialogView(this, view);
        dialogAbout.show();
    }

    private void rate() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void shareTakeNote() {
        String message = "\nI'm recommending this note taking app to you, you can easily create subsections in it!".concat("\n").concat("\n").concat("https//play.google.com/store/apps/details?id=").concat(getPackageName()).concat("\n");

        new ShareCompat.IntentBuilder(this).setType("text/plain").setSubject(getString(R.string.app_name)).setChooserTitle("share using...").setText(message).startChooser();
    }

    private void showLeaveDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_leave_app, findViewById(R.id.layout_leave_app_dialog), false);

        final AlertDialog dialogLeaveApp = getDialogView(this, view);
        view.findViewById(R.id.text_leave_app).setOnClickListener(v -> {
            dialogLeaveApp.dismiss();
            MainActivity.super.onBackPressed();
        });
        view.findViewById(R.id.text_cancel_leave).setOnClickListener(v -> dialogLeaveApp.dismiss());
        dialogLeaveApp.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START, true);
        else if (getSupportFragmentManager().getBackStackEntryCount() == 0) showLeaveDialog();
        else super.onBackPressed();
    }
}

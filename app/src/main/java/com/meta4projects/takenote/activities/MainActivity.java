package com.meta4projects.takenote.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.navigation.NavigationView;
import com.meta4projects.takenote.BuildConfig;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.fragments.CategoriesFragment;
import com.meta4projects.takenote.fragments.MainFragment;
import com.meta4projects.takenote.fragments.NoteTrashFragment;
import com.meta4projects.takenote.fragments.NotesFragment;

import static com.meta4projects.takenote.others.Util.isFirstTime;
import static com.meta4projects.takenote.others.Util.setFirstTime;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ImageView hamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        if (isFirstTime(this)) {
            setFirstTime(this);

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().insertCategory(new Category("Home"),
                            new Category("Work"),
                            new Category("Study"),
                            new Category("Ideas"));
                }
            });

            startActivity(new Intent(this, TutorialActivity.class));
            finish();
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        hamburger = findViewById(R.id.hamburger);

        hamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(this);

        Fragment fragment = new MainFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.all_notes:
                fragment = new NotesFragment();
                break;
            case R.id.all_categories:
                fragment = new CategoriesFragment();
                break;
            case R.id.notes_in_trash:
                fragment = new NoteTrashFragment();
                break;
            case R.id.show_tutorial:
                startActivity(new Intent(this, TutorialActivity.class));
                finish();
                break;
            case R.id.share_app:
                shareTakeNote();
                break;
            case R.id.contact:
                contactMe();
                break;
            default:
                fragment = new MainFragment();
        }

        if (fragment != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareTakeNote() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Take Note!");

                String message = "\nI'm recommending this note taking app to you, you can easily create subsections in it!".concat("\n").concat("\n")
                        .concat("https//play.google.com/store/apps/details?id").concat(BuildConfig.APPLICATION_ID).concat("\n");
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(Intent.createChooser(shareIntent, "Share with..."));
            }
        });
    }

    private void contactMe() {
        Intent intentEmail = new Intent(Intent.ACTION_SEND);
        intentEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{"ahmadumeta4.1@gmail.com"});
        intentEmail.putExtra(Intent.EXTRA_SUBJECT, "Regarding to Take Note!");

        intentEmail.setType("message/rfc822");
        startActivity(Intent.createChooser(intentEmail, "Choose an email client..."));
    }

    private void showLeaveDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_leave_app, (ViewGroup) findViewById(R.id.layout_leave_app_dialog), false);

        final AlertDialog dialogLeaveApp = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogLeaveApp.getWindow() != null) {
            dialogLeaveApp.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.text_leave_app).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogLeaveApp.dismiss();
                MainActivity.super.onBackPressed();
            }
        });

        view.findViewById(R.id.text_cancel_leave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogLeaveApp.dismiss();
            }
        });

        dialogLeaveApp.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            showLeaveDialog();
        }
    }
}

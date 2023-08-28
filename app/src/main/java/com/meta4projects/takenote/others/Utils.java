package com.meta4projects.takenote.others;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.entities.Category;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final ArrayList<String> categoryNames = new ArrayList<>();
    private static final String PREF_FIRST_TIME_QUERY = "firstTimeQuery";
    private static final String NOTE_INFO = "noteInfo";

    public static boolean isFirstTime(Context context) {
        SharedPreferences sharedPreferences;
        sharedPreferences = context.getSharedPreferences(NOTE_INFO, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_FIRST_TIME_QUERY, true);
    }

    public static void updateAllCategories(List<Category> categories, final Context context) {
        categoryNames.clear();

        for (Category category : categories) {
            category.update(context);
            categoryNames.add(category.getName());
        }
    }

    public static void showToast(String text, Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.layout_custom_toast, activity.findViewById(R.id.toast_layout));

        TextView message = view.findViewById(R.id.textView_toast_message);
        message.setText(text);

        Toast toast = new Toast(activity);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    public static void setFirstTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTE_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_FIRST_TIME_QUERY, false);
        editor.apply();
    }

    public static void loadNativeAd(Activity activity, TemplateView templateView, String adUnitId) {
        templateView.setVisibility(View.GONE);
        AdLoader adLoader = new AdLoader.Builder(activity, adUnitId)
                .forNativeAd(nativeAd -> {
                    templateView.setNativeAd(nativeAd);
                    if (activity.isDestroyed()) nativeAd.destroy();
                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        templateView.setVisibility(View.VISIBLE);
                    }
                }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static AlertDialog getDialogView(Context context, View view) {
        AlertDialog dialogView = new AlertDialog.Builder(context).setView(view).create();

        if (dialogView.getWindow() != null)
            dialogView.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return dialogView;
    }
}

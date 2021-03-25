package com.meta4projects.takenote.others;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.entities.Category;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static final ArrayList<String> categoryNames = new ArrayList<>();
    private static final String PREF_FIRST_TIME_QUERY = "firstTimeQuery";
    private static final String PREF_NAME = "first_pref";

    public static boolean isFirstTime(Context context) {
        SharedPreferences sharedPreferences;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
        View view = LayoutInflater.from(activity).inflate(R.layout.layout_custom_toast, (ViewGroup) activity.findViewById(R.id.toast_layout));

        TextView message = view.findViewById(R.id.textView_toast_message);
        message.setText(text);

        Toast toast = new Toast(activity);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    public static void setFirstTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_FIRST_TIME_QUERY, false);
        editor.apply();
    }


}

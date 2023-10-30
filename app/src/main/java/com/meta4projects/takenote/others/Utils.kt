package com.meta4projects.takenote.others

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.meta4projects.takenote.R
import com.meta4projects.takenote.database.entities.Category
import kotlinx.coroutines.CoroutineExceptionHandler

object Utils {

    const val TAG: String="Note TAG"

    @JvmField
    val categoryNames = ArrayList<String>()
    private const val PREF_FIRST_TIME_QUERY = "firstTimeQuery"
    private const val NOTE_INFO = "noteInfo"
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    fun isFirstTime(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(NOTE_INFO, Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean(PREF_FIRST_TIME_QUERY, true)
        val editor = sharedPreferences.edit()
        editor.putBoolean(PREF_FIRST_TIME_QUERY, false)
        editor.apply()
        return isFirstTime
    }

    fun updateAllCategories(categories: List<Category>, context: Context) {
        categoryNames.clear()
        for (category in categories) {
            category.update(context)
            categoryNames.add(category.name)
        }
    }

    @JvmStatic
    fun showToast(text: String?, activity: Activity) {
        val view = LayoutInflater.from(activity).inflate(R.layout.layout_custom_toast, activity.findViewById(R.id.toast_layout))
        val message = view.findViewById<TextView>(R.id.textView_toast_message)
        message.text = text
        val toast = Toast(activity)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = view
        toast.show()
    }

    @JvmStatic
    fun loadNativeAd(activity: Activity, templateView: TemplateView, adUnitId: String?) {
        templateView.visibility = View.GONE
        val adLoader = AdLoader.Builder(activity, adUnitId!!).forNativeAd { nativeAd: NativeAd ->
            templateView.setNativeAd(nativeAd)
            if (activity.isDestroyed) nativeAd.destroy()
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                templateView.visibility = View.VISIBLE
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun isNightMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    @JvmStatic
    fun getDialogView(context: Context?, view: View?): AlertDialog {
        val dialogView = AlertDialog.Builder(context!!).setView(view).create()
        if (dialogView.window != null) dialogView.window!!.setBackgroundDrawable(ColorDrawable(0))
        return dialogView
    }
}
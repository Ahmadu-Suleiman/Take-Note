package com.meta4projects.takenote.activities

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import com.meta4projects.takenote.R
import com.meta4projects.takenote.models.Subsection
import com.meta4projects.takenote.others.Utils.getDialogView
import com.meta4projects.takenote.others.Utils.showToast
import java.util.Locale
import java.util.Random

class SubsectionActivity : FullscreenActivity() {

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var editTextTitle: EditText
    private lateinit var editTextBody: EditText
    private var layoutSubsection: ConstraintLayout? = null
    private var textToSpeech: TextToSpeech? = null
    private var previousSubsection: Subsection? = null
    private var subsection: Subsection? = null
    private var isNewSubsection = false
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subsection)
        previousSubsection = Subsection()
        subsection = Subsection()
        isNewSubsection = true
        layoutSubsection = findViewById(R.id.subsection_layout)
        editTextTitle = findViewById(R.id.edit_text_title)
        editTextBody = findViewById(R.id.edit_text_body)
        val imageViewDone = findViewById<ImageView>(R.id.image_view_subsection_done)
        val imageViewRead = findViewById<ImageView>(R.id.image_view_subsection_read)
        val layoutDeleteSubsection = findViewById<ConstraintLayout>(R.id.constraintLayout_delete_subsection)
        val layoutChooseColorSubsection = findViewById<ConstraintLayout>(R.id.constraintLayout_subsection_color)
        val subsectionColors = intArrayOf(R.color.layout_subsection_color_default, R.color.layout_subsection_color_green, R.color.layout_subsection_color_yellow, R.color.layout_subsection_color_blue, R.color.layout_subsection_color_pink, R.color.layout_subsection_color_purple)
        if (intent.getBooleanExtra(NoteActivity.SUBSECTION_EDIT_EXTRA, false)) {
            val title = intent.getStringExtra(NoteActivity.SUBSECTION_TITLE_EXTRA)
            val body = intent.getStringExtra(NoteActivity.SUBSECTION_BODY_EXTRA)
            val color = intent.getIntExtra(NoteActivity.SUBSECTION_COLOR_EXTRA, 0)
            isNewSubsection = false
            if (color > 0) setColorToSubsectionViews(color)
            editTextTitle.setText(title)
            editTextBody.setText(body)
            subsection = Subsection(title ?: "", body ?: "", color)
            setSubsection(previousSubsection!!, false)
        } else {
            layoutDeleteSubsection.visibility = View.INVISIBLE
            subsection!!.color = subsectionColors[Random().nextInt(subsectionColors.size)]
            setColorToSubsectionViews(subsection!!.color)
        }
        layoutDeleteSubsection.setOnClickListener { showDeleteSubsectionDialog() }
        layoutChooseColorSubsection.setOnClickListener { showChooseNoteColorDialog(subsection!!) }
        textToSpeech = TextToSpeech(this) { status: Int -> if (status == TextToSpeech.SUCCESS) textToSpeech!!.language = Locale.getDefault() }
        imageViewDone.setOnClickListener { if (setSubsection(subsection!!, true)) done() }
        imageViewRead.setOnClickListener {
            val subsectionString = """
                                   ${editTextTitle.text}
                                   ${editTextBody.text}
                                   """.trimIndent()
            if (textToSpeech!!.isSpeaking) textToSpeech!!.stop() else textToSpeech!!.speak(subsectionString, TextToSpeech.QUEUE_FLUSH, null, "subsectionId")
        }
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentSubsection = Subsection()
                if (isNewSubsection) {
                    setSubsection(currentSubsection, false)
                    if (currentSubsection != previousSubsection) showSaveNoteDialog(currentSubsection) else finish()
                } else if (setSubsection(currentSubsection, true)) {
                    if (currentSubsection != previousSubsection) showSaveNoteDialog(currentSubsection) else finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this@SubsectionActivity, onBackPressedCallback)
    }

    private fun done() {
        val data = Intent()
        data.putExtra(SUBSECTION_TITLE, subsection!!.title)
        data.putExtra(SUBSECTION_BODY, subsection!!.body)
        data.putExtra(SUBSECTION_COLOR, subsection!!.color)
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (textToSpeech != null) textToSpeech!!.shutdown()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (currentFocus is EditText) {
            val editText = currentFocus as EditText?
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (editText!!.selectionEnd < editText.text.length) editText.setSelection(editText.selectionStart + 1)
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (editText!!.selectionStart > 0) editText.setSelection(editText.selectionStart - 1)
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showDeleteSubsectionDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_delete_subsection, findViewById(R.id.layout_delete_subsection_dialog), false)
        val dialogDeleteSubsection = getDialogView(this, view)
        view.findViewById<View>(R.id.text_delete_subsection).setOnClickListener {
            dialogDeleteSubsection.dismiss()
            val data = Intent()
            data.putExtra(SUBSECTION_DELETE, true)
            setResult(RESULT_OK, data)
            finish()
        }
        view.findViewById<View>(R.id.text_cancel_delete_subsection).setOnClickListener { dialogDeleteSubsection.dismiss() }
        dialogDeleteSubsection.show()
    }

    private fun setSubsection(subsection: Subsection, showToasts: Boolean): Boolean {
        val title = editTextTitle.text.toString().trim()
        val body = editTextBody.text.toString().trim()
        if (title.isEmpty()) {
            if (showToasts) showToast("title can not be empty!", this@SubsectionActivity)
            return false
        } else if (body.isEmpty()) {
            if (showToasts) showToast("body can not be empty!", this@SubsectionActivity)
            return false
        }
        subsection.title = title
        subsection.body = body
        subsection.color = this.subsection!!.color
        return true
    }

    private fun showSaveNoteDialog(subsection: Subsection) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_save_subsection_changes, findViewById(R.id.layout_save_subsection_dialog), false)
        val dialogSaveSubsection = getDialogView(this, view)
        view.findViewById<View>(R.id.text_save_subsection).setOnClickListener {
            dialogSaveSubsection.dismiss()
            val data = Intent()
            data.putExtra(SUBSECTION_TITLE, subsection.title)
            data.putExtra(SUBSECTION_BODY, subsection.body)
            data.putExtra(SUBSECTION_COLOR, subsection.color)
            setResult(RESULT_OK, data)
            finish()
        }
        view.findViewById<View>(R.id.text_no_save_subsection).setOnClickListener {
            dialogSaveSubsection.dismiss()
            setResult(RESULT_CANCELED)
            finish()
        }
        dialogSaveSubsection.show()
    }

    private fun showChooseNoteColorDialog(subsection: Subsection) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_choose_subsection_color, findViewById(R.id.layout_choose_subsection_color_dialog), false)
        val dialogChooseSubsectionColor = getDialogView(this, view)
        view.findViewById<View>(R.id.view_subsection_color_default).setOnClickListener {
            dialogChooseSubsectionColor.dismiss()
            subsection.color = R.color.layout_subsection_color_default
            setColorToSubsectionViews(R.color.layout_subsection_color_default)
        }
        view.findViewById<View>(R.id.view_subsection_color_blue).setOnClickListener {
            dialogChooseSubsectionColor.dismiss()
            subsection.color = R.color.layout_subsection_color_blue
            setColorToSubsectionViews(R.color.layout_subsection_color_blue)
        }
        view.findViewById<View>(R.id.view_subsection_color_green).setOnClickListener {
            dialogChooseSubsectionColor.dismiss()
            subsection.color = R.color.layout_subsection_color_green
            setColorToSubsectionViews(R.color.layout_subsection_color_green)
        }
        view.findViewById<View>(R.id.view_subsection_color_pink).setOnClickListener {
            dialogChooseSubsectionColor.dismiss()
            subsection.color = R.color.layout_subsection_color_pink
            setColorToSubsectionViews(R.color.layout_subsection_color_pink)
        }
        view.findViewById<View>(R.id.view_subsection_color_purple).setOnClickListener {
            dialogChooseSubsectionColor.dismiss()
            subsection.color = R.color.layout_subsection_color_purple
            setColorToSubsectionViews(R.color.layout_subsection_color_purple)
        }
        view.findViewById<View>(R.id.view_subsection_color_yellow).setOnClickListener {
            dialogChooseSubsectionColor.dismiss()
            subsection.color = R.color.layout_subsection_color_yellow
            setColorToSubsectionViews(R.color.layout_subsection_color_yellow)
        }
        dialogChooseSubsectionColor.show()
    }

    private fun setColorToSubsectionViews(color: Int) {
        layoutSubsection!!.setBackgroundResource(color)
        editTextTitle.setBackgroundResource(color)
        editTextBody.setBackgroundResource(color)
    }

    companion object {
        const val SUBSECTION_TITLE = "com.meta4projects.takenote.activities.subsection_title"
        const val SUBSECTION_BODY = "com.meta4projects.takenote.activities.subsection_body"
        const val SUBSECTION_COLOR = "com.meta4projects.takenote.activities.subsection_color"
        const val SUBSECTION_DELETE = "com.meta4projects.takenote.activities.subsection_delete"
    }
}
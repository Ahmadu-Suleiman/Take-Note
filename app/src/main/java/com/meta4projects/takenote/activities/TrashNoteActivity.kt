package com.meta4projects.takenote.activities

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.meta4projects.takenote.R
import com.meta4projects.takenote.database.NoteDatabase
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.fragments.NoteTrashFragment
import com.meta4projects.takenote.models.Subsection
import com.meta4projects.takenote.others.Image
import com.meta4projects.takenote.others.ViewType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrashNoteActivity : FullscreenActivity() {
    private var scrollViewTrashNote: ScrollView? = null
    private lateinit var textViewDateTrash: TextView
    private lateinit var textViewCategoryNameTrash: TextView
    private lateinit var textViewTitleTrash: TextView
    private lateinit var texViewFirstTrash: TextView
    private lateinit var imageViewNoteTrash: ImageView
    private lateinit var layoutDeleteNote: ConstraintLayout
    private lateinit var layoutRestoreNote: ConstraintLayout
    private var layoutNoteSubsectionTrash: LinearLayout? = null
    private lateinit var note: Note
    private var categoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash_note)
        scrollViewTrashNote = findViewById(R.id.scrollView_trash_note)
        textViewDateTrash = findViewById(R.id.textView_date_trash)
        layoutRestoreNote = findViewById(R.id.layout_restore)
        layoutDeleteNote = findViewById(R.id.layout_delete_permanently)
        textViewCategoryNameTrash = findViewById(R.id.textView_categoryName_trash)
        textViewTitleTrash = findViewById(R.id.text_title_trash)
        texViewFirstTrash = findViewById(R.id.first_text_trash)
        imageViewNoteTrash = findViewById(R.id.imageView_note_trash)
        layoutNoteSubsectionTrash = findViewById(R.id.layout_note_subsection_trash)
        note = Note()
        val noteId = intent.getIntExtra(NoteTrashFragment.TRASH_NOTE_ID_EXTRA, -1)
        CoroutineScope(Dispatchers.IO).launch {
            note = NoteDatabase.getINSTANCE(applicationContext).noteDao().getNote(noteId)
            withContext(Dispatchers.Main) {
                textViewDateTrash.text = SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(Date(note.dateTime))
                textViewTitleTrash.text = note.title
                texViewFirstTrash.text = note.firstEdittextInfo
                texViewFirstTrash.requestFocus()
                val bitmap = Image.getScaledBitmap(note.imagePath, this@TrashNoteActivity)
                Glide.with(this@TrashNoteActivity).asBitmap().load(bitmap).into(imageViewNoteTrash)
                imageViewNoteTrash.visibility = View.VISIBLE
                val viewTypes = note.viewTypes
                val subsections = note.subsections
                val edittextInfo = note.editTextInfo
                setAndInitializeViews(viewTypes, subsections, edittextInfo)
                if (note.color > 0) setColorToViews(note.color)
                categoryName = note.categoryName
                if (categoryName != null) textViewCategoryNameTrash.text = categoryName else textViewCategoryNameTrash.text = getString(R.string.none)
                layoutRestoreNote.setOnClickListener { restoreNote(note) }
                layoutDeleteNote.setOnClickListener { showDeleteNoteDialog(note) }
            }
        }
    }

    private fun restoreNote(note: Note?) {
        note!!.isInTrash = false
        note.dateTime = Date().time
        CoroutineScope(Dispatchers.IO).launch {
            NoteDatabase.getINSTANCE(applicationContext).noteDao().updateNote(note)
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
                restoreNote(note)
                finish()
            }
        }
    }

    private fun setAndInitializeViews(viewTypes: ArrayList<String>, subsections: ArrayList<Subsection>, edittextInfo: ArrayList<String>) {
        if (viewTypes.size > 0) {
            var i = 0
            var j = 0
            while (i < viewTypes.size / 2) {
                val viewType = viewTypes[j]
                val viewType2 = viewTypes[j + 1]
                if (viewType == ViewType.SUBSECTION && viewType2 == ViewType.EDITTEXT) {
                    val subsectionView = newLayoutSubsection()
                    val subsection = subsections[i]
                    val title = subsection.title
                    val body = subsection.body
                    val color = subsection.color
                    val layoutSubsection = subsectionView.findViewById<ConstraintLayout>(R.id.sub_subsection_layout)
                    val textViewSubsectionTitle = subsectionView.findViewById<TextView>(R.id.text_sub_section_title)
                    val textViewSubsectionBody = subsectionView.findViewById<TextView>(R.id.text_sub_section_body)
                    val textViewSubsectionColor = subsectionView.findViewById<TextView>(R.id.text_sub_section_color)
                    if (color > 0) layoutSubsection.setBackgroundResource(color)
                    textViewSubsectionTitle.text = title
                    textViewSubsectionBody.text = body
                    textViewSubsectionColor.text = color.toString()
                    val textView = newTextView()
                    val text = edittextInfo[i]
                    textView.text = text
                    layoutNoteSubsectionTrash!!.addView(subsectionView)
                    layoutNoteSubsectionTrash!!.addView(textView)
                }
                i += 1
                j += 2
            }

            //gives last view which is edittext focus
            layoutNoteSubsectionTrash!!.getChildAt(layoutNoteSubsectionTrash!!.childCount - 1).requestFocus()
        }
    }

    private fun newTextView(): TextView {
        return layoutInflater.inflate(R.layout.layout_text_view, layoutNoteSubsectionTrash, false) as TextView
    }

    private fun newLayoutSubsection(): View {
        return layoutInflater.inflate(R.layout.layout_subsection, layoutNoteSubsectionTrash, false)
    }

    private fun setColorToViews(color: Int) {
        note.color = color
        scrollViewTrashNote!!.setBackgroundResource(color)
        textViewTitleTrash.setBackgroundResource(color)
        texViewFirstTrash.setBackgroundResource(color)
        var i = 0
        var j = 1
        while (i < layoutNoteSubsectionTrash!!.childCount / 2) {
            val textView = layoutNoteSubsectionTrash!!.getChildAt(j) as TextView
            textView.setBackgroundResource(color)
            i += 1
            j += 2
        }
    }

    private fun showDeleteNoteDialog(note: Note) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, findViewById(R.id.layout_delete_note_dialog), false)
        val dialogDeleteNote = AlertDialog.Builder(this).setView(view).create()
        if (dialogDeleteNote.window != null) dialogDeleteNote.window!!.setBackgroundDrawable(ColorDrawable(0))
        view.findViewById<View>(R.id.text_delete_note).setOnClickListener {
            dialogDeleteNote.dismiss()
            CoroutineScope(Dispatchers.IO).launch {
                NoteDatabase.getINSTANCE(applicationContext).noteDao().deleteNote(note)
                withContext(Dispatchers.Main) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
        view.findViewById<View>(R.id.text_cancel_delete_note).setOnClickListener { dialogDeleteNote.dismiss() }
        dialogDeleteNote.show()
    }
}
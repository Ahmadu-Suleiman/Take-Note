package com.meta4projects.takenote.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meta4projects.takenote.R
import com.meta4projects.takenote.adapters.NotesAdapter
import com.meta4projects.takenote.database.NoteDatabase
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.fragments.CategoriesFragment
import com.meta4projects.takenote.others.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NotifyDataSetChanged")
class CategoryActivity : FullscreenActivity() {

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val categoryNotesList: MutableList<Note> = ArrayList()
    private lateinit var recyclerViewCategoryNotes: RecyclerView
    private var textViewEmptyNoteCategory: TextView? = null
    private var categoryNotesAdapter: NotesAdapter? = null
    private var categoryAddNewNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var categoryEditNewNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var clickedItemPosition = 0
    private lateinit var textViewCategoryName: TextView
    private var categoryName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        categoryName = intent.getStringExtra(CategoriesFragment.CATEGORY_NAME_EXTRA)
        textViewEmptyNoteCategory = findViewById(R.id.text_empty_note_in_category)
        val templateView = findViewById<View>(R.id.note_native_ad_category).findViewById<TemplateView>(R.id.native_ad)
        Utils.loadNativeAd(this, templateView, getString(R.string.native_category_unit_id))
        textViewCategoryName = findViewById(R.id.textView_categoryName)
        textViewCategoryName.text = categoryName
        recyclerViewCategoryNotes = findViewById(R.id.recyclerViewCategoryNotes)
        categoryNotesAdapter = NotesAdapter(categoryNotesList, this)
        categoryNotesAdapter!!.setListener { position: Int, note: Note ->
            val intent = Intent(this@CategoryActivity, NoteActivity::class.java)
            intent.putExtra(CATEGORY_EDIT_NOTE_EXTRA, true)
            intent.putExtra(STARTED_FROM_CATEGORY_EXTRA, true)
            intent.putExtra(CATEGORY_EDIT_NOTE_ID_EXTRA, note.noteId)
            intent.putExtra(CATEGORY_NAME_EXTRA_ACTIVITY, categoryName)
            clickedItemPosition = position
            categoryEditNewNoteLauncher!!.launch(intent)
        }
        recyclerViewCategoryNotes.adapter = categoryNotesAdapter
        recyclerViewCategoryNotes.layoutManager = LinearLayoutManager(this)
        val buttonAddCategoryNote = findViewById<FloatingActionButton>(R.id.imageView_add_category_note)
        buttonAddCategoryNote.setOnClickListener {
            val intent = Intent(this@CategoryActivity, NoteActivity::class.java)
            intent.putExtra(CATEGORY_ADD_NEW_NOTE_EXTRA, true)
            intent.putExtra(STARTED_FROM_CATEGORY_EXTRA, true)
            intent.putExtra(CATEGORY_NAME_EXTRA_ACTIVITY, categoryName)
            categoryAddNewNoteLauncher!!.launch(intent)
        }
        val layoutRenameCategory = findViewById<ConstraintLayout>(R.id.layoutChangeCategoryName)
        val layoutDeleteCategory = findViewById<ConstraintLayout>(R.id.layoutDeleteCategory)
        layoutRenameCategory.setOnClickListener { showRenameCategoryDialog() }
        layoutDeleteCategory.setOnClickListener { showDeleteCategoryDialog() }
        setCategoryNotes()
        registerLaunchers()
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(RESULT_OK)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this@CategoryActivity, onBackPressedCallback)
    }

    private fun setCategoryNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val notesInCategory = NoteDatabase.getINSTANCE(applicationContext).noteDao().getNotesInCategory(categoryName)
            withContext(Dispatchers.Main) {
                categoryNotesList.addAll(notesInCategory)
                categoryNotesAdapter!!.notifyDataSetChanged()
                setEmptyViewsNote(notesInCategory)
            }
        }
    }

    private fun onAddCategoryNote() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = NoteDatabase.getINSTANCE(applicationContext).noteDao().allNotes
            withContext(Dispatchers.Main) {
                categoryNotesList.add(0, notes[0])
                categoryNotesAdapter!!.notifyItemInserted(0)
                recyclerViewCategoryNotes.smoothScrollToPosition(0)
                setEmptyViewsNote(notes)
                Utils.showToast("note added successfully!", this@CategoryActivity)
            }
        }
    }

    private fun setEmptyViewsNote(notes: List<Note>) {
        if (notes.isEmpty()) textViewEmptyNoteCategory!!.visibility = View.VISIBLE else textViewEmptyNoteCategory!!.visibility = View.GONE
    }

    private fun registerLaunchers() {
        categoryAddNewNoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false)
                val categoryNameChanged = data.getBooleanExtra(NoteActivity.CATEGORY_NAME_CHANGED_EXTRA, false)
                if (isMovedToTrash || categoryNameChanged) onMoveCategoryNote() else onAddCategoryNote()
            }
        }
        categoryEditNewNoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) onEditCategoryNote(data)
        }
    }

    private fun onEditCategoryNote(data: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = NoteDatabase.getINSTANCE(applicationContext).noteDao().getNotesInCategory(categoryName)
            withContext(Dispatchers.Main) {
                val savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false)
                categoryNotesList.clear()
                categoryNotesList.addAll(notes)
                categoryNotesAdapter!!.notifyDataSetChanged()
                setEmptyViewsNote(notes)
                recyclerViewCategoryNotes.smoothScrollToPosition(0)
                if (savedChanges) Utils.showToast("note saved successfully!", this@CategoryActivity)
                else Utils.showToast("note edited successfully!", this@CategoryActivity)
            }
        }
    }

    private fun onMoveCategoryNote() {
        categoryNotesList.removeAt(clickedItemPosition)
        categoryNotesAdapter!!.notifyItemRemoved(clickedItemPosition)
        Utils.showToast("note moved successfully!", this@CategoryActivity)
        setEmptyViewsNote(categoryNotesList)
    }

    private fun showRenameCategoryDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_rename_category, findViewById(R.id.layout_rename_category_dialog), false)
        val dialogRenameCategory = Utils.getDialogView(this, view)
        val editTextCategory = view.findViewById<EditText>(R.id.input_rename_category)
        editTextCategory.setText(categoryName)
        view.findViewById<View>(R.id.text_rename_category).setOnClickListener {
            val newCategoryName = editTextCategory.text.toString().trim()
            var nameExists = false
            if (categoryName!!.isEmpty()) Utils.showToast("empty!", this@CategoryActivity) else {
                for (name in Utils.categoryNames) if (newCategoryName.equals(name, ignoreCase = true)) {
                    nameExists = true
                    break
                }
                if (nameExists) Utils.showToast("category name already exists!", this@CategoryActivity) else {
                    dialogRenameCategory.dismiss()
                    CoroutineScope(Dispatchers.IO).launch {
                        val category = NoteDatabase.getINSTANCE(applicationContext).categoryDao().getCategory(categoryName)
                        category.name = newCategoryName
                        NoteDatabase.getINSTANCE(applicationContext).categoryDao().updateCategory(category)
                        updateNotesInCategory(categoryName, newCategoryName)
                        categoryName = newCategoryName
                        withContext(Dispatchers.Main) {
                            textViewCategoryName.text = newCategoryName
                            Utils.showToast("category renamed successfully!", this@CategoryActivity)
                        }
                    }
                }
            }
        }
        view.findViewById<View>(R.id.text_cancel_rename_category).setOnClickListener { dialogRenameCategory.dismiss() }
        dialogRenameCategory.show()
    }

    private fun showDeleteCategoryDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_delete_category, findViewById(R.id.layout_delete_category_dialog), false)
        val dialogDeleteCategory = Utils.getDialogView(this, view)
        view.findViewById<View>(R.id.text_delete_category).setOnClickListener {
            dialogDeleteCategory.dismiss()

            CoroutineScope(Dispatchers.IO).launch {
                val category = NoteDatabase.getINSTANCE(applicationContext).categoryDao().getCategory(categoryName)
                NoteDatabase.getINSTANCE(applicationContext).categoryDao().deleteCategory(category)
                updateNotesInCategoryDelete(categoryName)
                withContext(Dispatchers.Main) {
                    setResult(RESULT_OK)
                    Utils.showToast("category deleted successfully!", this@CategoryActivity)
                    finish()
                }
            }
        }
        view.findViewById<View>(R.id.text_cancel_delete_category).setOnClickListener { dialogDeleteCategory.dismiss() }
        dialogDeleteCategory.show()
    }

    private fun updateNotesInCategory(previousName: String?, newName: String) {
        val notes = NoteDatabase.getINSTANCE(applicationContext).noteDao().allNotes
        for (note in notes) if (previousName.equals(note.categoryName, ignoreCase = true)) note.categoryName = newName
        NoteDatabase.getINSTANCE(applicationContext).noteDao().updateNotes(notes)
    }

    private fun updateNotesInCategoryDelete(categoryName: String?) {
        val notes = NoteDatabase.getINSTANCE(applicationContext).noteDao().allNotes
        for (note in notes) if (categoryName.equals(note.categoryName, ignoreCase = true)) note.categoryName = null
        NoteDatabase.getINSTANCE(applicationContext).noteDao().updateNotes(notes)
    }

    companion object {
        const val CATEGORY_ADD_NEW_NOTE_EXTRA = "com.meta4projects.takenote.CATEGORY_ADD_NOTE"
        const val CATEGORY_EDIT_NOTE_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_EDIT_NOTE"
        const val CATEGORY_EDIT_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_EDIT_NOTE_ID"
        const val STARTED_FROM_CATEGORY_EXTRA = "com.meta4projects.takenote.activities.STARTED_FROM_CATEGORY_NOTE_ID"
        const val CATEGORY_NAME_EXTRA_ACTIVITY = "com.meta4projects.takenote.activities.CATEGORY_NAME"
    }
}
package com.meta4projects.takenote.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meta4projects.takenote.R
import com.meta4projects.takenote.activities.MainActivity
import com.meta4projects.takenote.activities.NoteActivity
import com.meta4projects.takenote.adapters.NotesAdapter
import com.meta4projects.takenote.database.NoteDatabase.Companion.getINSTANCE
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.others.Utils.loadNativeAd
import com.meta4projects.takenote.others.Utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesFragment : Fragment() {
    private val notesList: MutableList<Note> = ArrayList()
    private lateinit var recyclerViewNotes: RecyclerView
    private var notesAdapter: NotesAdapter? = null
    private var textViewEmptyNote: TextView? = null
    private var addNewNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var editNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var clickedNotePosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialiseLaunchers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)
        textViewEmptyNote = view.findViewById(R.id.text_empty_note)
        val mainActivity = requireActivity() as MainActivity
        val hamburger = view.findViewById<ImageView>(R.id.hamburger)
        val search = view.findViewById<ImageView>(R.id.search)
        hamburger.setOnClickListener { mainActivity.hamburgerClick() }
        search.setOnClickListener { mainActivity.searchClick() }
        val templateView = view.findViewById<View>(R.id.note_native_ad_notes).findViewById<TemplateView>(R.id.native_ad)
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_all_notes_unit_id))
        recyclerViewNotes = view.findViewById(R.id.recyclerViewNotes)
        notesAdapter = NotesAdapter(notesList, requireActivity())
        notesAdapter!!.setListener { position: Int, note: Note ->
            val intent = Intent(activity, NoteActivity::class.java)
            intent.putExtra(MainFragment.EDIT_NOTE_EXTRA, true)
            intent.putExtra(MainFragment.EDIT_NOTE_ID_EXTRA, note.noteId)
            clickedNotePosition = position
            editNoteLauncher!!.launch(intent)
        }
        recyclerViewNotes.adapter = notesAdapter
        recyclerViewNotes.layoutManager = LinearLayoutManager(activity)
        val buttonAddNote = view.findViewById<FloatingActionButton>(R.id.imageView_notes_add_note)
        buttonAddNote.setOnClickListener {
            val intent = Intent(activity, NoteActivity::class.java)
            intent.putExtra(MainFragment.ADD_NEW_NOTE_EXTRA, true)
            addNewNoteLauncher!!.launch(intent)
        }
        setNotes()
        return view
    }

    override fun onResume() {
        super.onResume()
        setNotes()
    }

    private fun setNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = getINSTANCE(requireActivity().applicationContext).noteDao().allNotes
            withContext(Dispatchers.Main) {
                notesList.clear()
                notesList.addAll(notes)
                notesAdapter!!.notifyDataSetChanged()
                setEmptyViewsNote(notes)
            }
        }
    }

    private fun setEmptyViewsNote(notes: List<Note>) {
        if (notes.isEmpty()) textViewEmptyNote!!.visibility = View.VISIBLE else textViewEmptyNote!!.visibility = View.GONE
    }

    private fun initialiseLaunchers() {
        addNewNoteLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult -> if (result.resultCode == Activity.RESULT_OK) onAddNote() }
        editNoteLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false)
                if (isMovedToTrash) onMoveNote() else onEditNote(data)
            }
        }
    }

    private fun onAddNote() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = getINSTANCE(requireActivity().applicationContext).noteDao().allNotes
            withContext(Dispatchers.Main) {
                notesList.add(0, notes[0])
                notesAdapter!!.notifyItemInserted(0)
                recyclerViewNotes.smoothScrollToPosition(0)
                showToast("note added successfully!", requireActivity())
                setEmptyViewsNote(notes)
            }
        }
    }

    private fun onEditNote(data: Intent) {
        updateAllNotes()
        val savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false)
        if (savedChanges) showToast("note saved successfully!", requireActivity()) else showToast("note edited successfully!", requireActivity())
    }

    private fun onMoveNote() {
        notesList.removeAt(clickedNotePosition)
        notesAdapter!!.notifyItemRemoved(clickedNotePosition)
        showToast("note moved successfully!", requireActivity())
        setEmptyViewsNote(notesList)
    }

    private fun updateAllNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = getINSTANCE(requireActivity().applicationContext).noteDao().allNotes
            withContext(Dispatchers.Main) {
                notesList.clear()
                notesList.addAll(notes)
                notesAdapter!!.notifyDataSetChanged()
                setEmptyViewsNote(notes)
                recyclerViewNotes.smoothScrollToPosition(0)
            }
        }
    }
}
package com.meta4projects.takenote.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.meta4projects.takenote.R
import com.meta4projects.takenote.activities.NoteActivity
import com.meta4projects.takenote.adapters.NotesAdapter
import com.meta4projects.takenote.database.NoteDatabase
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.fragments.MainFragment
import com.meta4projects.takenote.others.Utils.loadNativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : FullscreenActivity() {
    private val notesList: MutableList<Note> = ArrayList()
    private var notesAdapter: NotesAdapter? = null
    private var editNoteLauncher: ActivityResultLauncher<Intent>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val searchView = findViewById<SearchView>(R.id.note_search)
        val templateView = findViewById<View>(R.id.search_native_ad_notes).findViewById<TemplateView>(R.id.native_ad)
        loadNativeAd(this, templateView, getString(R.string.native_search_note_unit_id))
        val recyclerViewNotes = findViewById<RecyclerView>(R.id.search_recycler_view)
        notesAdapter = NotesAdapter(notesList, this)
        notesAdapter!!.setListener { _: Int, note: Note ->
            val intent = Intent(this@SearchActivity, NoteActivity::class.java)
            intent.putExtra(MainFragment.EDIT_NOTE_EXTRA, true)
            intent.putExtra(MainFragment.EDIT_NOTE_ID_EXTRA, note.noteId)
            editNoteLauncher!!.launch(intent)
        }
        recyclerViewNotes.adapter = notesAdapter
        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                notesAdapter!!.filter.filter(newText)
                return true
            }
        })
        setNotes()
        initialiseLauncher()
    }

    private fun initialiseLauncher() {
        editNoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { setNotes() }
    }

    private fun setNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = NoteDatabase.getINSTANCE(this@SearchActivity).noteDao().allNotes
            withContext(Dispatchers.Main) {
                notesList.clear()
                notesList.addAll(notes)
                notesAdapter!!.refresh()
                notesAdapter!!.notifyDataSetChanged()
            }
        }
    }
}
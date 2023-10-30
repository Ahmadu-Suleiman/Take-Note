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
import com.meta4projects.takenote.R
import com.meta4projects.takenote.activities.MainActivity
import com.meta4projects.takenote.activities.TrashNoteActivity
import com.meta4projects.takenote.adapters.NotesAdapter
import com.meta4projects.takenote.database.NoteDatabase.Companion.getINSTANCE
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.others.Utils.getDialogView
import com.meta4projects.takenote.others.Utils.loadNativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteTrashFragment : Fragment() {
    private val trashNotesList: MutableList<Note> = ArrayList()
    private var textViewEmptyTrashNote: TextView? = null
    private var trashNotesAdapter: NotesAdapter? = null
    private var trashNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var clickedNotePosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trashNoteLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult -> if (result.resultCode == Activity.RESULT_OK) onRestoreOrDeleteNote() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_note_trash, container, false)
        val mainActivity = requireActivity() as MainActivity
        val hamburger = view.findViewById<ImageView>(R.id.hamburger)
        hamburger.setOnClickListener { mainActivity.hamburgerClick() }
        val imageViewDeleteAll = view.findViewById<ImageView>(R.id.imageView_trash_delete_all)
        textViewEmptyTrashNote = view.findViewById(R.id.text_empty_note_main_trash)
        val templateView = view.findViewById<View>(R.id.note_native_ad_trash).findViewById<TemplateView>(R.id.native_ad)
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_trash_note_unit_id))
        val recyclerViewTrashNotes = view.findViewById<RecyclerView>(R.id.recyclerViewTrashNotes)
        trashNotesAdapter = NotesAdapter(trashNotesList, requireActivity())
        trashNotesAdapter!!.setListener { position: Int, note: Note ->
            val intent = Intent(activity, TrashNoteActivity::class.java)
            intent.putExtra(TRASH_NOTE_ID_EXTRA, note.noteId)
            clickedNotePosition = position
            trashNoteLauncher!!.launch(intent)
        }
        recyclerViewTrashNotes.adapter = trashNotesAdapter
        recyclerViewTrashNotes.layoutManager = LinearLayoutManager(activity)
        imageViewDeleteAll.setOnClickListener { showDeleteAllTrashDialog() }
        setTrashNotes()
        return view
    }

    override fun onResume() {
        super.onResume()
        setTrashNotes()
    }

    private fun setTrashNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val trashNotes = getINSTANCE(requireActivity().applicationContext).noteDao().allNotesInTrash
            withContext(Dispatchers.Main) {
                trashNotesList.clear()
                trashNotesList.addAll(trashNotes)
                trashNotesAdapter!!.notifyDataSetChanged()
                setEmptyViewsNote(trashNotes)
            }
        }
    }

    private fun setEmptyViewsNote(notes: List<Note>) {
        if (notes.isEmpty()) textViewEmptyTrashNote!!.visibility = View.VISIBLE else textViewEmptyTrashNote!!.visibility = View.GONE
    }

    private fun onRestoreOrDeleteNote() {
        trashNotesList.removeAt(clickedNotePosition)
        trashNotesAdapter!!.notifyItemRemoved(clickedNotePosition)
        setEmptyViewsNote(trashNotesList)
    }

    private fun showDeleteAllTrashDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_delete_all_trash_notes, requireActivity().findViewById(R.id.layout_delete_all_trash_dialog), false)
        val dialogDeleteAllTrash = getDialogView(requireContext(), view)
        view.findViewById<View>(R.id.text_delete_all_trash).setOnClickListener {
            dialogDeleteAllTrash.dismiss()
            CoroutineScope(Dispatchers.IO).launch {
                getINSTANCE(requireContext()).noteDao().deleteAllTrashNotes()
                withContext(Dispatchers.Main) {
                    trashNotesList.clear()
                    trashNotesAdapter!!.notifyDataSetChanged()
                    setEmptyViewsNote(trashNotesList)
                }
            }
        }
        view.findViewById<View>(R.id.text_cancel_delete_all_trash).setOnClickListener { dialogDeleteAllTrash.dismiss() }
        dialogDeleteAllTrash.show()
    }

    companion object {
        const val TRASH_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.TRASH_NOTE_ID"
    }
}
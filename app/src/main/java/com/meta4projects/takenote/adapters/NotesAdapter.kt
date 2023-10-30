package com.meta4projects.takenote.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meta4projects.takenote.R
import com.meta4projects.takenote.adapters.NotesAdapter.NoteHolder
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.others.Image
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(notes: MutableList<Note>, activity: Activity) : RecyclerView.Adapter<NoteHolder>(), Filterable {
    private val notes: List<Note>
    private var allNotes: ArrayList<Note>

    private val filter: Filter
    private val activity: Activity
    private var listener: Listener? = null

    init {
        this.notes = notes
        allNotes = ArrayList(notes)
        this.activity = activity
        filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val filteredList: MutableList<Note> = ArrayList()
                val searchText = constraint.toString().lowercase(Locale.getDefault()).trim()
                if (searchText.isEmpty()) {
                    filteredList.addAll(allNotes)
                } else {
                    for (note in allNotes) {
                        if (note.title.lowercase(Locale.getDefault()).contains(searchText)) {
                            filteredList.add(note)
                        } else if (note.firstEdittextInfo.lowercase(Locale.getDefault()).contains(searchText)) {
                            filteredList.add(note)
                        } else {
                            for (edittextInfo in note.editTextInfo) {
                                if (edittextInfo.lowercase(Locale.getDefault()).contains(searchText)) {
                                    filteredList.add(note)
                                    break
                                }
                            }
                            for (subsection in note.subsections) {
                                if (subsection.title.lowercase(Locale.getDefault()).contains(searchText)) {
                                    filteredList.add(note)
                                    break
                                } else if (subsection.body.lowercase(Locale.getDefault()).contains(searchText)) {
                                    filteredList.add(note)
                                    break
                                }
                            }
                        }
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                notes.clear()
                notes.addAll((results.values as List<Note>))
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_note, parent, false)
        return NoteHolder(view, activity)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val note = notes[position]
        holder.setNote(note)
        holder.layout.setOnClickListener { if (listener != null) listener!!.onClick(position, note) }
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getFilter(): Filter {
        return filter
    }

    fun refresh() {
        allNotes.clear()
        allNotes.addAll(notes)
    }

    fun interface Listener {
        fun onClick(position: Int, note: Note)
    }

    class NoteHolder(itemView: View, val activity: Activity) : RecyclerView.ViewHolder(itemView) {
        val layout: ConstraintLayout
        private val textViewTitle: TextView
        private val textViewBody: TextView
        private val textViewDate: TextView
        private val imageViewNoteImage: ImageView

        init {
            layout = itemView.findViewById(R.id.layout_note)
            textViewTitle = itemView.findViewById(R.id.textView_title)
            textViewBody = itemView.findViewById(R.id.textView_body)
            textViewDate = itemView.findViewById(R.id.textView_date)
            imageViewNoteImage = itemView.findViewById(R.id.imageViewNoteImage)
        }

        fun setNote(note: Note) {
            textViewTitle.text = note.title
            textViewBody.text = note.firstEdittextInfo
            textViewDate.text = SimpleDateFormat("dd, MMMM, yyyy", Locale.getDefault()).format(Date(note.dateTime))
            if (note.color > 0) layout.setBackgroundResource(note.color)
            val bitmap = Image.getScaledBitmap(note.imagePath, activity)
            if (bitmap != null) {
                Glide.with(activity).asBitmap().load(bitmap).into(imageViewNoteImage)
                imageViewNoteImage.visibility = View.VISIBLE
            } else imageViewNoteImage.visibility = View.GONE
        }
    }
}
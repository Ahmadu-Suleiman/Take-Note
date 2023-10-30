package com.meta4projects.takenote.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.meta4projects.takenote.R
import com.meta4projects.takenote.activities.CategoryActivity
import com.meta4projects.takenote.activities.MainActivity
import com.meta4projects.takenote.activities.NoteActivity
import com.meta4projects.takenote.adapters.CategoriesAdapter
import com.meta4projects.takenote.adapters.NotesAdapter
import com.meta4projects.takenote.database.NoteDatabase.Companion.getINSTANCE
import com.meta4projects.takenote.database.entities.Category
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.others.Utils.categoryNames
import com.meta4projects.takenote.others.Utils.getDialogView
import com.meta4projects.takenote.others.Utils.loadNativeAd
import com.meta4projects.takenote.others.Utils.showToast
import com.meta4projects.takenote.others.Utils.updateAllCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment() {
    private val notesList: MutableList<Note> = ArrayList()
    private val categoryList: MutableList<Category> = ArrayList()
    private lateinit var recyclerViewAllNotes: RecyclerView
    private lateinit var recyclerViewAllCategories: RecyclerView
    private var textViewEmptyNote: TextView? = null
    private var textViewCategoryTitle: TextView? = null
    private var notesAdapter: NotesAdapter? = null
    private var allCategoryAdapter: CategoriesAdapter? = null
    private var categoryLauncher: ActivityResultLauncher<Intent>? = null
    private var addNewNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var editNoteLauncher: ActivityResultLauncher<Intent>? = null
    private var reviewInfo: ReviewInfo? = null
    private var reviewManager: ReviewManager? = null
    private lateinit var addFab: ExtendedFloatingActionButton
    private lateinit var buttonAddNote: FloatingActionButton
    private lateinit var buttonAddCategory: FloatingActionButton
    private lateinit var textViewAddNote: TextView
    private lateinit var textViewAddCategory: TextView
    private var isAllFabsVisible: Boolean? = null
    private var clickedNotePosition = 0
    private var interstitialAdNote: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialiseLaunchers()
        loadNoteInterstitial()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val mainActivity = requireActivity() as MainActivity
        val hamburger = view.findViewById<ImageView>(R.id.hamburger)
        val search = view.findViewById<ImageView>(R.id.search)
        hamburger.setOnClickListener { mainActivity.hamburgerClick() }
        search.setOnClickListener { mainActivity.searchClick() }
        recyclerViewAllNotes = view.findViewById(R.id.recyclerViewAllNotes)
        recyclerViewAllCategories = view.findViewById(R.id.recyclerViewAllCategories)
        textViewCategoryTitle = view.findViewById(R.id.text_categories_main)
        textViewEmptyNote = view.findViewById(R.id.text_empty_note_main)
        notesAdapter = NotesAdapter(notesList, requireActivity())
        recyclerViewAllNotes.adapter = notesAdapter
        notesAdapter!!.setListener { position: Int, note: Note ->
            val intent = Intent(activity, NoteActivity::class.java)
            intent.putExtra(EDIT_NOTE_EXTRA, true)
            intent.putExtra(EDIT_NOTE_ID_EXTRA, note.noteId)
            clickedNotePosition = position
            editNoteLauncher!!.launch(intent)
        }
        recyclerViewAllNotes.layoutManager = LinearLayoutManager(activity)
        allCategoryAdapter = CategoriesAdapter(categoryList)
        allCategoryAdapter!!.setListener { categoryName: String? ->
            val intent = Intent(activity, CategoryActivity::class.java)
            intent.putExtra(CategoriesFragment.CATEGORY_NAME_EXTRA, categoryName)
            categoryLauncher!!.launch(intent)
        }
        recyclerViewAllCategories.adapter = allCategoryAdapter
        recyclerViewAllCategories.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        val templateView = view.findViewById<View>(R.id.note_native_ad_main).findViewById<TemplateView>(R.id.native_ad)
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_main_note_unit_id))
        setActionButtons(view)
        setNotes()
        setCategories()
        reviewManager = ReviewManagerFactory.create(requireContext())
        val request = reviewManager!!.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo?> -> if (task.isSuccessful) reviewInfo = task.result }
        return view
    }

    override fun onResume() {
        super.onResume()
        setNotes()
        review()
    }

    private fun loadNoteInterstitial() {
        InterstitialAd.load(requireContext(), getString(R.string.interstitial_note_unit_id), AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAdNote = interstitialAd
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        addNote()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialAdNote = null
                    }

                    override fun onAdDismissedFullScreenContent() {
                        addNote()
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialAdNote = null
            }
        })
    }

    private fun setActionButtons(view: View) {
        addFab = view.findViewById(R.id.add_fab)
        buttonAddNote = view.findViewById(R.id.button_add_new_note)
        buttonAddCategory = view.findViewById(R.id.button_add_new_category)
        textViewAddNote = view.findViewById(R.id.add_new_note_text)
        textViewAddCategory = view.findViewById(R.id.add_category_text)
        buttonAddNote.visibility = View.GONE
        buttonAddCategory.visibility = View.GONE
        textViewAddNote.visibility = View.GONE
        textViewAddCategory.visibility = View.GONE
        isAllFabsVisible = false
        addFab.shrink()
        addFab.setOnClickListener {
            isAllFabsVisible = if (!isAllFabsVisible!!) {
                buttonAddNote.show()
                buttonAddCategory.show()
                textViewAddNote.visibility = View.VISIBLE
                textViewAddCategory.visibility = View.VISIBLE
                addFab.extend()
                true
            } else {
                buttonAddNote.hide()
                buttonAddCategory.hide()
                textViewAddNote.visibility = View.GONE
                textViewAddCategory.visibility = View.GONE
                addFab.shrink()
                false
            }
        }
        buttonAddNote.setOnClickListener { if (interstitialAdNote != null) interstitialAdNote!!.show(requireActivity()) else addNote() }
        buttonAddCategory.setOnClickListener { showAddCategoryDialog() }
    }

    private fun addNote() {
        val intent = Intent(activity, NoteActivity::class.java)
        intent.putExtra(ADD_NEW_NOTE_EXTRA, true)
        addNewNoteLauncher!!.launch(intent)
        loadNoteInterstitial()
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

    private fun initialiseLaunchers() {
        addNewNoteLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                onAddNote()
                updateCategories()
            }
        }
        editNoteLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false)
                if (isMovedToTrash) onMoveNote() else onEditNote(data)
                updateCategories()
            }
        }
        categoryLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateAllNotes()
                updateCategories()
            }
        }
    }

    private fun setEmptyViewsNote(notes: List<Note?>?) {
        if (notes!!.isEmpty()) textViewEmptyNote!!.visibility = View.VISIBLE else textViewEmptyNote!!.visibility = View.GONE
    }

    private fun setEmptyViewsCategory(categories: List<Category>) {
        if (categories.isEmpty()) {
            textViewCategoryTitle!!.visibility = View.GONE
            recyclerViewAllCategories.visibility = View.GONE
        } else {
            textViewCategoryTitle!!.visibility = View.VISIBLE
            recyclerViewAllCategories.visibility = View.VISIBLE
        }
    }

    private fun setCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = getINSTANCE(requireActivity().applicationContext).categoryDao().allCategories
            updateAllCategories(categories, requireContext())
            withContext(Dispatchers.Main) {
                categoryList.clear()
                categoryList.addAll(categories)
                allCategoryAdapter!!.notifyDataSetChanged()
                setEmptyViewsCategory(categories)
            }
        }
    }

    private fun showAddCategoryDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.layout_add_category, requireActivity().findViewById(R.id.layout_add_category_dialog), false)
        val dialogAddCategory = getDialogView(requireContext(), view)
        val editTextCategory = view.findViewById<EditText>(R.id.input_add_category)
        view.findViewById<View>(R.id.text_add_category).setOnClickListener {
            val categoryName = editTextCategory.text.toString().trim()
            var nameExists = false
            for (name in categoryNames) {
                if (categoryName.equals(name, ignoreCase = true)) {
                    nameExists = true
                    break
                }
            }
            if (nameExists) showToast("category name already exists!", requireActivity()) else if (categoryName.isEmpty()) showToast("category name cannot be empty!", requireActivity()) else {
                dialogAddCategory.dismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    val category = Category(categoryName)
                    getINSTANCE(requireActivity().applicationContext).categoryDao().insertCategory(category)
                    val categories = getINSTANCE(requireContext().applicationContext).categoryDao().allCategories
                    updateAllCategories(categories, requireContext())
                    withContext(Dispatchers.Main) {
                        val lastIndex = categoryList.size
                        categoryList.add(lastIndex, category)
                        allCategoryAdapter!!.notifyItemInserted(lastIndex)
                        recyclerViewAllCategories.smoothScrollToPosition(lastIndex)
                        setEmptyViewsCategory(categories)
                        showToast("new category added successfully!", requireActivity())
                    }
                }
            }
        }
        view.findViewById<View>(R.id.text_cancel_category).setOnClickListener { dialogAddCategory.dismiss() }
        dialogAddCategory.show()
    }

    private fun onAddNote() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = getINSTANCE(requireActivity().applicationContext).noteDao().allNotes
            withContext(Dispatchers.Main) {
                notesList.add(0, notes[0])
                notesAdapter!!.notifyItemInserted(0)
                recyclerViewAllNotes.smoothScrollToPosition(0)
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
                recyclerViewAllNotes.smoothScrollToPosition(0)
            }
        }
    }

    private fun updateCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = getINSTANCE(requireActivity().applicationContext).categoryDao().allCategories
            updateAllCategories(categories, requireContext())
            withContext(Dispatchers.Main) {
                categoryList.clear()
                categoryList.addAll(categories)
                allCategoryAdapter!!.notifyDataSetChanged()
                setEmptyViewsCategory(categories)
            }
        }
    }

    private fun review() {
        if (reviewInfo != null) reviewManager!!.launchReviewFlow(requireActivity(), reviewInfo!!)
    }

    override fun toString(): String {
        return "MainFragment{}"
    }

    companion object {
        const val ADD_NEW_NOTE_EXTRA = "com.meta4projects.takenote.ADD_NOTE"
        const val EDIT_NOTE_EXTRA = "com.meta4projects.takenote.activities.EDIT_NOTE"
        const val EDIT_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.EDIT_NOTE_ID"
    }
}
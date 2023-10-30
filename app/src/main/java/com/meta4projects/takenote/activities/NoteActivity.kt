package com.meta4projects.takenote.activities

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meta4projects.takenote.R
import com.meta4projects.takenote.database.Lists
import com.meta4projects.takenote.database.NoteDatabase
import com.meta4projects.takenote.database.entities.Note
import com.meta4projects.takenote.fragments.MainFragment
import com.meta4projects.takenote.models.Subsection
import com.meta4projects.takenote.others.Image
import com.meta4projects.takenote.others.Utils
import com.meta4projects.takenote.others.ViewType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class NoteActivity : FullscreenActivity() {

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val categoryNames = ArrayList<String>()
    private var startedFromCategory = false
    private var layoutMain: ConstraintLayout? = null
    private lateinit var bottomSheetOptions: ConstraintLayout
    private var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout?>? = null
    private var textViewDate: TextView? = null
    private var editTextTitle: EditText? = null
    private lateinit var editTextFirst: EditText
    private var layoutContainer: LinearLayout? = null
    private var layoutNoteSubsection: LinearLayout? = null
    private lateinit var imageViewNote: ImageView
    private var imageViewDone: ImageView? = null
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private var textToSpeech: TextToSpeech? = null
    private var addSubsectionLauncher: ActivityResultLauncher<Intent>? = null
    private var editSubsectionLauncher: ActivityResultLauncher<Intent>? = null
    private var viewImageLauncher: ActivityResultLauncher<Intent>? = null
    private var selectImageLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var note: Note
    private var previousNote: Note? = null
    private lateinit var currentNote: Note
    private var categoryName: String? = null
    private lateinit var noteColors: IntArray
    private var layoutEdittextViewId = -1
    private var layoutSubsectionId = -1
    private var clickedSubsectionPosition: Int? = null
    private var isNewNote = false
    private var savedChanges = false
    private var interstitialSubsectionAd: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)
        layoutEdittextViewId = newLayoutEdittext().id
        layoutSubsectionId = newLayoutSubsection().id
        layoutMain = findViewById(R.id.layout_main)
        layoutContainer = findViewById(R.id.layout_container)
        textViewDate = findViewById(R.id.textView_date)
        editTextTitle = findViewById(R.id.editText_title)
        imageViewNote = findViewById(R.id.imageView_note)
        editTextFirst = findViewById(R.id.first_edit_text)
        layoutNoteSubsection = findViewById(R.id.layout_note_subsection)
        imageViewDone = findViewById(R.id.image_view_complete)
        autoCompleteTextView = findViewById(R.id.autocomplete_text_view)
        val buttonSubsectionAdd = findViewById<FloatingActionButton>(R.id.image_view_add_subsection)
        bottomSheetOptions = findViewById(R.id.bottom_sheet_options)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetOptions)
        previousNote = Note()
        note = Note()
        currentNote = Note()
        noteColors = intArrayOf(R.color.layout_note_color_default, R.color.layout_note_color_green, R.color.layout_note_color_yellow, R.color.layout_note_color_blue, R.color.layout_note_color_pink, R.color.layout_note_color_purple)
        autoCompleteTextView.onFocusChangeListener = OnFocusChangeListener { v: View, hasFocus: Boolean ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
        autoCompleteTextView.onItemClickListener = OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
            categoryName = parent.getItemAtPosition(position).toString()
            note.categoryName = categoryName
        }
        buttonSubsectionAdd.setOnClickListener {
            if (isTitleFocused) Utils.showToast("cannot add subsection in title!", this@NoteActivity) else {
                if (interstitialSubsectionAd != null) interstitialSubsectionAd!!.show(this@NoteActivity) else launchSubsection()
            }
        }
        startedFromCategory = intent.getBooleanExtra(CategoryActivity.STARTED_FROM_CATEGORY_EXTRA, false)
        if (intent.getBooleanExtra(MainFragment.ADD_NEW_NOTE_EXTRA, false) || intent.getBooleanExtra(CategoryActivity.CATEGORY_ADD_NEW_NOTE_EXTRA, false)) {
            createNewNote()
        } else if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            val type = intent.type
            if (type == "text/plain") {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                createNewNote()
                editTextFirst.setText(text)
            } else if (type!!.startsWith("image/")) {
                val imageUri = if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (imageUri != null) {
                    val selectedImagePath = getPathFromUri(imageUri)
                    val bitmap = Image.getScaledBitmap(selectedImagePath, this@NoteActivity)
                    if (bitmap != null) {
                        Glide.with(this).asBitmap().load(bitmap).into(imageViewNote)
                        imageViewNote.visibility = View.VISIBLE
                        imageViewNote.isFocusableInTouchMode = true
                        imageViewNote.requestFocus()
                        imageViewNote.isFocusableInTouchMode = false
                        note.imagePath = selectedImagePath
                    }
                }
            }
        } else if (intent.getBooleanExtra(MainFragment.EDIT_NOTE_EXTRA, false) || intent.getBooleanExtra(CategoryActivity.CATEGORY_EDIT_NOTE_EXTRA, false)) {
            setPreviousNote()
        }
        imageViewNote.setOnClickListener {
            val intent = Intent(this@NoteActivity, ImageActivity::class.java)
            intent.putExtra(IMAGE_PATH_EXTRA, note.imagePath)
            viewImageLauncher!!.launch(intent)
        }
        textToSpeech = TextToSpeech(this) { status: Int -> if (status == TextToSpeech.SUCCESS) textToSpeech!!.language = Locale.getDefault() }
        registerLaunchers()
        loadInterstitialSubsection()
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
                } else {
                    setChangesToNote(currentNote, note.dateTime)
                    if (currentNote != previousNote) showSaveNoteDialog(currentNote) else finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this@NoteActivity, onBackPressedCallback)
    }

    private fun launchSubsection() {
        addSubsectionLauncher!!.launch(Intent(this@NoteActivity, SubsectionActivity::class.java))
        loadInterstitialSubsection()
    }

    private fun loadInterstitialSubsection() {
        InterstitialAd.load(this, getString(R.string.interstitial_subsection_unit_id), AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialSubsectionAd = interstitialAd
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        launchSubsection()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialSubsectionAd = null
                    }

                    override fun onAdDismissedFullScreenContent() {
                        launchSubsection()
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialSubsectionAd = null
            }
        })
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO + Utils.coroutineExceptionHandler).launch {
            categoryNames.clear()
            categoryNames.addAll(NoteDatabase.getINSTANCE(this@NoteActivity).categoryDao().allCategoryNames)
            withContext(Dispatchers.Main) {
                val arrayAdapter = ArrayAdapter(this@NoteActivity, R.layout.layout_dropdown_style, categoryNames)
                autoCompleteTextView.setAdapter(arrayAdapter)
                autoCompleteTextView.showSoftInputOnFocus = false
                autoCompleteTextView.isCursorVisible = false
            }
        }
    }

    private fun createNewNote() {
        isNewNote = true
        val dateTime = Date().time
        textViewDate!!.text = SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(Date(dateTime))
        val startedFromCategory = intent.getBooleanExtra(CategoryActivity.STARTED_FROM_CATEGORY_EXTRA, false)
        val categoryNameStartedFrom = intent.getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA_ACTIVITY)
        if (startedFromCategory) {
            autoCompleteTextView.setText(categoryNameStartedFrom)
            note.categoryName = categoryNameStartedFrom
        }
        note.color = noteColors[Random().nextInt(noteColors.size)]
        setColorToViews(note.color)
        setOptions(note)
        imageViewDone!!.setOnClickListener {
            setChangesToNote(note, dateTime)
            confirmChanges()
        }
    }

    private fun setPreviousNote() {
        isNewNote = false
        val id: Int = if (startedFromCategory) intent.getIntExtra(CategoryActivity.CATEGORY_EDIT_NOTE_ID_EXTRA, -1) else intent.getIntExtra(MainFragment.EDIT_NOTE_ID_EXTRA, -1)
        if (id == -1) {
            finish()
            return
        }
        CoroutineScope(Dispatchers.IO + Utils.coroutineExceptionHandler).launch {
            note = NoteDatabase.getINSTANCE(this@NoteActivity).noteDao().getNote(id)
            withContext(Dispatchers.Main) {
                textViewDate!!.text = SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(Date(note.dateTime))
                editTextTitle!!.setText(note.title)
                editTextFirst.setText(note.firstEdittextInfo)
                editTextFirst.requestFocus()
                val bitmap = Image.getScaledBitmap(note.imagePath, this@NoteActivity)
                if (bitmap != null) {
                    Glide.with(this@NoteActivity).asBitmap().load(bitmap).into(imageViewNote)
                    imageViewNote.visibility = View.VISIBLE
                }
                val viewTypes = note.viewTypes
                val subsections = note.subsections
                val edittextInfo = note.editTextInfo
                setAndInitializeViews(viewTypes, subsections, edittextInfo)
                if (note.color > 0) setColorToViews(note.color)
                categoryName = note.categoryName
                if (categoryName != null) autoCompleteTextView.setText(categoryName)
                setChangesToNote(previousNote, note.dateTime)
                setOptions(note)
                imageViewDone!!.setOnClickListener {
                    setChangesToNote(note, Date().time)
                    confirmChanges()
                }
            }
        }
    }

    private fun confirmChanges() {
        if (savedChanges) {
            if (isNewNote) saveNewNote(currentNote, startedFromCategory) else saveNoteEdit(currentNote, startedFromCategory)
        } else {
            if (isNewNote) saveNewNote(note, startedFromCategory) else if (note.isInTrash) trashNote(note) else saveNoteEdit(note, startedFromCategory)
        }
    }

    private fun saveNewNote(note: Note, startedFromCategory: Boolean) {
        CoroutineScope(Dispatchers.IO + Utils.coroutineExceptionHandler).launch {
            NoteDatabase.getINSTANCE(applicationContext).noteDao().insertNote(note)
            withContext(Dispatchers.Main) {
                val data = Intent()
                val categoryNameStartedFrom = intent.getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA_ACTIVITY)
                if (startedFromCategory) if (note.categoryName != categoryNameStartedFrom) data.putExtra(CATEGORY_NAME_CHANGED_EXTRA, true)
                if (savedChanges) data.putExtra(SAVED_CHANGES_EXTRA, true)
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    private fun saveNoteEdit(note: Note, startedFromCategory: Boolean) {
        CoroutineScope(Dispatchers.IO + Utils.coroutineExceptionHandler).launch {
            NoteDatabase.getINSTANCE(this@NoteActivity).noteDao().updateNote(note)
            withContext(Dispatchers.Main) {
                val data = Intent()
                val categoryNameStartedFrom = intent.getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA_ACTIVITY)
                if (startedFromCategory) if (note.categoryName != categoryNameStartedFrom) data.putExtra(CATEGORY_NAME_CHANGED_EXTRA, true)
                if (savedChanges) data.putExtra(SAVED_CHANGES_EXTRA, true)
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    private fun setChangesToNote(note: Note?, dateTime: Long) {
        if (!isNewNote) note!!.noteId = this.note.noteId
        var title = editTextTitle!!.text.toString().trim()
        val firstEdittextInfo = editTextFirst.text.toString().trim()
        if (title.isEmpty()) title = "No title"
        note!!.title = title
        note.firstEdittextInfo = firstEdittextInfo
        note.dateTime = dateTime
        note.categoryName = this.note.categoryName
        note.color = this.note.color
        note.isInTrash = this.note.isInTrash
        note.imagePath = this.note.imagePath
        val viewTypes = Lists.Items()
        val subsections = Lists.Subsections()
        val edittextInfo = Lists.Items()
        setViewTypesAndCorrespondingViewInfo(viewTypes, subsections, edittextInfo)
        note.viewTypes = viewTypes
        note.subsections = subsections
        note.editTextInfo = edittextInfo
    }

    private fun registerLaunchers() {
        addSubsectionLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val title = data.getStringExtra(SubsectionActivity.SUBSECTION_TITLE)
                val body = data.getStringExtra(SubsectionActivity.SUBSECTION_BODY)
                val color = data.getIntExtra(SubsectionActivity.SUBSECTION_COLOR, 0)
                addNewSubsection(title, body, color)
            }
        }
        editSubsectionLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                if (data.getBooleanExtra(SubsectionActivity.SUBSECTION_DELETE, false)) {
                    deleteSubsection()
                } else {
                    val title = data.getStringExtra(SubsectionActivity.SUBSECTION_TITLE)
                    val body = data.getStringExtra(SubsectionActivity.SUBSECTION_BODY)
                    val color = data.getIntExtra(SubsectionActivity.SUBSECTION_COLOR, 0)
                    replaceSubsection(title, body, color)
                }
            }
        }
        viewImageLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                note.imagePath = null
                imageViewNote.visibility = View.GONE
                Utils.showToast("image removed successfully!", this@NoteActivity)
            }
        }
        selectImageLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            val selectedImagePath: String?
            if (result.resultCode == RESULT_OK && data != null) {
                val selectedImageUri = data.data
                if (selectedImageUri != null) {
                    try {
                        selectedImagePath = getPathFromUri(selectedImageUri)
                        val bitmap = Image.getScaledBitmap(selectedImagePath, this@NoteActivity)
                        Glide.with(this).asBitmap().load(bitmap).into(imageViewNote)
                        imageViewNote.visibility = View.VISIBLE
                        imageViewNote.isFocusableInTouchMode = true
                        imageViewNote.requestFocus()
                        imageViewNote.isFocusableInTouchMode = false
                        note.imagePath = selectedImagePath
                    } catch (e: Exception) {
                        Utils.showToast("could not add image!", this@NoteActivity)
                    }
                }
            }
        }
    }

    private fun addNewSubsection(title: String?, body: String?, color: Int) {
        val subsectionView = newLayoutSubsection()
        val editTextView = newLayoutEdittext()
        val layoutSubsection = subsectionView.findViewById<ConstraintLayout>(R.id.sub_subsection_layout)
        val textViewSubsectionTitle = subsectionView.findViewById<TextView>(R.id.text_sub_section_title)
        val textViewSubsectionBody = subsectionView.findViewById<TextView>(R.id.text_sub_section_body)
        val textViewSubsectionColor = subsectionView.findViewById<TextView>(R.id.text_sub_section_color)
        if (color > 0) layoutSubsection.setBackgroundResource(color)
        textViewSubsectionTitle.text = title
        textViewSubsectionBody.text = body
        textViewSubsectionColor.text = color.toString()
        if (note.color > 0) editTextView.setBackgroundResource(note.color)
        subsectionView.setOnClickListener {
            val intent = Intent(this@NoteActivity, SubsectionActivity::class.java)
            intent.putExtra(SUBSECTION_EDIT_EXTRA, true)
            intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.text.toString())
            intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.text.toString())
            intent.putExtra(SUBSECTION_COLOR_EXTRA, textViewSubsectionColor.text.toString().toInt())
            clickedSubsectionPosition = layoutNoteSubsection!!.indexOfChild(subsectionView)
            editSubsectionLauncher!!.launch(intent)
        }
        val position = focusedViewPosition
        if (position == null) { //means that the only the first edittext exist
            if (editTextFirst == layoutContainer!!.focusedChild) {
                val cursorPosition = editTextFirst.selectionEnd
                val text = editTextFirst.text.toString()
                val index = if (text.indexOf(" ", cursorPosition) == -1) cursorPosition else text.indexOf(" ", cursorPosition)
                if (index < text.length) {
                    val text1 = text.substring(0, index)
                    val text2 = text.substring(index)
                    editTextFirst.setText(text1)
                    editTextView.setText(text2)
                }
                layoutNoteSubsection!!.addView(subsectionView, 0)
                layoutNoteSubsection!!.addView(editTextView, 1)
                layoutNoteSubsection!!.getChildAt(layoutNoteSubsection!!.indexOfChild(editTextView)).requestFocus()
            }
        } else { // the position is that of another edittext apart from the first one
            val editTextAbove = layoutNoteSubsection!!.getChildAt(position).findViewById<EditText>(R.id.dynamicEditText)
            val cursorPosition = editTextAbove.selectionEnd
            val text = editTextAbove.text.toString()
            val index = if (text.indexOf(" ", cursorPosition) == -1) cursorPosition else text.indexOf(" ", cursorPosition)
            if (index < text.length) {
                val text1 = text.substring(0, index)
                val text2 = text.substring(index)
                editTextAbove.setText(text1)
                editTextView.setText(text2)
            }
            layoutNoteSubsection!!.addView(subsectionView, position + 1)
            layoutNoteSubsection!!.addView(editTextView, position + 2)
            layoutNoteSubsection!!.getChildAt(layoutNoteSubsection!!.indexOfChild(editTextView)).requestFocus()
        }
    }

    private fun replaceSubsection(title: String?, body: String?, color: Int) {
        if (clickedSubsectionPosition != null) {
            val subsectionView = newLayoutSubsection()
            val layoutSubsection = subsectionView.findViewById<ConstraintLayout>(R.id.sub_subsection_layout)
            val textViewSubsectionTitle = subsectionView.findViewById<TextView>(R.id.text_sub_section_title)
            val textViewSubsectionBody = subsectionView.findViewById<TextView>(R.id.text_sub_section_body)
            val textViewSubsectionColor = subsectionView.findViewById<TextView>(R.id.text_sub_section_color)
            if (color > 0) layoutSubsection.setBackgroundResource(color)
            textViewSubsectionTitle.text = title
            textViewSubsectionBody.text = body
            textViewSubsectionColor.text = color.toString()
            subsectionView.setOnClickListener {
                val intent = Intent(this@NoteActivity, SubsectionActivity::class.java)
                intent.putExtra(SUBSECTION_EDIT_EXTRA, true)
                intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.text.toString())
                intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.text.toString())
                intent.putExtra(SUBSECTION_COLOR_EXTRA, textViewSubsectionColor.text.toString().toInt())
                clickedSubsectionPosition = layoutNoteSubsection!!.indexOfChild(subsectionView)
                editSubsectionLauncher!!.launch(intent)
            }
            layoutNoteSubsection!!.removeViewAt(clickedSubsectionPosition!!)
            layoutNoteSubsection!!.addView(subsectionView, clickedSubsectionPosition!!)
        }
    }

    private fun deleteSubsection() {
        val subsectionView = layoutNoteSubsection!!.getChildAt(clickedSubsectionPosition!!)
        val editTextBelow = layoutNoteSubsection!!.getChildAt(clickedSubsectionPosition!! + 1) as EditText
        val editTextAbove: EditText = layoutNoteSubsection!!.getChildAt(clickedSubsectionPosition!! - 1) as EditText

        //means that there's an existing edittext above it
        if (editTextAbove.id == layoutEdittextViewId) {
            val textBelow = editTextBelow.text.toString()
            editTextAbove.append(textBelow)
        } else {
            val textBelow = editTextBelow.text.toString()
            editTextFirst.append(textBelow)
        }
        layoutNoteSubsection!!.removeView(subsectionView)
        layoutNoteSubsection!!.removeView(editTextBelow)
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
                    subsectionView.setOnClickListener {
                        val intent = Intent(this@NoteActivity, SubsectionActivity::class.java)
                        intent.putExtra(SUBSECTION_EDIT_EXTRA, true)
                        intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.text.toString())
                        intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.text.toString())
                        intent.putExtra(SUBSECTION_COLOR_EXTRA, textViewSubsectionColor.text.toString().toInt())
                        clickedSubsectionPosition = layoutNoteSubsection!!.indexOfChild(subsectionView)
                        editSubsectionLauncher!!.launch(intent)
                    }
                    val editTextView = newLayoutEdittext()
                    val text = edittextInfo[i]
                    editTextView.setText(text)
                    layoutNoteSubsection!!.addView(subsectionView)
                    layoutNoteSubsection!!.addView(editTextView)
                }
                i += 1
                j += 2
            }

            //gives last view which is edittext focus
            layoutNoteSubsection!!.getChildAt(layoutNoteSubsection!!.childCount - 1).requestFocus()
        }
    }

    private fun setViewTypesAndCorrespondingViewInfo(viewTypes: ArrayList<String>, subsections: ArrayList<Subsection>, edittextInfo: ArrayList<String>) {
        var i = 0
        var j = 0
        while (i < layoutNoteSubsection!!.childCount / 2) {
            val subsectionView = layoutNoteSubsection!!.getChildAt(j)
            val edittextView = layoutNoteSubsection!!.getChildAt(j + 1) as EditText
            if (subsectionView.id == layoutSubsectionId && edittextView.id == layoutEdittextViewId) {
                val textViewTitle = subsectionView.findViewById<TextView>(R.id.text_sub_section_title)
                val textViewBody = subsectionView.findViewById<TextView>(R.id.text_sub_section_body)
                val textViewColor = subsectionView.findViewById<TextView>(R.id.text_sub_section_color)
                val subsectionTitle = textViewTitle.text.toString().trim()
                val subsectionBody = textViewBody.text.toString().trim()
                val subsectionColor = textViewColor.text.toString().trim().toInt()
                viewTypes.add(ViewType.SUBSECTION)
                subsections.add(Subsection(subsectionTitle, subsectionBody, subsectionColor))
                val text = edittextView.text.toString()
                viewTypes.add(ViewType.EDITTEXT)
                edittextInfo.add(text)
            }
            i += 1
            j += 2
        }
    }

    private fun setOptions(note: Note?) {
        bottomSheetOptions.setOnClickListener { if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED) else bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED) }
        val chooseColor: LinearLayout = findViewById(R.id.linearLayout_choose_color)
        val addImage: LinearLayout = findViewById(R.id.linearLayout_add_image)
        val share: LinearLayout = findViewById(R.id.linearLayout_share)
        val trashNote: LinearLayout = findViewById(R.id.linearLayout_trash_note)
        val hierarchy: LinearLayout = findViewById(R.id.linearLayout_hierarchy)
        val read: LinearLayout = findViewById(R.id.linearLayout_read_note)
        if (isNewNote) trashNote.visibility = View.GONE
        chooseColor.setOnClickListener {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            showChooseNoteColorDialog(note)
        }
        addImage.setOnClickListener {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this@NoteActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION) else selectImage()
        }
        share.setOnClickListener {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            IntentBuilder(this).setType("text/plain").setChooserTitle("share note using...").setText(noteText()).startChooser()
        }
        trashNote.setOnClickListener {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            showTrashNoteDialog(note)
        }
        hierarchy.setOnClickListener {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            showSubsectionHierarchyDialog()
        }
        read.setOnClickListener {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            if (textToSpeech!!.isSpeaking) textToSpeech!!.stop() else textToSpeech!!.speak(noteTextWithoutSubsections(), TextToSpeech.QUEUE_FLUSH, null, "noteId")
        }
    }

    private val focusedViewPosition: Int?
        get() {
            val view = layoutNoteSubsection!!.focusedChild
            return if (view != null && view.id == layoutEdittextViewId) layoutNoteSubsection!!.indexOfChild(view) else null
        }

    override fun onDestroy() {
        super.onDestroy()
        if (textToSpeech != null) textToSpeech!!.shutdown()
    }

    private val isTitleFocused: Boolean
        get() = editTextTitle!!.isFocused

    private fun newLayoutEdittext(): EditText {
        return layoutInflater.inflate(R.layout.layout_edittext, layoutNoteSubsection, false) as EditText
    }

    private fun newLayoutSubsection(): View {
        return layoutInflater.inflate(R.layout.layout_subsection, layoutNoteSubsection, false)
    }

    private fun selectImage() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectImageLauncher!!.launch(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun getPathFromUri(contentUri: Uri): String? {
        val filePath: String?
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        if (cursor == null) filePath = contentUri.path else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }
        return filePath
    }

    private fun showSubsectionHierarchyDialog() {
        val view = LayoutInflater.from(this@NoteActivity).inflate(R.layout.layout_subsection_hierarchy, findViewById(R.id.layout_subsection_hierarchy_dialog), false)
        val dialogSubsectionHierarchy = Utils.getDialogView(this, view)
        val subsectionHierarchy = view.findViewById<LinearLayout>(R.id.layout_subsection_hierarchy)
        if (layoutNoteSubsection!!.childCount > 0) {
            var i = 0
            while (i <= layoutNoteSubsection!!.childCount - 1) {
                val subsectionView = layoutNoteSubsection!!.getChildAt(i)
                if (subsectionView.id == layoutSubsectionId) {
                    val textViewTitle = subsectionView.findViewById<TextView>(R.id.text_sub_section_title)
                    val textViewColor = subsectionView.findViewById<TextView>(R.id.text_sub_section_color)
                    val subsectionTitle = textViewTitle.text.toString().trim { it <= ' ' }
                    val subsectionColor = textViewColor.text.toString().trim { it <= ' ' }.toInt()
                    val subsectionButton = layoutInflater.inflate(R.layout.layout_subsection_in_hierarchy, subsectionHierarchy, false) as Button
                    subsectionButton.text = subsectionTitle
                    subsectionButton.setBackgroundResource(subsectionColor)
                    subsectionButton.setOnClickListener {
                        subsectionView.isFocusableInTouchMode = true
                        subsectionView.clearFocus()
                        subsectionView.requestFocus()
                        subsectionView.isFocusableInTouchMode = false
                        subsectionView.performClick()
                        dialogSubsectionHierarchy.dismiss()
                    }
                    subsectionHierarchy.addView(subsectionButton)
                }
                i += 2
            }
            dialogSubsectionHierarchy.show()
        } else {
            Utils.showToast("no subsections!", this@NoteActivity)
        }
    }

    private fun showSaveNoteDialog(note: Note?) {
        note!!.dateTime = Date().time
        val view = LayoutInflater.from(this).inflate(R.layout.layout_save_note_changes, findViewById(R.id.layout_save_note_dialog), false)
        val dialogSaveNote = Utils.getDialogView(this, view)
        view.findViewById<View>(R.id.text_save_note).setOnClickListener {
            dialogSaveNote.dismiss()
            savedChanges = true
            confirmChanges()
        }
        view.findViewById<View>(R.id.text_no_save).setOnClickListener {
            dialogSaveNote.dismiss()
            setResult(RESULT_CANCELED)
            finish()
        }
        dialogSaveNote.show()
    }

    private fun showTrashNoteDialog(note: Note?) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_trash_note, findViewById(R.id.layout_trash_note_dialog), false)
        val dialogTrashNote = Utils.getDialogView(this, view)
        view.findViewById<View>(R.id.text_move_note).setOnClickListener {
            dialogTrashNote.dismiss()
            note!!.isInTrash = true
            confirmChanges()
        }
        view.findViewById<View>(R.id.text_cancel_move).setOnClickListener { dialogTrashNote.dismiss() }
        dialogTrashNote.show()
    }

    private fun trashNote(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            NoteDatabase.getINSTANCE(applicationContext).noteDao().updateNote(note)
            withContext(Dispatchers.Main) {
                val data = Intent()
                data.putExtra(NOTE_MOVED_TO_TRASH_EXTRA, true)
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    private fun noteTextWithoutSubsections(): String {
        val noteText = StringBuilder()
        val title = editTextTitle!!.text.toString().trim { it <= ' ' }
        val firstText = editTextFirst.text.toString().trim { it <= ' ' }
        noteText.append(title).append("\n")
        noteText.append(firstText).append("\n").append("\n")
        var i = 1
        while (i < layoutNoteSubsection!!.childCount) {
            val edittextView = layoutNoteSubsection!!.getChildAt(i) as EditText
            if (edittextView.id == layoutEdittextViewId) {
                val text = edittextView.text.toString()
                noteText.append(text).append("\n").append("\n")
            }
            i += 2
        }
        return noteText.toString()
    }

    private fun noteText(): String {
        val noteText = StringBuilder()
        val title = editTextTitle!!.text.toString().trim { it <= ' ' }
        val firstText = editTextFirst.text.toString().trim { it <= ' ' }
        noteText.append(title).append("\n")
        noteText.append(firstText).append("\n").append("\n")
        var i = 0
        var j = 0
        while (i < layoutNoteSubsection!!.childCount / 2) {
            val subsectionView = layoutNoteSubsection!!.getChildAt(j)
            val edittextView = layoutNoteSubsection!!.getChildAt(j + 1) as EditText
            if (subsectionView.id == layoutSubsectionId && edittextView.id == layoutEdittextViewId) {
                val textViewTitle = subsectionView.findViewById<TextView>(R.id.text_sub_section_title)
                val textViewBody = subsectionView.findViewById<TextView>(R.id.text_sub_section_body)
                val subsectionTitle = textViewTitle.text.toString().trim { it <= ' ' }
                val subsectionBody = textViewBody.text.toString().trim { it <= ' ' }
                noteText.append(subsectionTitle).append("\n")
                noteText.append(subsectionBody).append("\n").append("\n")
                val text = edittextView.text.toString()
                noteText.append(text).append("\n").append("\n")
            }
            i += 1
            j += 2
        }
        return noteText.toString()
    }

    private fun showChooseNoteColorDialog(note: Note?) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_choose_color, findViewById(R.id.layout_choose_note_color_dialog), false)
        val dialogChooseNoteColor = Utils.getDialogView(this, view)
        view.findViewById<View>(R.id.view_note_color_default).setOnClickListener {
            dialogChooseNoteColor.dismiss()
            note!!.color = R.color.layout_note_color_default
            setColorToViews(R.color.layout_note_color_default)
        }
        view.findViewById<View>(R.id.view_note_color_blue).setOnClickListener {
            dialogChooseNoteColor.dismiss()
            note!!.color = R.color.layout_note_color_blue
            setColorToViews(R.color.layout_note_color_blue)
        }
        view.findViewById<View>(R.id.view_note_color_green).setOnClickListener {
            dialogChooseNoteColor.dismiss()
            note!!.color = R.color.layout_note_color_green
            setColorToViews(R.color.layout_note_color_green)
        }
        view.findViewById<View>(R.id.view_note_color_pink).setOnClickListener {
            dialogChooseNoteColor.dismiss()
            note!!.color = R.color.layout_note_color_pink
            setColorToViews(R.color.layout_note_color_pink)
        }
        view.findViewById<View>(R.id.view_note_color_purple).setOnClickListener {
            dialogChooseNoteColor.dismiss()
            note!!.color = R.color.layout_note_color_purple
            setColorToViews(R.color.layout_note_color_purple)
        }
        view.findViewById<View>(R.id.view_note_color_yellow).setOnClickListener {
            dialogChooseNoteColor.dismiss()
            note!!.color = R.color.layout_note_color_yellow
            setColorToViews(R.color.layout_note_color_yellow)
        }
        dialogChooseNoteColor.show()
    }

    private fun setColorToViews(color: Int) {
        note.color = color
        layoutMain!!.setBackgroundResource(color)
        editTextTitle!!.setBackgroundResource(color)
        editTextFirst.setBackgroundResource(color)
        var i = 0
        var j = 1
        while (i < layoutNoteSubsection!!.childCount / 2) {
            val edittextView = layoutNoteSubsection!!.getChildAt(j) as EditText
            edittextView.setBackgroundResource(color)
            i += 1
            j += 2
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) selectImage()
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

    companion object {
        const val SUBSECTION_EDIT_EXTRA = "com.meta4projects.takenote.activities.edit_subsection"
        const val SUBSECTION_TITLE_EXTRA = "com.meta4projects.takenote.activities.subsection_title"
        const val SUBSECTION_BODY_EXTRA = "com.meta4projects.takenote.activities.subsection_body"
        const val SUBSECTION_COLOR_EXTRA = "com.meta4projects.takenote.activities.subsection_color"
        const val NOTE_MOVED_TO_TRASH_EXTRA = "com.meta4projects.takenote.activities.is_note_deleted"
        const val CATEGORY_NAME_CHANGED_EXTRA = "com.meta4projects.takenote.activities.is_category_name_changed"
        const val SAVED_CHANGES_EXTRA = "com.meta4projects.takenote.activities.saved_changes"
        const val IMAGE_PATH_EXTRA = "com.meta4projects.takenote.activities.image_path"
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1
    }
}
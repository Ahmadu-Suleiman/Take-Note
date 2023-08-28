package com.meta4projects.takenote.activities;

import static com.meta4projects.takenote.others.Utils.getDialogView;
import static com.meta4projects.takenote.others.Utils.showToast;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;
import com.meta4projects.takenote.fragments.MainFragment;
import com.meta4projects.takenote.models.Subsection;
import com.meta4projects.takenote.others.Image;
import com.meta4projects.takenote.others.ViewType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class NoteActivity extends FullscreenActivity {

    public static final String SUBSECTION_EDIT_EXTRA = "com.meta4projects.takenote.activities.edit_subsection";
    public static final String SUBSECTION_TITLE_EXTRA = "com.meta4projects.takenote.activities.subsection_title";
    public static final String SUBSECTION_BODY_EXTRA = "com.meta4projects.takenote.activities.subsection_body";
    public static final String SUBSECTION_COLOR_EXTRA = "com.meta4projects.takenote.activities.subsection_color";
    public static final String NOTE_MOVED_TO_TRASH_EXTRA = "com.meta4projects.takenote.activities.is_note_deleted";
    public static final String CATEGORY_NAME_CHANGED_EXTRA = "com.meta4projects.takenote.activities.is_category_name_changed";
    public static final String SAVED_CHANGES_EXTRA = "com.meta4projects.takenote.activities.saved_changes";
    public static final String IMAGE_PATH_EXTRA = "com.meta4projects.takenote.activities.image_path";

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    final ArrayList<String> categoryNames = new ArrayList<>();

    private boolean startedFromCategory;
    private ConstraintLayout layoutMain, bottomSheetOptions;
    private BottomSheetBehavior<ConstraintLayout> bottomSheetBehavior;
    private TextView textViewDate;
    private EditText editTextTitle, editTextFirst;
    private LinearLayout layoutContainer, layoutNoteSubsection;
    private ImageView imageViewNote, imageViewDone;
    private AutoCompleteTextView autoCompleteTextView;
    private TextToSpeech textToSpeech;

    private ActivityResultLauncher<Intent> addSubsectionLauncher;
    private ActivityResultLauncher<Intent> editSubsectionLauncher;
    private ActivityResultLauncher<Intent> viewImageLauncher;
    private ActivityResultLauncher<Intent> selectImageLauncher;

    private Note note;
    private Note previousNote;
    private Note currentNote;
    private String categoryName;

    private int[] noteColors;

    private int layoutEdittextViewId = -1;
    private int layoutSubsectionId = -1;
    private Integer clickedSubsectionPosition;

    private boolean isNewNote, savedChanges;

    private InterstitialAd interstitialSubsectionAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        layoutEdittextViewId = newLayoutEdittext().getId();
        layoutSubsectionId = newLayoutSubsection().getId();

        layoutMain = findViewById(R.id.layout_main);
        layoutContainer = findViewById(R.id.layout_container);
        textViewDate = findViewById(R.id.textView_date);
        editTextTitle = findViewById(R.id.editText_title);
        imageViewNote = findViewById(R.id.imageView_note);
        editTextFirst = findViewById(R.id.first_edit_text);
        layoutNoteSubsection = findViewById(R.id.layout_note_subsection);
        imageViewDone = findViewById(R.id.image_view_complete);
        autoCompleteTextView = findViewById(R.id.autocomplete_text_view);
        FloatingActionButton buttonSubsectionAdd = findViewById(R.id.image_view_add_subsection);

        bottomSheetOptions = findViewById(R.id.bottom_sheet_options);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetOptions);

        previousNote = new Note();
        note = new Note();
        currentNote = new Note();

        noteColors = new int[]{R.color.layout_note_color_default, R.color.layout_note_color_green, R.color.layout_note_color_yellow, R.color.layout_note_color_blue, R.color.layout_note_color_pink, R.color.layout_note_color_purple};

        autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            categoryName = parent.getItemAtPosition(position).toString();
            note.setCategoryName(categoryName);
        });

        buttonSubsectionAdd.setOnClickListener(v -> {
            if (isTitleFocused()) showToast("cannot add subsection in title!", NoteActivity.this);
            else {
                if (interstitialSubsectionAd != null)
                    interstitialSubsectionAd.show(NoteActivity.this);
                else launchSubsection();
            }
        });

        startedFromCategory = getIntent().getBooleanExtra(CategoryActivity.STARTED_FROM_CATEGORY_EXTRA, false);

        if (getIntent().getBooleanExtra(MainFragment.ADD_NEW_NOTE_EXTRA, false) || getIntent().getBooleanExtra(CategoryActivity.CATEGORY_ADD_NEW_NOTE_EXTRA, false)) {
            createNewNote();
        } else if (Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().getType() != null) {
            String type = getIntent().getType();
            if (type.equals("text/plain")) {
                String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                createNewNote();
                editTextFirst.setText(text);
            } else if (type.startsWith("image/")) {
                Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    String selectedImagePath = getPathFromUri(imageUri);

                    Bitmap bitmap = Image.getScaledBitmap(selectedImagePath, NoteActivity.this);
                    Glide.with(this).asBitmap().load(bitmap).into(imageViewNote);
                    imageViewNote.setVisibility(View.VISIBLE);
                    imageViewNote.setFocusableInTouchMode(true);
                    imageViewNote.requestFocus();
                    imageViewNote.setFocusableInTouchMode(false);

                    note.setImagePath(selectedImagePath);
                }
            }
        } else if (getIntent().getBooleanExtra(MainFragment.EDIT_NOTE_EXTRA, false) || getIntent().getBooleanExtra(CategoryActivity.CATEGORY_EDIT_NOTE_EXTRA, false)) {
            setPreviousNote();
        }

        imageViewNote.setOnClickListener(v -> {
            Intent intent = new Intent(NoteActivity.this, ImageActivity.class);
            intent.putExtra(IMAGE_PATH_EXTRA, note.getImagePath());
            viewImageLauncher.launch(intent);
        });

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) textToSpeech.setLanguage(Locale.getDefault());
        });

        registerLaunchers();
        loadInterstitialSubsection();
    }

    private void launchSubsection() {
        addSubsectionLauncher.launch(new Intent(NoteActivity.this, SubsectionActivity.class));
        loadInterstitialSubsection();
    }

    private void loadInterstitialSubsection() {
        InterstitialAd.load(this, getString(R.string.interstitial_subsection_unit_id), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                interstitialSubsectionAd = interstitialAd;

                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        launchSubsection();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        interstitialSubsectionAd = null;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        launchSubsection();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialSubsectionAd = null;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        AsyncTask.execute(() -> {
            categoryNames.clear();
            categoryNames.addAll(NoteDatabase.getINSTANCE(NoteActivity.this).categoryDao().getAllCategoryNames());

            runOnUiThread(() -> {
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(NoteActivity.this, R.layout.layout_dropdown_style, categoryNames);
                autoCompleteTextView.setAdapter(arrayAdapter);
                autoCompleteTextView.setShowSoftInputOnFocus(false);
                autoCompleteTextView.setCursorVisible(false);
            });
        });
    }

    private void createNewNote() {
        isNewNote = true;

        note = new Note();
        final long dateTime = new Date().getTime();

        textViewDate.setText(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date(dateTime)));

        final boolean startedFromCategory = getIntent().getBooleanExtra(CategoryActivity.STARTED_FROM_CATEGORY_EXTRA, false);
        String categoryNameStartedFrom = getIntent().getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA_ACTIVITY);

        if (startedFromCategory) {
            autoCompleteTextView.setText(categoryNameStartedFrom);
            note.setCategoryName(categoryNameStartedFrom);
        }

        note.setColor(noteColors[new Random().nextInt(noteColors.length)]);
        setColorToViews(note.getColor());
        setOptions(note);

        imageViewDone.setOnClickListener(v -> {
            setChangesToNote(note, dateTime);
            confirmChanges();
        });
    }

    private void saveNewNote(final Note note, final boolean startedFromCategory) {
        AsyncTask.execute(() -> {
            NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().insertNote(note);

            runOnUiThread(() -> {
                Intent data = new Intent();

                String categoryNameStartedFrom = getIntent().getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA_ACTIVITY);

                if (startedFromCategory)
                    if (!note.getCategoryName().equals(categoryNameStartedFrom))
                        data.putExtra(CATEGORY_NAME_CHANGED_EXTRA, true);

                if (savedChanges) data.putExtra(SAVED_CHANGES_EXTRA, true);
                setResult(RESULT_OK, data);
                finish();
            });
        });
    }

    private void setPreviousNote() {
        isNewNote = false;
        final int id;

        if (startedFromCategory)
            id = getIntent().getIntExtra(CategoryActivity.CATEGORY_EDIT_NOTE_ID_EXTRA, -1);
        else id = getIntent().getIntExtra(MainFragment.EDIT_NOTE_ID_EXTRA, -1);

        AsyncTask.execute(() -> {
            note = NoteDatabase.getINSTANCE(NoteActivity.this).noteDao().getNote(id);

            runOnUiThread(() -> {
                textViewDate.setText(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date(note.getDateTime())));
                editTextTitle.setText(note.getTitle());
                editTextFirst.setText(note.getFirstEdittextInfo());
                editTextFirst.requestFocus();

                Bitmap bitmap = Image.getScaledBitmap(note.getImagePath(), NoteActivity.this);

                if (bitmap != null) {
                    Glide.with(NoteActivity.this).asBitmap().load(bitmap).into(imageViewNote);
                    imageViewNote.setVisibility(View.VISIBLE);
                }

                ArrayList<String> viewTypes = note.getViewTypes();
                ArrayList<Subsection> subsections = note.getSubsections();
                ArrayList<String> edittextInfo = note.getEditTextInfo();

                setAndInitializeViews(viewTypes, subsections, edittextInfo);

                if (note.getColor() > 0) setColorToViews(note.getColor());

                categoryName = note.getCategoryName();
                if (categoryName != null) autoCompleteTextView.setText(categoryName);

                setChangesToNote(previousNote, note.getDateTime());

                setOptions(note);

                imageViewDone.setOnClickListener(v -> {
                    setChangesToNote(note, new Date().getTime());
                    confirmChanges();
                });
            });
        });
    }

    private void confirmChanges() {
        if (savedChanges) {
            if (isNewNote) saveNewNote(currentNote, startedFromCategory);
            else saveNoteEdit(currentNote, startedFromCategory);
        } else {
            if (isNewNote) saveNewNote(note, startedFromCategory);
            else if (note.isInTrash()) trashNote(note);
            else saveNoteEdit(note, startedFromCategory);
        }
    }

    private void saveNoteEdit(final Note note, final boolean startedFromCategory) {
        AsyncTask.execute(() -> {
            NoteDatabase.getINSTANCE(NoteActivity.this).noteDao().updateNote(note);

            runOnUiThread(() -> {
                Intent data = new Intent();
                String categoryNameStartedFrom = getIntent().getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA_ACTIVITY);

                if (startedFromCategory)
                    if (!note.getCategoryName().equals(categoryNameStartedFrom))
                        data.putExtra(CATEGORY_NAME_CHANGED_EXTRA, true);

                if (savedChanges) data.putExtra(SAVED_CHANGES_EXTRA, true);
                setResult(RESULT_OK, data);
                finish();
            });
        });
    }

    private void setChangesToNote(Note note, long dateTime) {
        if (!isNewNote) note.setNoteId(this.note.getNoteId());

        String title = editTextTitle.getText().toString().trim();
        String firstEdittextInfo = editTextFirst.getText().toString().trim();

        if (title.isEmpty()) {
            title = "No title";
        } else if (firstEdittextInfo.isEmpty()) {
            firstEdittextInfo = "\t\t\t\t\t";
        }

        note.setTitle(title);
        note.setFirstEdittextInfo(firstEdittextInfo);
        note.setDateTime(dateTime);
        note.setCategoryName(this.note.getCategoryName());
        note.setColor(this.note.getColor());
        note.setInTrash(this.note.isInTrash());

        note.setImagePath(this.note.getImagePath());

        ArrayList<String> viewTypes = new ArrayList<>();
        ArrayList<Subsection> subsections = new ArrayList<>();
        ArrayList<String> edittextInfo = new ArrayList<>();

        setViewTypesAndCorrespondingViewInfo(viewTypes, subsections, edittextInfo);

        note.setViewTypes(viewTypes);
        note.setSubsections(subsections);
        note.setEditTextInfo(edittextInfo);
    }

    private void registerLaunchers() {
        addSubsectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) {
                String title = data.getStringExtra(SubsectionActivity.SUBSECTION_TITLE);
                String body = data.getStringExtra(SubsectionActivity.SUBSECTION_BODY);
                int color = data.getIntExtra(SubsectionActivity.SUBSECTION_COLOR, 0);
                addNewSubsection(title, body, color);
            }
        });
        editSubsectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) {
                if (data.getBooleanExtra(SubsectionActivity.SUBSECTION_DELETE, false)) {
                    deleteSubsection();
                } else {
                    String title = data.getStringExtra(SubsectionActivity.SUBSECTION_TITLE);
                    String body = data.getStringExtra(SubsectionActivity.SUBSECTION_BODY);
                    int color = data.getIntExtra(SubsectionActivity.SUBSECTION_COLOR, 0);

                    replaceSubsection(title, body, color);
                }
            }
        });
        viewImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                note.setImagePath(null);
                imageViewNote.setVisibility(View.GONE);
                showToast("image removed successfully!", NoteActivity.this);
            }
        });
        selectImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            String selectedImagePath;
            if (result.getResultCode() == RESULT_OK && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        selectedImagePath = getPathFromUri(selectedImageUri);

                        Bitmap bitmap = Image.getScaledBitmap(selectedImagePath, NoteActivity.this);
                        Glide.with(this).asBitmap().load(bitmap).into(imageViewNote);
                        imageViewNote.setVisibility(View.VISIBLE);
                        imageViewNote.setFocusableInTouchMode(true);
                        imageViewNote.requestFocus();
                        imageViewNote.setFocusableInTouchMode(false);

                        note.setImagePath(selectedImagePath);
                    } catch (Exception e) {
                        showToast("could not add image!", NoteActivity.this);
                    }
                }
            }
        });
    }

    private void addNewSubsection(String title, String body, int color) {
        final View subsectionView = newLayoutSubsection();
        final EditText editTextView = newLayoutEdittext();

        final ConstraintLayout layoutSubsection = subsectionView.findViewById(R.id.sub_subsection_layout);
        final TextView textViewSubsectionTitle = subsectionView.findViewById(R.id.text_sub_section_title);
        final TextView textViewSubsectionBody = subsectionView.findViewById(R.id.text_sub_section_body);
        final TextView textViewSubsectionColor = subsectionView.findViewById(R.id.text_sub_section_color);

        if (color > 0) layoutSubsection.setBackgroundResource(color);
        textViewSubsectionTitle.setText(title);
        textViewSubsectionBody.setText(body);
        textViewSubsectionColor.setText(String.valueOf(color));

        if (note.getColor() > 0) editTextView.setBackgroundResource(note.getColor());
        subsectionView.setOnClickListener(v -> {
            Intent intent = new Intent(NoteActivity.this, SubsectionActivity.class);
            intent.putExtra(SUBSECTION_EDIT_EXTRA, true);
            intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.getText().toString());
            intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.getText().toString());
            intent.putExtra(SUBSECTION_COLOR_EXTRA, Integer.parseInt(textViewSubsectionColor.getText().toString()));

            clickedSubsectionPosition = layoutNoteSubsection.indexOfChild(subsectionView);
            editSubsectionLauncher.launch(intent);
        });

        final Integer position = getFocusedViewPosition();

        if (position == null) {//means that the only the first edittext exist
            if (editTextFirst.equals(layoutContainer.getFocusedChild())) {
                int cursorPosition = editTextFirst.getSelectionEnd();
                final String text = editTextFirst.getText().toString();
                int index = text.indexOf(" ", cursorPosition) == -1 ? cursorPosition : text.indexOf(" ", cursorPosition);

                if (index < text.length()) {
                    String text1 = text.substring(0, index);
                    String text2 = text.substring(index);
                    editTextFirst.setText(text1);
                    editTextView.setText(text2);
                }

                layoutNoteSubsection.addView(subsectionView, 0);
                layoutNoteSubsection.addView(editTextView, 1);

                layoutNoteSubsection.getChildAt(layoutNoteSubsection.indexOfChild(editTextView)).requestFocus();
            }
        } else {// the position is that of another edittext apart from the first one
            EditText editTextAbove = layoutNoteSubsection.getChildAt(position).findViewById(R.id.dynamicEditText);
            int cursorPosition = editTextAbove.getSelectionEnd();
            final String text = editTextAbove.getText().toString();
            int index = text.indexOf(" ", cursorPosition) == -1 ? cursorPosition : text.indexOf(" ", cursorPosition);
            if (index < text.length()) {
                String text1 = text.substring(0, index);
                String text2 = text.substring(index);

                editTextAbove.setText(text1);
                editTextView.setText(text2);
            }

            layoutNoteSubsection.addView(subsectionView, position + 1);
            layoutNoteSubsection.addView(editTextView, position + 2);
            layoutNoteSubsection.getChildAt(layoutNoteSubsection.indexOfChild(editTextView)).requestFocus();
        }
    }

    private void replaceSubsection(String title, String body, int color) {
        if (clickedSubsectionPosition != null) {
            final View subsectionView = newLayoutSubsection();

            ConstraintLayout layoutSubsection = subsectionView.findViewById(R.id.sub_subsection_layout);
            final TextView textViewSubsectionTitle = subsectionView.findViewById(R.id.text_sub_section_title);
            final TextView textViewSubsectionBody = subsectionView.findViewById(R.id.text_sub_section_body);
            final TextView textViewSubsectionColor = subsectionView.findViewById(R.id.text_sub_section_color);

            if (color > 0) layoutSubsection.setBackgroundResource(color);

            textViewSubsectionTitle.setText(title);
            textViewSubsectionBody.setText(body);

            textViewSubsectionColor.setText(String.valueOf(color));

            subsectionView.setOnClickListener(v -> {
                Intent intent = new Intent(NoteActivity.this, SubsectionActivity.class);

                intent.putExtra(SUBSECTION_EDIT_EXTRA, true);
                intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.getText().toString());
                intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.getText().toString());
                intent.putExtra(SUBSECTION_COLOR_EXTRA, Integer.parseInt(textViewSubsectionColor.getText().toString()));

                clickedSubsectionPosition = layoutNoteSubsection.indexOfChild(subsectionView);

                editSubsectionLauncher.launch(intent);
            });

            layoutNoteSubsection.removeViewAt(clickedSubsectionPosition);
            layoutNoteSubsection.addView(subsectionView, clickedSubsectionPosition);
        }
    }

    private void deleteSubsection() {
        View subsectionView = layoutNoteSubsection.getChildAt(clickedSubsectionPosition);
        EditText editTextBelow = (EditText) layoutNoteSubsection.getChildAt(clickedSubsectionPosition + 1);

        EditText editTextAbove = (EditText) layoutNoteSubsection.getChildAt(clickedSubsectionPosition - 1);

        //means that there's an existing edittext above it
        if (editTextAbove != null && editTextAbove.getId() == layoutEdittextViewId) {
            String textBelow = editTextBelow.getText().toString();
            editTextAbove.append(textBelow);
        } else {
            String textBelow = editTextBelow.getText().toString();
            editTextFirst.append(textBelow);
        }

        layoutNoteSubsection.removeView(subsectionView);
        layoutNoteSubsection.removeView(editTextBelow);
    }

    private void setAndInitializeViews(ArrayList<String> viewTypes, ArrayList<Subsection> subsections, ArrayList<String> edittextInfo) {
        if (viewTypes.size() > 0) {
            for (int i = 0, j = 0; i < viewTypes.size() / 2; i = i + 1, j = j + 2) {

                String viewType = viewTypes.get(j);
                String viewType2 = viewTypes.get(j + 1);

                if (viewType.equals(ViewType.SUBSECTION) && viewType2.equals(ViewType.EDITTEXT)) {
                    final View subsectionView = newLayoutSubsection();
                    Subsection subsection = subsections.get(i);

                    String title = subsection.getTitle();
                    String body = subsection.getBody();
                    int color = subsection.getColor();

                    ConstraintLayout layoutSubsection = subsectionView.findViewById(R.id.sub_subsection_layout);
                    final TextView textViewSubsectionTitle = subsectionView.findViewById(R.id.text_sub_section_title);
                    final TextView textViewSubsectionBody = subsectionView.findViewById(R.id.text_sub_section_body);
                    final TextView textViewSubsectionColor = subsectionView.findViewById(R.id.text_sub_section_color);

                    if (color > 0) layoutSubsection.setBackgroundResource(color);

                    textViewSubsectionTitle.setText(title);
                    textViewSubsectionBody.setText(body);
                    textViewSubsectionColor.setText(String.valueOf(color));

                    subsectionView.setOnClickListener(v -> {
                        Intent intent = new Intent(NoteActivity.this, SubsectionActivity.class);

                        intent.putExtra(SUBSECTION_EDIT_EXTRA, true);
                        intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.getText().toString());
                        intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.getText().toString());
                        intent.putExtra(SUBSECTION_COLOR_EXTRA, Integer.parseInt(textViewSubsectionColor.getText().toString()));

                        clickedSubsectionPosition = layoutNoteSubsection.indexOfChild(subsectionView);

                        editSubsectionLauncher.launch(intent);
                    });

                    final EditText editTextView = newLayoutEdittext();
                    String text = edittextInfo.get(i);

                    editTextView.setText(text);

                    layoutNoteSubsection.addView(subsectionView);
                    layoutNoteSubsection.addView(editTextView);
                }
            }

            //gives last view which is edittext focus
            layoutNoteSubsection.getChildAt(layoutNoteSubsection.getChildCount() - 1).requestFocus();
        }
    }

    private void setViewTypesAndCorrespondingViewInfo(ArrayList<String> viewTypes, ArrayList<Subsection> subsections, ArrayList<String> edittextInfo) {
        for (int i = 0, j = 0; i < layoutNoteSubsection.getChildCount() / 2; i = i + 1, j = j + 2) {
            View subsectionView = layoutNoteSubsection.getChildAt(j);
            EditText edittextView = (EditText) layoutNoteSubsection.getChildAt(j + 1);

            if (subsectionView.getId() == layoutSubsectionId && edittextView.getId() == layoutEdittextViewId) {
                TextView textViewTitle = subsectionView.findViewById(R.id.text_sub_section_title);
                TextView textViewBody = subsectionView.findViewById(R.id.text_sub_section_body);
                TextView textViewColor = subsectionView.findViewById(R.id.text_sub_section_color);

                String subsectionTitle = textViewTitle.getText().toString().trim();
                String subsectionBody = textViewBody.getText().toString().trim();
                int subsectionColor = Integer.parseInt(textViewColor.getText().toString().trim());

                viewTypes.add(ViewType.SUBSECTION);
                subsections.add(new Subsection(subsectionTitle, subsectionBody, subsectionColor));

                String text = edittextView.getText().toString();

                viewTypes.add(ViewType.EDITTEXT);
                edittextInfo.add(text);
            }
        }
    }

    private void setOptions(final Note note) {
        bottomSheetOptions.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        LinearLayout chooseColor, addImage, share, trashNote, hierarchy, read;

        chooseColor = findViewById(R.id.linearLayout_choose_color);
        addImage = findViewById(R.id.linearLayout_add_image);
        share = findViewById(R.id.linearLayout_share);
        trashNote = findViewById(R.id.linearLayout_trash_note);
        hierarchy = findViewById(R.id.linearLayout_hierarchy);
        read = findViewById(R.id.linearLayout_read_note);

        if (isNewNote) trashNote.setVisibility(View.GONE);

        chooseColor.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showChooseNoteColorDialog(note);
        });

        addImage.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(NoteActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            else selectImage();
        });

        share.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            new ShareCompat.IntentBuilder(this).setType("text/plain").setChooserTitle("share note using...").setText(noteText()).startChooser();
        });

        trashNote.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showTrashNoteDialog(note);
        });

        hierarchy.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showSubsectionHierarchyDialog();
        });

        read.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            if (textToSpeech.isSpeaking()) textToSpeech.stop();
            else
                textToSpeech.speak(noteTextWithoutSubsections(), TextToSpeech.QUEUE_FLUSH, null, "noteId");
        });
    }

    private Integer getFocusedViewPosition() {
        View view = layoutNoteSubsection.getFocusedChild();
        if (view != null && view.getId() == layoutEdittextViewId)
            return layoutNoteSubsection.indexOfChild(view);
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (textToSpeech != null) textToSpeech.shutdown();
    }

    private boolean isTitleFocused() {
        return editTextTitle.isFocused();
    }

    private EditText newLayoutEdittext() {
        return (EditText) getLayoutInflater().inflate(R.layout.layout_edittext, layoutNoteSubsection, false);
    }

    private View newLayoutSubsection() {
        return getLayoutInflater().inflate(R.layout.layout_subsection, layoutNoteSubsection, false);
    }

    private void selectImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            selectImageLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) filePath = contentUri.getPath();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }

        return filePath;
    }

    private void showSubsectionHierarchyDialog() {
        final View view = LayoutInflater.from(NoteActivity.this).inflate(R.layout.layout_subsection_hierarchy, findViewById(R.id.layout_subsection_hierarchy_dialog), false);
        final AlertDialog dialogSubsectionHierarchy = getDialogView(this, view);

        LinearLayout subsectionHierarchy = view.findViewById(R.id.layout_subsection_hierarchy);

        if (layoutNoteSubsection.getChildCount() > 0) {
            for (int i = 0; i <= layoutNoteSubsection.getChildCount() - 1; i = i + 2) {
                View subsectionView = layoutNoteSubsection.getChildAt(i);

                if (subsectionView.getId() == layoutSubsectionId) {
                    TextView textViewTitle = subsectionView.findViewById(R.id.text_sub_section_title);
                    TextView textViewColor = subsectionView.findViewById(R.id.text_sub_section_color);

                    String subsectionTitle = textViewTitle.getText().toString().trim();
                    int subsectionColor = Integer.parseInt(textViewColor.getText().toString().trim());

                    Button subsectionButton = (Button) getLayoutInflater().inflate(R.layout.layout_subsection_in_hierarchy, subsectionHierarchy, false);
                    subsectionButton.setText(subsectionTitle);
                    subsectionButton.setBackgroundResource(subsectionColor);

                    subsectionButton.setOnClickListener(v1 -> {
                        subsectionView.setFocusableInTouchMode(true);
                        subsectionView.clearFocus();
                        subsectionView.requestFocus();
                        subsectionView.setFocusableInTouchMode(false);
                        subsectionView.performClick();
                        dialogSubsectionHierarchy.dismiss();
                    });

                    subsectionHierarchy.addView(subsectionButton);
                }
            }
            dialogSubsectionHierarchy.show();
        } else {
            showToast("no subsections!", NoteActivity.this);
        }
    }

    private void showSaveNoteDialog(final Note note) {
        note.setDateTime(new Date().getTime());

        View view = LayoutInflater.from(this).inflate(R.layout.layout_save_note_changes, findViewById(R.id.layout_save_note_dialog), false);
        final AlertDialog dialogSaveNote = getDialogView(this, view);

        view.findViewById(R.id.text_save_note).setOnClickListener(v -> {
            dialogSaveNote.dismiss();
            savedChanges = true;
            confirmChanges();
        });

        view.findViewById(R.id.text_no_save).setOnClickListener(v -> {
            dialogSaveNote.dismiss();
            setResult(RESULT_CANCELED);
            finish();
        });

        dialogSaveNote.show();
    }

    private void showTrashNoteDialog(final Note note) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_trash_note, findViewById(R.id.layout_trash_note_dialog), false);
        final AlertDialog dialogTrashNote = getDialogView(this, view);
        view.findViewById(R.id.text_move_note).setOnClickListener(v -> {
            dialogTrashNote.dismiss();

            note.setInTrash(true);
            confirmChanges();
        });
        view.findViewById(R.id.text_cancel_move).setOnClickListener(v -> dialogTrashNote.dismiss());
        dialogTrashNote.show();
    }

    private void trashNote(final Note note) {
        AsyncTask.execute(() -> {
            NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNote(note);
            runOnUiThread(() -> {
                Intent data = new Intent();
                data.putExtra(NOTE_MOVED_TO_TRASH_EXTRA, true);
                setResult(RESULT_OK, data);
                finish();
            });
        });
    }

    private String noteTextWithoutSubsections() {
        StringBuilder noteText = new StringBuilder();

        String title = editTextTitle.getText().toString().trim();
        String firstText = editTextFirst.getText().toString().trim();

        noteText.append(title).append("\n");
        noteText.append(firstText).append("\n").append("\n");

        for (int i = 1; i < layoutNoteSubsection.getChildCount(); i = i + 2) {
            EditText edittextView = (EditText) layoutNoteSubsection.getChildAt(i);
            if (edittextView.getId() == layoutEdittextViewId) {
                String text = edittextView.getText().toString();
                noteText.append(text).append("\n").append("\n");
            }
        }

        return noteText.toString();
    }

    private String noteText() {
        StringBuilder noteText = new StringBuilder();

        String title = editTextTitle.getText().toString().trim();
        String firstText = editTextFirst.getText().toString().trim();

        noteText.append(title).append("\n");
        noteText.append(firstText).append("\n").append("\n");

        for (int i = 0, j = 0; i < layoutNoteSubsection.getChildCount() / 2; i = i + 1, j = j + 2) {
            View subsectionView = layoutNoteSubsection.getChildAt(j);
            EditText edittextView = (EditText) layoutNoteSubsection.getChildAt(j + 1);

            if (subsectionView.getId() == layoutSubsectionId && edittextView.getId() == layoutEdittextViewId) {
                TextView textViewTitle = subsectionView.findViewById(R.id.text_sub_section_title);
                TextView textViewBody = subsectionView.findViewById(R.id.text_sub_section_body);

                String subsectionTitle = textViewTitle.getText().toString().trim();
                String subsectionBody = textViewBody.getText().toString().trim();

                noteText.append(subsectionTitle).append("\n");
                noteText.append(subsectionBody).append("\n").append("\n");

                String text = edittextView.getText().toString();
                noteText.append(text).append("\n").append("\n");
            }
        }

        return noteText.toString();
    }

    private void showChooseNoteColorDialog(final Note note) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_choose_color, findViewById(R.id.layout_choose_note_color_dialog), false);
        final AlertDialog dialogChooseNoteColor = getDialogView(this, view);

        view.findViewById(R.id.view_note_color_default).setOnClickListener(v -> {
            dialogChooseNoteColor.dismiss();

            note.setColor(R.color.layout_note_color_default);
            setColorToViews(R.color.layout_note_color_default);
        });
        view.findViewById(R.id.view_note_color_blue).setOnClickListener(v -> {
            dialogChooseNoteColor.dismiss();

            note.setColor(R.color.layout_note_color_blue);
            setColorToViews(R.color.layout_note_color_blue);
        });
        view.findViewById(R.id.view_note_color_green).setOnClickListener(v -> {
            dialogChooseNoteColor.dismiss();

            note.setColor(R.color.layout_note_color_green);
            setColorToViews(R.color.layout_note_color_green);
        });
        view.findViewById(R.id.view_note_color_pink).setOnClickListener(v -> {
            dialogChooseNoteColor.dismiss();

            note.setColor(R.color.layout_note_color_pink);
            setColorToViews(R.color.layout_note_color_pink);
        });
        view.findViewById(R.id.view_note_color_purple).setOnClickListener(v -> {
            dialogChooseNoteColor.dismiss();

            note.setColor(R.color.layout_note_color_purple);
            setColorToViews(R.color.layout_note_color_purple);
        });
        view.findViewById(R.id.view_note_color_yellow).setOnClickListener(v -> {
            dialogChooseNoteColor.dismiss();

            note.setColor(R.color.layout_note_color_yellow);
            setColorToViews(R.color.layout_note_color_yellow);
        });

        dialogChooseNoteColor.show();
    }

    private void setColorToViews(int color) {
        note.setColor(color);

        layoutMain.setBackgroundResource(color);
        editTextTitle.setBackgroundResource(color);
        editTextFirst.setBackgroundResource(color);

        for (int i = 0, j = 1; i < layoutNoteSubsection.getChildCount() / 2; i = i + 1, j = j + 2) {
            EditText edittextView = (EditText) layoutNoteSubsection.getChildAt(j);
            edittextView.setBackgroundResource(color);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) selectImage();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getCurrentFocus() instanceof EditText) {
            EditText editText = (EditText) getCurrentFocus();

            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (editText.getSelectionEnd() < editText.getText().length())
                        editText.setSelection(editText.getSelectionStart() + 1);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (editText.getSelectionStart() > 0)
                        editText.setSelection(editText.getSelectionStart() - 1);
                    return true;
            }

            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            setChangesToNote(currentNote, note.getDateTime());
            if (!currentNote.equals(previousNote)) showSaveNoteDialog(currentNote);
            else super.onBackPressed();
        }
    }
}

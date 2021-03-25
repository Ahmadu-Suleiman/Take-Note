package com.meta4projects.takenote.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;
import com.meta4projects.takenote.fragments.MainFragment;
import com.meta4projects.takenote.models.Subsection;
import com.meta4projects.takenote.others.Image;
import com.meta4projects.takenote.others.Util;
import com.meta4projects.takenote.others.ViewType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class NoteActivity extends AppCompatActivity {
    
    public static final int ADD_SUBSECTION_REQUEST_CODE = 1;
    public static final int EDIT_SUBSECTION_REQUEST_CODE = 2;
    public static final int VIEW_IMAGE_REQUEST_CODE = 3;
    
    public static final String SUBSECTION_EDIT_EXTRA = "com.meta4projects.takenote.activities.edit_subsection";
    public static final String SUBSECTION_TITLE_EXTRA = "com.meta4projects.takenote.activities.subsection_title";
    public static final String SUBSECTION_BODY_EXTRA = "com.meta4projects.takenote.activities.subsection_body";
    public static final String SUBSECTION_COLOR_EXTRA = "com.meta4projects.takenote.activities.subsection_color";
    public static final String NOTE_MOVED_TO_TRASH_EXTRA = "com.meta4projects.takenote.activities.is_note_deleted";
    public static final String CATEGORY_NAME_CHANGED_EXTRA = "com.meta4projects.takenote.activities.is_category_name_changed";
    public static final String SAVED_CHANGES_EXTRA = "com.meta4projects.takenote.activities.saved_changes";
    public static final String IMAGE_PATH_EXTRA = "com.meta4projects.takenote.activities.image_path";
    
    private static final int REQUEST_CODE_SELECT_IMAGE = 4;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 5;
    
    final ArrayList<String> categoryNames = new ArrayList<>();
    
    boolean startedFromCategory;
    
    private InterstitialAd interstitialAdNote;
    
    private CoordinatorLayout layoutMain;
    private TextView textViewDate, textViewCategoryName;
    private EditText editTextTitle, editTextFirst;
    private LinearLayout layoutContainer, layoutNoteSubsection;
    private ImageView imageViewNote, imageViewDone;
    private Spinner spinner;
    
    private Note note;
    private Note previousNote;
    private Note currentNote;
    private String categoryName;
    
    private int[] noteColors;
    
    private int layoutEdittextViewId = -1;
    private int layoutSubsectionId = -1;
    private Integer clickedSubsectionPosition = -1;
    
    private boolean spinnerTouched = false;
    
    private boolean isNewNote, savedChanges;
    
    //todo lock note,subsection dialog,search,calender,social media link,intent text,onscreen button
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        
        interstitialAdNote = new InterstitialAd(this);
        interstitialAdNote.setAdUnitId("ca-app-pub-5207738458603169/8418592059");
        interstitialAdNote.loadAd(new AdRequest.Builder().build());
        
        layoutEdittextViewId = newLayoutEdittext().getId();
        layoutSubsectionId = newLayoutSubsection().getId();
        
        layoutMain = findViewById(R.id.layout_main);
        layoutContainer = findViewById(R.id.layout_container);
        textViewDate = findViewById(R.id.textView_date);
        textViewCategoryName = findViewById(R.id.textView_categoryName_Note);
        editTextTitle = findViewById(R.id.editText_title);
        imageViewNote = findViewById(R.id.imageView_note);
        editTextFirst = findViewById(R.id.first_edit_text);
        layoutNoteSubsection = findViewById(R.id.layout_note_subsection);
        FloatingActionButton buttonSubsectionAdd = findViewById(R.id.image_view_add_subsection);
        imageViewDone = findViewById(R.id.image_view_complete);
        spinner = findViewById(R.id.spinner_add_category);
        
        previousNote = new Note();
        note = new Note();
        currentNote = new Note();
        
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                categoryNames.addAll(NoteDatabase.getINSTANCE(NoteActivity.this).categoryDao().getAllCategoryNames());
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(NoteActivity.this, android.R.layout.simple_spinner_item, categoryNames);
                        arrayAdapter.setDropDownViewResource(R.layout.layout_spinner_style);
                        spinner.setAdapter(arrayAdapter);
                    }
                });
            }
        });
        
        noteColors = new int[]{R.color.layout_note_color_default, R.color.layout_note_color_green,
                R.color.layout_note_color_yellow, R.color.layout_note_color_blue,
                R.color.layout_note_color_pink, R.color.layout_note_color_purple};
        
        textViewCategoryName.setText(R.string.none);
        
        spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                spinnerTouched = true;
                spinner.performClick();
                return true;
            }
        });
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerTouched) {
                    categoryName = parent.getItemAtPosition(position).toString();
                    note.setCategoryName(categoryName);
                    textViewCategoryName.setText(categoryName);
                }
                
                spinnerTouched = false;
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        buttonSubsectionAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTitleFocused()) {
                    Util.showToast("can't add subsection in title!", NoteActivity.this);
                } else {
                    startActivityForResult(new Intent(NoteActivity.this, SubsectionActivity.class), ADD_SUBSECTION_REQUEST_CODE);
                }
            }
        });
        
        startedFromCategory = getIntent().getBooleanExtra(CategoryActivity.STARTED_FROM_CATEGORY_EXTRA, false);
        
        if (getIntent().getBooleanExtra(MainFragment.ADD_NEW_NOTE_EXTRA, false)
                || getIntent().getBooleanExtra(CategoryActivity.CATEGORY_ADD_NEW_NOTE_EXTRA, false)) {
            createNewNote();
        } else if (getIntent().getBooleanExtra(MainFragment.EDIT_NOTE_EXTRA, false)
                || getIntent().getBooleanExtra(CategoryActivity.CATEGORY_EDIT_NOTE_EXTRA, false)) {
            setPreviousNote();
        }
        
        imageViewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NoteActivity.this, ImageActivity.class);
                intent.putExtra(IMAGE_PATH_EXTRA, note.getImagePath());
                startActivityForResult(intent, VIEW_IMAGE_REQUEST_CODE);
            }
        });

//todo fix this
//        layoutContainer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                View view = layoutNoteSubsection.getChildAt(layoutNoteSubsection.getChildCount() - 1);
//
//                if (view != null && (view.getId() == layoutEdittextViewId)) {
//                    view.requestFocus();
//                } else {
//                    editTextFirst.requestFocus();
//                }
//            }
//        });
        
        interstitialAdNote.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                interstitialAdNote.loadAd(new AdRequest.Builder().build());
                
                if (savedChanges) {
                    if (isNewNote) {
                        saveNewNote(currentNote, startedFromCategory);
                    } else {
                        saveNoteEdit(currentNote, startedFromCategory);
                    }
                } else {
                    if (isNewNote) {
                        saveNewNote(note, startedFromCategory);
                    } else if (note.isInTrash()) {
                        trashNote(note);
                    } else {
                        saveNoteEdit(note, startedFromCategory);
                    }
                }
            }
        });
    }
    
    private void createNewNote() {
        isNewNote = true;
        
        note = new Note();
        final long dateTime = new Date().getTime();
        
        textViewDate.setText(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date(dateTime)));
        
        final boolean startedFromCategory = getIntent().getBooleanExtra(CategoryActivity.STARTED_FROM_CATEGORY_EXTRA, false);
        String categoryNameStartedFrom = getIntent().getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA);
        
        if (startedFromCategory) {
            spinner.setSelection(categoryNames.indexOf(categoryNameStartedFrom));
            note.setCategoryName(categoryNameStartedFrom);
            textViewCategoryName.setText(categoryNameStartedFrom);
        }
        
        note.setColor(noteColors[new Random().nextInt(noteColors.length)]);
        
        setColorToViews(note.getColor());
        
        setOptions(note);
        
        imageViewDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (setChangesToNote(note, dateTime, true)) {
                    if (interstitialAdNote.isLoaded()) {
                        interstitialAdNote.show();
                    } else {
                        saveNewNote(note, startedFromCategory);
                    }
                }
            }
        });
    }
    
    private void saveNewNote(final Note note, final boolean startedFromCategory) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().insertNote(note);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent data = new Intent();
                        
                        String categoryNameStartedFrom = getIntent().getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA);
                        
                        if (startedFromCategory) {
                            if (!note.getCategoryName().equals(categoryNameStartedFrom)) {
                                data.putExtra(CATEGORY_NAME_CHANGED_EXTRA, true);
                            }
                        }
                        
                        if (savedChanges) {
                            data.putExtra(SAVED_CHANGES_EXTRA, true);
                        }
                        
                        setResult(RESULT_OK, data);
                        
                        finish();
                    }
                });
            }
        });
    }
    
    private void setPreviousNote() {
        isNewNote = false;
        final int id;
        
        if (startedFromCategory) {
            id = getIntent().getIntExtra(CategoryActivity.CATEGORY_EDIT_NOTE_ID_EXTRA, -1);
        } else {
            id = getIntent().getIntExtra(MainFragment.EDIT_NOTE_ID_EXTRA, -1);
        }
        
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                note = NoteDatabase.getINSTANCE(NoteActivity.this).noteDao().getNote(id);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewDate.setText(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date(note.getDateTime())));
                        editTextTitle.setText(note.getTitle());
                        editTextFirst.setText(note.getFirstEdittextInfo());
                        editTextFirst.requestFocus();
                        
                        Bitmap bitmap = Image.getScaledBitmap(note.getImagePath(), NoteActivity.this);
                        
                        if (bitmap != null) {
                            imageViewNote.setImageBitmap(bitmap);
                            imageViewNote.setVisibility(View.VISIBLE);
                        }
                        
                        ArrayList<String> viewTypes = note.getViewTypes();
                        ArrayList<Subsection> subsections = note.getSubsections();
                        ArrayList<String> edittextInfo = note.getEditTextInfo();
                        
                        setAndInitializeViews(viewTypes, subsections, edittextInfo);
                        
                        if (note.getColor() > 0) {
                            setColorToViews(note.getColor());
                        }
                        
                        categoryName = note.getCategoryName();
                        if (categoryName != null) {
                            spinner.setSelection(categoryNames.indexOf(categoryName));
                            textViewCategoryName.setText(categoryName);
                        }
                        
                        setChangesToNote(previousNote, note.getDateTime(), false);
                        
                        setOptions(note);
                        
                        imageViewDone.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                
                                if (setChangesToNote(note, new Date().getTime(), true)) {
                                    if (interstitialAdNote.isLoaded()) {
                                        interstitialAdNote.show();
                                    } else {
                                        saveNoteEdit(note, startedFromCategory);
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });
    }
    
    private void saveNoteEdit(final Note note, final boolean startedFromCategory) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NoteDatabase.getINSTANCE(NoteActivity.this).noteDao().updateNote(note);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent data = new Intent();
                        
                        String categoryNameStartedFrom = getIntent().getStringExtra(CategoryActivity.CATEGORY_NAME_EXTRA);
                        
                        if (startedFromCategory) {
                            if (!note.getCategoryName().equals(categoryNameStartedFrom)) {
                                data.putExtra(CATEGORY_NAME_CHANGED_EXTRA, true);
                            }
                        }
                        
                        if (savedChanges) {
                            data.putExtra(SAVED_CHANGES_EXTRA, true);
                        }
                        
                        setResult(RESULT_OK, data);
                        
                        finish();
                    }
                });
            }
        });
    }
    
    private boolean setChangesToNote(Note note, long dateTime, boolean showToasts) {
        if (!isNewNote) {
            note.setNoteId(this.note.getNoteId());
        }
        final String title = editTextTitle.getText().toString().trim();
        final String firstEdittextInfo = editTextFirst.getText().toString().trim();
        
        if (title.isEmpty()) {
            if (showToasts) {
                Util.showToast("title can't be empty!", NoteActivity.this);
            }
            return false;
        } else if (firstEdittextInfo.isEmpty()) {
            if (showToasts) {
                Util.showToast("top body can't be empty!", NoteActivity.this);
            }
            return false;
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
        
        return true;
    }
    
    private void addNewSubsection(String title, String body, int color) {
        
        final View subsectionView = newLayoutSubsection();
        final EditText editTextView = newLayoutEdittext();
        
        final ConstraintLayout layoutSubsection = subsectionView.findViewById(R.id.sub_subsection_layout);
        final TextView textViewSubsectionTitle = subsectionView.findViewById(R.id.text_sub_section_title);
        final TextView textViewSubsectionBody = subsectionView.findViewById(R.id.text_sub_section_body);
        final TextView textViewSubsectionColor = subsectionView.findViewById(R.id.text_sub_section_color);
        
        if (color > 0) {
            layoutSubsection.setBackgroundResource(color);
        }
        
        textViewSubsectionTitle.setText(title);
        textViewSubsectionBody.setText(body);
        
        textViewSubsectionColor.setText(String.valueOf(color));
        
        if (note.getColor() > 0) {
            editTextView.setBackgroundResource(note.getColor());
        }
        
        subsectionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NoteActivity.this, SubsectionActivity.class);
                intent.putExtra(SUBSECTION_EDIT_EXTRA, true);
                intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.getText().toString());
                intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.getText().toString());
                intent.putExtra(SUBSECTION_COLOR_EXTRA, Integer.parseInt(textViewSubsectionColor.getText().toString()));
                
                clickedSubsectionPosition = layoutNoteSubsection.indexOfChild(subsectionView);
                
                startActivityForResult(intent, EDIT_SUBSECTION_REQUEST_CODE);
            }
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
            
            if (color > 0) {
                layoutSubsection.setBackgroundResource(color);
            }
            
            textViewSubsectionTitle.setText(title);
            textViewSubsectionBody.setText(body);
            
            textViewSubsectionColor.setText(String.valueOf(color));
            
            subsectionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(NoteActivity.this, SubsectionActivity.class);
                    
                    intent.putExtra(SUBSECTION_EDIT_EXTRA, true);
                    intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.getText().toString());
                    intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.getText().toString());
                    intent.putExtra(SUBSECTION_COLOR_EXTRA, Integer.parseInt(textViewSubsectionColor.getText().toString()));
                    
                    clickedSubsectionPosition = layoutNoteSubsection.indexOfChild(subsectionView);
                    
                    startActivityForResult(intent, EDIT_SUBSECTION_REQUEST_CODE);
                }
            });
            
            layoutNoteSubsection.removeViewAt(clickedSubsectionPosition);
            layoutNoteSubsection.addView(subsectionView, clickedSubsectionPosition);
        }
    }
    
    private void deleteSubsection() {
        View subsectionView = layoutNoteSubsection.getChildAt(clickedSubsectionPosition);
        EditText editTextBelow = (EditText) layoutNoteSubsection.getChildAt(clickedSubsectionPosition + 1);
        
        EditText editTextAbove = (EditText) layoutNoteSubsection.getChildAt(clickedSubsectionPosition - 1);
        
        //means that there's a created edittext above it
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
                    
                    if (color > 0) {
                        layoutSubsection.setBackgroundResource(color);
                    }
                    
                    textViewSubsectionTitle.setText(title);
                    textViewSubsectionBody.setText(body);
                    textViewSubsectionColor.setText(String.valueOf(color));
                    
                    subsectionView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(NoteActivity.this, SubsectionActivity.class);
                            
                            intent.putExtra(SUBSECTION_EDIT_EXTRA, true);
                            intent.putExtra(SUBSECTION_TITLE_EXTRA, textViewSubsectionTitle.getText().toString());
                            intent.putExtra(SUBSECTION_BODY_EXTRA, textViewSubsectionBody.getText().toString());
                            intent.putExtra(SUBSECTION_COLOR_EXTRA, Integer.parseInt(textViewSubsectionColor.getText().toString()));
                            
                            clickedSubsectionPosition = layoutNoteSubsection.indexOfChild(subsectionView);
                            
                            startActivityForResult(intent, EDIT_SUBSECTION_REQUEST_CODE);
                        }
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
        ConstraintLayout bottomSheetOptions = findViewById(R.id.bottom_sheet_options);
        final BottomSheetBehavior<ConstraintLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetOptions);
        
        bottomSheetOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
        
        LinearLayout chooseColor, addImage, share, trashNote;
        
        chooseColor = findViewById(R.id.linearLayout_choose_color);
        addImage = findViewById(R.id.linearLayout_add_image);
        share = findViewById(R.id.linearLayout_share);
        trashNote = findViewById(R.id.linearLayout_trash_note);
        
        if (isNewNote) {
            trashNote.setVisibility(View.GONE);
        }
        
        chooseColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                
                showChooseNoteColorDialog(note);
            }
        });
        
        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(NoteActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
                } else {
                    selectImage();
                }
            }
        });
        
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                
                Intent intent = ShareCompat.IntentBuilder.from(NoteActivity.this)
                        .setText(noteText())
                        .setChooserTitle("Share note to...")
                        .setType("text/plain")
                        .createChooserIntent();
                startActivity(intent);
            }
        });
        
        trashNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                
                showTrashNoteDialog(note);
            }
        });
    }
    
    private Integer getFocusedViewPosition() {
        View view = layoutNoteSubsection.getFocusedChild();
        if (view == null) {
            return null;
        } else if (view.getId() == layoutEdittextViewId) {
            return layoutNoteSubsection.indexOfChild(view);
        }
        return null;
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }
    
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        
        return filePath;
    }
    
    private void showSaveNoteDialog(final Note note, final boolean isNewNote, final boolean startedFromCategory) {
        note.setDateTime(new Date().getTime());
        
        View view = LayoutInflater.from(this).inflate(R.layout.layout_save_note_changes, (ViewGroup) findViewById(R.id.layout_save_note_dialog), false);
        
        final AlertDialog dialogSaveNote = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        if (dialogSaveNote.getWindow() != null) {
            dialogSaveNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        
        view.findViewById(R.id.text_save_note).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogSaveNote.dismiss();
                savedChanges = true;
                
                if (interstitialAdNote.isLoaded()) {
                    interstitialAdNote.show();
                } else {
                    if (isNewNote) {
                        saveNewNote(note, startedFromCategory);
                    } else {
                        saveNoteEdit(note, startedFromCategory);
                    }
                }
            }
        });
        
        view.findViewById(R.id.text_no_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSaveNote.dismiss();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        
        dialogSaveNote.show();
    }
    
    private void showTrashNoteDialog(final Note note) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_trash_note, (ViewGroup) findViewById(R.id.layout_trash_note_dialog), false);
        
        final AlertDialog dialogTrashNote = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        if (dialogTrashNote.getWindow() != null) {
            dialogTrashNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        
        view.findViewById(R.id.text_move_note).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogTrashNote.dismiss();
                
                note.setInTrash(true);
                if (interstitialAdNote.isLoaded()) {
                    interstitialAdNote.show();
                } else {
                    trashNote(note);
                }
            }
        });
        
        view.findViewById(R.id.text_cancel_move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogTrashNote.dismiss();
            }
        });
        
        dialogTrashNote.show();
    }
    
    private void trashNote(final Note note) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNote(note);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent data = new Intent();
                        data.putExtra(NOTE_MOVED_TO_TRASH_EXTRA, true);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                });
            }
        });
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
        View view = LayoutInflater.from(this).inflate(R.layout.layout_choose_color, (ViewGroup) findViewById(R.id.layout_choose_note_color_dialog), false);
        
        final AlertDialog dialogChooseNoteColor = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        if (dialogChooseNoteColor.getWindow() != null) {
            dialogChooseNoteColor.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        
        view.findViewById(R.id.view_note_color_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseNoteColor.dismiss();
                
                note.setColor(R.color.layout_note_color_default);
                setColorToViews(R.color.layout_note_color_default);
            }
        });
        view.findViewById(R.id.view_note_color_blue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseNoteColor.dismiss();
                
                note.setColor(R.color.layout_note_color_blue);
                setColorToViews(R.color.layout_note_color_blue);
            }
        });
        view.findViewById(R.id.view_note_color_green).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseNoteColor.dismiss();
                
                note.setColor(R.color.layout_note_color_green);
                setColorToViews(R.color.layout_note_color_green);
            }
        });
        view.findViewById(R.id.view_note_color_pink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseNoteColor.dismiss();
                
                note.setColor(R.color.layout_note_color_pink);
                setColorToViews(R.color.layout_note_color_pink);
            }
        });
        view.findViewById(R.id.view_note_color_purple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseNoteColor.dismiss();
                
                note.setColor(R.color.layout_note_color_purple);
                setColorToViews(R.color.layout_note_color_purple);
            }
        });
        view.findViewById(R.id.view_note_color_yellow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseNoteColor.dismiss();
                
                note.setColor(R.color.layout_note_color_yellow);
                setColorToViews(R.color.layout_note_color_yellow);
            }
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
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String selectedImagePath;
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == ADD_SUBSECTION_REQUEST_CODE) {
                String title = data.getStringExtra(SubsectionActivity.SUBSECTION_TITLE);
                String body = data.getStringExtra(SubsectionActivity.SUBSECTION_BODY);
                int color = data.getIntExtra(SubsectionActivity.SUBSECTION_COLOR, 0);
                
                addNewSubsection(title, body, color);
            } else if (requestCode == EDIT_SUBSECTION_REQUEST_CODE) {
                if (data.getBooleanExtra(SubsectionActivity.SUBSECTION_DELETE, false)) {
                    
                    deleteSubsection();
                } else {
                    String title = data.getStringExtra(SubsectionActivity.SUBSECTION_TITLE);
                    String body = data.getStringExtra(SubsectionActivity.SUBSECTION_BODY);
                    int color = data.getIntExtra(SubsectionActivity.SUBSECTION_COLOR, 0);
                    
                    replaceSubsection(title, body, color);
                }
            } else if (requestCode == REQUEST_CODE_SELECT_IMAGE) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        selectedImagePath = getPathFromUri(selectedImageUri);
                        
                        Bitmap bitmap = Image.getScaledBitmap(selectedImagePath, NoteActivity.this);
                        imageViewNote.setImageBitmap(bitmap);
                        imageViewNote.setVisibility(View.VISIBLE);
                        imageViewNote.requestFocus();
                        
                        note.setImagePath(selectedImagePath);
                    } catch (Exception e) {
                        Util.showToast("couldn't add image!", NoteActivity.this);
                    }
                }
            }
        } else if (requestCode == VIEW_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            note.setImagePath(null);
            imageViewNote.setVisibility(View.GONE);
            Util.showToast("image removed successfully!", NoteActivity.this);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isNewNote) {
            setChangesToNote(currentNote, note.getDateTime(), false);
            if (!currentNote.equals(previousNote)) {
                showSaveNoteDialog(currentNote, isNewNote, startedFromCategory);
            } else {
                super.onBackPressed();
            }
        } else if (setChangesToNote(currentNote, note.getDateTime(), true)) {
            if (!currentNote.equals(previousNote)) {
                showSaveNoteDialog(currentNote, false, startedFromCategory);
            } else {
                super.onBackPressed();
            }
        }
    }
}

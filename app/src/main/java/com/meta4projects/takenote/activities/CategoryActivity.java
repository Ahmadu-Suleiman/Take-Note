package com.meta4projects.takenote.activities;

import static com.meta4projects.takenote.fragments.CategoriesFragment.CATEGORY_NAME_EXTRA;
import static com.meta4projects.takenote.others.Utils.categoryNames;
import static com.meta4projects.takenote.others.Utils.getDialogView;
import static com.meta4projects.takenote.others.Utils.loadNativeAd;
import static com.meta4projects.takenote.others.Utils.showToast;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class CategoryActivity extends FullscreenActivity {

    public static final String CATEGORY_ADD_NEW_NOTE_EXTRA = "com.meta4projects.takenote.CATEGORY_ADD_NOTE";
    public static final String CATEGORY_EDIT_NOTE_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_EDIT_NOTE";
    public static final String CATEGORY_EDIT_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_EDIT_NOTE_ID";
    public static final String STARTED_FROM_CATEGORY_EXTRA = "com.meta4projects.takenote.activities.STARTED_FROM_CATEGORY_NOTE_ID";
    public static final String CATEGORY_NAME_EXTRA_ACTIVITY = "com.meta4projects.takenote.activities.CATEGORY_NAME";
    private final List<Note> categoryNotesList = new ArrayList<>();

    private RecyclerView recyclerViewCategoryNotes;
    private TextView textViewEmptyNoteCategory;
    private NotesAdapter categoryNotesAdapter;

    private ActivityResultLauncher<Intent> categoryAddNewNoteLauncher;
    private ActivityResultLauncher<Intent> categoryEditNewNoteLauncher;

    private int clickedItemPosition;
    private TextView textViewCategoryName;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        categoryName = getIntent().getStringExtra(CATEGORY_NAME_EXTRA);

        textViewEmptyNoteCategory = findViewById(R.id.text_empty_note_in_category);
        TemplateView templateView = findViewById(R.id.note_native_ad_category).findViewById(R.id.native_ad);
        loadNativeAd(this, templateView, getString(R.string.native_category_unit_id));

        textViewCategoryName = findViewById(R.id.textView_categoryName);
        textViewCategoryName.setText(categoryName);

        recyclerViewCategoryNotes = findViewById(R.id.recyclerViewCategoryNotes);
        categoryNotesAdapter = new NotesAdapter(categoryNotesList, this);
        categoryNotesAdapter.setListener((position, note) -> {
            Intent intent = new Intent(CategoryActivity.this, NoteActivity.class);
            intent.putExtra(CATEGORY_EDIT_NOTE_EXTRA, true);
            intent.putExtra(STARTED_FROM_CATEGORY_EXTRA, true);
            intent.putExtra(CATEGORY_EDIT_NOTE_ID_EXTRA, note.getNoteId());
            intent.putExtra(CATEGORY_NAME_EXTRA_ACTIVITY, categoryName);

            clickedItemPosition = position;

            categoryEditNewNoteLauncher.launch(intent);
        });
        recyclerViewCategoryNotes.setAdapter(categoryNotesAdapter);
        recyclerViewCategoryNotes.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton buttonAddCategoryNote = findViewById(R.id.imageView_add_category_note);
        buttonAddCategoryNote.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, NoteActivity.class);
            intent.putExtra(CATEGORY_ADD_NEW_NOTE_EXTRA, true);
            intent.putExtra(STARTED_FROM_CATEGORY_EXTRA, true);
            intent.putExtra(CATEGORY_NAME_EXTRA_ACTIVITY, categoryName);

            categoryAddNewNoteLauncher.launch(intent);
        });

        ConstraintLayout layoutRenameCategory = findViewById(R.id.layoutChangeCategoryName);
        ConstraintLayout layoutDeleteCategory = findViewById(R.id.layoutDeleteCategory);
        layoutRenameCategory.setOnClickListener(v -> showRenameCategoryDialog());
        layoutDeleteCategory.setOnClickListener(v -> showDeleteCategoryDialog());

        setCategoryNotes();
        registerLaunchers();
    }

    private void setCategoryNotes() {
        AsyncTask.execute(() -> {
            final List<Note> notesInCategory = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getNotesInCategory(categoryName);

            runOnUiThread(() -> {
                categoryNotesList.addAll(notesInCategory);
                categoryNotesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(notesInCategory);
            });
        });
    }

    private void onAddCategoryNote() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getAllNotes();

            runOnUiThread(() -> {
                categoryNotesList.add(0, notes.get(0));
                categoryNotesAdapter.notifyItemInserted(0);
                recyclerViewCategoryNotes.smoothScrollToPosition(0);
                setEmptyViewsNote(notes);
                showToast("note added successfully!", CategoryActivity.this);
            });
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.isEmpty()) textViewEmptyNoteCategory.setVisibility(View.VISIBLE);
        else textViewEmptyNoteCategory.setVisibility(View.GONE);
    }

    private void registerLaunchers() {
        categoryAddNewNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) {
                boolean isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false);
                boolean categoryNameChanged = data.getBooleanExtra(NoteActivity.CATEGORY_NAME_CHANGED_EXTRA, false);

                if (isMovedToTrash || categoryNameChanged) onMoveCategoryNote();
                else onAddCategoryNote();
            }
        });
        categoryEditNewNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) onEditCategoryNote(data);
        });
    }

    private void onEditCategoryNote(final Intent data) {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getNotesInCategory(categoryName);

            runOnUiThread(() -> {
                boolean savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false);
                categoryNotesList.clear();
                categoryNotesList.addAll(notes);
                categoryNotesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(notes);
                recyclerViewCategoryNotes.smoothScrollToPosition(0);

                if (savedChanges) showToast("note saved successfully!", CategoryActivity.this);
                else showToast("note edited successfully!", CategoryActivity.this);
            });
        });
    }

    private void onMoveCategoryNote() {
        categoryNotesList.remove(clickedItemPosition);
        categoryNotesAdapter.notifyItemRemoved(clickedItemPosition);
        showToast("note moved successfully!", CategoryActivity.this);
        setEmptyViewsNote(categoryNotesList);
    }

    private void showRenameCategoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_rename_category, findViewById(R.id.layout_rename_category_dialog), false);
        final AlertDialog dialogRenameCategory = getDialogView(this, view);

        final EditText editTextCategory = view.findViewById(R.id.input_rename_category);
        editTextCategory.setText(categoryName);

        view.findViewById(R.id.text_rename_category).setOnClickListener(v -> {
            final String newCategoryName = editTextCategory.getText().toString().trim();
            boolean nameExists = false;

            if (categoryName.isEmpty()) showToast("empty!", CategoryActivity.this);
            else {
                for (String name : categoryNames)
                    if (newCategoryName.equalsIgnoreCase(name)) {
                        nameExists = true;
                        break;
                    }

                if (nameExists) showToast("category name already exists!", CategoryActivity.this);
                else {
                    dialogRenameCategory.dismiss();
                    AsyncTask.execute(() -> {
                        final Category category = NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().getCategory(categoryName);
                        category.setName(newCategoryName);

                        NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().updateCategory(category);

                        updateNotesInCategory(categoryName, newCategoryName);
                        categoryName = newCategoryName;

                        runOnUiThread(() -> {
                            textViewCategoryName.setText(newCategoryName);
                            showToast("category renamed successfully!", CategoryActivity.this);
                        });
                    });
                }
            }
        });

        view.findViewById(R.id.text_cancel_rename_category).setOnClickListener(v -> dialogRenameCategory.dismiss());
        dialogRenameCategory.show();
    }

    private void showDeleteCategoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_category, findViewById(R.id.layout_delete_category_dialog), false);
        final AlertDialog dialogDeleteCategory = getDialogView(this, view);

        view.findViewById(R.id.text_delete_category).setOnClickListener(v -> {
            dialogDeleteCategory.dismiss();
            AsyncTask.execute(() -> {
                Category category = NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().getCategory(categoryName);
                NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().deleteCategory(category);
                updateNotesInCategoryDelete(categoryName);

                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    showToast("category deleted successfully!", CategoryActivity.this);
                    finish();
                });
            });
        });

        view.findViewById(R.id.text_cancel_delete_category).setOnClickListener(v -> dialogDeleteCategory.dismiss());
        dialogDeleteCategory.show();
    }

    private void updateNotesInCategory(final String previousName, final String newName) {
        List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getAllNotes();
        for (Note note : notes)
            if (previousName.equalsIgnoreCase(note.getCategoryName()))
                note.setCategoryName(newName);
        NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNotes(notes);
    }

    private void updateNotesInCategoryDelete(String categoryName) {
        List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getAllNotes();
        for (Note note : notes)
            if (categoryName.equalsIgnoreCase(note.getCategoryName())) note.setCategoryName(null);
        NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNotes(notes);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }
}

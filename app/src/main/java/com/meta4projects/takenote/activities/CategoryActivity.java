package com.meta4projects.takenote.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.adapters.CategoriesAdapter;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.database.entities.Note;
import com.meta4projects.takenote.others.Util;

import java.util.ArrayList;
import java.util.List;

import static com.meta4projects.takenote.others.Util.categoryNames;
import static com.meta4projects.takenote.others.Util.showToast;

public class CategoryActivity extends AppCompatActivity {

    public static final int CATEGORY_ADD_NEW_NOTE_REQUEST_CODE = 1;
    public static final String CATEGORY_ADD_NEW_NOTE_EXTRA = "com.meta4projects.takenote.CATEGORY_ADD_NOTE";

    public static final int CATEGORY_EDIT_NOTE_REQUEST_CODE = 2;
    public static final String CATEGORY_EDIT_NOTE_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_EDIT_NOTE";
    public static final String CATEGORY_EDIT_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_EDIT_NOTE_ID";
    public static final String STARTED_FROM_CATEGORY_EXTRA = "com.meta4projects.takenote.activities.STARTED_FROM_CATEGORY_NOTE_ID";
    public static final String CATEGORY_NAME_EXTRA = "com.meta4projects.takenote.activities.CATEGORY_NAME";

    private RecyclerView recyclerViewCategoryNotes;
    private final List<Note> categoryNotesList = new ArrayList<>();
    private ImageView imageViewEmptyNoteCategory;
    private TextView textViewEmptyNoteCategory;
    private NotesAdapter categoryNotesAdapter;

    private int clickedItemPosition;

    private TextView textViewCategoryName;

    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryName = getIntent().getStringExtra(CategoriesAdapter.CATEGORY_NAME_EXTRA);

        textViewCategoryName = findViewById(R.id.textView_categoryName);
        recyclerViewCategoryNotes = findViewById(R.id.recyclerViewCategoryNotes);
        FloatingActionButton buttonAddCategoryNote = findViewById(R.id.imageView_add_category_note);
        ConstraintLayout layoutRenameCategory = findViewById(R.id.layoutChangeCategoryName);
        ConstraintLayout layoutDeleteCategory = findViewById(R.id.layoutDeleteCategory);

        imageViewEmptyNoteCategory = findViewById(R.id.empty_image_add_note_in_category);
        textViewEmptyNoteCategory = findViewById(R.id.text_empty_note_in_category);

        textViewCategoryName.setText(categoryName);

        categoryNotesAdapter = new NotesAdapter(categoryNotesList, this);
        recyclerViewCategoryNotes.setAdapter(categoryNotesAdapter);
        categoryNotesAdapter.setListener(new NotesAdapter.Listener() {
            @Override
            public void onClick(int position, Note note) {
                Intent intent = new Intent(CategoryActivity.this, NoteActivity.class);
                intent.putExtra(CATEGORY_EDIT_NOTE_EXTRA, true);
                intent.putExtra(STARTED_FROM_CATEGORY_EXTRA, true);
                intent.putExtra(CATEGORY_EDIT_NOTE_ID_EXTRA, note.getNoteId());
                intent.putExtra(CATEGORY_NAME_EXTRA, categoryName);

                clickedItemPosition = position;

                CategoryActivity.this.startActivityForResult(intent, CATEGORY_EDIT_NOTE_REQUEST_CODE);
            }
        });
        recyclerViewCategoryNotes.setLayoutManager(new LinearLayoutManager(this));

        buttonAddCategoryNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CategoryActivity.this, NoteActivity.class);
                intent.putExtra(CATEGORY_ADD_NEW_NOTE_EXTRA, true);
                intent.putExtra(STARTED_FROM_CATEGORY_EXTRA, true);
                intent.putExtra(CATEGORY_NAME_EXTRA, categoryName);

                startActivityForResult(intent, CATEGORY_ADD_NEW_NOTE_REQUEST_CODE);
            }
        });

        layoutRenameCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRenameCategoryDialog();
            }
        });

        layoutDeleteCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteCategoryDialog();
            }
        });

        setCategoryNotes();
    }

    private void setCategoryNotes() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notesInCategory = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getNotesInCategory(categoryName);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        categoryNotesList.addAll(notesInCategory);
                        categoryNotesAdapter.notifyDataSetChanged();
                        setEmptyViewsNote(notesInCategory);
                    }
                });
            }
        });
    }

    private void onAddCategoryNote() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getAllNotes();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        categoryNotesList.add(0, notes.get(0));
                        categoryNotesAdapter.notifyItemInserted(0);
                        recyclerViewCategoryNotes.smoothScrollToPosition(0);
                        setEmptyViewsNote(notes);
                        Util.showToast("note added successfully!", CategoryActivity.this);
                    }
                });
            }
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.size() == 0) {
            imageViewEmptyNoteCategory.setVisibility(View.VISIBLE);
            textViewEmptyNoteCategory.setVisibility(View.VISIBLE);
        } else {
            imageViewEmptyNoteCategory.setVisibility(View.GONE);
            textViewEmptyNoteCategory.setVisibility(View.GONE);
        }
    }

    private void onEditCategoryNote(final Intent data) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getNotesInCategory(categoryName);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false);

                        categoryNotesList.clear();
                        categoryNotesList.addAll(notes);
                        categoryNotesAdapter.notifyDataSetChanged();
                        setEmptyViewsNote(notes);
                        recyclerViewCategoryNotes.smoothScrollToPosition(0);

                        if (savedChanges) {
                            Util.showToast("note saved successfully!", CategoryActivity.this);
                        } else {
                            Util.showToast("note edited successfully!", CategoryActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void onMoveCategoryNote() {
        categoryNotesList.remove(clickedItemPosition);

        categoryNotesAdapter.notifyItemRemoved(clickedItemPosition);
        Util.showToast("note moved successfully!", CategoryActivity.this);
        setEmptyViewsNote(categoryNotesList);
    }

    private void showRenameCategoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_rename_category, (ViewGroup) findViewById(R.id.layout_rename_category_dialog), false);

        final AlertDialog dialogRenameCategory = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogRenameCategory.getWindow() != null) {
            dialogRenameCategory.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        final EditText editTextCategory = view.findViewById(R.id.input_rename_category);
        editTextCategory.setText(categoryName);

        view.findViewById(R.id.text_rename_category).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String newCategoryName = editTextCategory.getText().toString().trim();
                boolean nameExists = false;

                if (categoryName.isEmpty()) {
                    showToast("empty!", CategoryActivity.this);
                } else {

                    for (String name : categoryNames) {
                        if (newCategoryName.equalsIgnoreCase(name)) {
                            nameExists = true;
                            break;
                        }
                    }

                    if (nameExists) {
                        showToast("category name already exists!", CategoryActivity.this);
                    } else {
                        dialogRenameCategory.dismiss();
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                final Category category = NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().getCategory(categoryName);
                                category.setName(newCategoryName);

                                NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().updateCategory(category);

                                updateNotesInCategory(categoryName, newCategoryName);
                                categoryName = newCategoryName;

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textViewCategoryName.setText(newCategoryName);
                                        showToast("category renamed successfully!", CategoryActivity.this);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        });

        view.findViewById(R.id.text_cancel_rename_category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogRenameCategory.dismiss();
            }
        });

        dialogRenameCategory.show();
    }

    private void showDeleteCategoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_category, (ViewGroup) findViewById(R.id.layout_delete_category_dialog), false);

        final AlertDialog dialogDeleteCategory = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogDeleteCategory.getWindow() != null) {
            dialogDeleteCategory.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.text_delete_category).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogDeleteCategory.dismiss();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        Category category = NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().getCategory(categoryName);

                        NoteDatabase.getINSTANCE(getApplicationContext()).categoryDao().deleteCategory(category);
                        updateNotesInCategoryDelete(categoryName);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setResult(RESULT_OK);
                                showToast("category deleted successfully!", CategoryActivity.this);

                                finish();
                            }
                        });
                    }
                });
            }
        });

        view.findViewById(R.id.text_cancel_delete_category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDeleteCategory.dismiss();
            }
        });

        dialogDeleteCategory.show();
    }

    private void updateNotesInCategory(final String previousName, final String newName) {
        List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getAllNotes();

        for (Note note : notes) {
            if (previousName.equalsIgnoreCase(note.getCategoryName())) {
                note.setCategoryName(newName);
            }
        }

        NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNotes(notes);
    }

    private void updateNotesInCategoryDelete(String categoryName) {
        List<Note> notes = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getAllNotes();

        for (Note note : notes) {
            if (categoryName.equalsIgnoreCase(note.getCategoryName())) {
                note.setCategoryName(null);
            }
        }

        NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNotes(notes);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CATEGORY_EDIT_NOTE_REQUEST_CODE && data != null) {
                boolean isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false);
                boolean categoryNameChanged = data.getBooleanExtra(NoteActivity.CATEGORY_NAME_CHANGED_EXTRA, false);

                if (isMovedToTrash || categoryNameChanged) {
                    onMoveCategoryNote();
                } else {
                    onEditCategoryNote(data);
                }
            } else if (requestCode == CATEGORY_ADD_NEW_NOTE_REQUEST_CODE) {
                onAddCategoryNote();
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }
}

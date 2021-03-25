package com.meta4projects.takenote.fragments;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.NoteActivity;
import com.meta4projects.takenote.adapters.CategoriesAdapter;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.others.Util.categoryNames;
import static com.meta4projects.takenote.others.Util.showToast;
import static com.meta4projects.takenote.others.Util.updateAllCategories;

public class MainFragment extends Fragment {

    public static final int ADD_NEW_NOTE_REQUEST_CODE = 1;
    public static final String ADD_NEW_NOTE_EXTRA = "com.meta4projects.takenote.ADD_NOTE";

    public static final int EDIT_NOTE_REQUEST_CODE = 2;
    public static final String EDIT_NOTE_EXTRA = "com.meta4projects.takenote.activities.EDIT_NOTE";
    public static final String EDIT_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.EDIT_NOTE_ID";

    private RecyclerView recyclerViewAllNotes, recyclerViewAllCategories;

    private ImageView imageViewEmptyNote;
    private TextView textViewEmptyNote, textViewCategoryTitle;

    private NotesAdapter notesAdapter;
    private final List<Note> notesList = new ArrayList<>();

    private CategoriesAdapter allCategoryAdapter;
    private final List<Category> categoryList = new ArrayList<>();

    private int clickedNotePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        recyclerViewAllNotes = view.findViewById(R.id.recyclerViewAllNotes);
        recyclerViewAllCategories = view.findViewById(R.id.recyclerViewAllCategories);

        textViewCategoryTitle = view.findViewById(R.id.text_categories_main);

        imageViewEmptyNote = view.findViewById(R.id.empty_image_add_note_main);
        textViewEmptyNote = view.findViewById(R.id.text_empty_note_main);

        notesAdapter = new NotesAdapter(notesList, getActivity());
        recyclerViewAllNotes.setAdapter(notesAdapter);
        notesAdapter.setListener(new NotesAdapter.Listener() {
            @Override
            public void onClick(int position, Note note) {
                Intent intent = new Intent(getActivity(), NoteActivity.class);
                intent.putExtra(EDIT_NOTE_EXTRA, true);
                intent.putExtra(EDIT_NOTE_ID_EXTRA, note.getNoteId());

                clickedNotePosition = position;

                startActivityForResult(intent, EDIT_NOTE_REQUEST_CODE);
            }
        });
        recyclerViewAllNotes.setLayoutManager(new LinearLayoutManager(getActivity()));

        allCategoryAdapter = new CategoriesAdapter(categoryList, this);
        recyclerViewAllCategories.setAdapter(allCategoryAdapter);
        recyclerViewAllCategories.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        FloatingActionButton buttonAddNote = view.findViewById(R.id.button_add_new_note);
        buttonAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NoteActivity.class);
                intent.putExtra(ADD_NEW_NOTE_EXTRA, true);

                startActivityForResult(intent, ADD_NEW_NOTE_REQUEST_CODE);
            }
        });

        FloatingActionButton buttonAddCategory = view.findViewById(R.id.button_add_new_category);
        buttonAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        setNotes();
        setCategories();

        return view;
    }

    private void setNotes() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notes = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).noteDao().getAllNotes();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notesList.addAll(notes);
                        notesAdapter.notifyDataSetChanged();
                        setEmptyViewsNote(notes);
                    }
                });
            }
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.size() == 0) {
            imageViewEmptyNote.setVisibility(View.VISIBLE);
            textViewEmptyNote.setVisibility(View.VISIBLE);
        } else {
            imageViewEmptyNote.setVisibility(View.GONE);
            textViewEmptyNote.setVisibility(View.GONE);
        }
    }

    private void setEmptyViewsCategory(List<Category> categories) {
        if (categories.size() == 0) {
            textViewCategoryTitle.setVisibility(View.GONE);
            recyclerViewAllCategories.setVisibility(View.GONE);
        } else {
            textViewCategoryTitle.setVisibility(View.VISIBLE);
            recyclerViewAllCategories.setVisibility(View.VISIBLE);
        }
    }

    private void setCategories() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Category> categories = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).categoryDao().getAllCategories();
                updateAllCategories(categories, getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        categoryList.addAll(categories);
                        allCategoryAdapter.notifyDataSetChanged();
                        setEmptyViewsCategory(categories);
                    }
                });
            }
        });
    }

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_add_category, (ViewGroup) Objects.requireNonNull(getActivity()).findViewById(R.id.layout_add_category_dialog), false);

        final AlertDialog dialogAddCategory = new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();

        if (dialogAddCategory.getWindow() != null) {
            dialogAddCategory.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        final EditText editTextCategory = view.findViewById(R.id.input_add_category);

        view.findViewById(R.id.text_add_category).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String categoryName = editTextCategory.getText().toString().trim();
                boolean nameExists = false;

                for (String name : categoryNames) {
                    if (categoryName.equalsIgnoreCase(name)) {
                        nameExists = true;
                        break;
                    }
                }

                if (nameExists) {
                    showToast("category name already exists!", getActivity());
                } else {
                    dialogAddCategory.dismiss();
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            final Category category = new Category(categoryName);
                            NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).categoryDao().insertCategory(category);

                            final List<Category> categories = NoteDatabase.getINSTANCE(getActivity().getApplicationContext()).categoryDao().getAllCategories();
                            updateAllCategories(categories, getActivity());

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int lastIndex = categoryList.size();
                                    categoryList.add(lastIndex, category);
                                    allCategoryAdapter.notifyItemInserted(lastIndex);
                                    recyclerViewAllCategories.smoothScrollToPosition(lastIndex);
                                    setEmptyViewsCategory(categories);

                                    showToast("new category added successfully!", getActivity());
                                }
                            });
                        }
                    });
                }
            }
        });

        view.findViewById(R.id.text_cancel_category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogAddCategory.dismiss();
            }
        });

        dialogAddCategory.show();
    }

    private void onAddNote() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notes = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).noteDao().getAllNotes();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notesList.add(0, notes.get(0));
                        notesAdapter.notifyItemInserted(0);
                        recyclerViewAllNotes.smoothScrollToPosition(0);
                        showToast("note added successfully!", getActivity());
                        setEmptyViewsNote(notes);
                    }
                });
            }
        });
    }

    private void onEditNote(final Intent data) {
        updateAllNotes();

        boolean savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false);

        if (savedChanges) {
            showToast("note saved successfully!", getActivity());
        } else {
            showToast("note edited successfully!", getActivity());
        }
    }

    private void onMoveNote() {
        notesList.remove(clickedNotePosition);

        notesAdapter.notifyItemRemoved(clickedNotePosition);
        showToast("note moved successfully!", getActivity());
        setEmptyViewsNote(notesList);
    }

    private void updateAllNotes() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notes = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).noteDao().getAllNotes();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notesList.clear();
                        notesList.addAll(notes);
                        notesAdapter.notifyDataSetChanged();
                        setEmptyViewsNote(notes);
                        recyclerViewAllNotes.smoothScrollToPosition(0);
                    }
                });
            }
        });
    }

    private void updateCategories() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Category> categories = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).categoryDao().getAllCategories();
                updateAllCategories(categories, getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        categoryList.clear();
                        categoryList.addAll(categories);
                        allCategoryAdapter.notifyDataSetChanged();
                        setEmptyViewsCategory(categories);
                    }
                });
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ADD_NEW_NOTE_REQUEST_CODE) {
                onAddNote();
            } else if (requestCode == EDIT_NOTE_REQUEST_CODE && data != null) {
                boolean isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false);

                if (isMovedToTrash) {
                    onMoveNote();
                } else {
                    onEditNote(data);
                }
            } else if (requestCode == CategoriesAdapter.CATEGORY_REQUEST_CODE) {
                updateAllNotes();
            }

            updateCategories();
        }
    }
}

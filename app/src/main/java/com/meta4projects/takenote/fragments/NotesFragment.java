package com.meta4projects.takenote.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.NoteActivity;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.fragments.MainFragment.ADD_NEW_NOTE_EXTRA;
import static com.meta4projects.takenote.fragments.MainFragment.ADD_NEW_NOTE_REQUEST_CODE;
import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_EXTRA;
import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_ID_EXTRA;
import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_REQUEST_CODE;
import static com.meta4projects.takenote.others.Util.showToast;

public class NotesFragment extends Fragment {

    RecyclerView recyclerViewNotes;
    NotesAdapter notesAdapter;
    FloatingActionButton buttonAddNote;

    private ImageView imageViewEmptyNote;
    private TextView textViewEmptyNote;

    private final List<Note> notesList = new ArrayList<>();

    private int clickedNotePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerViewNotes = view.findViewById(R.id.recyclerViewNotes);
        buttonAddNote = view.findViewById(R.id.imageView_notes_add_note);

        imageViewEmptyNote = view.findViewById(R.id.empty_image_add_note);
        textViewEmptyNote = view.findViewById(R.id.text_empty_note);

        notesAdapter = new NotesAdapter(notesList, getActivity());
        recyclerViewNotes.setAdapter(notesAdapter);
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
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(getActivity()));

        buttonAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NoteActivity.class);
                intent.putExtra(ADD_NEW_NOTE_EXTRA, true);

                startActivityForResult(intent, ADD_NEW_NOTE_REQUEST_CODE);
            }
        });

        setNotes();

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
                        recyclerViewNotes.smoothScrollToPosition(0);
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
                        recyclerViewNotes.smoothScrollToPosition(0);
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
            }
        }
    }
}

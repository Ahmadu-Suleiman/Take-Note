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

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.TrashNoteActivity;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class NoteTrashFragment extends Fragment {

    public static final int TRASH_NOTE_REQUEST_CODE = 1;
    public static final String TRASH_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.TRASH_NOTE_ID";

    private ImageView imageViewEmptyTrashNote;
    private TextView textViewEmptyTrashNote;

    private NotesAdapter trashNotesAdapter;
    private final List<Note> trashNotesList = new ArrayList<>();

    private int clickedNotePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_trash, container, false);

        RecyclerView recyclerViewTrashNotes = view.findViewById(R.id.recyclerViewTrashNotes);

        imageViewEmptyTrashNote = view.findViewById(R.id.empty_image_add_note_trash);
        textViewEmptyTrashNote = view.findViewById(R.id.text_empty_note_main_trash);

        trashNotesAdapter = new NotesAdapter(trashNotesList, getActivity());
        recyclerViewTrashNotes.setAdapter(trashNotesAdapter);
        trashNotesAdapter.setListener(new NotesAdapter.Listener() {
            @Override
            public void onClick(int position, Note note) {
                Intent intent = new Intent(getActivity(), TrashNoteActivity.class);
                intent.putExtra(TRASH_NOTE_ID_EXTRA, note.getNoteId());

                clickedNotePosition = position;

                startActivityForResult(intent, TRASH_NOTE_REQUEST_CODE);
            }
        });
        recyclerViewTrashNotes.setLayoutManager(new LinearLayoutManager(getActivity()));

        setTrashNotes();

        return view;
    }

    private void setTrashNotes() {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> trashNotes = NoteDatabase.getINSTANCE(Objects.requireNonNull(getActivity()).getApplicationContext()).noteDao().getAllNotesInTrash();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        trashNotesList.addAll(trashNotes);
                        trashNotesAdapter.notifyDataSetChanged();
                        setEmptyViewsNote(trashNotes);
                    }
                });
            }
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.size() == 0) {
            imageViewEmptyTrashNote.setVisibility(View.VISIBLE);
            textViewEmptyTrashNote.setVisibility(View.VISIBLE);
        } else {
            imageViewEmptyTrashNote.setVisibility(View.GONE);
            textViewEmptyTrashNote.setVisibility(View.GONE);
        }
    }

    private void onRestoreOrDeleteNote() {
        trashNotesList.remove(clickedNotePosition);
        trashNotesAdapter.notifyItemRemoved(clickedNotePosition);
        setEmptyViewsNote(trashNotesList);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            onRestoreOrDeleteNote();
        }
    }
}

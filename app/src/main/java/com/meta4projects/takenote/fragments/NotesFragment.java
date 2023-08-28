package com.meta4projects.takenote.fragments;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.fragments.MainFragment.ADD_NEW_NOTE_EXTRA;
import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_EXTRA;
import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_ID_EXTRA;
import static com.meta4projects.takenote.others.Utils.loadNativeAd;
import static com.meta4projects.takenote.others.Utils.showToast;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.MainActivity;
import com.meta4projects.takenote.activities.NoteActivity;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment {

    private final List<Note> notesList = new ArrayList<>();
    private RecyclerView recyclerViewNotes;
    private NotesAdapter notesAdapter;
    private TextView textViewEmptyNote;
    private ActivityResultLauncher<Intent> addNewNoteLauncher;
    private ActivityResultLauncher<Intent> editNoteLauncher;
    private int clickedNotePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialiseLaunchers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        textViewEmptyNote = view.findViewById(R.id.text_empty_note);

        MainActivity mainActivity = (MainActivity) requireActivity();
        ImageView hamburger = view.findViewById(R.id.hamburger);
        ImageView search = view.findViewById(R.id.search);
        hamburger.setOnClickListener(v -> mainActivity.hamburgerClick());
        search.setOnClickListener(v -> mainActivity.searchClick());

        TemplateView templateView = view.findViewById(R.id.note_native_ad_notes).findViewById(R.id.native_ad);
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_all_notes_unit_id));

        recyclerViewNotes = view.findViewById(R.id.recyclerViewNotes);
        notesAdapter = new NotesAdapter(notesList, getActivity());
        notesAdapter.setListener((position, note) -> {
            Intent intent = new Intent(getActivity(), NoteActivity.class);
            intent.putExtra(EDIT_NOTE_EXTRA, true);
            intent.putExtra(EDIT_NOTE_ID_EXTRA, note.getNoteId());
            clickedNotePosition = position;
            editNoteLauncher.launch(intent);
        });
        recyclerViewNotes.setAdapter(notesAdapter);
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(getActivity()));

        FloatingActionButton buttonAddNote = view.findViewById(R.id.imageView_notes_add_note);
        buttonAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NoteActivity.class);
            intent.putExtra(ADD_NEW_NOTE_EXTRA, true);

            addNewNoteLauncher.launch(intent);
        });

        setNotes();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setNotes();
    }

    private void setNotes() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotes();

            requireActivity().runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(notes);
            });
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.isEmpty()) textViewEmptyNote.setVisibility(View.VISIBLE);
        else textViewEmptyNote.setVisibility(View.GONE);
    }

    private void initialiseLaunchers() {
        addNewNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) onAddNote();
        });

        editNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) {
                boolean isMovedToTrash = data.getBooleanExtra(NoteActivity.NOTE_MOVED_TO_TRASH_EXTRA, false);
                if (isMovedToTrash) onMoveNote();
                else onEditNote(data);
            }
        });
    }

    private void onAddNote() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotes();
            requireActivity().runOnUiThread(() -> {
                notesList.add(0, notes.get(0));
                notesAdapter.notifyItemInserted(0);
                recyclerViewNotes.smoothScrollToPosition(0);
                showToast("note added successfully!", getActivity());
                setEmptyViewsNote(notes);
            });
        });
    }

    private void onEditNote(final Intent data) {
        updateAllNotes();

        boolean savedChanges = data.getBooleanExtra(NoteActivity.SAVED_CHANGES_EXTRA, false);
        if (savedChanges) showToast("note saved successfully!", getActivity());
        else showToast("note edited successfully!", getActivity());
    }

    private void onMoveNote() {
        notesList.remove(clickedNotePosition);
        notesAdapter.notifyItemRemoved(clickedNotePosition);
        showToast("note moved successfully!", getActivity());
        setEmptyViewsNote(notesList);
    }

    private void updateAllNotes() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotes();
            requireActivity().runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(notes);
                recyclerViewNotes.smoothScrollToPosition(0);
            });
        });
    }
}

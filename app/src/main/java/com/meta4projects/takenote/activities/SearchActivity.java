package com.meta4projects.takenote.activities;

import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_EXTRA;
import static com.meta4projects.takenote.fragments.MainFragment.EDIT_NOTE_ID_EXTRA;
import static com.meta4projects.takenote.others.Utils.loadNativeAd;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SearchView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends FullscreenActivity {

    private final List<Note> notesList = new ArrayList<>();
    private NotesAdapter notesAdapter;
    private ActivityResultLauncher<Intent> editNoteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        SearchView searchView = findViewById(R.id.note_search);

        TemplateView templateView = findViewById(R.id.search_native_ad_notes).findViewById(R.id.native_ad);
        loadNativeAd(this, templateView, getString(R.string.native_search_note_unit_id));

        RecyclerView recyclerViewNotes = findViewById(R.id.search_recycler_view);
        notesAdapter = new NotesAdapter(notesList, this);
        notesAdapter.setListener((position, note) -> {
            Intent intent = new Intent(SearchActivity.this, NoteActivity.class);
            intent.putExtra(EDIT_NOTE_EXTRA, true);
            intent.putExtra(EDIT_NOTE_ID_EXTRA, note.getNoteId());

            editNoteLauncher.launch(intent);
        });
        recyclerViewNotes.setAdapter(notesAdapter);
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                notesAdapter.getFilter().filter(newText);
                return true;
            }
        });

        setNotes();
        initialiseLauncher();
    }

    private void initialiseLauncher() {
        editNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> setNotes());
    }

    private void setNotes() {
        AsyncTask.execute(() -> {
            final List<Note> notes = NoteDatabase.getINSTANCE(SearchActivity.this).noteDao().getAllNotes();

            runOnUiThread(() -> {
                notesList.clear();
                notesList.addAll(notes);
                notesAdapter.refresh();
                notesAdapter.notifyDataSetChanged();
            });
        });
    }
}
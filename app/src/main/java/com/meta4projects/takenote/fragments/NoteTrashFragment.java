package com.meta4projects.takenote.fragments;

import static android.app.Activity.RESULT_OK;
import static com.meta4projects.takenote.others.Utils.getDialogView;
import static com.meta4projects.takenote.others.Utils.loadNativeAd;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.TemplateView;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.activities.MainActivity;
import com.meta4projects.takenote.activities.TrashNoteActivity;
import com.meta4projects.takenote.adapters.NotesAdapter;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteTrashFragment extends Fragment {

    public static final String TRASH_NOTE_ID_EXTRA = "com.meta4projects.takenote.activities.TRASH_NOTE_ID";
    private final List<Note> trashNotesList = new ArrayList<>();

    private TextView textViewEmptyTrashNote;
    private NotesAdapter trashNotesAdapter;
    private ActivityResultLauncher<Intent> trashNoteLauncher;
    private int clickedNotePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trashNoteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) onRestoreOrDeleteNote();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_trash, container, false);

        MainActivity mainActivity = (MainActivity) requireActivity();
        ImageView hamburger = view.findViewById(R.id.hamburger);
        hamburger.setOnClickListener(v -> mainActivity.hamburgerClick());

        ImageView imageViewDeleteAll = view.findViewById(R.id.imageView_trash_delete_all);
        textViewEmptyTrashNote = view.findViewById(R.id.text_empty_note_main_trash);

        TemplateView templateView = view.findViewById(R.id.note_native_ad_trash).findViewById(R.id.native_ad);
        loadNativeAd(requireActivity(), templateView, getString(R.string.native_trash_note_unit_id));

        RecyclerView recyclerViewTrashNotes = view.findViewById(R.id.recyclerViewTrashNotes);
        trashNotesAdapter = new NotesAdapter(trashNotesList, getActivity());
        trashNotesAdapter.setListener((position, note) -> {
            Intent intent = new Intent(getActivity(), TrashNoteActivity.class);
            intent.putExtra(TRASH_NOTE_ID_EXTRA, note.getNoteId());
            clickedNotePosition = position;
            trashNoteLauncher.launch(intent);
        });
        recyclerViewTrashNotes.setAdapter(trashNotesAdapter);
        recyclerViewTrashNotes.setLayoutManager(new LinearLayoutManager(getActivity()));
        imageViewDeleteAll.setOnClickListener(v -> showDeleteAllTrashDialog());
        setTrashNotes();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTrashNotes();
    }

    private void setTrashNotes() {
        AsyncTask.execute(() -> {
            final List<Note> trashNotes = NoteDatabase.getINSTANCE(requireActivity().getApplicationContext()).noteDao().getAllNotesInTrash();
            requireActivity().runOnUiThread(() -> {
                trashNotesList.clear();
                trashNotesList.addAll(trashNotes);
                trashNotesAdapter.notifyDataSetChanged();
                setEmptyViewsNote(trashNotes);
            });
        });
    }

    private void setEmptyViewsNote(List<Note> notes) {
        if (notes.isEmpty()) textViewEmptyTrashNote.setVisibility(View.VISIBLE);
        else textViewEmptyTrashNote.setVisibility(View.GONE);
    }

    private void onRestoreOrDeleteNote() {
        trashNotesList.remove(clickedNotePosition);
        trashNotesAdapter.notifyItemRemoved(clickedNotePosition);
        setEmptyViewsNote(trashNotesList);
    }

    private void showDeleteAllTrashDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_delete_all_trash_notes, requireActivity().findViewById(R.id.layout_delete_all_trash_dialog), false);
        final AlertDialog dialogDeleteAllTrash = getDialogView(requireContext(), view);

        view.findViewById(R.id.text_delete_all_trash).setOnClickListener(v -> {
            dialogDeleteAllTrash.dismiss();
            AsyncTask.execute(() -> {
                NoteDatabase.getINSTANCE(requireContext()).noteDao().deleteAllTrashNotes();
                requireActivity().runOnUiThread(() -> {
                    trashNotesList.clear();
                    trashNotesAdapter.notifyDataSetChanged();
                    setEmptyViewsNote(trashNotesList);
                });
            });
        });

        view.findViewById(R.id.text_cancel_delete_all_trash).setOnClickListener(v -> dialogDeleteAllTrash.dismiss());
        dialogDeleteAllTrash.show();
    }
}

package com.meta4projects.takenote.activities;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.meta4projects.takenote.R;
import com.meta4projects.takenote.database.NoteDatabase;
import com.meta4projects.takenote.database.entities.Note;
import com.meta4projects.takenote.fragments.NoteTrashFragment;
import com.meta4projects.takenote.models.Subsection;
import com.meta4projects.takenote.others.Image;
import com.meta4projects.takenote.others.ViewType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TrashNoteActivity extends FullscreenActivity {

    private ScrollView scrollViewTrashNote;
    private TextView textViewDateTrash, textViewCategoryNameTrash;
    private TextView textViewTitleTrash, texViewFirstTrash;
    private ImageView imageViewNoteTrash;
    private ConstraintLayout layoutDeleteNote, layoutRestoreNote;
    private LinearLayout layoutNoteSubsectionTrash;
    private Note note;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_note);

        scrollViewTrashNote = findViewById(R.id.scrollView_trash_note);
        textViewDateTrash = findViewById(R.id.textView_date_trash);
        layoutRestoreNote = findViewById(R.id.layout_restore);
        layoutDeleteNote = findViewById(R.id.layout_delete_permanently);
        textViewCategoryNameTrash = findViewById(R.id.textView_categoryName_trash);
        textViewTitleTrash = findViewById(R.id.text_title_trash);
        texViewFirstTrash = findViewById(R.id.first_text_trash);
        imageViewNoteTrash = findViewById(R.id.imageView_note_trash);
        layoutNoteSubsectionTrash = findViewById(R.id.layout_note_subsection_trash);

        note = new Note();
        final int noteId = getIntent().getIntExtra(NoteTrashFragment.TRASH_NOTE_ID_EXTRA, -1);

        AsyncTask.execute(() -> {
            note = NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().getNote(noteId);

            runOnUiThread(() -> {
                textViewDateTrash.setText(new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date(note.getDateTime())));
                textViewTitleTrash.setText(note.getTitle());
                texViewFirstTrash.setText(note.getFirstEdittextInfo());
                texViewFirstTrash.requestFocus();

                Bitmap bitmap = Image.getScaledBitmap(note.getImagePath(), TrashNoteActivity.this);

                if (bitmap != null) {
                    Glide.with(this).asBitmap().load(bitmap).into(imageViewNoteTrash);
                    imageViewNoteTrash.setVisibility(View.VISIBLE);
                }

                ArrayList<String> viewTypes = note.getViewTypes();
                ArrayList<Subsection> subsections = note.getSubsections();
                ArrayList<String> edittextInfo = note.getEditTextInfo();

                setAndInitializeViews(viewTypes, subsections, edittextInfo);

                if (note.getColor() > 0) setColorToViews(note.getColor());

                categoryName = note.getCategoryName();
                if (categoryName != null) textViewCategoryNameTrash.setText(categoryName);
                else textViewCategoryNameTrash.setText(getString(R.string.none));

                layoutRestoreNote.setOnClickListener(v -> restoreNote(note));

                layoutDeleteNote.setOnClickListener(v -> showDeleteNoteDialog(note));
            });
        });
    }

    private void restoreNote(final Note note) {
        note.setInTrash(false);
        note.setDateTime(new Date().getTime());

        AsyncTask.execute(() -> {
            NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().updateNote(note);

            runOnUiThread(() -> {
                setResult(RESULT_OK);
                restoreNote(note);
                finish();
            });
        });
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

                    final TextView textView = newTextView();
                    String text = edittextInfo.get(i);

                    textView.setText(text);

                    layoutNoteSubsectionTrash.addView(subsectionView);
                    layoutNoteSubsectionTrash.addView(textView);
                }
            }

            //gives last view which is edittext focus
            layoutNoteSubsectionTrash.getChildAt(layoutNoteSubsectionTrash.getChildCount() - 1).requestFocus();
        }
    }

    private TextView newTextView() {
        return (TextView) getLayoutInflater().inflate(R.layout.layout_text_view, layoutNoteSubsectionTrash, false);
    }

    private View newLayoutSubsection() {
        return getLayoutInflater().inflate(R.layout.layout_subsection, layoutNoteSubsectionTrash, false);
    }

    private void setColorToViews(int color) {
        note.setColor(color);
        scrollViewTrashNote.setBackgroundResource(color);
        textViewTitleTrash.setBackgroundResource(color);
        texViewFirstTrash.setBackgroundResource(color);

        for (int i = 0, j = 1; i < layoutNoteSubsectionTrash.getChildCount() / 2; i = i + 1, j = j + 2) {
            TextView textView = (TextView) layoutNoteSubsectionTrash.getChildAt(j);
            textView.setBackgroundResource(color);
        }
    }

    private void showDeleteNoteDialog(final Note note) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, findViewById(R.id.layout_delete_note_dialog), false);

        final AlertDialog dialogDeleteNote = new AlertDialog.Builder(this).setView(view).create();

        if (dialogDeleteNote.getWindow() != null)
            dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        view.findViewById(R.id.text_delete_note).setOnClickListener(v -> {
            dialogDeleteNote.dismiss();

            AsyncTask.execute(() -> {
                NoteDatabase.getINSTANCE(getApplicationContext()).noteDao().deleteNote(note);

                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            });
        });

        view.findViewById(R.id.text_cancel_delete_note).setOnClickListener(v -> dialogDeleteNote.dismiss());

        dialogDeleteNote.show();
    }
}

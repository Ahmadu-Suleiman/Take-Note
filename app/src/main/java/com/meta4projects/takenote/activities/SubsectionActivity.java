package com.meta4projects.takenote.activities;

import static com.meta4projects.takenote.others.Utils.getDialogView;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.models.Subsection;
import com.meta4projects.takenote.others.Utils;

import java.util.Locale;
import java.util.Random;

public class SubsectionActivity extends FullscreenActivity {

    public static final String SUBSECTION_TITLE = "com.meta4projects.takenote.activities.subsection_title";
    public static final String SUBSECTION_BODY = "com.meta4projects.takenote.activities.subsection_body";
    public static final String SUBSECTION_COLOR = "com.meta4projects.takenote.activities.subsection_color";
    public static final String SUBSECTION_DELETE = "com.meta4projects.takenote.activities.subsection_delete";

    private EditText editTextTitle, editTextBody;
    private ConstraintLayout layoutSubsection;
    private TextToSpeech textToSpeech;
    private Subsection previousSubsection, subsection;
    private boolean isNewSubsection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsection);

        previousSubsection = new Subsection();
        subsection = new Subsection();

        isNewSubsection = true;

        layoutSubsection = findViewById(R.id.subsection_layout);
        editTextTitle = findViewById(R.id.edit_text_title);
        editTextBody = findViewById(R.id.edit_text_body);
        ImageView imageViewDone = findViewById(R.id.image_view_subsection_done);
        ImageView imageViewRead = findViewById(R.id.image_view_subsection_read);
        ConstraintLayout layoutDeleteSubsection = findViewById(R.id.constraintLayout_delete_subsection);
        ConstraintLayout layoutChooseColorSubsection = findViewById(R.id.constraintLayout_subsection_color);


        int[] subsectionColors = new int[]{R.color.layout_subsection_color_default, R.color.layout_subsection_color_green, R.color.layout_subsection_color_yellow, R.color.layout_subsection_color_blue, R.color.layout_subsection_color_pink, R.color.layout_subsection_color_purple};

        if (getIntent().getBooleanExtra(NoteActivity.SUBSECTION_EDIT_EXTRA, false)) {
            String title = getIntent().getStringExtra(NoteActivity.SUBSECTION_TITLE_EXTRA);
            String body = getIntent().getStringExtra(NoteActivity.SUBSECTION_BODY_EXTRA);
            int color = getIntent().getIntExtra(NoteActivity.SUBSECTION_COLOR_EXTRA, 0);

            isNewSubsection = false;

            if (color > 0) setColorToSubsectionViews(color);

            editTextTitle.setText(title);
            editTextBody.setText(body);

            subsection = new Subsection(title, body, color);
            setSubsection(previousSubsection, false);
        } else {
            layoutDeleteSubsection.setVisibility(View.INVISIBLE);
            subsection.setColor(subsectionColors[new Random().nextInt(subsectionColors.length)]);
            setColorToSubsectionViews(subsection.getColor());
        }

        layoutDeleteSubsection.setOnClickListener(v -> showDeleteSubsectionDialog());

        layoutChooseColorSubsection.setOnClickListener(v -> showChooseNoteColorDialog(subsection));

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) textToSpeech.setLanguage(Locale.getDefault());
        });

        imageViewDone.setOnClickListener(v -> {
            if (setSubsection(subsection, true)) done();
        });

        imageViewRead.setOnClickListener(v -> {
            String subsectionString = editTextTitle.getText().toString().concat("\n").concat(editTextBody.getText().toString());
            if (textToSpeech.isSpeaking()) textToSpeech.stop();
            else
                textToSpeech.speak(subsectionString, TextToSpeech.QUEUE_FLUSH, null, "subsectionId");
        });
    }

    private void done() {
        Intent data = new Intent();
        data.putExtra(SUBSECTION_TITLE, subsection.getTitle());
        data.putExtra(SUBSECTION_BODY, subsection.getBody());
        data.putExtra(SUBSECTION_COLOR, subsection.getColor());

        setResult(RESULT_OK, data);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) textToSpeech.shutdown();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getCurrentFocus() instanceof EditText) {
            EditText editText = (EditText) getCurrentFocus();

            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (editText.getSelectionEnd() < editText.getText().length())
                        editText.setSelection(editText.getSelectionStart() + 1);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (editText.getSelectionStart() > 0)
                        editText.setSelection(editText.getSelectionStart() - 1);
                    return true;
            }
            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showDeleteSubsectionDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_subsection, findViewById(R.id.layout_delete_subsection_dialog), false);
        final AlertDialog dialogDeleteSubsection = getDialogView(this, view);

        view.findViewById(R.id.text_delete_subsection).setOnClickListener(v -> {
            dialogDeleteSubsection.dismiss();

            Intent data = new Intent();
            data.putExtra(SUBSECTION_DELETE, true);
            setResult(RESULT_OK, data);
            finish();
        });

        view.findViewById(R.id.text_cancel_delete_subsection).setOnClickListener(v -> dialogDeleteSubsection.dismiss());
        dialogDeleteSubsection.show();
    }

    private boolean setSubsection(Subsection subsection, boolean showToasts) {
        String title = editTextTitle.getText().toString().trim();
        String body = editTextBody.getText().toString().trim();

        if (title.isEmpty()) {
            if (showToasts) Utils.showToast("title can not be empty!", SubsectionActivity.this);
            return false;
        } else if (body.isEmpty()) {
            if (showToasts) Utils.showToast("body can not be empty!", SubsectionActivity.this);
            return false;
        }

        subsection.setTitle(title);
        subsection.setBody(body);
        subsection.setColor(this.subsection.getColor());
        return true;
    }

    private void showSaveNoteDialog(final Subsection subsection) {
        final View view = LayoutInflater.from(this).inflate(R.layout.layout_save_subsection_changes, findViewById(R.id.layout_save_subsection_dialog), false);
        final AlertDialog dialogSaveSubsection = getDialogView(this, view);

        view.findViewById(R.id.text_save_subsection).setOnClickListener(v -> {
            dialogSaveSubsection.dismiss();

            Intent data = new Intent();
            data.putExtra(SUBSECTION_TITLE, subsection.getTitle());
            data.putExtra(SUBSECTION_BODY, subsection.getBody());
            data.putExtra(SUBSECTION_COLOR, subsection.getColor());

            setResult(RESULT_OK, data);
            finish();
        });

        view.findViewById(R.id.text_no_save_subsection).setOnClickListener(v -> {
            dialogSaveSubsection.dismiss();

            setResult(RESULT_CANCELED);
            finish();
        });

        dialogSaveSubsection.show();
    }

    private void showChooseNoteColorDialog(final Subsection subsection) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_choose_subsection_color, findViewById(R.id.layout_choose_subsection_color_dialog), false);
        final AlertDialog dialogChooseSubsectionColor = getDialogView(this, view);

        view.findViewById(R.id.view_subsection_color_default).setOnClickListener(v -> {
            dialogChooseSubsectionColor.dismiss();

            subsection.setColor(R.color.layout_subsection_color_default);
            setColorToSubsectionViews(R.color.layout_subsection_color_default);
        });
        view.findViewById(R.id.view_subsection_color_blue).setOnClickListener(v -> {
            dialogChooseSubsectionColor.dismiss();

            subsection.setColor(R.color.layout_subsection_color_blue);
            setColorToSubsectionViews(R.color.layout_subsection_color_blue);
        });
        view.findViewById(R.id.view_subsection_color_green).setOnClickListener(v -> {
            dialogChooseSubsectionColor.dismiss();

            subsection.setColor(R.color.layout_subsection_color_green);
            setColorToSubsectionViews(R.color.layout_subsection_color_green);
        });
        view.findViewById(R.id.view_subsection_color_pink).setOnClickListener(v -> {
            dialogChooseSubsectionColor.dismiss();

            subsection.setColor(R.color.layout_subsection_color_pink);
            setColorToSubsectionViews(R.color.layout_subsection_color_pink);
        });
        view.findViewById(R.id.view_subsection_color_purple).setOnClickListener(v -> {
            dialogChooseSubsectionColor.dismiss();

            subsection.setColor(R.color.layout_subsection_color_purple);
            setColorToSubsectionViews(R.color.layout_subsection_color_purple);
        });
        view.findViewById(R.id.view_subsection_color_yellow).setOnClickListener(v -> {
            dialogChooseSubsectionColor.dismiss();

            subsection.setColor(R.color.layout_subsection_color_yellow);
            setColorToSubsectionViews(R.color.layout_subsection_color_yellow);
        });

        dialogChooseSubsectionColor.show();
    }

    private void setColorToSubsectionViews(int color) {
        layoutSubsection.setBackgroundResource(color);
        editTextTitle.setBackgroundResource(color);
        editTextBody.setBackgroundResource(color);
    }

    @Override
    public void onBackPressed() {
        Subsection currentSubsection = new Subsection();

        if (isNewSubsection) {
            setSubsection(currentSubsection, false);
            if (!currentSubsection.equals(previousSubsection))
                showSaveNoteDialog(currentSubsection);
            else super.onBackPressed();
        } else if (setSubsection(currentSubsection, true)) {
            if (!currentSubsection.equals(previousSubsection))
                showSaveNoteDialog(currentSubsection);
            else super.onBackPressed();
        }
    }
}

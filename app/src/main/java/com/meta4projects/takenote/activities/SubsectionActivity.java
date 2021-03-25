package com.meta4projects.takenote.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.meta4projects.takenote.R;
import com.meta4projects.takenote.models.Subsection;
import com.meta4projects.takenote.others.Util;

import java.util.Random;

public class SubsectionActivity extends AppCompatActivity {

    public static final String SUBSECTION_TITLE = "com.meta4projects.takenote.activities.subsection_title";
    public static final String SUBSECTION_BODY = "com.meta4projects.takenote.activities.subsection_body";
    public static final String SUBSECTION_COLOR = "com.meta4projects.takenote.activities.subsection_color";

    public static final String SUBSECTION_DELETE = "com.meta4projects.takenote.activities.subsection_delete";

    private EditText editTextTitle, editTextBody;
    private ConstraintLayout layoutSubsection;

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
        ConstraintLayout layoutDeleteSubsection = findViewById(R.id.constraintLayout_delete_subsection);
        ConstraintLayout layoutChooseColorSubsection = findViewById(R.id.constraintLayout_subsection_color);


        int[] subsectionColors = new int[]{R.color.layout_subsection_color_default, R.color.layout_subsection_color_green,
                R.color.layout_subsection_color_yellow, R.color.layout_subsection_color_blue,
                R.color.layout_subsection_color_pink, R.color.layout_subsection_color_purple};

        if (getIntent().getBooleanExtra(NoteActivity.SUBSECTION_EDIT_EXTRA, false)) {
            String title = getIntent().getStringExtra(NoteActivity.SUBSECTION_TITLE_EXTRA);
            String body = getIntent().getStringExtra(NoteActivity.SUBSECTION_BODY_EXTRA);
            int color = getIntent().getIntExtra(NoteActivity.SUBSECTION_COLOR_EXTRA, 0);

            isNewSubsection = false;

            if (color > 0) {
                setColorToSubsectionViews(color);
            }

            editTextTitle.setText(title);
            editTextBody.setText(body);

            subsection = new Subsection(title, body, color);

            setSubsection(previousSubsection, false);
        } else {
            layoutDeleteSubsection.setVisibility(View.INVISIBLE);

            subsection.setColor(subsectionColors[new Random().nextInt(subsectionColors.length)]);
            setColorToSubsectionViews(subsection.getColor());
            setColorToSubsectionViews(subsection.getColor());
        }

        layoutDeleteSubsection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteSubsectionDialog();
            }
        });

        layoutChooseColorSubsection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooseNoteColorDialog(subsection);
            }
        });

        imageViewDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (setSubsection(subsection, true)) {
                    Intent data = new Intent();
                    data.putExtra(SUBSECTION_TITLE, subsection.getTitle());
                    data.putExtra(SUBSECTION_BODY, subsection.getBody());
                    data.putExtra(SUBSECTION_COLOR, subsection.getColor());

                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });
    }

    private void showDeleteSubsectionDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_subsection, (ViewGroup) findViewById(R.id.layout_delete_subsection_dialog), false);

        final AlertDialog dialogDeleteSubsection = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogDeleteSubsection.getWindow() != null) {
            dialogDeleteSubsection.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.text_delete_subsection).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogDeleteSubsection.dismiss();

                Intent data = new Intent();
                data.putExtra(SUBSECTION_DELETE, true);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        view.findViewById(R.id.text_cancel_delete_subsection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDeleteSubsection.dismiss();
            }
        });

        dialogDeleteSubsection.show();
    }

    private boolean setSubsection(Subsection subsection, boolean showToasts) {
        String title = editTextTitle.getText().toString().trim();
        String body = editTextBody.getText().toString().trim();

        if (title.isEmpty()) {
            if (showToasts) {
                Util.showToast("title can't be empty!", SubsectionActivity.this);
            }
            return false;
        } else if (body.isEmpty()) {
            if (showToasts) {
                Util.showToast("body can't be empty!", SubsectionActivity.this);
            }
            return false;
        }
        subsection.setTitle(title);
        subsection.setBody(body);
        subsection.setColor(this.subsection.getColor());

        return true;
    }

    private void showSaveNoteDialog(final Subsection subsection) {
        final View view = LayoutInflater.from(this).inflate(R.layout.layout_save_subsection_changes, (ViewGroup) findViewById(R.id.layout_save_subsection_dialog), false);

        final AlertDialog dialogSaveSubsection = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogSaveSubsection.getWindow() != null) {
            dialogSaveSubsection.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.text_save_subsection).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialogSaveSubsection.dismiss();

                Intent data = new Intent();
                data.putExtra(SUBSECTION_TITLE, subsection.getTitle());
                data.putExtra(SUBSECTION_BODY, subsection.getBody());
                data.putExtra(SUBSECTION_COLOR, subsection.getColor());

                setResult(RESULT_OK, data);
                finish();
            }
        });

        view.findViewById(R.id.text_no_save_subsection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSaveSubsection.dismiss();

                setResult(RESULT_CANCELED);
                finish();
            }
        });

        dialogSaveSubsection.show();
    }

    private void showChooseNoteColorDialog(final Subsection subsection) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_choose_subsection_color, (ViewGroup) findViewById(R.id.layout_choose_subsection_color_dialog), false);

        final AlertDialog dialogChooseSubsectionColor = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialogChooseSubsectionColor.getWindow() != null) {
            dialogChooseSubsectionColor.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.view_subsection_color_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseSubsectionColor.dismiss();

                subsection.setColor(R.color.layout_subsection_color_default);
                setColorToSubsectionViews(R.color.layout_subsection_color_default);
            }
        });
        view.findViewById(R.id.view_subsection_color_blue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseSubsectionColor.dismiss();

                subsection.setColor(R.color.layout_subsection_color_blue);
                setColorToSubsectionViews(R.color.layout_subsection_color_blue);
            }
        });
        view.findViewById(R.id.view_subsection_color_green).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseSubsectionColor.dismiss();

                subsection.setColor(R.color.layout_subsection_color_green);
                setColorToSubsectionViews(R.color.layout_subsection_color_green);
            }
        });
        view.findViewById(R.id.view_subsection_color_pink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseSubsectionColor.dismiss();

                subsection.setColor(R.color.layout_subsection_color_pink);
                setColorToSubsectionViews(R.color.layout_subsection_color_pink);
            }
        });
        view.findViewById(R.id.view_subsection_color_purple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseSubsectionColor.dismiss();

                subsection.setColor(R.color.layout_subsection_color_purple);
                setColorToSubsectionViews(R.color.layout_subsection_color_purple);
            }
        });
        view.findViewById(R.id.view_subsection_color_yellow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChooseSubsectionColor.dismiss();

                subsection.setColor(R.color.layout_subsection_color_yellow);
                setColorToSubsectionViews(R.color.layout_subsection_color_yellow);
            }
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
            if (!currentSubsection.equals(previousSubsection)) {
                showSaveNoteDialog(currentSubsection);
            } else {
                super.onBackPressed();
            }
        } else if (setSubsection(currentSubsection, true)) {
            if (!currentSubsection.equals(previousSubsection)) {
                showSaveNoteDialog(currentSubsection);
            } else {
                super.onBackPressed();
            }
        }
    }
}

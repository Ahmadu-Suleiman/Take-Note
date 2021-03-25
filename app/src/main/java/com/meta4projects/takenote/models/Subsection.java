package com.meta4projects.takenote.models;

import java.util.Objects;

public class Subsection {

    private String title;
    private String body;
    private int color;

    public Subsection(String title, String body, int color) {
        this.title = title;
        this.body = body;
        this.color = color;
    }

    public Subsection() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subsection that = (Subsection) o;
        return color == that.color &&
                Objects.equals(title, that.title) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, body, color);
    }
}

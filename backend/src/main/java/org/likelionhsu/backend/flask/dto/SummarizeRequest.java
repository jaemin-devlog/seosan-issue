package org.likelionhsu.backend.flask.dto;

public class SummarizeRequest {
    private String text;

    public SummarizeRequest() {}

    public SummarizeRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
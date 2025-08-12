package org.likelionhsu.backend.flask.dto.response;

public class SummarizeResponse {
    private String summary;

    public SummarizeResponse() {}

    public SummarizeResponse(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
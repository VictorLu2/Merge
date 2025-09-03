package com.books.dto;

public class UploadResponse {
    private final String url;

    public UploadResponse(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}


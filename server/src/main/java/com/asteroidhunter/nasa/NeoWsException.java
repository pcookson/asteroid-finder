package com.asteroidhunter.nasa;

public class NeoWsException extends RuntimeException {

    private final int status;
    private final String bodySnippet;

    public NeoWsException(int status, String bodySnippet) {
        super("NeoWs request failed with status " + status
                + (bodySnippet == null || bodySnippet.isBlank() ? "" : ": " + bodySnippet));
        this.status = status;
        this.bodySnippet = bodySnippet;
    }

    public int getStatus() {
        return status;
    }

    public String getBodySnippet() {
        return bodySnippet;
    }
}

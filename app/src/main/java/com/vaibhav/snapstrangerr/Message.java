package com.vaibhav.snapstrangerr;

public class Message {
    private String text;
    private String sender;
    private long timestamp;

    // Required empty constructor for Firestore
    public Message() {}

    public Message(String text, String sender, long timestamp) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    // Getters
    public String getText() { return text; }
    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }
}

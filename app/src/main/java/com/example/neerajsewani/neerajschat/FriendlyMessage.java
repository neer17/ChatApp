package com.example.neerajsewani.neerajschat;

public class FriendlyMessage {
    String mUsername, mMessage, mUrl;

    public FriendlyMessage(){}
    public FriendlyMessage(String username, String message, String url){
        mUsername = username;
        mMessage = message;
        mUrl = url;
    }

    public String getmMessage() {
        return mMessage;
    }

    public String getmUrl() {
        return mUrl;
    }

    public String getmUsername() {
        return mUsername;
    }
}

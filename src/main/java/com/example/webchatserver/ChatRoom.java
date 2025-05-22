package com.example.webchatserver;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the data you may need to store about a Chat room
 * You may add more method or attributes as needed
 * **/
public class ChatRoom {
    // Attributes
    private String  code; //each user has an unique ID associate to their ws session and their username
    private Map<String, String> users = new HashMap<>(); // format: [ <userID, username> ]
    private String chatLog = "";

    // Constructors
    // when created the chat room has at least one user
    public ChatRoom(String code, String userID, String username){
        this.code = code;
        // when created the user has not entered their username yet
        this.users.put(userID, username);
    }

    // Setters
    /**
     * This method will add the new userID to the room if not exists, or it will add a new userID,name pair
     * **/
    public void setUserName(String userID, String name) {
        // update the name
        if(users.containsKey(userID)){
            users.remove(userID);
            users.put(userID, name);
        }else{ // add new user
            users.put(userID, name);
        }
    }

    public void setCode(String code) {
        this.code = code;
    }

    // Getters
    public String getChatLog() {
        return chatLog;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getUsers() {
        return users;
    }


    // Updates
    public void updateChatLog(String message) {
        chatLog += message + "\n";
    }

    /**
     * This method will remove a user from this room
     * **/
    public void removeUser(String userID){
        if(users.containsKey(userID)){
            users.remove(userID);
        }
    }

    // Helper methods
    public boolean inRoom(String userID){
        return users.containsKey(userID);
    }

    public boolean hasUser(String userId) {
        return users.containsKey(userId);
    }

    public Object getUserId() {
        return users.keySet().toArray()[0];
    }
}

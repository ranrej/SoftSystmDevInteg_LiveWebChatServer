package com.example.webchatserver;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    // Declare a static map to store all chat rooms, with room ID as key and ChatRoom object as value
    private static Map<String, ChatRoom> rooms = new HashMap<>(); //structure: <roomID, ChatRoom>

    // Declare a static map to store all users, with user ID as key and username as value
    private static Map<String, String> users = new HashMap<>(); //structure: <userID, username>
    private static Map<String, String> roomHistoryList = new HashMap<String, String>(); //structure: <roomID, history>

    // Declare a static map to store the sessions for each room
    private static Map<String, Set<Session>> roomSessions = new HashMap<>(); //structure: <roomID, Set<Session>>

    // This method is called when a new WebSocket connection is established
    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        // Get the username from the query parameters
        Map<String, List<String>> params = session.getRequestParameterMap();
        String username = params.get("username").get(0);
        String userId = params.get("userID").get(0);

        // Add the user to the users map
        if (!users.containsKey(userId)) {
            users.put(userId, username);
        }

        // Check if the room already exists in the rooms map
        ChatRoom chatRoom = new ChatRoom(roomID, userId, username);
        rooms.put(roomID, chatRoom);

        // Add the session to the set of sessions for the current room
        roomSessions.computeIfAbsent(roomID, k -> new HashSet<>()).add(session);
    }

    // This method is called when a WebSocket connection is closed
    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String roomId = session.getPathParameters().get("roomID");
        String userId = session.getRequestParameterMap().get("userID").get(0);

        // remove user from 'rooms'
        ChatRoom chatRoom = rooms.get(roomId);
        if (chatRoom != null) {
            if (users.containsKey(userId)) {
                chatRoom.removeUser(userId);
            }
        }

        // remove user from 'users'
        if (users.containsKey(userId)) {
            if (users.containsKey(userId)) {
                // Remove the user from the 'users' HashMap
                users.remove(userId);
            }
        }

        // printing the user that left the chat
        System.out.println("User left the chat: " + userId);

        // adding event to the history of the room -> this is taken care of in the 'handleMessage' method

//        // Remove the session from the set of sessions for the current room
//        Set<Session> sessions = roomSessions.get(roomId);
//        if (sessions != null) {
//            sessions.remove(session);
//        }
    }

    // This method is called when a message is received from a client
    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {

        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");

        String message = null;
        try {
            message = (String) jsonmsg.get("msg");
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }

        String roomID = null;
        try {
            roomID = ((String) jsonmsg.get("roomID")).trim();
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }

        String Username = null;
        try {
            Username = (String) jsonmsg.get("username");
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }

        String userID = null;
        try {
            userID = (String) jsonmsg.get("userID");
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }


        // user leaves chat room
        if (type.equals("userLeft")) {
            // save message
            String logHistory = roomHistoryList.get(roomID); // load log history
            String left_msg = "{\"type\": \"chat\", \"message\":\"(Server): " + Username + " has left the chat room\"}"; // what I want to print

            session.getBasicRemote().sendText(logHistory + left_msg); // what I want to print to chat log

            // Broadcast to the server to the rest of the users in the current room
            Set<Session> sessions = roomSessions.get(roomID);
            if (sessions != null) {
                String currentUserId = session.getRequestParameterMap().get("userID").get(0);
                for (Session peer : sessions) {
                    String peerUserId = peer.getRequestParameterMap().get("userID").get(0);
                    if (!peerUserId.equals(currentUserId)) {
                        peer.getBasicRemote().sendText(left_msg);
                        break;
                    }
                }
                sessions.remove(session); // Remove the session from the set of sessions for the current room
            }

            roomHistoryList.put(roomID, logHistory + left_msg); // save new with log history

            printRoomHistoryList();

            // close session
            close(session);
        }
        // user first joined the chat
        else if (type.equals("userJoined")) {
            // new messages to be added
            String join_msg = "{\"type\": \"chat\", \"message\":\"(Server): " + Username + " joined the chat room.\"}";
            String welcome_msg = "{\"type\": \"chat\", \"message\":\"(Server): Welcome, " + Username + "!\"}";


            // save message
            // if there is history
            if(roomHistoryList.containsKey(roomID)) {
                String logHistory = roomHistoryList.get(roomID);
                String chatLoad_msg = "{\"type\": \"chat\", \"message\":\"(Server): " + "Chat room history loaded\"}";

                // print history: only to be shown for the current user
                String[] historyprint = logHistory.split("(?<=})");
                for (String history : historyprint) {
                    session.getBasicRemote().sendText(history);
                }
                roomHistoryList.put(roomID, logHistory + "\n" + chatLoad_msg);
                logHistory = roomHistoryList.get(roomID); //reload
                roomHistoryList.put(roomID, logHistory + "\n" + join_msg);
                logHistory = roomHistoryList.get(roomID); //reload
                roomHistoryList.put(roomID, logHistory + "\n" + welcome_msg); // add to log history


                // show to current user
                session.getBasicRemote().sendText(chatLoad_msg);
                session.getBasicRemote().sendText(join_msg);
                session.getBasicRemote().sendText(welcome_msg);

                // Broadcast to the server to the rest of the users in the current room
                Set<Session> sessions = roomSessions.get(roomID);
                if (sessions != null) {
                    String currentUserId = session.getRequestParameterMap().get("userID").get(0);
                    for (Session peer : sessions) {
                        String peerUserId = peer.getRequestParameterMap().get("userID").get(0);
                        if (!peerUserId.equals(currentUserId)) {
                            peer.getBasicRemote().sendText(chatLoad_msg);
                            peer.getBasicRemote().sendText(join_msg);
                            peer.getBasicRemote().sendText(welcome_msg);
                            break;
                        }
                    }
                }
            }
            // chat room has no history
            else {
                String start_msg = "{\"type\": \"chat\", \"message\":\"(Server): " + " (This is the start of the chat)\"}";

                roomHistoryList.put(roomID, start_msg);
                String logHistory = roomHistoryList.get(roomID);
                roomHistoryList.put(roomID, logHistory + "\n" + join_msg);
                logHistory = roomHistoryList.get(roomID);
                roomHistoryList.put(roomID, logHistory + "\n" + welcome_msg);

                // only print to current user
                session.getBasicRemote().sendText(start_msg);
                session.getBasicRemote().sendText(join_msg);
                session.getBasicRemote().sendText(welcome_msg);

                // todo: Broadcast to the server to the rest of the users in the current room ??
                Set<Session> sessions = roomSessions.get(roomID);
                if (sessions != null) {
                    String currentUserId = session.getRequestParameterMap().get("userID").get(0);
                    for (Session peer : sessions) {
                        String peerUserId = peer.getRequestParameterMap().get("userID").get(0);
                        if (!peerUserId.equals(currentUserId)) {
                            session.getBasicRemote().sendText(start_msg);
                            session.getBasicRemote().sendText(join_msg);
                            session.getBasicRemote().sendText(welcome_msg);
                            break;
                        }
                    }
                }
            }

            printRoomHistoryList();
        }
        // user messages in chat room
        else if (type.equals("chat")) {
            // making sure the user can't send empty messages
            if (message != null && (message.length() > 0) && (message != "")){
                // print to log chat message
                String user_msg = "{\"type\": \"chat\", \"message\":\"" + Username + ": " + message + "\"}";
                session.getBasicRemote().sendText(user_msg);

                // Broadcast to the server to the rest of the users in the current room
                Set<Session> sessions = roomSessions.get(roomID);
                if (sessions != null) {
                    String currentUserId = session.getRequestParameterMap().get("userID").get(0);
                    for (Session peer : sessions) {
                        String peerUserId = peer.getRequestParameterMap().get("userID").get(0);
                        if (!peerUserId.equals(currentUserId)) {
                            peer.getBasicRemote().sendText(user_msg);
                            break;
                        }
                    }
                }

                // save message
                String logHistory = roomHistoryList.get(roomID);
                roomHistoryList.put(roomID, logHistory + "\n" + user_msg);
                printRoomHistoryList();
            }
        }
    }

    // This method returns the current time as a string in the format "HH:mm:ss"
    private String timestamp() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    // Method to get the room codes
    public static Set<String> getRoomCodes() {
        return rooms.keySet();
    }

    // print roomHistoryList content
    public static void printRoomHistoryList(){
        System.out.println("Room History List");
        for (Map.Entry<String, String> entry : roomHistoryList.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }
    }

}

package com.example.webchatserver;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

@WebServlet(name = "chatServlet", value = {"/chat-servlet", "/rooms-servlet"})
public class ChatServlet extends HttpServlet {
    // Attributes
    private String message;
    public static Set<String> rooms = new HashSet<>(); //static so this set is unique
    private Gson gson = new Gson();

    /**
     * Method generates unique room codes
     **/
    public String generatingRandomUpperAlphanumericString(int length) {
        String generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        // generating unique room code
        while (rooms.contains(generatedString)){
            generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        }
        rooms.add(generatedString);

        return generatedString;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();

        // generates new rooms: 'Create Room' button
        if (path.equals("/chat-servlet")) {
            response.setContentType("text/plain");

            // send the random code as the response's content
            PrintWriter out = response.getWriter();
            out.println(generatingRandomUpperAlphanumericString(5));
        }
        // extracts all rooms created: 'Refresh List of Rooms' button
        else if (path.equals("/rooms-servlet")) {
            Set<String> roomCodes = ChatServer.getRoomCodes();
            String roomCodesJsonString = this.gson.toJson(roomCodes);

            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print(roomCodesJsonString);
            out.flush();
        }
    }

    public void destroy() {
    }
}
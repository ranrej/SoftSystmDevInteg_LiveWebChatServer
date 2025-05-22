let ws;
let currentUsername = '';
let currentUserID = '';
let currentRoom = '';

// when new room wants to be created
function newRoom() {
    // calling the ChatServlet to retrieve a new room ID
    let callURL = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',
        },
    })
        .then(response => response.text())
        .then(response => {
            let btn = document.createElement("newroom");
            btn.textContent = response + " (new) ";

            btn.addEventListener('click', function() {
                // Check if the user has already joined the room
                currentRoom = response;
                enterRoom();
            });
            document.getElementById("roomsList").appendChild(btn);

            // debugging
            console.log("new room code: " + response);
        });
}

async function UserLeaves() {
    // make message input, send button and leave chat room disabled
    document.getElementById("leaveButton").disabled = true;
    document.getElementById("messageInput").disabled = true;
    document.getElementById("sendButton").disabled = true;

    try {
        // remove the user from the current room
        await sendPromise(ws, JSON.stringify({
            type: 'userLeft',
            username: currentUsername,
            roomID: currentRoom,
            userID: currentUserID
        }));

    } catch (error) {
        console.error('Error sending message:', error);
    }
}

function sendPromise(ws, message) {
    return new Promise((resolve, reject) => {
        ws.send(message, error => {
            if (error) { // if there's a error
                reject(error);
            } else { // else, there's no error
                resolve();
                // close the WebSocket connection
                ws.close();
            }
        });
    });
}

// when want to enter an existing room
function enterRoom() {
    // make message input, send button and leave chat room disabled
    document.getElementById("leaveButton").disabled = true;
    document.getElementById("messageInput").disabled = true;
    document.getElementById("sendButton").disabled = true;

    // reset values
    currentUsername = '';
    currentUserID = '';
    currentUserID = generateUserID();
    document.getElementById("logMessages").value = "";

    console.log("Entered room: " + currentRoom);

    // update the h1 tag to show the current room code
    document.getElementById("chatTitle").innerText = "Current Chat Room: " + currentRoom;

    // prompt user to enter a username
    createNewUser();
}

// create a new user and save user (this should be the first thing that happens)
function createNewUser() {
    document.getElementById("username").disabled = false;

    // prompt user to enter a username
    // if user presses "Enter" key
    document.getElementById("username").addEventListener("keyup", function (event) {
        if (event.key === "Enter") {
            currentUsername = '';
            currentUsername = document.getElementById("username").value;
            document.getElementById("username").disabled = true;
            document.getElementById("enterUserButton").disabled = true;

            // resume entering the room
            resumeEnterRoom();
        }
    });
    // or if user presses "Enter" button
    document.getElementById("enterUserButton").addEventListener('click', function() {
        currentUsername = '';
        currentUsername = document.getElementById("username").value;
        document.getElementById("username").disabled = true;
        document.getElementById("enterUserButton").disabled = true;

        // resume entering the room
        resumeEnterRoom();
    });
}

function resumeEnterRoom() {
    // make message input, send button and leave chat room visible
    document.getElementById("leaveButton").disabled = false;
    document.getElementById("messageInput").disabled = false;
    document.getElementById("sendButton").disabled = false;

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/" + currentRoom + "?username=" + encodeURIComponent(currentUsername) + "&userID=" + encodeURIComponent(currentUserID));

    // This function is executed when the WebSocket connection is successfully established
    ws.onopen = function() {
        // Send a message to the server in JSON format
        // The message indicates that a user has joined the chat room
        ws.send(JSON.stringify({
            type: 'userJoined', // The type of the message is 'userJoined'
            username: currentUsername,  // The username of the user who has joined
            userID: currentUserID, // The userID of the user who has joined
            roomID: currentRoom // The roomID of the room that the user has joined
        }));
    }

    // this part is what actually allows printing to be shown on the chat log
    // The 'onmessage' event is triggered when a message is received from the server through the WebSocket connection.
    ws.onmessage = function (event) {

        // This line logs the raw data received from the server to the console.
        console.log(event.data);

        // The data received from the server is in JSON format. This line parses the JSON string into a JavaScript object.
        let message = JSON.parse(event.data);

        // This line appends the received message to the 'logMessages' textarea in the HTML.
        // The 'timestamp()' function is called to get the current time, which is prepended to the message.
        // The '\n' at the end of the line adds a newline character, so each message appears on a new line in the textarea.
        document.getElementById("logMessages").value += "[" + timestamp() + "] " + message.message + "\n";
    }

    // if user presses "Enter" key
    document.getElementById("messageInput").addEventListener("keyup", function (event) {
        if (event.key === "Enter") {
            sendMessage();
        }
    });
    // or if user presses "Enter" button
    document.getElementById("sendButton").addEventListener('click', function() {
        sendMessage();
    });

    // debugging
    printDebug();
}

// todo: refresh list of created chats
function refreshChatList() {
    // Define the URL of the new endpoint
    let url = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/rooms-servlet";

    // Make an HTTP GET request to the endpoint
    fetch(url)
        .then(response => response.json()) // Parse the response as JSON
        .then(data => {
            // Get the roomsList element
            let roomsList = document.getElementById("roomsList");

            // Clear the current list
            roomsList.innerHTML = "";

            // Iterate over the list of rooms
            for (let room of data) {
                // Create a new button for each room
                let btn = document.createElement("newroom");
                btn.textContent = room;

                // Add an event listener to the button
                // When the button is clicked, it will call the enterRoom function with the room code
                btn.addEventListener('click', function() {
                    currentRoom = room;
                    enterRoom();
                });

                // Append the button to the roomsList element
                roomsList.appendChild(btn);
            }
        });
}

// when user enters a message, it should update the chat log both on UI and ChatRoom object
function sendMessage() {
    let message = document.getElementById("messageInput").value;
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({
            type: 'chat',
            msg: message,
            username: currentUsername,
            userID: currentUserID,
            roomID: currentRoom
        }));
        document.getElementById("messageInput").value = "";
    } else {
        console.error("WebSocket is not open: cannot send message");
    }
}

// returns the current time
function timestamp() {
    var d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}

// generates a unique user ID
function generateUserID() {
    let timestamp = Date.now();
    let timestampStr = timestamp.toString();
    return timestampStr.slice(-10);
}

// debugging
function printDebug() {
    console.log("-------------------------------");
    console.log("username: " + currentUsername);
    console.log("currentRoom: " + currentRoom);
    console.log("currentUserID: " + currentUserID);
    console.log("-------------------------------");
}
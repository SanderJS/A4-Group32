package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<no.ntnu.datakomm.chat.ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        // TODO Step 1: implement this method
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        if(connection == null) {
            try {
                connection = new Socket(host, port);
                OutputStream out = connection.getOutputStream();
                toServer = new PrintWriter(out, true);

                InputStream in = connection.getInputStream();
                fromServer = new BufferedReader(new InputStreamReader(in));

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() throws IOException {
        // TODO Step 4: implement this method
        // Hint: remember to check if connection is active
        if(isConnectionActive()){
            this.onDisconnect();
            connection.close();
            connection = null;
        }
        else{

        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // TODO Step 2: Implement this method
        // Hint: Remember to check if connection is active
        if (isConnectionActive()){
            toServer.println(cmd);
            return true;
        }
        else{
            return false;
        }

    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // TODO Step 2: implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        if(isConnectionActive()){
            sendCommand("msg " + message);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // TODO Step 3: implement this method
        // Hint: Reuse sendCommand() method
        if(isConnectionActive()){
            sendCommand("login "+ username);
        }
        else {

        }

    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // TODO Step 5: implement this method
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        if (isConnectionActive()) {
            sendCommand("users ");
        }
        else
        {

        }

    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // TODO Step 6: Implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        if (isConnectionActive()){
            sendCommand("privmsg " + recipient + " " + message);
            return true;
        }
        else{
            return false;
        }

    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
        if(isConnectionActive()) {
            sendCommand("help");
        }
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() throws IOException {
        // TODO Step 3: Implement this method
        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        String a = "";
        try {
            a = fromServer.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            connection.close();
        }
        return a;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            try {
                parseIncomingCommands();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() throws IOException {
        while (isConnectionActive()) {
            // TODO Step 3: Implement this method
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method
            String[] responseInParts = waitServerResponse().split(" ", 2);

            switch(responseInParts[0]){
                case "loginok":
                    onLoginResult(true,"");

                    break;
                case"loginerr":
                    onLoginResult(false,responseInParts[1]);
                    break;

                case"users":
                    onUsersList(responseInParts[1].split(" "));
                    break;

                case"privmsg":
                    String[] a = responseInParts[1].split(" ",2);
                    onMsgReceived(true, a[0], a[1]);
                    break;

                case"msg":
                    String[] b = responseInParts[1].split(" ",2);
                    onMsgReceived(false, b[0],b[1]);
                    break;
                case"msgerr":
                    String e = responseInParts[1];
                    onMsgError(e);
                    break;
                case"cmderr":
                    String d = responseInParts[1];
                    onCmdError(d);
                    break;
                case"supported":
                    String[] c = responseInParts[1].split(" ");
                    onSupported(c);
                    break;

            }


            // TODO Step 5: update this method, handle user-list response from the server
            // Hint: In Step 5 reuse onUserList() method

            // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
            // TODO Step 7: add support for incoming message errors (type: msgerr)
            // TODO Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners

            // TODO Step 8: add support for incoming supported command list (type: supported)

        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(no.ntnu.datakomm.chat.ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(no.ntnu.datakomm.chat.ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *  @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (no.ntnu.datakomm.chat.ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // TODO Step 4: Implement this method
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        for (no.ntnu.datakomm.chat.ChatListener n : listeners) {
            n.onDisconnect();
        }
    }


    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        // TODO Step 5: Implement this method
        for(no.ntnu.datakomm.chat.ChatListener l : listeners){
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        // TODO Step 7: Implement this method
        for (no.ntnu.datakomm.chat.ChatListener n : listeners) {
            TextMessage a = new TextMessage(sender, priv, text);
            n.onMessageReceived(a);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errorMsg) {
        // TODO Step 7: Implement this method
        for (no.ntnu.datakomm.chat.ChatListener n : listeners) {
            n.onMessageError(errorMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method
        for (no.ntnu.datakomm.chat.ChatListener n : listeners) {
            n.onCommandError(errMsg);
        }

    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
        for (no.ntnu.datakomm.chat.ChatListener n : listeners) {
            n.onSupportedCommands(commands);
        }
    }
}

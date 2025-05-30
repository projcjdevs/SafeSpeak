package server;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chat session between multiple users.
 * This class demonstrates encapsulation by hiding internal data structures
 * and providing controlled access through methods.
 */
public class Session {
    private String sessionId;           // Unique identifier for this session
    private Set<String> participants;   // Usernames of participants
    private List<Message> messages;     // Messages in this session
    private long creationTime;          // When the session was created
    
    /**
     * Creates a new session with a randomly generated ID
     */
    public Session() {
        this.sessionId = UUID.randomUUID().toString();
        this.participants = new HashSet<>();
        this.messages = new ArrayList<>();
        this.creationTime = System.currentTimeMillis();
    }
    
    /**
     * Adds a user to this session
     * @param username The username to add
     * @return true if the user was added, false if already present
     */
    public boolean addParticipant(String username) {
        return participants.add(username);
    }
    
    /**
     * Removes a user from this session
     * @param username The username to remove
     * @return true if the user was removed, false if not in session
     */
    public boolean removeParticipant(String username) {
        return participants.remove(username);
    }
    
    /**
     * Adds a message to this session's history
     * @param message The message to add
     */
    public void addMessage(Message message) {
        messages.add(message);
    }
    
    /**
     * Checks if a user is part of this session
     * @param username The username to check
     * @return true if user is in this session
     */
    public boolean hasParticipant(String username) {
        return participants.contains(username);
    }
    
    // Getters - part of encapsulation to control access to private fields
    public String getSessionId() {
        return sessionId;
    }
    
    public Set<String> getParticipants() {
        // Return a copy to prevent external modification
        return new HashSet<>(participants);
    }
    
    public int getParticipantCount() {
        return participants.size();
    }
    
    public long getCreationTime() {
        return creationTime;
    }
}
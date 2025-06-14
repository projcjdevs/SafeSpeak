package server;

import java.util.*;

public class Session {
    private String sessionId;
    private Set<String> participants;
    private List<Message> messages;
    private long creationTime;
    private String pendingInvite;  // Username with pending invitation
    
    /**
     * Creates a new chat session with a random UUID
     */
    public Session() {
        this.sessionId = UUID.randomUUID().toString();
        this.participants = new HashSet<>();
        this.messages = new ArrayList<>();
        this.creationTime = System.currentTimeMillis();
        this.pendingInvite = null;
    }
    
    /**
     * Adds a user to this session
     * @param username The username to add
     * @return true if user was added, false if already present
     */
    public boolean addParticipant(String username) {
        return participants.add(username);
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
    
    /**
     * Sets a pending invitation for a user
     * @param username The invited username
     */
    public void setInvitePending(String username) {
        this.pendingInvite = username;
    }
    
    /**
     * Checks if a user has a pending invitation
     * @param username The username to check
     * @return true if the user has a pending invitation
     */
    public boolean isInvitePending(String username) {
        return username.equals(pendingInvite);
    }
    
    /**
     * Clears any pending invitation
     */
    public void clearPendingInvite() {
        this.pendingInvite = null;
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
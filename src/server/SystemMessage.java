package server;

/**
 * Represents a system-generated message (not from a user).
 * This demonstrates polymorphism by providing a different implementation
 * of the Message interface than TextMessage.
 */
public class SystemMessage implements Message {
    private final String content;
    private final long timestamp;
    private final SystemMessageType type;
    
    public enum SystemMessageType {
        USER_JOINED,
        USER_LEFT,
        SESSION_CREATED,
        SESSION_ENDED
    }
    
    public SystemMessage(String content, SystemMessageType type) {
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }
    
    @Override
    public String getSender() {
        return "SYSTEM";
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public SystemMessageType getType() {
        return type;
    }
}
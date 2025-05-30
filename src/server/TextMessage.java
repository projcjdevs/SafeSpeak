package server;

/**
 * Represents a text message sent by a user.
 * This demonstrates inheritance by implementing the Message interface.
 */
public class TextMessage implements Message {
    private final String sender;
    private final String content;
    private final long timestamp;
    
    public TextMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getSender() {
        return sender;
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
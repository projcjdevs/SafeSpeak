package server;

/**
 * Interface representing a message in the chat system.
 * This demonstrates abstraction by defining what a message should do
 * without specifying implementation details.
 */
public interface Message {
    /**
     * Gets the sender of this message
     * @return The username of the sender
     */
    String getSender();
    
    /**
     * Gets the content of this message
     * @return The message content
     */
    String getContent();
    
    /**
     * Gets the timestamp when this message was created
     * @return The timestamp in milliseconds
     */
    long getTimestamp();
}
package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ChatUI extends JFrame {
    private MessageClient client;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private Map<String, JTextArea> sessionChats = new HashMap<>();
    private String currentSessionId = null;
    
    public ChatUI(MessageClient client) {
        this.client = client;
        client.setMessageHandler(this::handleServerMessage);
        setupUI();
    }
    
    private void setupUI() {
        setTitle("SafeSpeak Chat");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        
        // User list on the right
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    startChatWithUser(selectedUser);
                }
            }
        });
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        mainPanel.add(userScroll, BorderLayout.EAST);
        
        // Message input at bottom
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Add the main panel to the frame
        add(mainPanel);
        
        // Allow enter to send message
        messageField.addActionListener(e -> sendMessage());
        
        setVisible(true);
    }
    
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] parts = message.split(":", 4); // Limit to 4 parts
                
                if (parts.length < 2) return;
                
                String command = parts[0];
                
                if ("USERLIST".equals(command)) {
                    // Update user list
                    userListModel.clear();
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        for (String user : parts[1].split(",")) {
                            userListModel.addElement(user);
                        }
                    }
                }
                else if ("SESSION_CREATED".equals(command) && parts.length >= 3) {
                    String sessionId = parts[1];
                    String otherUser = parts[2];
                    currentSessionId = sessionId;
                    
                    // Create a chat session if it doesn't exist
                    if (!sessionChats.containsKey(sessionId)) {
                        sessionChats.put(sessionId, new JTextArea());
                    }
                    
                    // Update chat display
                    chatArea.setText("");
                    chatArea.append("--- Chat session with " + otherUser + " ---\n");
                    chatArea.append(sessionChats.get(sessionId).getText());
                }
                else if ("SESSION_INVITED".equals(command) && parts.length >= 3) {
                    String sessionId = parts[1];
                    String inviter = parts[2];
                    currentSessionId = sessionId;
                    
                    // Create a chat session if it doesn't exist
                    if (!sessionChats.containsKey(sessionId)) {
                        sessionChats.put(sessionId, new JTextArea());
                    }
                    
                    // Update chat display
                    chatArea.setText("");
                    chatArea.append("--- " + inviter + " invited you to chat ---\n");
                    chatArea.append(sessionChats.get(sessionId).getText());
                }
                else if ("MSG".equals(command) && parts.length >= 4) {
                    String sessionId = parts[1];
                    String sender = parts[2];
                    String content = parts[3];
                    
                    if (currentSessionId != null && currentSessionId.equals(sessionId)) {
                        // Add to chat
                        appendMessage(sender + ": " + content);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
            }
        });
    }
    
    private void appendMessage(String message) {
        if (currentSessionId != null) {
            JTextArea sessionChat = sessionChats.get(currentSessionId);
            if (sessionChat != null) {
                sessionChat.append(message + "\n");
                chatArea.append(message + "\n");
            }
        }
    }
    
    private void sendMessage() {
        if (currentSessionId == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to chat with first.");
            return;
        }
        
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            client.sendMessage(currentSessionId, message);
            appendMessage("You: " + message);
            messageField.setText("");
        }
    }
    
    private void startChatWithUser(String username) {
        client.createSession(username);
    }
}
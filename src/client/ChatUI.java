package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.Timer;

public class ChatUI extends JFrame {
    private MessageClient client;
    private String username;

    private JTabbedPane chatTabs;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JList<String> contactList;
    private DefaultListModel<String> contactListModel;
    private Map<String, JTextArea> chatAreas = new HashMap<>();
    private Map<String, JTextField> inputFields = new HashMap<>();
    private Map<String, String> sessions = new HashMap<>();

    public ChatUI(MessageClient client) {
        this.client = client;
        setupUI();

        client.setMessageHandler(this::processServerMessage);

        client.requestContactList();
        client.requestUserList();

        // Set up startup sequence to retry user list requests
        for (int i = 1; i <= 3; i++) {
            final int attempt = i;
            Timer initialTimer = new Timer(i * 1000, e -> {
                System.out.println("Startup request #" + attempt);
                client.requestUserList();
                // ALSO request contact list on each retry
                client.requestContactList();
            });
            initialTimer.setRepeats(false);
            initialTimer.start();
        }

        Timer refreshTimer = new Timer(5000, e -> client.requestUserList());
        refreshTimer.start();
    }

    private void setupUI() {
        setTitle("SafeSpeak Chat");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        chatTabs = new JTabbedPane();
        splitPane.setLeftComponent(chatTabs);

        JPanel rightPanel = new JPanel(new BorderLayout());

        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder("Online Users"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScroll = new JScrollPane(userList);
        userPanel.add(userListScroll, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Users");
        refreshButton.addActionListener(e -> {
            System.out.println("Manually refreshing user list");
            client.requestUserList();
        });
        userPanel.add(refreshButton, BorderLayout.SOUTH);

        JPanel contactPanel = new JPanel(new BorderLayout());
        contactPanel.setBorder(BorderFactory.createTitledBorder("Known Connections"));

        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        JScrollPane contactScroll = new JScrollPane(contactList);

        setupContactListInteraction();

        JPanel contactButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addContactButton = new JButton("Add Contact");
        addContactButton.addActionListener(e -> showContactSearchDialog());

        JButton refreshContactsButton = new JButton("Refresh Contacts");
        refreshContactsButton.addActionListener(e -> {
            System.out.println("Manually refreshing contacts");
            client.requestContactList();
        });

        contactButtonPanel.add(addContactButton);
        contactButtonPanel.add(refreshContactsButton);

        contactPanel.add(contactScroll, BorderLayout.CENTER);
        contactPanel.add(contactButtonPanel, BorderLayout.SOUTH);

        rightPanel.add(userPanel, BorderLayout.NORTH);
        rightPanel.add(contactPanel, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String recipient = userList.getSelectedValue();
                    if (recipient != null) {
                        client.createSession(recipient);
                    }
                }
            }
        });

        JPanel welcomePanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome to SafeSpeak! Select a user to start chatting.", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        chatTabs.addTab("Welcome", welcomePanel);

        setVisible(true);
    }

    private void setupContactListInteraction() {
        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String contactName = contactList.getSelectedValue();
                    if (contactName != null) {
                        boolean isOnline = false;
                        for (int i = 0; i < userListModel.size(); i++) {
                            if (userListModel.getElementAt(i).equals(contactName)) {
                                isOnline = true;
                                break;
                            }
                        }
                        if (isOnline) {
                            client.createSession(contactName);
                        } else {
                            JOptionPane.showMessageDialog(
                                ChatUI.this,
                                contactName + " is currently offline.",
                                "User Offline",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }
                }
            }
        });
    }

    private void processServerMessage(String message) {
        System.out.println("Processing message in UI: " + message);
        String[] parts = message.split(":");

        if (parts.length < 1) return;

        String command = parts[0];

        switch (command) {
            case "USERLIST":
                String userListStr = parts.length > 1 ? parts[1] : "";
                updateUserList(userListStr.isEmpty() ? new String[0] : userListStr.split(","));
                break;
            case "SESSION_CREATED":
                if (parts.length >= 3) {
                    String sessionId = parts[1];
                    String recipient = parts[2];
                    boolean pending = parts.length > 3 && parts[3].equals("PENDING");
                    handleSessionCreated(sessionId, recipient, pending);
                }
                break;
            case "SESSION_INVITATION":
                if (parts.length >= 3) {
                    String sessionId = parts[1];
                    String inviter = parts[2];
                    showInvitationDialog(sessionId, inviter);
                }
                break;
            case "SESSION_ACCEPTED":
                if (parts.length >= 3) {
                    String sessionId = parts[1];
                    String accepter = parts[2];
                    JOptionPane.showMessageDialog(this, accepter + " accepted your invitation!");
                    updateChatAreaWithSystemMessage(sessionId, accepter + " joined the conversation");
                }
                break;
            case "SESSION_REJECTED":
                if (parts.length >= 3) {
                    String sessionId = parts[1];
                    String rejecter = parts[2];
                    JOptionPane.showMessageDialog(this, rejecter + " declined your invitation.");
                    updateChatAreaWithSystemMessage(sessionId, rejecter + " declined the invitation");
                }
                break;
            case "MSG":
                if (parts.length >= 4) {
                    String sessionId = parts[1];
                    String sender = parts[2];
                    String content = parts[3];
                    displayMessage(sessionId, sender, content);
                }
                break;
            case "SEARCH_RESULTS":
                String searchResults = parts.length > 1 ? parts[1] : "";
                handleSearchResults(searchResults);
                break;
            case "CONTACT_LIST":
                String contactsStr = parts.length > 1 ? parts[1] : "";
                System.out.println("***** UI RECEIVED CONTACT_LIST: " + contactsStr + " *****");
                String[] contacts = contactsStr.isEmpty() ? new String[0] : contactsStr.split(",");
                updateContactList(contacts);
                break;
            case "CONTACT_ADDED":
                if (parts.length >= 2) {
                    String contact = parts[1];
                    JOptionPane.showMessageDialog(this, contact + " added to your contacts!");
                    client.requestContactList();
                }
                break;
            default:
        }
    }

    private void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            System.out.println("Updating user list with " + users.length + " users: " + 
                              Arrays.toString(users));
            
            for (String user : users) {
                // Don't show ourselves in the list
                if (!user.isEmpty()) {
                    if (user.equals(client.getUsername())) {
                        // Set our username and don't add to list
                        this.username = user;
                        setTitle("SafeSpeak - " + username);
                        System.out.println("Identified self as: " + username);
                    } else {
                        userListModel.addElement(user);
                        System.out.println("Added online user: " + user);
                    }
                }
            }
        });
    }

    private void handleSessionCreated(String sessionId, String recipient, boolean pending) {
        SwingUtilities.invokeLater(() -> {
            sessions.put(sessionId, recipient);
            createSessionTab(sessionId, recipient);
            String status = pending ? " (invitation pending)" : "";
            updateChatAreaWithSystemMessage(sessionId, "Session started with " + recipient + status);
        });
    }

    private void createSessionTab(String sessionId, String recipient) {
        if (chatAreas.containsKey(sessionId)) return;

        JPanel chatPanel = new JPanel(new BorderLayout());

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Send");

        ActionListener sendAction = e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                client.sendMessage(sessionId, message);
                displayMessage(sessionId, "You", message);
                inputField.setText("");
            }
        };

        inputField.addActionListener(sendAction);
        sendButton.addActionListener(sendAction);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        chatAreas.put(sessionId, chatArea);
        inputFields.put(sessionId, inputField);

        chatTabs.addTab(recipient, chatPanel);
        chatTabs.setSelectedComponent(chatPanel);
    }

    private void displayMessage(String sessionId, String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            JTextArea chatArea = chatAreas.get(sessionId);

            if (chatArea == null) {
                String recipient = sessions.getOrDefault(sessionId, sender);
                createSessionTab(sessionId, recipient);
                chatArea = chatAreas.get(sessionId);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());

            chatArea.append("[" + timestamp + "] " + sender + ": " + content + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateChatAreaWithSystemMessage(String sessionId, String message) {
        SwingUtilities.invokeLater(() -> {
            JTextArea chatArea = chatAreas.get(sessionId);
            if (chatArea != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timestamp = sdf.format(new Date());
                chatArea.append("[" + timestamp + "] * " + message + " *\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }

    private void showInvitationDialog(String sessionId, String inviter) {
        SwingUtilities.invokeLater(() -> {
            String message = inviter + " wants to start a conversation with you.";
            int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Chat Invitation",
                JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                client.acceptSessionInvitation(sessionId);
                createSessionTab(sessionId, inviter);
                sessions.put(sessionId, inviter);
                updateChatAreaWithSystemMessage(sessionId, "You accepted " + inviter + "'s invitation");
            } else {
                client.rejectSessionInvitation(sessionId);
            }
        });
    }

    private void showContactSearchDialog() {
        String email = JOptionPane.showInputDialog(
            this,
            "Enter email to search for contacts:",
            "Find Contacts",
            JOptionPane.QUESTION_MESSAGE
        );

        if (email != null && !email.trim().isEmpty()) {
            client.searchContacts(email.trim());
        }
    }

    private void handleSearchResults(String results) {
        SwingUtilities.invokeLater(() -> {
            String[] users = results.isEmpty() ? new String[0] : results.split(",");

            if (users.length == 0) {
                JOptionPane.showMessageDialog(this, "No users found with that email.");
                return;
            }

            JComboBox<String> userSelect = new JComboBox<>(users);
            int option = JOptionPane.showConfirmDialog(
                this,
                new Object[]{"Select user to add:", userSelect},
                "Add Contact",
                JOptionPane.OK_CANCEL_OPTION
            );

            if (option == JOptionPane.OK_OPTION) {
                String selectedUser = (String) userSelect.getSelectedItem();
                if (selectedUser != null) {
                    client.addContact(selectedUser);
                }
            }
        });
    }

    private void updateContactList(String[] contacts) {
        SwingUtilities.invokeLater(() -> {
            contactListModel.clear();
            System.out.println("[UI] Updating contacts list with " + contacts.length + 
                              " contacts: " + Arrays.toString(contacts));
            
            for (String contact : contacts) {
                if (!contact.isEmpty()) {
                    contactListModel.addElement(contact);
                    System.out.println("[UI] Added contact to list: " + contact);
                }
            }
            
            // Debug final list content
            System.out.println("[UI] Final contact list size: " + contactListModel.size());
            for (int i = 0; i < contactListModel.size(); i++) {
                System.out.println("[UI] Contact #" + i + ": " + contactListModel.get(i));
            }
        });
    }
}
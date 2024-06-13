package client;

import javax.swing.*;
import java.awt.*;

public class LoginUI extends JFrame {
    private MessageClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverField;
    private JButton loginButton;
    private JButton registerButton;

    public LoginUI() {
        this.client = new MessageClient();
        setupUI();
    }

    private void setupUI() {
        setTitle("SafeSpeak Login");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Server:"));
        serverField = new JTextField("localhost:9090");
        panel.add(serverField);

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel(""));
        loginButton = new JButton("Login");
        panel.add(loginButton);

        panel.add(new JLabel(""));
        registerButton = new JButton("Register");
        panel.add(registerButton);

        loginButton.addActionListener(e -> attemptLogin());
        registerButton.addActionListener(e -> showRegisterDialog());

        add(panel);
        setVisible(true);

        getRootPane().setDefaultButton(loginButton);
    }

    private void attemptLogin() {
        String serverInfo = serverField.getText();
        String[] parts = serverInfo.split(":");
        final String host = parts[0];
        final int port;
        if (parts.length > 1) {
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid port number");
                return;
            }
        } else {
            port = 9090;
        }

        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Connecting...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                if (!client.connect(host, port)) {
                    return false;
                }
                return client.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        openChatWindow();
                    } else {
                        JOptionPane.showMessageDialog(LoginUI.this,
                                "Login failed. Check server address, username and password.");
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(LoginUI.this,
                            "Connection error: " + e.getMessage());
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            }
        }.execute();
    }

    private void showRegisterDialog() {
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField emailField = new JTextField();

        Object[] fields = {
            "Username:", userField,
            "Password:", passField,
            "Email:", emailField
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Register New User", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            String email = emailField.getText();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                return;
            }

            String serverInfo = serverField.getText();
            String[] parts = serverInfo.split(":");
            final String host = parts[0];
            final int port;
            if (parts.length > 1) {
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid port number");
                    return;
                }
            } else {
                port = 9090;
            }

            // Connect if not already connected
            if (!client.connect(host, port)) {
                JOptionPane.showMessageDialog(this, "Could not connect to server.");
                return;
            }

            boolean success = client.register(username, password, email);
            if (success) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.");
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed. Username or email may already exist.");
            }
        }
    }

    private void openChatWindow() {
        setVisible(false);
        dispose();
        SwingUtilities.invokeLater(() -> {
            ChatUI chatUI = new ChatUI(client);
            chatUI.setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI());
    }
}
package client;

import javax.swing.*;
import java.awt.*;

public class LoginUI extends JFrame {
    private MessageClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverField;
    private JButton loginButton;

    public LoginUI() {
        this.client = new MessageClient();
        setupUI();
    }

    private void setupUI() {
        setTitle("SafeSpeak Login");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 10, 10));
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

        loginButton.addActionListener(e -> attemptLogin());

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
            protected Boolean doInBackground() throws Exception {
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
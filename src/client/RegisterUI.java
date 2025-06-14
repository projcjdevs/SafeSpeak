package client;

import javax.swing.*;
import java.awt.*;

public class RegisterUI extends JFrame {
    private MessageClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JTextField serverField;
    private JButton registerButton;
    
    public RegisterUI() {
        this.client = new MessageClient();
        setupUI();
    }
    
    private void setupUI() {
        setTitle("SafeSpeak Registration");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2, 10, 15));
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
        
        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        registerButton = new JButton("Register");
        registerButton.addActionListener(e -> attemptRegistration());
        buttonPanel.add(registerButton);
        
        JButton backButton = new JButton("Back to Welcome");
        backButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new WelcomeUI());
        });
        buttonPanel.add(backButton);
        
        panel.add(new JLabel(""));
        panel.add(buttonPanel);
        
        add(panel);
        setVisible(true);
        
        getRootPane().setDefaultButton(registerButton);
    }
    
    private void attemptRegistration() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
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
        
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                if (!client.connect(host, port)) {
                    return false;
                }
                return client.register(username, password, email);
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(RegisterUI.this, 
                            "Registration successful! You can now log in.");
                        dispose();
                        SwingUtilities.invokeLater(() -> new LoginUI());
                    } else {
                        JOptionPane.showMessageDialog(RegisterUI.this,
                            "Registration failed. Username or email may already exist.");
                        registerButton.setEnabled(true);
                        registerButton.setText("Register");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(RegisterUI.this,
                            "Connection error: " + e.getMessage());
                    registerButton.setEnabled(true);
                    registerButton.setText("Register");
                }
            }
        }.execute();
    }
}
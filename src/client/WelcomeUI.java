package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WelcomeUI extends JFrame {
    
    public WelcomeUI() {
        setupUI();
    }
    
    private void setupUI() {
        setTitle("Welcome to SafeSpeak");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Logo/header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel logoLabel = new JLabel("SafeSpeak");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 36));
        logoLabel.setForeground(new Color(41, 128, 185));
        headerPanel.add(logoLabel);
        
        // Welcome message
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        
        JLabel welcomeLabel = new JLabel("Secure Messaging for Everyone");
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel descriptionLabel = new JLabel("Connect with friends securely and privately");
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        messagePanel.add(Box.createVerticalStrut(20));
        messagePanel.add(welcomeLabel);
        messagePanel.add(Box.createVerticalStrut(10));
        messagePanel.add(descriptionLabel);
        messagePanel.add(Box.createVerticalStrut(40));
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
        JButton loginButton = new JButton("Login to Account");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(200, 40));
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton registerButton = new JButton("Create New Account");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(200, 40));
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(Box.createVerticalStrut(15));
        buttonPanel.add(registerButton);
        
        // Add action listeners
        loginButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginUI());
        });
        
        registerButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new RegisterUI());
        });
        
        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(messagePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set main content pane
        setContentPane(mainPanel);
        setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WelcomeUI());
    }
}
package org.example.ui;

import org.example.controller.QuizController;
import org.example.model.Question;
import org.example.service.QuizService;
import org.example.service.UserManager;
import org.example.utility.PerformanceLogger;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents the main view for the Quiz Application.
 */
public class QuizView {
    private final JFrame frame;
    private final JLabel userCountLabel;
    private final UserManager userManager;
    private final QuizService quizService;
    private ExecutorService executorService;
    private JButton setThreadsButton;
    private int activeUserWindows = 0;

    /**
     * Initializes the main QuizView.
     *
     * @param userManager  Manages active users in the application.
     * @param quizService  Handles quiz-related operations.
     */
    public QuizView(UserManager userManager, QuizService quizService) {
        this.userManager = userManager;
        this.quizService = quizService;

        frame = new JFrame("Quiz Application");
        userCountLabel = new JLabel("Active Users: 0");
        setThreadsButton = new JButton("Set Threads and Start");
        initializeUI();
    }

    /**
     * Ensures GUI initialization on the Event Dispatch Thread (EDT).
     */
    private void initializeUI() {
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLayout(new BorderLayout());

            frame.add(userCountLabel, BorderLayout.NORTH);

            setThreadsButton.addActionListener(e -> handleSetThreads());
            frame.add(setThreadsButton, BorderLayout.CENTER);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Handles setting the number of threads and creates user windows.
     */
    private void handleSetThreads() {
        String input = JOptionPane.showInputDialog(frame, "Enter the number of threads (e.g., 1, 5, 100):");
        try {
            int threadCount = Integer.parseInt(input);
            if (threadCount < 1) {
                throw new NumberFormatException();
            }
            setThreadsButton.setEnabled(false);

            if (executorService != null) {
                executorService.shutdownNow(); // Shut down the existing thread pool.
            }

            executorService = Executors.newFixedThreadPool(threadCount);
            createUserWindows(threadCount);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid input. Please enter a positive number.");
        }
    }

    /**
     * Creates user windows for the quiz application.
     *
     * @param threadCount The number of windows to create.
     */
    private void createUserWindows(int threadCount) {
        for (int i = 1; i <= threadCount; i++) {
            activeUserWindows++;
            updateUserCount(); // Update the user count when a window is created.

            JFrame userFrame = new JFrame("User " + i + " - Quiz");
            userFrame.setSize(400, 200);
            userFrame.setLayout(new BorderLayout());
            userFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            // Listener to track window closure and update user count.
            userFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    synchronized (QuizView.this) {
                        activeUserWindows--;
                        userManager.removeUser(); // Update the user manager when a window is closed.
                        updateUserCount();
                    }
                }
            });

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel nameLabel = new JLabel("Enter your name:");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(nameLabel, gbc);

            JTextField nameField = new JTextField(15);
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(nameField, gbc);

            JButton startButton = new JButton("Start Quiz");
            gbc.gridx = 0;
            gbc.gridy = 2;
            panel.add(startButton, gbc);

            startButton.addActionListener(e -> {
                startButton.setEnabled(false);
                handleStartQuiz(nameField.getText(), userFrame, startButton);
            });

            userFrame.add(panel, BorderLayout.CENTER);
            userFrame.setLocationRelativeTo(null);
            userFrame.setVisible(true);
        }
    }

    /**
     * Starts a new quiz session for the user.
     *
     * @param userName    The name of the user starting the quiz.
     * @param userFrame   The frame for the user's quiz session.
     * @param startButton The button to start the quiz.
     */
    private void handleStartQuiz(String userName, JFrame userFrame, JButton startButton) {
        if (userName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(userFrame, "Please enter your name to start the quiz.");
            startButton.setEnabled(true);
            return;
        }

        userManager.addUser();
        updateUserCount();

        executorService.submit(() -> {
            logPerformance("Quiz Session for " + userName, () -> {
                try {
                    List<Question> questions = quizService.startQuiz(5);

                    SwingUtilities.invokeLater(() -> {
                        userFrame.setVisible(false); // Hide the user frame during the quiz.

                        new QuizSessionView(new QuizController(questions), () -> {
                            userManager.removeUser();
                            updateUserCount();
                        }).showQuiz();
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(userFrame, "Error starting quiz: " + e.getMessage());
                        userManager.removeUser();
                        updateUserCount();
                        startButton.setEnabled(true);
                    });
                }
            });
        });
    }

    /**
     * Logs the performance of a given task.
     *
     * @param sessionName The name of the session being logged.
     * @param task        The task to measure performance for.
     */
    private void logPerformance(String sessionName, Runnable task) {
        PerformanceLogger logger = new PerformanceLogger();
        logger.start();
        try {
            task.run();
        } finally {
            logger.stop();
            PerformanceLogger.logPerformance(sessionName, logger.getExecutionTime());
        }
    }

    /**
     * Updates the active user count on the user count label.
     */
    private void updateUserCount() {
        SwingUtilities.invokeLater(() -> userCountLabel.setText("Active Users: " + userManager.getActiveUserCount()));
    }
}

package org.example.ui;

import org.example.controller.QuizController;
import org.example.model.Question;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuizSessionView {
    private static final Logger LOGGER = Logger.getLogger(QuizSessionView.class.getName());
    private static final int TOTAL_TIME = 120; // Total quiz time in seconds
    private static final int LOW_TIME_THRESHOLD = 60; // Time threshold for warning

    private final QuizController quizController;
    private final Runnable onQuizEnd;
    private final AtomicInteger timeRemaining;
    private final ExecutorService executorService;

    private volatile Timer sessionTimer;
    private JFrame quizFrame;
    private JLabel timerLabel;
    private JLabel questionLabel;
    private JPanel optionsPanel;
    private ButtonGroup buttonGroup;

    public QuizSessionView(QuizController quizController, Runnable onQuizEnd) {
        this.quizController = quizController;
        this.onQuizEnd = onQuizEnd;
        this.timeRemaining = new AtomicInteger(TOTAL_TIME);
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public void showQuiz() {
        SwingUtilities.invokeLater(this::initializeQuizUI);
    }

    private void initializeQuizUI() {
        quizFrame = new JFrame("Quiz Session");
        quizFrame.setSize(600, 400);
        quizFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        quizFrame.setLayout(new BorderLayout(10, 10));

        setupTopPanel();
        setupOptionsPanel();
        setupNavigationPanel();

        startSessionTimer();

        quizFrame.setLocationRelativeTo(null);
        quizFrame.setVisible(true);

        updateQuestion();
    }

    private void setupTopPanel() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Timer Label
        timerLabel = new JLabel("Time remaining: " + formatTime(timeRemaining.get()));
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Question Label
        questionLabel = new JLabel();
        questionLabel.setFont(new Font("Arial", Font.BOLD, 15));
        questionLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        topPanel.add(timerLabel, gbc);

        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        topPanel.add(questionLabel, gbc);

        quizFrame.add(topPanel, BorderLayout.NORTH);
    }

    private void setupOptionsPanel() {
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        buttonGroup = new ButtonGroup();
        quizFrame.add(new JScrollPane(optionsPanel), BorderLayout.CENTER);
    }

    private void setupNavigationPanel() {
        JPanel navigationPanel = new JPanel();
        JButton previousButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        JButton submitButton = new JButton("Submit");

        previousButton.addActionListener(e -> navigatePrevious());
        nextButton.addActionListener(e -> navigateNext());
        submitButton.addActionListener(e -> submitQuiz());

        navigationPanel.add(previousButton);
        navigationPanel.add(nextButton);
        navigationPanel.add(submitButton);
        quizFrame.add(navigationPanel, BorderLayout.SOUTH);
    }

    private void navigatePrevious() {
        executorService.submit(() -> {
            SwingUtilities.invokeLater(() -> {
                quizController.moveToPreviousQuestion();
                updateQuestion();
            });
        });
    }

    private void navigateNext() {
        executorService.submit(() -> {
            int selectedIndex = getSelectedOptionIndex();
            if (selectedIndex == -1) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(quizFrame, "Please select an option!")
                );
                return;
            }

            SwingUtilities.invokeLater(() -> {
                quizController.validateAnswer(selectedIndex);
                if (!quizController.isLastQuestion()) {
                    quizController.moveToNextQuestion();
                    updateQuestion();
                }
            });
        });
    }

    private void submitQuiz() {
        executorService.submit(() -> {
            if (!quizController.isQuizComplete()) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(quizFrame, "Please answer all questions before submitting!")
                );
                return;
            }

            try {
                int score = quizController.calculateScore();
                SwingUtilities.invokeLater(() -> {
                    sessionTimer.stop();
                    quizFrame.dispose();
                    JOptionPane.showMessageDialog(quizFrame, "Quiz Ended! Your score: " + score);
                    onQuizEnd.run();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error calculating score", ex);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(quizFrame, "Error calculating score.")
                );
            }
        });
    }

    private void updateQuestion() {
        SwingUtilities.invokeLater(() -> {
            Question currentQuestion = quizController.getCurrentQuestion();
            if (currentQuestion == null || currentQuestion.getQuestionText() == null) {
                questionLabel.setText("Question not available!");
                LOGGER.warning("No question available or question text is null.");
                return;
            }

            questionLabel.setText(currentQuestion.getQuestionText());
            optionsPanel.removeAll();
            buttonGroup = new ButtonGroup();

            int selectedAnswer = quizController.getUserAnswers().get(quizController.getCurrentIndex());
            for (int i = 0; i < currentQuestion.getOptions().length; i++) {
                JRadioButton optionButton = new JRadioButton(currentQuestion.getOptions()[i]);
                optionButton.setFont(new Font("Arial", Font.PLAIN, 14));
                optionButton.setAlignmentX(Component.LEFT_ALIGNMENT);

                if (i == selectedAnswer) {
                    optionButton.setSelected(true);
                }

                buttonGroup.add(optionButton);
                optionsPanel.add(optionButton);
            }

            optionsPanel.revalidate();
            optionsPanel.repaint();
        });
    }

    private void startSessionTimer() {
        sessionTimer = new Timer(1000, e -> {
            int remainingTime = timeRemaining.decrementAndGet();

            SwingUtilities.invokeLater(() -> {
                timerLabel.setText("Time remaining: " + formatTime(remainingTime));

                if (remainingTime <= LOW_TIME_THRESHOLD) {
                    timerLabel.setForeground(Color.RED);
                }

                if (remainingTime <= 0) {
                    sessionTimer.stop();
                    quizFrame.dispose();
                    JOptionPane.showMessageDialog(null, "Time's up! Quiz ended.");
                    onQuizEnd.run();
                }
            });
        });
        sessionTimer.start();
    }

    private int getSelectedOptionIndex() {
        for (int i = 0; i < optionsPanel.getComponentCount(); i++) {
            Component comp = optionsPanel.getComponent(i);
            if (comp instanceof JRadioButton && ((JRadioButton) comp).isSelected()) {
                return i;
            }
        }
        return -1;
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    // Cleanup method to shutdown executor service
    public void cleanup() {
        if (sessionTimer != null) {
            sessionTimer.stop();
        }
        executorService.shutdown();
    }
}
//package org.example.ui;
//
//import org.example.controller.QuizController;
//import org.example.model.Question;
//
//import javax.swing.*;
//import java.awt.*;
//
//public class QuizSessionView {
//    private final QuizController quizController;
//    private final Runnable onQuizEnd;
//    private Timer sessionTimer;
//    private int timeRemaining = 120; // Example: 300 seconds (5 minutes)
//
//    public QuizSessionView(QuizController quizController, Runnable onQuizEnd) {
//        this.quizController = quizController;
//        this.onQuizEnd = onQuizEnd;
//    }
//
//    public void showQuiz() {
//        SwingUtilities.invokeLater(() -> {
//            JFrame quizFrame = new JFrame("Quiz Session");
//            quizFrame.setSize(600, 300);
//            quizFrame.setLayout(new BorderLayout());
//
//            // Use GridBagLayout for more control over positioning in topPanel
//            JPanel topPanel = new JPanel(new GridBagLayout());
//            GridBagConstraints gbc = new GridBagConstraints();
//            gbc.gridx = 0;
//            gbc.gridy = 0;
//            gbc.anchor = GridBagConstraints.EAST; // Align timer to the right
//            JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Timer panel
//            JLabel timerLabel = new JLabel("Time remaining: " + formatTime(timeRemaining));
//
//            timerPanel.add(timerLabel);
//            topPanel.add(timerPanel, gbc);
//
//            // Spacer panel for distance between the timer and the question
//            gbc.gridy = 1; // Move the question label down a bit
//            JPanel spacerPanel = new JPanel();
//            spacerPanel.setPreferredSize(new Dimension(0, 10)); // Height to create space
//            topPanel.add(spacerPanel, gbc);
//
//            // Question label (left-aligned)
//            gbc.gridy = 2;
//            gbc.anchor = GridBagConstraints.WEST;
//            gbc.fill = GridBagConstraints.HORIZONTAL;
//            gbc.weightx = 1.0; // Give it horizontal weight to spread across the panel
//
//            JLabel questionLabel = new JLabel();
//            questionLabel.setFont(new Font("Arial", Font.BOLD, 15));
//            questionLabel.setHorizontalAlignment(SwingConstants.LEFT);
//            questionLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
//            topPanel.add(questionLabel, gbc);
//
//            quizFrame.add(topPanel, BorderLayout.NORTH);
//
//            // Options panel (vertical layout)
//            JPanel optionsPanel = new JPanel();
//            optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS)); // Arrange options vertically
//            quizFrame.add(optionsPanel, BorderLayout.CENTER);
//
//            ButtonGroup buttonGroup = new ButtonGroup();
//
//            JPanel navigationPanel = new JPanel();
//            quizFrame.add(navigationPanel, BorderLayout.SOUTH);
//
//            JButton previousButton = new JButton("Previous");
//            JButton nextButton = new JButton("Next");
//            JButton submitButton = new JButton("Submit");
//
//            navigationPanel.add(previousButton);
//            navigationPanel.add(nextButton);
//            navigationPanel.add(submitButton);
//
//            previousButton.addActionListener(e -> {
//                quizController.moveToPreviousQuestion();
//                updateQuestion(questionLabel, optionsPanel, buttonGroup);
//            });
//
//            nextButton.addActionListener(e -> {
//                int selectedIndex = getSelectedOptionIndex(optionsPanel);
//                if (selectedIndex == -1) {
//                    JOptionPane.showMessageDialog(null, "Please select an option!");
//                } else {
//                    quizController.validateAnswer(selectedIndex);
//                    if (!quizController.isLastQuestion()) {
//                        quizController.moveToNextQuestion();
//                        updateQuestion(questionLabel, optionsPanel, buttonGroup);
//                    }
//                }
//            });
//
//            submitButton.addActionListener(e -> {
//                if (!quizController.isQuizComplete()) {
//                    JOptionPane.showMessageDialog(null, "Please answer all questions before submitting!");
//                } else {
//                    SwingWorker<Integer, Void> worker = new SwingWorker<>() {
//                        @Override
//                        protected Integer doInBackground() {
//                            return quizController.calculateScore();
//                        }
//
//                        @Override
//                        protected void done() {
//                            try {
//                                int score = get();
//                                quizFrame.dispose();
//                                JOptionPane.showMessageDialog(null, "Quiz Ended! Your score: " + score);
//                                sessionTimer.stop();
//                                onQuizEnd.run();
//                            } catch (Exception ex) {
//                                JOptionPane.showMessageDialog(null, "Error calculating score.");
//                            }
//                        }
//                    };
//                    worker.execute();
//                }
//            });
//
//            startSessionTimer(quizFrame, timerLabel);
//            quizFrame.setLocationRelativeTo(null);
//            quizFrame.setVisible(true);
//
//            updateQuestion(questionLabel, optionsPanel, buttonGroup);
//        });
//    }
//
//    private void updateQuestion(JLabel questionLabel, JPanel optionsPanel, ButtonGroup buttonGroup) {
//        SwingUtilities.invokeLater(() -> {
//            Question currentQuestion = quizController.getCurrentQuestion();
//            if (currentQuestion == null || currentQuestion.getQuestionText() == null) {
//                questionLabel.setText("Question not available!");
//                System.err.println("No question available or question text is null.");
//                return;
//            }
//            questionLabel.setText(currentQuestion.getQuestionText());
//
//            optionsPanel.removeAll();
//            buttonGroup.clearSelection();
//
//            int selectedAnswer = quizController.getUserAnswers().get(quizController.getCurrentIndex());
//            for (int i = 0; i < currentQuestion.getOptions().length; i++) {
//                JRadioButton optionButton = new JRadioButton(currentQuestion.getOptions()[i]);
//                optionButton.setFont(new Font("Arial", Font.PLAIN, 14)); // Apply font style to options
//                optionButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align text to the left
//                if (i == selectedAnswer) {
//                    optionButton.setSelected(true);
//                }
//                buttonGroup.add(optionButton);
//                optionsPanel.add(optionButton);
//            }
//
//            optionsPanel.revalidate();
//            optionsPanel.repaint();
//        });
//    }
//
//    private int getSelectedOptionIndex(JPanel optionsPanel) {
//        for (int i = 0; i < optionsPanel.getComponentCount(); i++) {
//            if (((JRadioButton) optionsPanel.getComponent(i)).isSelected()) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    private void startSessionTimer(JFrame quizFrame, JLabel timerLabel) {
//        sessionTimer = new Timer(1000, e -> {
//            timeRemaining--;
//            timerLabel.setText("Time remaining: " + formatTime(timeRemaining));
//            if (timeRemaining <= 60) {
//                timerLabel.setForeground(Color.RED);
//            }
//            if (timeRemaining <= 0) {
//                sessionTimer.stop();
//                quizFrame.dispose();
//                JOptionPane.showMessageDialog(null, "Time's up! Quiz ended.");
//                onQuizEnd.run();
//            }
//        });
//        sessionTimer.start();
//    }
//
//    private String formatTime(int seconds) {
//        int minutes = seconds / 60;
//        int secs = seconds % 60;
//        return String.format("%02d:%02d", minutes, secs);
//    }
//}

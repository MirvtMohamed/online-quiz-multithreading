package org.example.controller;

import org.example.model.Question;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuizController {
    private static final Logger LOGGER = Logger.getLogger(QuizController.class.getName());

    // thread-safe collections and synchronization mechanisms
    private final List<Question> questions;
    private final List<Integer> userAnswers;

    //  ReentrantLock for more flexible synchronization
    private final ReentrantLock lock = new ReentrantLock();

    // Volatile to ensure visibility across threads
    private volatile int currentIndex = 0;

    public QuizController(List<Question> questions) {
        // Defensive copy and validation
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Questions list cannot be null or empty");
        }

        // Create an unmodifiable defensive copy
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));

        // Pre-initialize user answers with thread-safe method
        this.userAnswers = initializeUserAnswers(questions.size());
    }

    /**
     * Initialize user answers safely
     * @param size number of questions
     * @return thread-safe list of answers
     */
    private List<Integer> initializeUserAnswers(int size) {
        List<Integer> answers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            answers.add(-1); // No answer selected
        }
        return Collections.synchronizedList(answers);
    }

    /**
     * Get current question with thread-safe access
     * @return current Question
     * @throws IllegalStateException if no questions available
     */
    public Question getCurrentQuestion() {
        lock.lock();
        try {
            validateQuestionAccess();
            return questions.get(currentIndex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get current index with thread-safe access
     * @return current index
     */
    public int getCurrentIndex() {
        lock.lock();
        try {
            return currentIndex;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Validate answer with comprehensive error checking
     * @param selectedIndex user's selected answer index
     * @return whether answer is correct
     * @throws IllegalArgumentException for invalid index
     */
    public boolean validateAnswer(int selectedIndex) {
        lock.lock();
        try {
            validateQuestionAccess();
            Question currentQuestion = questions.get(currentIndex);

            // Validate selected index
            if (selectedIndex < 0 || selectedIndex >= currentQuestion.getOptions().length) {
                throw new IllegalArgumentException("Invalid answer index: " + selectedIndex);
            }

            userAnswers.set(currentIndex, selectedIndex);
            return selectedIndex == currentQuestion.getCorrectAnswerIndex();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error validating answer", e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Move to next question with boundary checking
     */
    public void moveToNextQuestion() {
        lock.lock();
        try {
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Move to previous question with boundary checking
     */
    public void moveToPreviousQuestion() {
        lock.lock();
        try {
            if (currentIndex > 0) {
                currentIndex--;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get user answers as an immutable copy
     * @return copy of user answers
     */
    public List<Integer> getUserAnswers() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(userAnswers));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Calculate score with robust error handling
     * @return quiz score
     * @throws IllegalStateException if quiz is incomplete
     */
    public int calculateScore() {
        lock.lock();
        try {
            if (!isQuizComplete()) {
                throw new IllegalStateException("Quiz is not complete. All questions must be answered.");
            }

            int score = 0;
            for (int i = 0; i < questions.size(); i++) {
                if (Objects.equals(userAnswers.get(i), questions.get(i).getCorrectAnswerIndex())) {
                    score++;
                }
            }
            return score;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if quiz is complete
     * @return true if all questions are answered
     */
    public boolean isQuizComplete() {
        lock.lock();
        try {
            return !userAnswers.contains(-1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if current question is the last question
     * @return true if last question
     */
    public boolean isLastQuestion() {
        lock.lock();
        try {
            return currentIndex == questions.size() - 1;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Validate current question access
     * @throws IllegalStateException if no questions or invalid index
     */
    private void validateQuestionAccess() {
        if (questions.isEmpty() || currentIndex < 0 || currentIndex >= questions.size()) {
            throw new IllegalStateException("Invalid question access");
        }
    }
}



package org.example.service;




import org.example.model.Question;
import org.example.model.QuestionBank;
import org.example.utility.PerformanceLogger;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QuizService {
    private final QuestionBank questionBank;
    private final Lock questionBankLock = new ReentrantLock();

    public QuizService(QuestionBank questionBank) {
        this.questionBank = questionBank;
    }

    /**
     * Thread-safe method to retrieve a list of random questions.
     *
     * @param questionCount Number of questions to retrieve.
     * @return List of random questions.
     */
    public List<Question> startQuiz(int questionCount) {
        //performance
        PerformanceLogger logger = new PerformanceLogger();
        questionBankLock.lock();
        try {
            logger.start();
            List<Question> questions = questionBank.getRandomQuestions(questionCount);
            logger.stop();
            PerformanceLogger.logPerformance("Fetching Questions", logger.getExecutionTime());
            return questions;
        } finally {
            questionBankLock.unlock();
        }

    }
}


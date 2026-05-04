package org.example;

import org.example.model.QuestionBank;


import org.example.service.QuizService;
import org.example.service.UserManager;
import org.example.ui.QuizView;
import org.example.utility.PerformanceComparison;

import javax.swing.*;

public class QuizApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuestionBank questionBank = new QuestionBank();
            QuizService quizService = new QuizService(questionBank);
            UserManager userManager = new UserManager();

            new QuizView(userManager, quizService);
            new PerformanceComparison();
            PerformanceComparison.comparePerformance(quizService);


        });

    }
}
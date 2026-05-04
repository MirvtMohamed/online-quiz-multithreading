package org.example.model;



import java.util.ArrayList;

import java.util.List;
import java.sql.*;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class QuestionBank {



        private final String URL = "jdbc:mysql://localhost:3306/quiz_database";
        private final String USER = "root";
        private final String PASSWORD = "root123#";

        private final Lock databaseLock = new ReentrantLock();

    public List<Question> getRandomQuestions(int count) {
        List<Question> randomQuestions = new ArrayList<>();

        databaseLock.lock();
        try {
            // Load the MySQL JDBC driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT question_text, options, correct_answer_index FROM questions ORDER BY RAND() LIMIT ?")) {

                pstmt.setInt(1, count);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String questionText = rs.getString("question_text");
                        String optionsJson = rs.getString("options");
                        int correctIndex = rs.getInt("correct_answer_index");

                        // Parse JSON options
                        String[] options = optionsJson.replaceAll("[\\[\\]\"]", "").split(",");

                        randomQuestions.add(new Question(questionText, options, correctIndex));
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("MySQL JDBC Driver not found.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseLock.unlock();
        }

        return randomQuestions;
    }



    }


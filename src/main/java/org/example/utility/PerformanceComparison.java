package org.example.utility;

import org.example.service.QuizService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerformanceComparison {

    public static void comparePerformance(QuizService quizService) {
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        PerformanceLogger multiThreadedLogger = new PerformanceLogger();
        PerformanceLogger singleThreadedLogger = new PerformanceLogger();

        // Define the user session task (for example, this could be fetching quiz data)
        Runnable userSessionTask = () -> {
            // Simulate task execution (e.g., fetching quiz data)
            quizService.startQuiz(5); // Or any other task you want to measure
        };

        // Multithreaded execution
        multiThreadedLogger.start();
        for (int i = 0; i < 10; i++) {
            threadPool.execute(userSessionTask);
        }
        threadPool.shutdown();
        while (!threadPool.isTerminated()) { } // Wait for threads to finish
        multiThreadedLogger.stop();

        // Single-threaded execution
        singleThreadedLogger.start();
        for (int i = 0; i < 10; i++) {
            userSessionTask.run(); // Execute task sequentially
        }
        singleThreadedLogger.stop();

        // Log results to console
        PerformanceLogger.logPerformance("Multithreaded Execution", multiThreadedLogger.getExecutionTime());
        PerformanceLogger.logPerformance("Single-Threaded Execution", singleThreadedLogger.getExecutionTime());
    }
}

/*public void comparePerformance() {
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        PerformanceLogger multiThreadedLogger = new PerformanceLogger();
        PerformanceLogger singleThreadedLogger = new PerformanceLogger();

        // Define the user session task (for example, this could be fetching questions)
        Runnable userSessionTask = () -> {
            // Simulate task execution (e.g., fetching quiz data)
            quizService.startQuiz(5); // Or any other task you want to measure
        };

        // Multithreaded execution
        multiThreadedLogger.start();
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                // Wrapping the Runnable into a Callable
                tasks.add(() -> {
                    userSessionTask.run();
                    return null; // Callable requires a return value, we use null here
                });
            }
            threadPool.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
        multiThreadedLogger.stop();

        // Single-threaded execution
        singleThreadedLogger.start();
        for (int i = 0; i < 10; i++) {
            userSessionTask.run(); // Execute task sequentially
        }
        singleThreadedLogger.stop();

        // Log results to console
        PerformanceLogger.logPerformance("Multithreaded Execution", multiThreadedLogger.getExecutionTime());
        PerformanceLogger.logPerformance("Single-Threaded Execution", singleThreadedLogger.getExecutionTime());
    }*/

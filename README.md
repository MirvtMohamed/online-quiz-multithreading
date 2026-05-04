 Project Title: Multithreaded Online Quiz System (Java + Maven)

 Overview

A Java-based quiz application that supports multiple concurrent users using multithreading. The system ensures synchronized access to shared resources like the question bank and provides real-time score updates.

 Key Concepts
Multithreading (user sessions handled via threads)
Synchronization (thread-safe question access)
DAO Pattern (QuestionDAO for DB operations)
Separation of Concerns (UI / Logic / Data layers)

Features
Multiple users can take quizzes simultaneously
Thread-safe question bank access
Real-time score calculation
MySQL database integration
Clean architecture with DAO layer
Javadoc documentation for all public methods

 Multithreading Design
Each user runs in a separate thread
Shared resources are synchronized to avoid race conditions
Fixed user count logic implemented
removeUser() handled via window listener for better lifecycle management

package application;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class studentDatabase {
    // JDBC driver name and database URL 
    static final String JDBC_DRIVER = "org.h2.Driver";   
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

    //  Database credentials 
    static final String USER = "sa"; 
    static final String PASS = ""; 

    private Connection connection = null;
    private Statement statement = null;

    public void connectToDatabase() throws SQLException {
        try {
            Class.forName(JDBC_DRIVER);
            System.out.println("Connecting to database...");
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.createStatement();
            createTables();
            updateTables(); // Add this line to update existing tables with new columns
            createTestUserIfNotExists();
            System.out.println("Database connection and setup complete");
            //statement.execute("DROP ALL OBJECTS");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
        }
    }
    
    private void updateTables() {
        try {
            // Check if needsClarification column exists in the answers table
            ResultSet rs = connection.getMetaData().getColumns(null, null, "ANSWERS", "NEEDSCLARIFICATION");
            if (!rs.next()) {
                // Column doesn't exist, so add it
                System.out.println("Adding needsClarification column to answers table...");
                statement.execute("ALTER TABLE answers ADD COLUMN needsClarification BOOLEAN DEFAULT FALSE");
                System.out.println("Column added successfully");
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error updating tables: " + e.getMessage());
        }
    }
    
    public void createTestUserIfNotExists() throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM cse360users WHERE userName = 'testuser'";
        ResultSet rs = statement.executeQuery(checkQuery);
        rs.next();
        if (rs.getInt(1) == 0) {
            System.out.println("Creating test user..."); // Debug log
            String insertQuery = "INSERT INTO cse360users (userName, password, role) VALUES ('testuser', 'test', 'student')";
            statement.execute(insertQuery);
            System.out.println("Test user created successfully"); // Debug log
        } else {
            System.out.println("Test user already exists"); // Debug log
        }
    }
    
    public void deleteQuestion(int questionId) throws SQLException {
        // First delete all answers for this question
        String deleteAnswers = "DELETE FROM answers WHERE questionId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteAnswers)) {
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        }
        
        // Then delete the question
        String deleteQuestion = "DELETE FROM questions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteQuestion)) {
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        }
    }
    
    public void updateQuestion(int questionId, String newTitle, String newContent) throws SQLException {
        String query = "UPDATE questions SET title = ?, content = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newTitle);
            pstmt.setString(2, newContent);
            pstmt.setInt(3, questionId);
            pstmt.executeUpdate();
        }
    }

    public void updateAnswer(int answerId, String newContent) throws SQLException {
        String query = "UPDATE answers SET content = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newContent);
            pstmt.setInt(2, answerId);
            pstmt.executeUpdate();
        }
    }

    public void deleteAnswer(int answerId) throws SQLException {
        // First check if this answer is marked as resolved for any question
        String updateQuestion = "UPDATE questions SET resolved = FALSE, resolvedAnswerId = NULL WHERE resolvedAnswerId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateQuestion)) {
            pstmt.setInt(1, answerId);
            pstmt.executeUpdate();
        }
        
        // Then delete the answer
        String deleteAnswer = "DELETE FROM answers WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteAnswer)) {
            pstmt.setInt(1, answerId);
            pstmt.executeUpdate();
        }
    }

    public void unmarkResolved(int questionId) throws SQLException {
        String query = "UPDATE questions SET resolved = FALSE, resolvedAnswerId = NULL WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        }
    }

    public void markAnswerNeedsClarification(int answerId, boolean needsClarification) throws SQLException {
        try {
            String query = "UPDATE answers SET needsClarification = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setBoolean(1, needsClarification);
                pstmt.setInt(2, answerId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            // If this fails, the column might not exist yet
            System.err.println("Error setting needsClarification: " + e.getMessage());
            throw e;
        }
    }
    
    private void createTables() throws SQLException {
        // Original user table
        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userName VARCHAR(255) UNIQUE, "
                + "password VARCHAR(255), "
                + "role VARCHAR(20))";
        statement.execute(userTable);

        // Questions table remains the same
        String questionsTable = "CREATE TABLE IF NOT EXISTS questions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "title VARCHAR(255), "
                + "content TEXT, "
                + "userId INT, "
                + "createDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "resolved BOOLEAN DEFAULT FALSE, "
                + "resolvedAnswerId INT, "
                + "FOREIGN KEY (userId) REFERENCES cse360users(id))";
        statement.execute(questionsTable);

        // Answers table - Add needsClarification column
        String answersTable = "CREATE TABLE IF NOT EXISTS answers ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "content TEXT, "
                + "questionId INT, "
                + "userId INT, "
                + "createDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "needsClarification BOOLEAN DEFAULT FALSE, "
                + "FOREIGN KEY (questionId) REFERENCES questions(id), "
                + "FOREIGN KEY (userId) REFERENCES cse360users(id))";
        statement.execute(answersTable);

        // Reviewers table
        String reviewersTable = "CREATE TABLE IF NOT EXISTS reviewers ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userId INT, "
                + "reviewerId INT, "
                + "weight DOUBLE DEFAULT 1.0, "
                + "FOREIGN KEY (userId) REFERENCES cse360users(id), "
                + "FOREIGN KEY (reviewerId) REFERENCES cse360users(id))";
        statement.execute(reviewersTable);

        // Feedback table
        String feedbackTable = "CREATE TABLE IF NOT EXISTS feedback ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "answerId INT, "
                + "userId INT, "
                + "content TEXT, "
                + "createDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (answerId) REFERENCES answers(id), "
                + "FOREIGN KEY (userId) REFERENCES cse360users(id))";
        statement.execute(feedbackTable);
    }

    // Question Management Methods
    
    public int addQuestion(String title, String content, int userId) throws SQLException {
        String query = "INSERT INTO questions (title, content, userId) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public List<QuestionData> getQuestions() throws SQLException {
        List<QuestionData> questions = new ArrayList<>();
        String query = "SELECT q.*, u.userName, "
                + "(SELECT COUNT(*) FROM answers WHERE questionId = q.id) as answerCount "
                + "FROM questions q "
                + "JOIN cse360users u ON q.userId = u.id "
                + "ORDER BY q.createDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                QuestionData question = new QuestionData(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("userName"),
                    rs.getTimestamp("createDate"),
                    rs.getBoolean("resolved"),
                    rs.getInt("answerCount")
                );
                questions.add(question);
            }
        }
        return questions;
    }

    public List<QuestionData> searchQuestions(String searchTerm) throws SQLException {
        List<QuestionData> questions = new ArrayList<>();
        String query = "SELECT q.*, u.userName, "
                + "(SELECT COUNT(*) FROM answers WHERE questionId = q.id) as answerCount "
                + "FROM questions q "
                + "JOIN cse360users u ON q.userId = u.id "
                + "WHERE LOWER(q.title) LIKE LOWER(?) OR LOWER(q.content) LIKE LOWER(?) "
                + "ORDER BY q.createDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                QuestionData question = new QuestionData(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("userName"),
                    rs.getTimestamp("createDate"),
                    rs.getBoolean("resolved"),
                    rs.getInt("answerCount")
                );
                questions.add(question);
            }
        }
        return questions;
    }

    // Answer Management Methods
    
    public int addAnswer(String content, int questionId, int userId) throws SQLException {
        String query = "INSERT INTO answers (content, questionId, userId) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, content);
            pstmt.setInt(2, questionId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public List<AnswerData> getAnswersForQuestion(int questionId) throws SQLException {
        List<AnswerData> answers = new ArrayList<>();
        String query = "SELECT a.*, u.userName FROM answers a "
                + "JOIN cse360users u ON a.userId = u.id "
                + "WHERE a.questionId = ? "
                + "ORDER BY a.createDate";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                boolean needsClarification = false;
                try {
                    needsClarification = rs.getBoolean("needsClarification");
                } catch (SQLException e) {
                    // Column might not exist yet, just use default false
                    System.out.println("needsClarification column not available yet, using default false");
                }
                
                AnswerData answer = new AnswerData(
                    rs.getInt("id"),
                    rs.getString("content"),
                    rs.getString("userName"),
                    rs.getTimestamp("createDate"),
                    needsClarification
                );
                answers.add(answer);
            }
        }
        return answers;
    }

    public void markAnswerAsResolved(int questionId, int answerId) throws SQLException {
        String query = "UPDATE questions SET resolved = TRUE, resolvedAnswerId = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerId);
            pstmt.setInt(2, questionId);
            pstmt.executeUpdate();
        }
    }
    
    

    // Reviewer Management Methods
    
    public void addReviewer(int userId, int reviewerId, double weight) throws SQLException {
        String query = "INSERT INTO reviewers (userId, reviewerId, weight) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, reviewerId);
            pstmt.setDouble(3, weight);
            pstmt.executeUpdate();
        }
    }

    public List<ReviewerRecord> getReviewersForUser(int userId) throws SQLException {
        List<ReviewerRecord> reviewers = new ArrayList<>();
        String query = "SELECT r.*, u.userName FROM reviewers r "
                + "JOIN cse360users u ON r.reviewerId = u.id "
                + "WHERE r.userId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ReviewerRecord reviewer = new ReviewerRecord(
                    rs.getInt("reviewerId"),
                    rs.getString("userName"),
                    rs.getDouble("weight")
                );
                reviewers.add(reviewer);
            }
        }
        return reviewers;
    }

    public void updateReviewerWeight(int userId, int reviewerId, double weight) throws SQLException {
        String query = "UPDATE reviewers SET weight = ? WHERE userId = ? AND reviewerId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setDouble(1, weight);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, reviewerId);
            pstmt.executeUpdate();
        }
    }

    // Feedback Management Methods
    
    public void addFeedback(int answerId, int userId, String content) throws SQLException {
        String query = "INSERT INTO feedback (answerId, userId, content) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
        }
    }

    public List<FeedbackData> getFeedbackForAnswer(int answerId, int userId) throws SQLException {
        List<FeedbackData> feedbackList = new ArrayList<>();
        String query = "SELECT f.*, u.userName FROM feedback f "
                + "JOIN cse360users u ON f.userId = u.id "
                + "WHERE f.answerId = ? AND (f.userId = ? OR EXISTS "
                + "(SELECT 1 FROM reviewers r WHERE r.userId = ? AND r.reviewerId = f.userId))";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FeedbackData feedback = new FeedbackData(
                    rs.getInt("id"),
                    rs.getString("content"),
                    rs.getString("userName"),
                    rs.getTimestamp("createDate")
                );
                feedbackList.add(feedback);
            }
        }
        return feedbackList;
    }

    // Helper Methods
    
    public int getUserId(String userName) throws SQLException {
        String query = "SELECT id FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }

    public void closeConnection() {
        try {
            if (statement != null) statement.close();
        } catch (SQLException se2) {
            se2.printStackTrace();
        }
        try {
            if (connection != null) connection.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }
}

// Data classes for returning results

class QuestionData {
    public final int id;
    public final String title;
    public final String content;
    public final String userName;
    public final Timestamp createDate;
    public final boolean resolved;
    public final int answerCount;

    public QuestionData(int id, String title, String content, String userName, 
                       Timestamp createDate, boolean resolved, int answerCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.userName = userName;
        this.createDate = createDate;
        this.resolved = resolved;
        this.answerCount = answerCount;
    }
}

class AnswerData {
    public final int id;
    public final String content;
    public final String userName;
    public final Timestamp createDate;
    public final boolean needsClarification;

    public AnswerData(int id, String content, String userName, Timestamp createDate, boolean needsClarification) {
        this.id = id;
        this.content = content;
        this.userName = userName;
        this.createDate = createDate;
        this.needsClarification = needsClarification;
    }
    
    // Constructor without needsClarification for backward compatibility
    public AnswerData(int id, String content, String userName, Timestamp createDate) {
        this(id, content, userName, createDate, false);
    }
}

class ReviewerRecord {
    public final int reviewerId;
    public final String userName;
    public final double weight;

    public ReviewerRecord(int reviewerId, String userName, double weight) {
        this.reviewerId = reviewerId;
        this.userName = userName;
        this.weight = weight;
    }
}

class FeedbackData {
    public final int id;
    public final String content;
    public final String userName;
    public final Timestamp createDate;

    public FeedbackData(int id, String content, String userName, Timestamp createDate) {
        this.id = id;
        this.content = content;
        this.userName = userName;
        this.createDate = createDate;
    }
}

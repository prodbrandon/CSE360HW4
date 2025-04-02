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
                + "weight DOUBLE DEFAULT 1.0, "
                + "FOREIGN KEY (userId) REFERENCES cse360users(id))";
        statement.execute(reviewersTable);
        
        // Reviews table
        String reviewsTable = "CREATE TABLE IF NOT EXISTS reviews ("
        		+ "id INT AUTO_INCREMENT PRIMARY KEY, "
        		+ "reviewerId INT, "
        		+ "questionId INT, "
        		+ "answerId INT, "
        		+ "content TEXT, "
//        		+ "FOREIGN KEY (questionId) REFERENCES questions(id), "
//        		+ "FOREIGN KEY (answerId) REFERENCES answers(id), "
                + "FOREIGN KEY (reviewerId) REFERENCES reviewers(id))";
        statement.execute(reviewsTable);

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
        
        String messagesTable = "CREATE TABLE IF NOT EXISTS messages ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "senderId INT, "
                + "receiverId INT, "
                + "relatedQuestionId INT, "
                + "relatedAnswerId INT, "
                + "content TEXT, "
                + "isRead BOOLEAN DEFAULT FALSE, "
                + "createDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (senderId) REFERENCES cse360users(id), "
                + "FOREIGN KEY (receiverId) REFERENCES cse360users(id))";
        statement.execute(messagesTable);
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
    
    /**
     * Gets the name of a reviewer by their ID
     * @param reviewerId The ID of the reviewer
     * @return The username of the reviewer
     * @throws SQLException
     */
    public String getReviewerName(int reviewerId) throws SQLException {
        String query = "SELECT u.userName FROM reviewers r JOIN cse360users u ON r.userId = u.id WHERE r.id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, reviewerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("userName");
            }
        }
        return "Unknown Reviewer";
    }
    
    /**
     * Gets all reviews for a user's answers
     * @param userId The ID of the user
     * @return List of ReviewData objects for reviews on the user's answers
     * @throws SQLException
     */
    public List<ReviewData> getReviewsForUserAnswers(int userId) throws SQLException {
        List<ReviewData> reviews = new ArrayList<>();
        
        // First get all the user's answers
        String answerQuery = "SELECT id FROM answers WHERE userId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(answerQuery)) {
            pstmt.setInt(1, userId);
            ResultSet answerRs = pstmt.executeQuery();
            
            // For each answer, get its reviews
            while (answerRs.next()) {
                int answerId = answerRs.getInt("id");
                
                String reviewQuery = "SELECT * FROM reviews WHERE answerId = ?";
                try (PreparedStatement reviewPstmt = connection.prepareStatement(reviewQuery)) {
                    reviewPstmt.setInt(1, answerId);
                    ResultSet reviewRs = reviewPstmt.executeQuery();
                    
                    while (reviewRs.next()) {
                        ReviewData review = new ReviewData(
                            reviewRs.getInt("id"),
                            reviewRs.getInt("reviewerId"),
                            -1,
                            answerId,
                            reviewRs.getString("content")
                        );
                        reviews.add(review);
                    }
                }
            }
        }
        
        return reviews;
    }
    
    /**
     * Gets all answers for a specific user
     * @param userId The ID of the user
     * @return List of AnswerData objects
     * @throws SQLException
     */
    public List<AnswerData> getAnswersForUser(int userId) throws SQLException {
        List<AnswerData> answers = new ArrayList<>();
        String query = "SELECT a.*, q.title as questionTitle FROM answers a " +
                      "JOIN questions q ON a.questionId = q.id " +
                      "WHERE a.userId = ? " +
                      "ORDER BY a.createDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                boolean needsClarification = false;
                try {
                    needsClarification = rs.getBoolean("needsClarification");
                } catch (SQLException e) {
                    // Column might not exist yet, use default false
                }
                
                AnswerData answer = new AnswerData(
                    rs.getInt("id"),
                    rs.getString("content"),
                    getUserName(rs.getInt("userId")),
                    rs.getTimestamp("createDate"),
                    needsClarification
                );
                answers.add(answer);
            }
        }
        return answers;
    }
    
    /**
     * Gets all questions for a specific user
     * @param userId The ID of the user
     * @return List of QuestionData objects
     * @throws SQLException
     */
    public List<QuestionData> getQuestionsForUser(int userId) throws SQLException {
        List<QuestionData> questions = new ArrayList<>();
        String query = "SELECT q.*, " +
                      "(SELECT COUNT(*) FROM answers WHERE questionId = q.id) as answerCount " +
                      "FROM questions q " +
                      "WHERE q.userId = ? " +
                      "ORDER BY q.createDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                QuestionData question = new QuestionData(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    getUserName(rs.getInt("userId")),
                    rs.getTimestamp("createDate"),
                    rs.getBoolean("resolved"),
                    rs.getInt("answerCount")
                );
                questions.add(question);
            }
        }
        return questions;
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
    
    public QuestionData getQuestionById(int questionId) throws SQLException {
    	QuestionData question;
    	String query = "SELECT * FROM questions WHERE id = ?";
    	try(PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1, questionId);
    		
    		ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                question = new QuestionData(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    getUserName(rs.getInt("userId")),
                    rs.getTimestamp("createDate"),
                    rs.getBoolean("resolved"),
                    0
                );
                return question;
            }
    	}
    	
    	return null;
    }
    
    public AnswerData getAnswerById(int answerId) throws SQLException {
    	AnswerData answer;
    	String query = "SELECT * FROM answers WHERE id = ?";
    	try(PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1, answerId);
    		
    		ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	boolean needsClarification = false;
                try {
                    needsClarification = rs.getBoolean("needsClarification");
                } catch (SQLException e) {
                    // Column might not exist yet, just use default false
                    System.out.println("needsClarification column not available yet, using default false");
                }
                answer = new AnswerData(
            		rs.getInt("id"),
                    rs.getString("content"),
                    getUserName(rs.getInt("userId")),
                    rs.getTimestamp("createDate"),
                    needsClarification
                );
                return answer;
            }
    	}
    	
    	return null;
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
    
    public void addReviewer(int userId, double weight) throws SQLException {
        String query = "INSERT INTO reviewers (userId, weight) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setDouble(2, weight);
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
    
    // Review Management Methods
    
    public int addReview(int reviewerId, int questionId, int answerId, String content) throws SQLException {
    	String query = "INSERT INTO reviews (reviewerId, questionId, answerId, content) VALUES (?, ?, ?, ?)";
    	try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
    		pstmt.setInt(1, reviewerId);
    		pstmt.setInt(2, questionId);
    		pstmt.setInt(3, answerId);
    		pstmt.setString(4, content);
    		pstmt.executeUpdate();
    		
    		ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
    
    public int updateReview(int reviewId, int reviewerId, int questionId, int answerId, String content) throws SQLException {
    	String query = "UPDATE reviews SET content = ? WHERE id = ? AND reviewerId = ? AND questionId = ? AND answerId = ?";
    	try (PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setString(1, content);
    		pstmt.setInt(2, reviewId);
    		pstmt.setInt(3, reviewerId);
    		pstmt.setInt(4, questionId);
    		pstmt.setInt(5, answerId);
    		
    		int changedRows = pstmt.executeUpdate();
    		if (changedRows > 0) {
    			return reviewId;
    		} else {
    			return -1;
    		}
    	}
    }
    
    // Get Review methods
    public List<ReviewData> getReviewsForQuestion(int questionId) throws SQLException {
    	List<ReviewData> reviews = new ArrayList<>();
    	String query = "SELECT * FROM reviews WHERE questionId = ?";
    	try (PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1,  questionId);
    		
    		ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ReviewData review = new ReviewData(
                    rs.getInt("id"),
                    rs.getInt("reviewerId"),
                    questionId,
                    -1,
                    rs.getString("content")
                );
                reviews.add(review);
            }
    	}
    	
    	return reviews;
    }
    
    public List<ReviewData> getReviewsForAnswer(int answerId) throws SQLException {
    	List<ReviewData> reviews = new ArrayList<>();
    	String query = "SELECT * FROM reviews WHERE answerId = ?";
    	try (PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1,  answerId);
    		
    		ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ReviewData review = new ReviewData(
                    rs.getInt("id"),
                    rs.getInt("reviewerId"),
                    -1,
                    answerId,
                    rs.getString("content")
                );
                reviews.add(review);
            }
    	}
    	
    	return reviews;
    }
    
    /**
     * Get all reviews written by a specific reviewer
     * @param reviewerId The ID of the reviewer
     * @return List of ReviewData objects
     * @throws SQLException
     */
    public List<ReviewData> getReviewsByReviewer(int reviewerId) throws SQLException {
        List<ReviewData> reviews = new ArrayList<>();
        String query = "SELECT * FROM reviews WHERE reviewerId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, reviewerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ReviewData review = new ReviewData(
                    rs.getInt("id"),
                    reviewerId,
                    rs.getInt("questionId"),
                    rs.getInt("answerId"),
                    rs.getString("content")
                );
                reviews.add(review);
            }
        }
        
        return reviews;
    }
    
    public boolean deleteReview(int reviewId, int reviewerId, int questionId, int answerId) throws SQLException {
    	String query = "DELETE FROM reviews WHERE id = ? AND reviewerId = ? and questionId = ? and answerId = ?";
    	try (PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1, reviewId);
    		pstmt.setInt(2,  reviewerId);
    		pstmt.setInt(3,  questionId);
    		pstmt.setInt(4, answerId);
    		
    		return pstmt.executeUpdate() > 0;
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
    
    public String getUserName(int userId) throws SQLException {
    	String query = "SELECT * FROM cse360users WHERE id = ?";
    	try (PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setInt(1, userId);
    		ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("userName");
            }
    	} 
    	
    	return null;
    }
    
    public int getReviewerId(int userId) throws SQLException {
    	String query = "SELECT id FROM reviewers WHERE userId = ?";
    	try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
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
    
    /**
     * Sends a message from one user to another
     * @param senderId The ID of the sender
     * @param receiverId The ID of the receiver
     * @param relatedQuestionId The ID of the related question, or -1 if not related to a question
     * @param relatedAnswerId The ID of the related answer, or -1 if not related to an answer
     * @param content The message content
     * @return The ID of the new message, or -1 if failed
     * @throws SQLException
     */
    public int sendMessage(int senderId, int receiverId, int relatedQuestionId, int relatedAnswerId, String content) throws SQLException {
        String query = "INSERT INTO messages (senderId, receiverId, relatedQuestionId, relatedAnswerId, content) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setInt(3, relatedQuestionId);
            pstmt.setInt(4, relatedAnswerId);
            pstmt.setString(5, content);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
    
    /**
     * Gets all messages for a user
     * @param userId The ID of the user
     * @return List of MessageData objects
     * @throws SQLException
     */
    public List<MessageData> getMessagesForUser(int userId) throws SQLException {
        List<MessageData> messages = new ArrayList<>();
        String query = "SELECT m.*, " +
                       "sender.userName as senderName, " +
                       "receiver.userName as receiverName " +
                       "FROM messages m " +
                       "JOIN cse360users sender ON m.senderId = sender.id " +
                       "JOIN cse360users receiver ON m.receiverId = receiver.id " +
                       "WHERE m.receiverId = ? OR m.senderId = ? " +
                       "ORDER BY m.createDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MessageData message = new MessageData(
                    rs.getInt("id"),
                    rs.getInt("senderId"),
                    rs.getInt("receiverId"),
                    rs.getString("senderName"),
                    rs.getString("receiverName"),
                    rs.getInt("relatedQuestionId"),
                    rs.getInt("relatedAnswerId"),
                    rs.getString("content"),
                    rs.getBoolean("isRead"),
                    rs.getTimestamp("createDate")
                );
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * Gets all unread messages for a user
     * @param userId The ID of the user
     * @return List of MessageData objects
     * @throws SQLException
     */
    public List<MessageData> getUnreadMessagesForUser(int userId) throws SQLException {
        List<MessageData> messages = new ArrayList<>();
        String query = "SELECT m.*, " +
                       "sender.userName as senderName, " +
                       "receiver.userName as receiverName " +
                       "FROM messages m " +
                       "JOIN cse360users sender ON m.senderId = sender.id " +
                       "JOIN cse360users receiver ON m.receiverId = receiver.id " +
                       "WHERE m.receiverId = ? AND m.isRead = FALSE " +
                       "ORDER BY m.createDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MessageData message = new MessageData(
                    rs.getInt("id"),
                    rs.getInt("senderId"),
                    rs.getInt("receiverId"),
                    rs.getString("senderName"),
                    rs.getString("receiverName"),
                    rs.getInt("relatedQuestionId"),
                    rs.getInt("relatedAnswerId"),
                    rs.getString("content"),
                    rs.getBoolean("isRead"),
                    rs.getTimestamp("createDate")
                );
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * Marks a message as read
     * @param messageId The ID of the message
     * @param userId The ID of the user (to ensure they are the recipient)
     * @return true if successful, false otherwise
     * @throws SQLException
     */
    public boolean markMessageAsRead(int messageId, int userId) throws SQLException {
        String query = "UPDATE messages SET isRead = TRUE WHERE id = ? AND receiverId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, messageId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Gets user ID for question owner
     * @param questionId The ID of the question
     * @return The user ID of the question owner
     * @throws SQLException
     */
    public int getQuestionOwnerId(int questionId) throws SQLException {
        String query = "SELECT userId FROM questions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("userId");
            }
        }
        return -1;
    }

    /**
     * Gets user ID for answer owner
     * @param answerId The ID of the answer
     * @return The user ID of the answer owner
     * @throws SQLException
     */
    public int getAnswerOwnerId(int answerId) throws SQLException {
        String query = "SELECT userId FROM answers WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("userId");
            }
        }
        return -1;
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

class ReviewData {
	public final int id;
	public final int reviewerId;
	public final int questionId;
	public final int answerId;
	public final String content;
	
	public ReviewData(int id, int reviewerId, int questionId, int answerId, String content) {
		this.id = id;
		this.reviewerId = reviewerId;
		this.questionId = questionId;
		this.answerId = answerId;
		this.content = content;
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

class MessageData {
    public final int id;
    public final int senderId;
    public final int receiverId;
    public final String senderName;
    public final String receiverName;
    public final int relatedQuestionId;
    public final int relatedAnswerId;
    public final String content;
    public final boolean isRead;
    public final Timestamp createDate;

    public MessageData(int id, int senderId, int receiverId, String senderName, String receiverName, 
                      int relatedQuestionId, int relatedAnswerId, String content, 
                      boolean isRead, Timestamp createDate) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.relatedQuestionId = relatedQuestionId;
        this.relatedAnswerId = relatedAnswerId;
        this.content = content;
        this.isRead = isRead;
        this.createDate = createDate;
    }
}

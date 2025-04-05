package application;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.SQLException;
import java.util.List;

/**
 * JUnit test class for validating reviewer and messaging functionality in studentDatabase.
 */
public class ReviewerMessageTest {
    
    private studentDatabase dbHelper;
    private int testUserId;
    private int testReviewerId;
    private int testQuestionId;
    private int testAnswerId;
    
    /**
     * Sets up test environment with test user, question, answer, and reviewer
     */
    @Before
    public void setUp() throws SQLException {
        dbHelper = new studentDatabase();
        dbHelper.connectToDatabase();
        
        // Get or create test user
        testUserId = dbHelper.getUserId("testuser");
        if (testUserId == -1) {
            throw new RuntimeException("Test user not found in database");
        }
        
        // Make test user a reviewer
        dbHelper.addReviewer(testUserId, 1.0);
        testReviewerId = dbHelper.getReviewerId(testUserId);
        
        // Create test question and answer
        testQuestionId = dbHelper.addQuestion("Test Question Title", "Test Question Content", testUserId);
        testAnswerId = dbHelper.addAnswer("Test Answer Content", testQuestionId, testUserId);
    }
    
    /**
     * Cleans up test data after each test
     */
    @After
    public void tearDown() {
        try {
            // Clean up in reverse order of creation
            if (testAnswerId != -1) {
                dbHelper.deleteAnswer(testAnswerId);
            }
            if (testQuestionId != -1) {
                dbHelper.deleteQuestion(testQuestionId);
            }
            // Note: No direct way to delete reviewer in current implementation
            dbHelper.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests creating a review for a question
     */
    @Test
    public void creatingQuestionReviewTest() throws SQLException {
        String reviewContent = "This is a test question review";
        
        int reviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, reviewContent);
        assertTrue("Review ID should be greater than 0", reviewId > 0);
        
        // Verify review exists in database
        List<ReviewData> reviews = dbHelper.getReviewsForQuestion(testQuestionId);
        assertFalse("Should find at least one review", reviews.isEmpty());
        assertEquals(reviewContent, reviews.get(0).content);
    }
    
    /**
     * Tests creating a review for an answer
     */
    @Test
    public void creatingAnswerReviewTest() throws SQLException {
        String reviewContent = "This is a test answer review";
        
        int reviewId = dbHelper.addReview(testReviewerId, -1, testAnswerId, reviewContent);
        assertTrue("Review ID should be greater than 0", reviewId > 0);
        
        // Verify review exists in database
        List<ReviewData> reviews = dbHelper.getReviewsForAnswer(testAnswerId);
        assertFalse("Should find at least one review", reviews.isEmpty());
        assertEquals(reviewContent, reviews.get(0).content);
    }
    
    /**
     * Tests retrieving all reviews by a specific reviewer
     */
    @Test
    public void gettingReviewsByReviewerTest() throws SQLException {
        // Create two test reviews
        String qReviewContent = "Question review content";
        String aReviewContent = "Answer review content";
        
        int qReviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, qReviewContent);
        int aReviewId = dbHelper.addReview(testReviewerId, -1, testAnswerId, aReviewContent);
        
        // Get all reviews by this reviewer
        List<ReviewData> reviews = dbHelper.getReviewsByReviewer(testReviewerId);
        
        assertFalse("Should find at least two reviews", reviews.size() < 2);
        assertTrue("Should contain question review", 
            reviews.stream().anyMatch(r -> r.content.equals(qReviewContent)));
        assertTrue("Should contain answer review", 
            reviews.stream().anyMatch(r -> r.content.equals(aReviewContent)));
    }
    
    /**
     * Tests sending a message about a question
     */
    @Test
    public void sendingQuestionMessageTest() throws SQLException {
        String messageContent = "Test question message";
        
        int messageId = dbHelper.sendMessage(testReviewerId, testUserId, testQuestionId, -1, messageContent);
        assertTrue("Message ID should be greater than 0", messageId > 0);
        
        // Verify message exists for recipient
        List<MessageData> messages = dbHelper.getMessagesForUser(testUserId);
        assertFalse("Should find at least one message", messages.isEmpty());
        assertEquals(messageContent, messages.get(0).content);
        assertEquals(testQuestionId, messages.get(0).relatedQuestionId);
    }
    
    /**
     * Tests sending a message about an answer
     */
    @Test
    public void sendingAnswerMessageTest() throws SQLException {
        String messageContent = "Test answer message";
        
        int messageId = dbHelper.sendMessage(testReviewerId, testUserId, -1, testAnswerId, messageContent);
        assertTrue("Message ID should be greater than 0", messageId > 0);
        
        // Verify message exists for recipient
        List<MessageData> messages = dbHelper.getMessagesForUser(testUserId);
        assertFalse("Should find at least one message", messages.isEmpty());
        assertEquals(messageContent, messages.get(0).content);
        assertEquals(testAnswerId, messages.get(0).relatedAnswerId);
    }
    
}
package application;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import databasePart1.DatabaseHelper;

/**
 * Test class for verifying the Staff Home Page functionality meets all user stories.
 * This test class focuses on the business logic rather than the UI components.
 */
public class StaffHomePageTest {
    
    private studentDatabase dbHelper;
    private DatabaseHelper databaseHelper;
    private int testUserId;
    private int testQuestionId;
    private int testAnswerId;
    private int testReviewerId;
    private int testReviewId;
    
    /**
     * Set up the test environment with test data before each test.
     */
    @Before
    public void setUp() throws SQLException {
        // Initialize database helpers
        dbHelper = new studentDatabase();
        dbHelper.connectToDatabase();
        
        databaseHelper = new DatabaseHelper();
        databaseHelper.connectToDatabase();
        
        // Create test user if not exists
        testUserId = dbHelper.getUserId("testuser");
        if (testUserId == -1) {
            throw new RuntimeException("Test user not found in database");
        }
        
        // Create test reviewer
        testReviewerId = dbHelper.addReviewer(testUserId, 1.0);
        
        // Create test question and answer
        testQuestionId = dbHelper.addQuestion("Test Question Title", "Test Question Content", testUserId);
        testAnswerId = dbHelper.addAnswer("Test Answer Content", testQuestionId, testUserId);
        
        // Create test review
        testReviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, "Test Review Content");
    }
    
    /**
     * Clean up test data after each test.
     */
    @After
    public void tearDown() {
        try {
            // Clean up in reverse order of creation
            dbHelper.deleteReview(testReviewId, testReviewerId, testQuestionId, -1);
            dbHelper.deleteAnswer(testAnswerId);
            dbHelper.deleteQuestion(testQuestionId);
            dbHelper.deleteReviewer(testUserId);
            
            dbHelper.closeConnection();
            databaseHelper.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * User Story 1: Staff members should be able to see a comprehensive list of 
     * all questions, answers, and reviews to assess interactions between 
     * reviewers and students.
     */
    @Test
    public void testStaffCanViewAllContent() throws SQLException {
        // Test that staff can retrieve all questions
        List<QuestionData> questions = dbHelper.getQuestions();
        assertNotNull("Questions list should not be null", questions);
        assertFalse("Questions list should not be empty", questions.isEmpty());
        
        // Check if the test question is in the list
        boolean foundTestQuestion = false;
        for (QuestionData question : questions) {
            if (question.id == testQuestionId) {
                foundTestQuestion = true;
                break;
            }
        }
        assertTrue("The test question should be in the list", foundTestQuestion);
        
        // Test that staff can retrieve all answers for a question
        List<AnswerData> answers = dbHelper.getAnswersForQuestion(testQuestionId);
        assertNotNull("Answers list should not be null", answers);
        assertFalse("Answers list should not be empty", answers.isEmpty());
        
        // Check if the test answer is in the list
        boolean foundTestAnswer = false;
        for (AnswerData answer : answers) {
            if (answer.id == testAnswerId) {
                foundTestAnswer = true;
                break;
            }
        }
        assertTrue("The test answer should be in the list", foundTestAnswer);
        
        // Test that staff can retrieve all reviews for a question
        List<ReviewData> reviews = dbHelper.getReviewsForQuestion(testQuestionId);
        assertNotNull("Reviews list should not be null", reviews);
        assertFalse("Reviews list should not be empty", reviews.isEmpty());
        
        // Check if the test review is in the list
        boolean foundTestReview = false;
        for (ReviewData review : reviews) {
            if (review.id == testReviewId) {
                foundTestReview = true;
                break;
            }
        }
        assertTrue("The test review should be in the list", foundTestReview);
    }
    
    /**
     * User Story 2: Staff members should be able to make necessary changes to any
     * question, answer, or review to regulate inappropriate content.
     */
    @Test
    public void testStaffCanEditContent() throws SQLException {
        // Test that staff can edit questions
        String updatedQuestionContent = "Updated Test Question Content";
        dbHelper.updateQuestion(testQuestionId, "Updated Test Question Title", updatedQuestionContent);
        
        // Verify the question was updated
        QuestionData updatedQuestion = dbHelper.getQuestionById(testQuestionId);
        assertNotNull("Updated question should not be null", updatedQuestion);
        assertEquals("Question content should be updated", updatedQuestionContent, updatedQuestion.content);
        
        // Test that staff can edit answers
        String updatedAnswerContent = "Updated Test Answer Content";
        dbHelper.updateAnswer(testAnswerId, updatedAnswerContent);
        
        // Verify the answer was updated
        AnswerData updatedAnswer = dbHelper.getAnswerById(testAnswerId);
        assertNotNull("Updated answer should not be null", updatedAnswer);
        assertEquals("Answer content should be updated", updatedAnswerContent, updatedAnswer.content);
        
        // Test that staff can edit reviews
        String updatedReviewContent = "Updated Test Review Content";
        int result = dbHelper.updateReview(testReviewId, testReviewerId, testQuestionId, -1, updatedReviewContent);
        
        // Verify the update was successful
        assertTrue("Review update should return a valid ID", result > 0);
        
        // Verify the review was updated by retrieving it again
        List<ReviewData> updatedReviews = dbHelper.getReviewsForQuestion(testQuestionId);
        boolean reviewUpdated = false;
        for (ReviewData review : updatedReviews) {
            if (review.id == testReviewId && review.content.equals(updatedReviewContent)) {
                reviewUpdated = true;
                break;
            }
        }
        assertTrue("The review content should be updated", reviewUpdated);
        
        // Test that staff can delete content
        boolean reviewDeleted = dbHelper.deleteReview(testReviewId, testReviewerId, testQuestionId, -1);
        assertTrue("Review should be deleted successfully", reviewDeleted);
        
        // Verify the review was deleted
        List<ReviewData> reviewsAfterDeletion = dbHelper.getReviewsForQuestion(testQuestionId);
        boolean reviewStillExists = false;
        for (ReviewData review : reviewsAfterDeletion) {
            if (review.id == testReviewId) {
                reviewStillExists = true;
                break;
            }
        }
        assertFalse("The review should no longer exist", reviewStillExists);
        
        // Re-create the review for cleanup in tearDown
        testReviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, "Test Review Content");
    }
    
    /**
     * User Story 3: Staff members should have a way to message other staff,
     * instructors, or admins to address issues.
     */
    @Test
    public void testStaffCanSendMessages() throws SQLException {
        // Create a test recipient user (simulating another staff member)
        String recipientUserName = "testrecipient";
        int recipientUserId = -1;
        
        try {
            // Create the user if it doesn't exist
            if (!databaseHelper.doesUserExist(recipientUserName)) {
                User recipientUser = new User(recipientUserName, "password", "staff");
                databaseHelper.register(recipientUser);
            }
            
            recipientUserId = dbHelper.getUserId(recipientUserName);
            assertTrue("Recipient user ID should be valid", recipientUserId > 0);
            
            // Test that staff can send messages
            String messageContent = "Test message from staff";
            int messageId = dbHelper.sendMessage(testUserId, recipientUserId, -1, -1, messageContent);
            
            // Verify the message was sent successfully
            assertTrue("Message ID should be valid", messageId > 0);
            
            // Retrieve messages for the recipient
            List<MessageData> recipientMessages = dbHelper.getMessagesForUser(recipientUserId);
            assertNotNull("Recipient messages list should not be null", recipientMessages);
            assertFalse("Recipient messages list should not be empty", recipientMessages.isEmpty());
            
            // Check if the test message is in the list
            boolean foundTestMessage = false;
            for (MessageData message : recipientMessages) {
                if (message.id == messageId && message.content.equals(messageContent)) {
                    foundTestMessage = true;
                    break;
                }
            }
            assertTrue("The test message should be in the recipient's messages", foundTestMessage);
            
            // Test that staff can retrieve and read messages
            List<MessageData> senderMessages = dbHelper.getMessagesForUser(testUserId);
            assertNotNull("Sender messages list should not be null", senderMessages);
            assertFalse("Sender messages list should not be empty", senderMessages.isEmpty());
            
            // Mark a message as read
            boolean markResult = dbHelper.markMessageAsRead(messageId, recipientUserId);
            assertTrue("Message should be marked as read successfully", markResult);
            
            // Verify the message is now marked as read
            recipientMessages = dbHelper.getMessagesForUser(recipientUserId);
            boolean messageIsRead = false;
            for (MessageData message : recipientMessages) {
                if (message.id == messageId && message.isRead) {
                    messageIsRead = true;
                    break;
                }
            }
            assertTrue("The message should be marked as read", messageIsRead);
            
        } finally {
            // Clean up the test recipient if we created it
            if (recipientUserId > 0) {
                databaseHelper.deleteUser(recipientUserName);
            }
        }
    }
    
    /**
     * User Story 4: Staff members should have a polished UI that allows them
     * to view all content and take necessary actions.
     * 
     * Note: This test simulates UI interactions without actually creating UI components,
     * focusing on the business logic behind the UI actions.
     */
    @Test
    public void testStaffUIFunctionality() throws SQLException {
        // Test the logic behind the question filtering functionality
        List<QuestionData> allQuestions = dbHelper.getQuestions();
        assertNotNull("All questions list should not be null", allQuestions);
        
        // Filter for resolved questions (simulating UI filter action)
        List<QuestionData> resolvedQuestions = new ArrayList<>();
        for (QuestionData question : allQuestions) {
            if (question.resolved) {
                resolvedQuestions.add(question);
            }
        }
        
        // Filter for unresolved questions (simulating UI filter action)
        List<QuestionData> unresolvedQuestions = new ArrayList<>();
        for (QuestionData question : allQuestions) {
            if (!question.resolved) {
                unresolvedQuestions.add(question);
            }
        }
        
        // Verify that the sum of resolved and unresolved questions equals all questions
        assertEquals("Sum of resolved and unresolved questions should equal total", 
                     allQuestions.size(), resolvedQuestions.size() + unresolvedQuestions.size());
        
        // Test toggling question resolution (simulating UI action)
        boolean isInitiallyResolved = dbHelper.getQuestionById(testQuestionId).resolved;
        
        if (isInitiallyResolved) {
            dbHelper.unmarkResolved(testQuestionId);
        } else {
            dbHelper.markAnswerAsResolved(testQuestionId, testAnswerId);
        }
        
        // Verify the question resolution status was toggled
        boolean isNowResolved = dbHelper.getQuestionById(testQuestionId).resolved;
        assertNotEquals("Question resolution status should be toggled", isInitiallyResolved, isNowResolved);
        
        // Test toggling answer clarification flag (simulating UI action)
        boolean initialClarificationStatus = dbHelper.getAnswerById(testAnswerId).needsClarification;
        
        dbHelper.markAnswerNeedsClarification(testAnswerId, !initialClarificationStatus);
        
        // Verify the clarification flag was toggled
        boolean newClarificationStatus = dbHelper.getAnswerById(testAnswerId).needsClarification;
        assertNotEquals("Answer clarification flag should be toggled", 
                        initialClarificationStatus, newClarificationStatus);
        
        // Reset state for cleanup
        if (isInitiallyResolved != isNowResolved) {
            if (isInitiallyResolved) {
                dbHelper.markAnswerAsResolved(testQuestionId, testAnswerId);
            } else {
                dbHelper.unmarkResolved(testQuestionId);
            }
        }
        
        dbHelper.markAnswerNeedsClarification(testAnswerId, initialClarificationStatus);
    }
}
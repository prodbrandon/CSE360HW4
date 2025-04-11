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
        dbHelper = new studentDatabase();
        dbHelper.connectToDatabase();

        databaseHelper = new DatabaseHelper();
        databaseHelper.connectToDatabase();

        // Ensure test user exists
        if (!databaseHelper.doesUserExist("testuser")) {
            databaseHelper.register(new User("testuser", "password", "staff"));
        }
        testUserId = dbHelper.getUserId("testuser");

        // Ensure reviewer exists
        testReviewerId = dbHelper.addReviewer(testUserId, 1.0);

        // Create test question, answer, review
        testQuestionId = dbHelper.addQuestion("Test Question Title", "Test Question Content", testUserId);
        testAnswerId = dbHelper.addAnswer("Test Answer Content", testQuestionId, testUserId);
        testReviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, "Test Review Content");
    }

    /**
     * Clean up test data after each test.
     */
    @After
    public void tearDown() {
        try {
            dbHelper.deleteReview(testReviewId, testReviewerId, testQuestionId, -1);
            dbHelper.deleteAnswer(testAnswerId);
            dbHelper.deleteQuestion(testQuestionId);
            dbHelper.deleteReviewer(testReviewerId);
            dbHelper.closeConnection();
            databaseHelper.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStaffCanViewAllContent() throws SQLException {
        List<QuestionData> questions = dbHelper.getQuestions();
        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        boolean foundQuestion = questions.stream().anyMatch(q -> q.id == testQuestionId);
        assertTrue(foundQuestion);

        List<AnswerData> answers = dbHelper.getAnswersForQuestion(testQuestionId);
        assertNotNull(answers);
        assertFalse(answers.isEmpty());

        boolean foundAnswer = answers.stream().anyMatch(a -> a.id == testAnswerId);
        assertTrue(foundAnswer);

        List<ReviewData> reviews = dbHelper.getReviewsForQuestion(testQuestionId);
        assertNotNull(reviews);
        assertFalse(reviews.isEmpty());

        boolean foundReview = reviews.stream().anyMatch(r -> r.id == testReviewId);
        assertTrue(foundReview);
    }

    @Test
    public void testStaffCanEditContent() throws SQLException {
        String updatedQuestion = "Updated Question Content";
        dbHelper.updateQuestion(testQuestionId, "Updated Title", updatedQuestion);
        QuestionData q = dbHelper.getQuestionById(testQuestionId);
        assertEquals(updatedQuestion, q.content);

        String updatedAnswer = "Updated Answer Content";
        dbHelper.updateAnswer(testAnswerId, updatedAnswer);
        AnswerData a = dbHelper.getAnswerById(testAnswerId);
        assertEquals(updatedAnswer, a.content);

        String updatedReview = "Updated Review Content";
        int updateResult = dbHelper.updateReview(testReviewId, testReviewerId, testQuestionId, -1, updatedReview);
        assertTrue(updateResult > 0);

        List<ReviewData> updatedReviews = dbHelper.getReviewsForQuestion(testQuestionId);
        boolean foundUpdated = updatedReviews.stream().anyMatch(r -> r.id == testReviewId && r.content.equals(updatedReview));
        assertTrue(foundUpdated);

        boolean deleted = dbHelper.deleteReview(testReviewId, testReviewerId, testQuestionId, -1);
        assertTrue(deleted);

        List<ReviewData> afterDelete = dbHelper.getReviewsForQuestion(testQuestionId);
        boolean stillExists = afterDelete.stream().anyMatch(r -> r.id == testReviewId);
        assertFalse(stillExists);

        testReviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, "Test Review Content");
    }

    @Test
    public void testStaffCanSendMessages() throws SQLException {
        String recipientUserName = "testrecipient";
        int recipientUserId = -1;

        try {
            if (!databaseHelper.doesUserExist(recipientUserName)) {
                databaseHelper.register(new User(recipientUserName, "password", "staff"));
            }
            recipientUserId = dbHelper.getUserId(recipientUserName);
            assertTrue(recipientUserId > 0);

            String content = "Test message from staff";
            int messageId = dbHelper.sendMessage(testUserId, recipientUserId, -1, -1, content);
            assertTrue(messageId > 0);

            List<MessageData> messages = dbHelper.getMessagesForUser(recipientUserId);
            assertTrue(messages.stream().anyMatch(m -> m.id == messageId && m.content.equals(content)));

            boolean marked = dbHelper.markMessageAsRead(messageId, recipientUserId);
            assertTrue(marked);

            messages = dbHelper.getMessagesForUser(recipientUserId);
            boolean isRead = messages.stream().anyMatch(m -> m.id == messageId && m.isRead);
            assertTrue(isRead);
        } finally {
            if (recipientUserId > 0) {
                try {
                    databaseHelper.deleteUser(recipientUserName);
                } catch (Exception ignored) {}
            }
        }
    }

    @Test
    public void testStaffUIFunctionality() throws SQLException {
        List<QuestionData> all = dbHelper.getQuestions();
        List<QuestionData> resolved = new ArrayList<>();
        List<QuestionData> unresolved = new ArrayList<>();

        for (QuestionData q : all) {
            if (q.resolved) resolved.add(q);
            else unresolved.add(q);
        }

        assertEquals(all.size(), resolved.size() + unresolved.size());

        boolean wasResolved = dbHelper.getQuestionById(testQuestionId).resolved;
        if (wasResolved) dbHelper.unmarkResolved(testQuestionId);
        else dbHelper.markAnswerAsResolved(testQuestionId, testAnswerId);

        boolean isNowResolved = dbHelper.getQuestionById(testQuestionId).resolved;
        assertNotEquals(wasResolved, isNowResolved);

        boolean wasClarification = dbHelper.getAnswerById(testAnswerId).needsClarification;
        dbHelper.markAnswerNeedsClarification(testAnswerId, !wasClarification);
        boolean isClarificationNow = dbHelper.getAnswerById(testAnswerId).needsClarification;
        assertNotEquals(wasClarification, isClarificationNow);

        if (wasResolved != isNowResolved) {
            if (wasResolved) dbHelper.markAnswerAsResolved(testQuestionId, testAnswerId);
            else dbHelper.unmarkResolved(testQuestionId);
        }

        dbHelper.markAnswerNeedsClarification(testAnswerId, wasClarification);
    }
}

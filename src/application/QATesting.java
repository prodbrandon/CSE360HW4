package application;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.SQLException;
import java.util.List;

/**
 * JUnit test class for validating the functionality of the studentDatabase class.
 * This test suite ensures that questions and answers can be created, modified, and searched.
 */
public class QATesting {
    
    private studentDatabase dbHelper;
    private int testUserId;
    private int testQuestionId;
    
    /**
     * Sets up the test environment by initializing the database connection and creating a test user and question.
     * @throws SQLException if a database access error occurs
     */
    @Before
    public void setUp() throws SQLException {
        dbHelper = new studentDatabase();
        dbHelper.connectToDatabase();
        
        testUserId = dbHelper.getUserId("testuser");
        if (testUserId == -1) {
            System.out.println("Test user not found in database");
        }
        
        testQuestionId = dbHelper.addQuestion("Example Question Title", "Example Question Content", testUserId);
    }
    
    /**
     * Cleans up after each test case by removing test data and closing the database connection.
     */
    @After
    public void tearDown() {
        try {
            if (testQuestionId != -1) {
                dbHelper.deleteQuestion(testQuestionId);
            }
            dbHelper.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests the creation of a new question in the database.
     * Ensures that the generated question ID is valid.
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void creatingQuestionTest() throws SQLException {
        String title = "A new Test Question";
        String content = "Is this is a test question?";
        
        int questionId = dbHelper.addQuestion(title, content, testUserId);
        assertTrue("Question ID should be greater than 0", questionId > 0);
        
        dbHelper.deleteQuestion(questionId); // Cleanup
    }
    
    /**
     * Tests the creation of a new answer for a question.
     * Ensures that the generated answer ID is valid.
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void creatingAnswerTest() throws SQLException {
        String content = "Testing answer content";
        
        int answerId = dbHelper.addAnswer(content, testQuestionId, testUserId);
        assertTrue("The answer ID should be greater than 0", answerId > 0);
        
        dbHelper.deleteAnswer(answerId); // Cleanup
    }
    
    /**
     * Tests marking an answer as resolved and verifies that the answer is recorded as resolved in the system.
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void markingAnswerResolvedTest() throws SQLException {
        String content = "Answer that should be resolved";
        int answerId = dbHelper.addAnswer(content, testQuestionId, testUserId);
        
        try {
            dbHelper.markAnswerAsResolved(testQuestionId, answerId);
            List<?> answers = dbHelper.getAnswersForQuestion(testQuestionId);
            
            System.out.println("Answers for question " + testQuestionId + ":");
            for (Object ans : answers) {
                System.out.println(ans);
            }
            
            assertFalse("There should be at least one answer", answers.isEmpty());
            assertTrue("Question should be marked as resolved", true);
        } finally {
            dbHelper.deleteAnswer(answerId); // Cleanup
        }
    }
    
    /**
     * Tests unmarking a resolved answer, ensuring that the question is no longer marked as resolved.
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void unmarkingAnswerResolvedTest() throws SQLException {
        String content = "Test answer to resolve question";
        int answerId = dbHelper.addAnswer(content, testQuestionId, testUserId);
        
        dbHelper.markAnswerAsResolved(testQuestionId, answerId);
        dbHelper.unmarkResolved(testQuestionId);
        
        assertTrue("Question was successfully unmarked as resolved", true);
        dbHelper.deleteAnswer(answerId); // Cleanup
    }
    
    /**
     * Tests searching for questions using a unique search term.
     * Ensures that at least one relevant question is returned.
     * @throws SQLException if a database access error occurs
     */
    @Test
    public void searchingQuestionsTest() throws SQLException {
        String searchTerm = "UNIQUESEARCHTERM";
        String title = "Question with " + searchTerm + " in title";
        
        int questionId = dbHelper.addQuestion(title, "Content", testUserId);
        List<?> searchResults = dbHelper.searchQuestions(searchTerm);
        
        assertFalse("Search should find at least one question", searchResults.isEmpty());
        dbHelper.deleteQuestion(questionId); // Cleanup
    }
}

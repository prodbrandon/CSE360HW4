package test;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import application.studentDatabase;
import application.StudentHomePage;

/**
 * Test class for StudentHomePage functionality.
 * This is a simplified version that doesn't depend on TestFX.
 */
public class StudentHomePageUITest {
    
    private studentDatabase dbHelper;
    private int testUserId;
    
    @Before
    public void setUp() throws SQLException {
        dbHelper = new studentDatabase();
        dbHelper.connectToDatabase();
        
        testUserId = dbHelper.getUserId("testuser");
        if (testUserId == -1) {
            System.out.println("Test user not found in database");
        }
    }
    
    @After
    public void tearDown() {
        try {
            dbHelper.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testDatabaseConnection() {
        assertNotNull("Database helper should not be null", dbHelper);
        
        try {
            // Use raw type List
            List questions = dbHelper.getQuestions();
            assertNotNull("Should be able to retrieve questions", questions);
        } catch (SQLException e) {
            fail("Database operation failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testQuestionCreation() {
        try {
            String title = "Test Question for UI";
            String content = "This is a test question for the UI test";
            
            int questionId = dbHelper.addQuestion(title, content, testUserId);
            assertTrue("Question ID should be greater than 0", questionId > 0);
            
            dbHelper.deleteQuestion(questionId);
        } catch (SQLException e) {
            fail("Question creation failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testSearchFunctionality() {
        try {
            String searchTerm = "UNIQUEUISEARCHTERM";
            String title = "UI Test Question with " + searchTerm;
            int questionId = dbHelper.addQuestion(title, "Content for search test", testUserId);
            
            // Use raw type List
            List results = dbHelper.searchQuestions(searchTerm);
            
            assertFalse("Search should return results", results.isEmpty());
            
            dbHelper.deleteQuestion(questionId);
        } catch (SQLException e) {
            fail("Search test failed: " + e.getMessage());
        }
    }
}
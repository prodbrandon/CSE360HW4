package application;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReviewTesting {

	private studentDatabase dbHelper;
    private int testUserId;
    private int testQuestionId;
    private int testReviewerId;
    
    /**
     * Creates the initial database connection and creates
     * the testUser, if not already created, and the testQuestion.
     * 
     * @throws SQLException if the database is unable to connect or perform the operation.
     */
    @Before
    public void setUp() throws SQLException {
        dbHelper = new studentDatabase();
        dbHelper.connectToDatabase();
        
        testUserId = dbHelper.getUserId("testuser");
        if (testUserId == -1) {
            System.out.println("Test user not found in database");
        } 
        
        testReviewerId = dbHelper.addReviewer(testUserId, 5.0);
        if (testReviewerId == -1) {
        	System.out.println("Failed to add test reviewer");
        }
        
        testQuestionId = dbHelper.addQuestion("Test Question Title", "Test Question Content", testUserId);
    }
    
    /**
     * Removes the created testQuestion and closes the
     * database connection.
     */
    @After
    public void tearDown() {
        try {
            if (testQuestionId != -1) {
                dbHelper.deleteQuestion(testQuestionId);
            }
            dbHelper.deleteReviewer(testUserId);
            
            dbHelper.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests adding a review to the database. Expects a not -1 return as the reviewId.
     * 
     * @throws SQLException if adding the review fails.
     */
    @Test
    public void testAddReview() {
    	try {
			int reviewId = dbHelper.addReview(testReviewerId, -1, -1, "Test Review");
			assertNotEquals("Return ID should not be -1", -1, reviewId);
			
			dbHelper.deleteReview(reviewId, testReviewerId, -1, -1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    /**
     * Tests updating an existing review. Expects the updated reviewId as return.
     * 
     * @throws SQLException if updating the review fails.
     */
    @Test
    public void testUpdateReview() {
    	try {
			int reviewId = dbHelper.addReview(testReviewerId, -1, -1, "Test Review");
			int returnId = dbHelper.updateReview(reviewId, testReviewerId, -1, -1, "New Test");
			assertEquals("Return ID from update should = reviewId", reviewId, returnId);
			
			dbHelper.deleteReview(reviewId, testReviewerId, -1, -1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Tests deleting a review. Expects true to return meaning the deletion completed.
     * 
     * @throws SQLException if the deletion fails.
     */
    @Test
    public void testDeleteReview() {
    	try {
			int reviewId = dbHelper.addReview(testReviewerId, -1, -1, "Test Review");
			assertTrue(dbHelper.deleteReview(reviewId, testReviewerId, -1, -1));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Tests getting the reviews for a question. Expects a list of 1 review from the database.
     * 
     * @throws SQLException if the database errors when fetching the reviews.
     */
    @Test
    public void testGetReviewsForQuestion() {
    	try {
			int reviewId = dbHelper.addReview(testReviewerId, testQuestionId, -1, "Test Review");
			List<ReviewData> reviews = dbHelper.getReviewsForQuestion(testQuestionId);
			assertEquals("Reviews list should have 1 review", 1, reviews.size());
			
			dbHelper.deleteReview(reviewId, testReviewerId, testQuestionId, -1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Tests getting the reviews for an answer. Expects a list of 1 review from the database.
     * 
     * @throws SQLException if the database errors when fetching the reviews.
     */
    @Test
    public void testGetReviewsForAnswer() {
    	try {
    		int answerId = dbHelper.addAnswer("Test Answer", testQuestionId, testUserId);
			int reviewId = dbHelper.addReview(testReviewerId, -1, answerId, "Test Review");
			List<ReviewData> reviews = dbHelper.getReviewsForAnswer(answerId);
			assertEquals("Reviews list should have 1 review", 1, reviews.size());
			
			dbHelper.deleteReview(reviewId, testReviewerId, -1, answerId);
			dbHelper.deleteAnswer(answerId);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}

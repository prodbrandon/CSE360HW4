package application;
import static org.junit.Assert.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.*;
import databasePart1.DatabaseHelper;
import application.ReviewerRequest;

public class ReviewerSystemTests {
	    private static DatabaseHelper dbHelper;
	    private static final String TEST_USER = "testStudent";
	    private static final String TEST_INSTRUCTOR = "testInstructor";
	    private static int testStudentId;

	    @BeforeClass
	    public static void setupClass() throws SQLException {
	        dbHelper = new DatabaseHelper();
	        dbHelper.connectToDatabase();
	        
	        // Setup test data
	        dbHelper.register(new User(TEST_USER, "password", "student"));
	        testStudentId = dbHelper.getUserId(TEST_USER);
	        dbHelper.register(new User(TEST_INSTRUCTOR, "password", "instructor"));
	    }

	    @After
	    public void cleanup() throws SQLException {
	        // Clearing the reviewer requests between tests
	        dbHelper.getConnection().prepareStatement("DELETE FROM reviewer_requests").executeUpdate();
	        // Resetting the user roles
	        dbHelper.getConnection().prepareStatement("UPDATE cse360users SET role = 'student' WHERE userName = '" + TEST_USER + "'").executeUpdate();
	    }

	    @Test
	    public void testReviewerRequestSubmission() throws SQLException {
	        // Submitting the request
	        dbHelper.submitReviewerRequest(testStudentId, "I have experience helping peers");
	        
	        // Making sre the request is in database
	        List<ReviewerRequest> requests = dbHelper.getPendingReviewerRequests();
	        assertEquals(1, requests.size());
	        assertEquals("PENDING", requests.get(0).getStatus());
	        assertEquals(testStudentId, requests.get(0).getUserId());
	    }

	    @Test
	    public void testMakingRequestEntity() throws SQLException {
	        Timestamp testingTime = Timestamp.valueOf(LocalDateTime.now());
	        ReviewerRequest request = new ReviewerRequest(
	            1, testStudentId, TEST_USER, "Qualified", 
	            testingTime, "PENDING", null
	        );
	        
	        assertEquals(1, request.getId());
	        assertEquals("Qualified", request.getJustify());
	        assertEquals("PENDING", request.getStatus());
	    }

	    @Test
	    public void testApprovingRequestUpdatesRole() throws SQLException {
	        // Making the test request
	        dbHelper.submitReviewerRequest(testStudentId, "Good qualifications");
	        ReviewerRequest request = dbHelper.getPendingReviewerRequests().get(0);
	        
	        // Approving the request
	        dbHelper.updateReviewerRequestStatus(request.getId(), "APPROVED", "Good track record");
	        dbHelper.addUserRole(request.getUserId(), "reviewer");
	        
	        // Making sure it updates
	        String roles = dbHelper.getUserRole(TEST_USER);
	        assertTrue(roles.contains("reviewer"));
	        
	        List<ReviewerRequest> updatedRequests = dbHelper.getPendingReviewerRequests();
	        assertEquals(0, updatedRequests.size());
	    }

	    @Test
	    public void testRejectingRequestWithComments() throws SQLException {
	        dbHelper.submitReviewerRequest(testStudentId, "New request");
	        ReviewerRequest request = dbHelper.getPendingReviewerRequests().get(0);
	        
	        // Rejecting the request
	        dbHelper.updateReviewerRequestStatus(request.getId(), "REJECTED", "Needs more contributions");
	        
	        PreparedStatement statement = dbHelper.getConnection().prepareStatement(
	            "SELECT status, instructorComments FROM reviewer_requests WHERE id = ?");
	        statement.setInt(1, request.getId());
	        ResultSet rs = statement.executeQuery();
	        
	        assertTrue(rs.next());
	        assertEquals("REJECTED", rs.getString("status"));
	        assertEquals("Needs more contributions", rs.getString("instructorComments"));
	    }

//	    @Test
//	    public void testRequestValidationInUI() {
//	        // Testing the form validation logic
//	        assertTrue(RequestReviewerRolePage.validateForm("Valid justification", true));
//	        assertFalse(RequestReviewerRolePage.validateForm("", true)); 
//	        assertFalse(RequestReviewerRolePage.validateForm("Valid", false));
//	    }

	    @AfterClass
	    public static void tearDownClass() throws SQLException {
	        // Cleanup of test users
	        dbHelper.deleteUser(TEST_USER);
	        dbHelper.deleteUser(TEST_INSTRUCTOR);
	        dbHelper.closeConnection();
	    }
}
// ReviewerRequest.java
package application;

import java.sql.Timestamp;

public class ReviewerRequest {
    private final int id;
    private final int userId;
    private final String userName;
    private final String justify;
    private final Timestamp requestDate;
    private final String status;
    private final String instructorComments;

    public ReviewerRequest(int id, int userId, String userName, String justify, 
                          Timestamp requestDate, String status, String instructorComments) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.justify = justify;
        this.requestDate = requestDate;
        this.status = status;
        this.instructorComments = instructorComments;
    }

    // Getters
    public int getId() { 
    	return id; 
    }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getJustify() { return justify; }
    public Timestamp getRequestDate() { return requestDate; }
    public String getStatus() { return status; }
    public String getInstructorComments() { return instructorComments; }
}
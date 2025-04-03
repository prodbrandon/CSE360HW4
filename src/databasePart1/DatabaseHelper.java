package databasePart1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Importing the User class from application package
import application.User;
import application.ReviewerRequest;

/**
 * The DatabaseHelper class manages database operations including user authentication,
 * role management, invitation code handling, and reviewer request management.
 */
public class DatabaseHelper {
    static final String JDBC_DRIVER = "org.h2.Driver";   
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

    // Database credentials 
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
            System.out.println("Database connection and setup complete");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            throw new SQLException("JDBC Driver not found", e);
        }
    }

    private void createTables() throws SQLException {
        // Users table with increased role column size
        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userName VARCHAR(255) UNIQUE, "
                + "password VARCHAR(255), "
                + "role VARCHAR(255))";
        statement.execute(userTable);
        
        // Invitation codes table
        String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
                + "code VARCHAR(10) PRIMARY KEY, "
                + "isUsed BOOLEAN DEFAULT FALSE)";
        statement.execute(invitationCodesTable);
        
        // One-time passwords table
        String oneTimePasswordsTable = "CREATE TABLE IF NOT EXISTS OneTimePasswords ("
                + "userName VARCHAR(255) UNIQUE, "
                + "oneTimePassword VARCHAR(10) PRIMARY KEY, "
                + "isUsed BOOLEAN DEFAULT FALSE)";
        statement.execute(oneTimePasswordsTable);

        // Reviewer requests table
        String reviewerRequestsTable = "CREATE TABLE IF NOT EXISTS reviewer_requests ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userId INT NOT NULL, "
                + "justification TEXT NOT NULL, "
                + "requestDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "status VARCHAR(20) DEFAULT 'PENDING', "
                + "instructorComments TEXT, "
                + "FOREIGN KEY (userId) REFERENCES cse360users(id))";
        statement.execute(reviewerRequestsTable);
    }

    public Connection getConnection() {
        return this.connection;
    }

    public boolean isDatabaseEmpty() throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM cse360users";
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet.next() && resultSet.getInt("count") == 0;
    }

    // ========== USER MANAGEMENT METHODS ==========
    public void register(User user) throws SQLException {
        String insertUser = "INSERT INTO cse360users (userName, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.executeUpdate();
        }
    }

    public boolean login(User user) throws SQLException {
        String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public String getUserRole(String userName) throws SQLException {
        String query = "SELECT role FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        }
        return null;
    }

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

    // ========== REVIEWER REQUEST METHODS ==========
    public void submitReviewerRequest(int userId, String justification) throws SQLException {
        String query = "INSERT INTO reviewer_requests (userId, justification) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, justification);
            pstmt.executeUpdate();
        }
    }

    public List<ReviewerRequest> getPendingReviewerRequests() throws SQLException {
        List<ReviewerRequest> requests = new ArrayList<>();
        String query = "SELECT r.*, u.userName FROM reviewer_requests r "
                + "JOIN cse360users u ON r.userId = u.id "
                + "WHERE r.status = 'PENDING' "
                + "ORDER BY r.requestDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(new ReviewerRequest(
                    rs.getInt("id"),
                    rs.getInt("userId"),
                    rs.getString("userName"),
                    rs.getString("justification"),
                    rs.getTimestamp("requestDate"),
                    rs.getString("status"),
                    rs.getString("instructorComments")
                ));
            }
        }
        return requests;
    }

    public void updateReviewerRequestStatus(int requestId, String status, String comments) throws SQLException {
        String query = "UPDATE reviewer_requests SET status = ?, instructorComments = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setString(2, comments);
            pstmt.setInt(3, requestId);
            pstmt.executeUpdate();
        }
    }

    public void addUserRole(int userId, String newRole) throws SQLException {
        String currentRoles = getUserRoleById(userId);
        if (currentRoles == null || !currentRoles.contains(newRole)) {
            String updatedRoles = currentRoles == null ? newRole : currentRoles + "," + newRole;
            String query = "UPDATE cse360users SET role = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, updatedRoles);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
            }
        }
    }

    private String getUserRoleById(int userId) throws SQLException {
        String query = "SELECT role FROM cse360users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        }
        return null;
    }

    // ========== OTHER DATABASE OPERATIONS ==========
    public List<User> getUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT userName, password, role FROM cse360users";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(new User(
                    rs.getString("userName"),
                    rs.getString("password"),
                    rs.getString("role")
                ));
            }
        }
        return users;
    }

    public void closeConnection() {
        try {
            if (statement != null) statement.close();
            if (connection != null) connection.close();
            System.out.println("Database connection closed");
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }
    
    // Additional methods for functionality (invitation codes, OTP, etc.)
    public String generateInvitationCode() {
        // Generate a random 6-character alphanumeric code
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        try {
            String query = "INSERT INTO InvitationCodes (code, isUsed) VALUES (?, FALSE)";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, code);
                pstmt.executeUpdate();
            }
            return code;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public boolean validateInvitationCode(String code) {
        try {
            String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Mark the code as used
                    String updateQuery = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
                    try (PreparedStatement updatePstmt = connection.prepareStatement(updateQuery)) {
                        updatePstmt.setString(1, code);
                        updatePstmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public String generateOTP(String userName) {
        try {
            // Check if user already has an OTP
            String checkQuery = "SELECT * FROM OneTimePasswords WHERE userName = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(checkQuery)) {
                pstmt.setString(1, userName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return ""; // User already has an OTP
                }
            }
            
            // Generate a random 6-character OTP
            String otp = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            
            String insertQuery = "INSERT INTO OneTimePasswords (userName, oneTimePassword, isUsed) VALUES (?, ?, FALSE)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
                pstmt.setString(1, userName);
                pstmt.setString(2, otp);
                pstmt.executeUpdate();
            }
            return otp;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public boolean validateOTP(String userName, String otp) {
        try {
            String query = "SELECT * FROM OneTimePasswords WHERE userName = ? AND oneTimePassword = ? AND isUsed = FALSE";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, userName);
                pstmt.setString(2, otp);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Mark the OTP as used
                    String updateQuery = "UPDATE OneTimePasswords SET isUsed = TRUE WHERE oneTimePassword = ?";
                    try (PreparedStatement updatePstmt = connection.prepareStatement(updateQuery)) {
                        updatePstmt.setString(1, otp);
                        updatePstmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public void updatePassword(String userName, String newPassword) {
        try {
            String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, newPassword);
                pstmt.setString(2, userName);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean doesUserExist(String userName) {
        try {
            String query = "SELECT * FROM cse360users WHERE userName = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, userName);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean isLastAdmin(String userName) {
        try {
            String query = "SELECT COUNT(*) AS adminCount FROM cse360users WHERE role LIKE '%admin%'";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("adminCount") <= 1) {
                    // Check if this user is an admin
                    String userRoleQuery = "SELECT role FROM cse360users WHERE userName = ?";
                    try (PreparedStatement userPstmt = connection.prepareStatement(userRoleQuery)) {
                        userPstmt.setString(1, userName);
                        ResultSet userRs = userPstmt.executeQuery();
                        if (userRs.next()) {
                            String role = userRs.getString("role");
                            return role != null && role.contains("admin");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public void deleteUser(String userName) {
        try {
            String query = "DELETE FROM cse360users WHERE userName = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, userName);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateUserRoles(String userName, ArrayList<String> roles, String currentUserName) {
        try {
            // Combine roles into a comma-separated string
            StringBuilder roleString = new StringBuilder();
            for (int i = 0; i < roles.size(); i++) {
                roleString.append(roles.get(i));
                if (i < roles.size() - 1) {
                    roleString.append(",");
                }
            }
            
            String query = "UPDATE cse360users SET role = ? WHERE userName = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, roleString.toString());
                pstmt.setString(2, userName);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
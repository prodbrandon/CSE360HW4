package databasePart1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import application.User;

/**
 * The DatabaseHelper class manages database operations including user authentication,
 * role management, and invitation code handling.
 */
public class DatabaseHelper {
    // JDBC driver name and database URL 
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
            // To reset database, uncomment next line:
            //statement.execute("DROP ALL OBJECTS");
            createTables();
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        // Users table with increased role column size
        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userName VARCHAR(255) UNIQUE, "
                + "password VARCHAR(255), "
                + "role VARCHAR(255))";  // Changed from VARCHAR(20) to VARCHAR(255)
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
    }

    public boolean isDatabaseEmpty() throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM cse360users";
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getInt("count") == 0;
        }
        return true;
    }

    public void register(User user) throws SQLException {
        String insertUser = "INSERT INTO cse360users (userName, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.executeUpdate();
            printAllUsers();
        }
    }
    
    // Delete a user from the database
    public void deleteUser(String userName) {
    	String query = "DELETE FROM cse360users WHERE userName = ?";
    	try(PreparedStatement pstmt = connection.prepareStatement(query)) {
    		pstmt.setString(1,  userName);;
    		int rows = pstmt.executeUpdate();
    		if(rows > 0) {
    			System.out.println("User " + userName + "has been deleted.");
    		}
    		else {
    			System.out.println("No user found with username: " + userName);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Problem deleting the user " + userName + ": " + e.getMessage());
    		e.printStackTrace();
    	}
    }

    public boolean login(User user) throws SQLException {
        String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ? AND role = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean doesUserExist(String userName) {
        String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getUserRole(String userName) {
        String query = "SELECT role FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String generateInvitationCode() {
        String code = UUID.randomUUID().toString().substring(0, 4);
        String query = "INSERT INTO InvitationCodes (code) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return code;
    }

    public boolean validateInvitationCode(String code) {
        String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                markInvitationCodeAsUsed(code);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void markInvitationCodeAsUsed(String code) {
        String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    // Generate a one-time password for a certain username only if it does not already have one
    public String generateOTP(String userName) {
        String query = "SELECT oneTimePassword FROM OneTimePasswords WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }

        String onetimePassword = UUID.randomUUID().toString().substring(0, 10);
        query = "INSERT INTO OneTimePasswords (userName, oneTimePassword) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            pstmt.setString(2, onetimePassword);
            pstmt.executeUpdate();
            return onetimePassword;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Ensure a one-time password exists and is associated with the given username
    public boolean validateOTP(String userName, String oneTimePassword) {
        String query = "SELECT * FROM OneTimePasswords WHERE userName = ? AND oneTimePassword = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            pstmt.setString(2, oneTimePassword);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                markOTPAsUsed(oneTimePassword);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Remove a used one-time password
    private void markOTPAsUsed(String oneTimePassword) {
        String query = "DELETE FROM OneTimePasswords WHERE oneTimePassword = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, oneTimePassword);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Determine if there is only one admin left or not 
    public boolean isLastAdmin(String userName) {
        String query = "SELECT COUNT(*) FROM cse360users WHERE role LIKE '%admin%'";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int adminCount = rs.getInt(1);
                return adminCount == 1 && getUserRole(userName).contains("admin");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Update the roles for a user
    public void updateUserRoles(String userName, List<String> roles, String currentUserName) {
        // Check if this user is the last admin
        boolean isLastAdmin = isLastAdmin(userName);
        
        // If this is the last admin and we're trying to remove admin role
        if (isLastAdmin && !roles.contains("admin")) {
            System.out.println("Cannot remove admin role from the last admin user");
            return;
        }
        
        // If user is trying to modify their own roles
        if (userName.equals(currentUserName)) {
            // If they're an admin and trying to remove their admin role
            if (getUserRole(userName).contains("admin") && !roles.contains("admin")) {
                System.out.println("Cannot remove your own admin role");
                return;
            }
        }

        String roleString = String.join(",", roles);
        String query = "UPDATE cse360users SET role = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, roleString);
            pstmt.setString(2, userName);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Roles updated successfully for user: " + userName);
                printAllUsers();
            } else {
                System.out.println("No user found with username: " + userName);
            }
        } catch (SQLException e) {
            System.err.println("Error updating roles for user " + userName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update the password for a user
    public void updatePassword(String userName, String password) {
        String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, password);
            pstmt.setString(2, userName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Return user objects of all the users in the database
    public List<User> getUsers() {
        String query = "SELECT userName, password, role FROM cse360users";
        List<User> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String userName = rs.getString("userName");
                String password = rs.getString("password");
                String role = rs.getString("role");
                users.add(new User(userName, password, role));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public void printAllUsers() {
        String selectAllUsers = "SELECT * FROM cse360users";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectAllUsers)) {
            ResultSet rs = selectStmt.executeQuery();
            System.out.println("\n=== Current Database Contents ===");
            System.out.println("Username\t\tRole\t\tPassword");
            System.out.println("----------------------------------------");
            while (rs.next()) {
                System.out.printf("%-20s%-15s%s%n",
                    rs.getString("userName"),
                    rs.getString("role"),
                    rs.getString("password")
                );
            }
            System.out.println("----------------------------------------\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    public void closeConnection() {
        try {
            if (statement != null) statement.close();
        } catch(SQLException se2) {
            se2.printStackTrace();
        }
        try {
            if (connection != null) connection.close();
        } catch(SQLException se) {
            se.printStackTrace();
        }
    }
}
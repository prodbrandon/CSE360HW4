package databasePart1;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import application.User;


/**
 * The DatabaseHelper class is responsible for managing the connection to the database,
 * performing operations such as user registration, login validation, and handling invitation codes.
 */
public class DatabaseHelper {

	// JDBC driver name and database URL 
	static final String JDBC_DRIVER = "org.h2.Driver";   
	static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

	//  Database credentials 
	static final String USER = "sa"; 
	static final String PASS = ""; 

	private Connection connection = null;
	private Statement statement = null; 
	//	PreparedStatement pstmt

	public void connectToDatabase() throws SQLException {
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh.
			//statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}

	private void createTables() throws SQLException {
		String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255) UNIQUE, "
				+ "password VARCHAR(255), "
				+ "role VARCHAR(20))";
		statement.execute(userTable);
		
		// Create the invitation codes table
	    String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
	            + "code VARCHAR(10) PRIMARY KEY, "
	            + "isUsed BOOLEAN DEFAULT FALSE)";
	    statement.execute(invitationCodesTable);
	    
	    // Create the one-time passwords table
	    String oneTimePasswordsTable = "CREATE TABLE IF NOT EXISTS OneTimePasswords ("
	    		+ "userName VARCHAR(255) UNIQUE, "
	    		+ "oneTimePassword VARCHAR(10) PRIMARY KEY, "
	    		+ "isUsed BOOLEAN DEFAULT FALSE)";
	    statement.execute(oneTimePasswordsTable);
	}


	// Check if the database is empty
	public boolean isDatabaseEmpty() throws SQLException {
		String query = "SELECT COUNT(*) AS count FROM cse360users";
		ResultSet resultSet = statement.executeQuery(query);
		if (resultSet.next()) {
			return resultSet.getInt("count") == 0;
		}
		return true;
	}

	// Registers a new user in the database.
	public void register(User user) throws SQLException {
	    // First insert the new user
	    String insertUser = "INSERT INTO cse360users (userName, password, role) VALUES (?, ?, ?)";
	    try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
	        pstmt.setString(1, user.getUserName());
	        pstmt.setString(2, user.getPassword());
	        pstmt.setString(3, user.getRole());
	        pstmt.executeUpdate();
	        
	        // Now select and print all users
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
	        }
	    }
	}

	// Validates a user's login credentials.
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
	
	// Checks if a user already exists in the database based on their userName.
	public boolean doesUserExist(String userName) {
	    String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            // If the count is greater than 0, the user exists
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // If an error occurs, assume user doesn't exist
	}
	
	// Retrieves the role of a user from the database using their UserName.
	public String getUserRole(String userName) {
	    String query = "SELECT role FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("role"); // Return the role if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return null; // If no user exists or an error occurs
	}
	
	// Generates a new invitation code and inserts it into the database.
	public String generateInvitationCode() {
	    String code = UUID.randomUUID().toString().substring(0, 4); // Generate a random 4-character code
	    String query = "INSERT INTO InvitationCodes (code) VALUES (?)";

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return code;
	}
	
	// Validates an invitation code to check if it is unused.
	public boolean validateInvitationCode(String code) {
	    String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            // Mark the code as used
	            markInvitationCodeAsUsed(code);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}
	
	// Marks the invitation code as used in the database.
	private void markInvitationCodeAsUsed(String code) {
	    String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	// Generates a new one-time password and inserts it into the database
	public String generateOTP(String userName) {
		// First check if the userName already has a one-time password
		String query = "SELECT oneTimePassword FROM OneTimePasswords WHERE userName = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, userName);
			ResultSet rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return "";
				//return rs.getString("oneTimePassword");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Generate the one-time password and insert its userName and one-time password
		String onetimePassword = UUID.randomUUID().toString().substring(0, 10); // Generate a random 10-character code
		query = "INSERT INTO OneTimePasswords (userName, oneTimePassword) VALUES (?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1,  userName);
			pstmt.setString(2,  onetimePassword);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return onetimePassword;
	}
	
	// Validates a one-time password to check if it is unused and belongs to the user.
	public boolean validateOTP(String userName, String oneTimePassword) {
	    String query = "SELECT * FROM OneTimePasswords WHERE userName = ? AND oneTimePassword = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	    	pstmt.setString(1,  userName);
	        pstmt.setString(2, oneTimePassword);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            // Mark the code as used
	            markOTPAsUsed(oneTimePassword);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}
		
	// Remove the one-time password from the database to allow a new one to be generated
	private void markOTPAsUsed(String oneTimePassword) {
	    String query = "DELETE FROM OneTimePasswords WHERE oneTimePassword = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, oneTimePassword);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	// Updates a password for a given username
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
	
	// Returns a list of the users 
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

	// Closes the database connection and statement.
	public void closeConnection() {
		try{ 
			if(statement!=null) statement.close(); 
		} catch(SQLException se2) { 
			se2.printStackTrace();
		} 
		try { 
			if(connection!=null) connection.close(); 
		} catch(SQLException se){ 
			se.printStackTrace(); 
		} 
	}

}

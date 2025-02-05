package application;

import javafx.scene.Scene;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import passwordEvaluationTestbed.PasswordEvaluator;
import userNameRecognizerTestbed.UserNameRecognizer;

import java.sql.SQLException;

import databasePart1.*;

/**
 * SetupAccountPage class handles the account setup process for new users.
 * Users provide their userName, password, and a valid invitation code to register.
 */

public class SetupAccountPage {
	
	String buttonVal = "";
	
    private final DatabaseHelper databaseHelper;
    // DatabaseHelper to handle database operations.
    public SetupAccountPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Displays the Setup Account page in the provided stage.
     * @param primaryStage The primary stage where the scene will be displayed.
     */
    public void show(Stage primaryStage) {
    	// Input fields for userName, password, and invitation code
    	ToggleGroup group = new ToggleGroup();
    	
    	RadioButton admin = new RadioButton("Admin");
    	RadioButton student = new RadioButton("Student");
    	RadioButton instructor = new RadioButton("Instructor");
    	RadioButton staff = new RadioButton("Staff");
    	RadioButton reviewer = new RadioButton("Reviewer");
    	
    	admin.setToggleGroup(group);
    	student.setToggleGroup(group);
    	instructor.setToggleGroup(group);
    	staff.setToggleGroup(group);
    	reviewer.setToggleGroup(group);
    	
    	admin.setOnAction(e -> {
    		if (admin.isSelected()) {
    			buttonVal = "admin";
    			System.out.println("admin selected");
    		}
    	});
    	
    	student.setOnAction(e -> {
    		if (student.isSelected()) {
    			buttonVal = "student";
    			System.out.println("student selected");
    		}
    	});
    	
    	instructor.setOnAction(e -> {
    		if (instructor.isSelected()) {
    			buttonVal = "instruction";
    			System.out.println("instructor selected");
    		}
    	});
    	
    	staff.setOnAction(e -> {
    		if (staff.isSelected()) {
    			buttonVal = "staff";
    			System.out.println("staff selected");
    		}
    	});
    	
    	reviewer.setOnAction(e -> {
    		if (reviewer.isSelected()) {
    			buttonVal = "reviewer";
    			System.out.println("reviewer selected");
    		}
    	});
    	
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);
        
        TextField inviteCodeField = new TextField();
        inviteCodeField.setPromptText("Enter InvitationCode");
        inviteCodeField.setMaxWidth(250);
        
        // Label to display error messages for invalid input or registration issues
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        Label roleErrLabel = new Label();
        roleErrLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        // Label to display possible username error messages
        Label usernameErrorLabel = new Label();
        usernameErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        // label to display possible password error messages
        Label passwordErrorLabel = new Label();
        passwordErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        // Label to show successful username validation
        Label usernameSuccessLabel = new Label("Username: no errors");
        usernameSuccessLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        
        // label to show successful password validation
        Label passwordSuccessLabel = new Label("Password: no errors");
        passwordSuccessLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        
        Button setupButton = new Button("Setup");
        
        setupButton.setOnAction(a -> {
        	// Retrieve user input
            String userName = userNameField.getText();
            String password = passwordField.getText();
            String code = inviteCodeField.getText();
            
            // check for username validation
            String usernameValidationMessage = UserNameRecognizer.checkForValidUserName(userName);
            if (!usernameValidationMessage.isEmpty()) {
                usernameErrorLabel.setText("Username Error: " + usernameValidationMessage);
                usernameSuccessLabel.setText(""); 
                return; // stops program in case validation fails
            } else {
                usernameErrorLabel.setText(""); // Clear error message
                usernameSuccessLabel.setText("Username: no errors"); // success 
            }

            // check for password validation
            String passwordValidationMessage = PasswordEvaluator.evaluatePassword(password);
            if (!passwordValidationMessage.isEmpty()) {
                passwordErrorLabel.setText("Password Error: " + passwordValidationMessage);
                passwordSuccessLabel.setText(""); 
                return; // stops program in case validation fails
            } else {
                passwordErrorLabel.setText(""); // Clear error message
                passwordSuccessLabel.setText("Password: no errors"); // success
            }
            
            if (group.getSelectedToggle() == null) {
                roleErrLabel.setText("Please choose a role");
                return;
            }
            
            try {
            	// Check if the user already exists
            	if(!databaseHelper.doesUserExist(userName)) {
            		
            		// Validate the invitation code
            		if(databaseHelper.validateInvitationCode(code)) {
            			
            			// Create a new user and register them in the database
		            	User user=new User(userName, password, buttonVal);
		            	
		                databaseHelper.register(user);
		                
		             // Navigate to the Welcome Login Page	
		                new WelcomeLoginPage(databaseHelper).show(primaryStage,user);
            		}
            		else {
            			errorLabel.setText("Please enter a valid invitation code");
            		}
            	}
            	else {
            		errorLabel.setText("This useruserName is taken!!.. Please use another to setup an account");
            	}
            	
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(userNameField, passwordField, inviteCodeField, setupButton, errorLabel, admin, instructor, staff, student, reviewer, roleErrLabel, usernameErrorLabel, passwordErrorLabel, usernameSuccessLabel, passwordSuccessLabel);

        primaryStage.setScene(new Scene(layout, 800, 600));
        primaryStage.setTitle("Account Setup");
        primaryStage.show();
    }
}

package application;
import passwordEvaluationTestbed.PasswordEvaluator;
import userNameRecognizerTestbed.*;


import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

import databasePart1.*;

/**
 * The SetupAdmin class handles the setup process for creating an administrator account.
 * This is intended to be used by the first user to initialize the system with admin credentials.
 */
public class AdminSetupPage {
	
    private final DatabaseHelper databaseHelper;

    public AdminSetupPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public void show(Stage primaryStage) {
    	// Input fields for userName and password
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter Admin userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);
        
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
            
            try {
            	// Create a new User object with admin role and register in the database
            	User user=new User(userName, password, "admin");
                databaseHelper.register(user);
                System.out.println("Administrator setup completed.");
                
                // Navigate to the Welcome Login Page
                new UserLoginPage(databaseHelper).show(primaryStage);
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        VBox layout = new VBox(10, userNameField, passwordField, setupButton, usernameErrorLabel, passwordErrorLabel, usernameSuccessLabel, passwordSuccessLabel);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Administrator Setup");
        primaryStage.show();
    }
}

package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import passwordEvaluationTestbed.PasswordEvaluator;

import java.sql.SQLException;

import databasePart1.*;

/**
 * SetupAccountPage class handles the account setup process for new users.
 * Users provide their userName, password, and a valid invitation code to register.
 */
public class UserNewPasswordPage {
	
    private final DatabaseHelper databaseHelper;
    // DatabaseHelper to handle database operations.
    public UserNewPasswordPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Displays the Setup Account page in the provided stage.
     * @param primaryStage The primary stage where the scene will be displayed.
     */
    public void show(Stage primaryStage, String userName) {
    	// Input fields for userName, password, and invitation code

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);
       
        
        // Label to display error messages for invalid input or registration issues
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        // label to display possible password error messages
        Label passwordErrorLabel = new Label();
        passwordErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        // label to show successful password validation
        Label passwordSuccessLabel = new Label("Password: no errors");
        passwordSuccessLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        

        // Button to setup new password
        Button setupButton = new Button("Set New Password");
        
        setupButton.setOnAction(a -> {
        	// Retrieve user input
            String password = passwordField.getText();
            
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
            
            // Update password with new password
            databaseHelper.updatePassword(userName, password);
            errorLabel.setText("password reset, please logout");
        });
        
        // Button to log the user out after updating their password
        Button quitButton = new Button("Logout");
	    quitButton.setOnAction(a -> {
	    	new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
	    });

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(passwordField, setupButton, errorLabel, passwordErrorLabel, passwordSuccessLabel, quitButton);

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Account Setup");
        primaryStage.show();
    }
}

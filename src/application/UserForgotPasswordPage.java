package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

import databasePart1.*;

/**
 * The UserLoginPage class provides a login interface for users to access their accounts.
 * It validates the user's credentials and navigates to the appropriate page upon successful login.
 */
public class UserForgotPasswordPage {
	
    private final DatabaseHelper databaseHelper;

    public UserForgotPasswordPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public void show(Stage primaryStage) {
    	// Input field for the user's userName, one-time password
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        userNameField.setMaxWidth(250);

        TextField oneTimePasswordField = new TextField();
        oneTimePasswordField.setPromptText("Enter one-time Password");
        oneTimePasswordField.setMaxWidth(250);
        
        // Label to display error messages
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        // Button to let the user choose to move on to updating their password
        Button updatePasswordButton = new Button("Update Password");
        updatePasswordButton.setOnAction(a -> {
        	// Retrieve user input
            String userName = userNameField.getText();
            String oneTimePassword = oneTimePasswordField.getText();
            
    		// Validate the one-time password with the userName
    		if(databaseHelper.validateOTP(userName, oneTimePassword)) {
    			
             // Navigate to the create a new password page
                new UserNewPasswordPage(databaseHelper).show(primaryStage, userName);
    		}
    		else {
    			errorLabel.setText("Please enter a valid one-time password that belongs to the userName");
    		}
        });
        
        // Button to make user logout
        Button quitButton = new Button("Logout");
	    quitButton.setOnAction(a -> {
	    	new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
	    });
        
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(userNameField, oneTimePasswordField, updatePasswordButton, quitButton, errorLabel);

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("User Login");
        primaryStage.show();
    }
}

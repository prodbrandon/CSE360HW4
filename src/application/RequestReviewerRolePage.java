// RequestReviewerRolePage.java
package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

public class RequestReviewerRolePage {
    private final DatabaseHelper databaseHelper;
    private final String userName;

    public RequestReviewerRolePage(DatabaseHelper databaseHelper, String userName) {
        this.databaseHelper = databaseHelper;
        this.userName = userName;
    }

    public void show(Stage primaryStage) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        // Title label
        Label titleLabel = new Label("Request to Become a Reviewer");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Instruction label
        Label instructionLabel = new Label("Please explain why you'd like to become a reviewer and what qualifies you:");
        
        // Justification text area
        TextArea justificationArea = new TextArea();
        justificationArea.setPromptText("Enter your justification here...");
        justificationArea.setPrefRowCount(10);
        justificationArea.setWrapText(true);
        
        // Responsibilities checkbox
        CheckBox responsibilitiesCheckBox = new CheckBox("I understand the responsibilities of being a reviewer");
        
        // Error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        
        // Success label
        Label successLabel = new Label();
        successLabel.setStyle("-fx-text-fill: green;");
        
        // Submit button
        Button submitButton = new Button("Submit Request");
        submitButton.setOnAction(e -> {
            String justification = justificationArea.getText().trim();
            
            // Validate input
            if (justification.isEmpty()) {
                errorLabel.setText("Please provide a justification for your request");
                successLabel.setText("");
                return;
            }
            
            if (!responsibilitiesCheckBox.isSelected()) {
                errorLabel.setText("Please acknowledge the responsibilities");
                successLabel.setText("");
                return;
            }
            
            try {
                // Get user ID
                int userId = databaseHelper.getUserId(userName);
                if (userId == -1) {
                    errorLabel.setText("Error: User not found");
                    successLabel.setText("");
                    return;
                }
                
                // Submit request
                databaseHelper.submitReviewerRequest(userId, justification);
                
                // Clear form and show success message
                justificationArea.clear();
                responsibilitiesCheckBox.setSelected(false);
                errorLabel.setText("");
                successLabel.setText("Your request has been submitted successfully!");
                
            } catch (SQLException ex) {
                errorLabel.setText("Error submitting request: " + ex.getMessage());
                successLabel.setText("");
                ex.printStackTrace();
            }
        });
        
        // Back button
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            try {
                studentDatabase studentDB = new studentDatabase();
                studentDB.connectToDatabase();
                User user = new User(userName, "", "student");
                new StudentHomePage(studentDB).show(primaryStage);
            } catch (SQLException ex) {
                errorLabel.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        // Add all components to layout
        layout.getChildren().addAll(
            titleLabel,
            instructionLabel,
            justificationArea,
            responsibilitiesCheckBox,
            errorLabel,
            successLabel,
            submitButton,
            backButton
        );
        
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Request Reviewer Role");
    }
}
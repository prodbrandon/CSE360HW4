package application;

import databasePart1.DatabaseHelper;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * This page displays a simple welcome message for the user.
 */

public class InstructorHomePage {
	
	private final DatabaseHelper databaseHelper;
    private final String instructorUserName;

    public InstructorHomePage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.instructorUserName = "instructor"; // Default username
    }
    
    public InstructorHomePage(DatabaseHelper databaseHelper, String userName) {
        this.databaseHelper = databaseHelper;
        this.instructorUserName = userName;
    }

    public void show(Stage primaryStage) {
    	VBox layout = new VBox(10);
	    layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
	    
	    // Button to let the user logout
	    Button quitButton = new Button("Logout");
	    quitButton.setOnAction(a -> {
	    	new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
	    });
	    
	    // Label to display Hello user
	    Label userLabel = new Label("Hello, Instructor!");
	    userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
	    
	    // Add button to review reviewer requests
        Button reviewRequestsButton = new Button("Review Reviewer Requests");
        reviewRequestsButton.setOnAction(a -> {
            new ReviewerRequestsPage(databaseHelper, instructorUserName).show(primaryStage);
        });
        
        // Button to view student questions and answers
        Button viewQuestionsButton = new Button("View All Questions");
        viewQuestionsButton.setOnAction(a -> {
            try {
                studentDatabase studentDB = new studentDatabase();
                studentDB.connectToDatabase();
                new StudentHomePage(studentDB).show(primaryStage);
            } catch (Exception e) {
                System.err.println("Error opening student questions: " + e.getMessage());
                e.printStackTrace();
            }
        });

	    layout.getChildren().addAll(userLabel, reviewRequestsButton, viewQuestionsButton, quitButton);
	    Scene userScene = new Scene(layout, 800, 400);

	    // Set the scene to primary stage
	    primaryStage.setScene(userScene);
	    primaryStage.setTitle("Instructor Page");
    }
}
package application;

import java.sql.SQLException;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InstructorHomePage {
    private final studentDatabase studentDb;
    private final String instructorUserName;

    public InstructorHomePage(studentDatabase studentDb, String userName) {
        this.studentDb = studentDb;
        this.instructorUserName = userName;
    }

    public void show(Stage primaryStage) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        Button quitButton = new Button("Logout");
        quitButton.setOnAction(a -> {
            DatabaseHelper dbHelper = new DatabaseHelper();
            try {
                dbHelper.connectToDatabase();
                new SetupLoginSelectionPage(dbHelper).show(primaryStage);
            } catch (SQLException e) {
                showAlert("Error", "Database connection error");
            }
        });
        
        Label userLabel = new Label("Hello, Instructor!");
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button reviewRequestsButton = new Button("Review Reviewer Requests");
        reviewRequestsButton.setOnAction(a -> {
            new ReviewerRequestManager(studentDb, instructorUserName, "instructor").show(primaryStage);
        });
        
        Button viewQuestionsButton = new Button("View All Questions");
        viewQuestionsButton.setOnAction(a -> {
            new StudentHomePage(studentDb).show(primaryStage);
        });

        layout.getChildren().addAll(userLabel, reviewRequestsButton, viewQuestionsButton, quitButton);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Instructor Page");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
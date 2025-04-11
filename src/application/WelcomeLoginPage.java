package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.sql.SQLException;

import databasePart1.*;

public class WelcomeLoginPage {
    private final DatabaseHelper databaseHelper;
    private final studentDatabase studentDatabaseHelper;

    public WelcomeLoginPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
		this.studentDatabaseHelper = new studentDatabase();
    }
    
    public void show(Stage primaryStage, User user) {
        VBox layout = new VBox(5);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        Label welcomeLabel = new Label("Welcome!!");
        welcomeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button continueButton = new Button("Continue to your Page");
        continueButton.setOnAction(a -> {
            String role = user.getRole();
            System.out.println("Current user role: " + role);
            
            switch(role) {
                case "admin":
                    new AdminHomePage(databaseHelper, user.getUserName()).show(primaryStage);
                    break;
                case "student":
                    try {
                        studentDatabaseHelper.connectToDatabase();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    new StudentHomePage(studentDatabaseHelper).show(primaryStage);
                    break;
                case "instruction":
					try {
						studentDatabaseHelper.connectToDatabase();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    new InstructorHomePage(studentDatabaseHelper, user.getUserName()).show(primaryStage);
                    break;
                case "staff":
                    // Updated to use the new StaffHomePage instead of the old bare-bones page
                    try {
                        studentDatabaseHelper.connectToDatabase();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    new StaffHomePage(databaseHelper).show(primaryStage);
                    break;
                case "reviewer":
                    try {
                        studentDatabaseHelper.connectToDatabase();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    new ReviewerHomePage(studentDatabaseHelper, user).show(primaryStage);
                    break;
                default:
                    // Handle unknown role
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid Role");
                    alert.setContentText("Unknown role: " + role);
                    alert.showAndWait();
                    break;
            }
        });
        Button quitButton = new Button("Quit");
        quitButton.setOnAction(a -> {
            databaseHelper.closeConnection();
            Platform.exit();
        });
        
        if ("admin".equals(user.getRole())) {
            Button inviteButton = new Button("Invite");
            inviteButton.setOnAction(a -> {
                new InvitationPage().show(databaseHelper, primaryStage);
            });
            layout.getChildren().add(inviteButton);
        }

        layout.getChildren().addAll(welcomeLabel, continueButton, quitButton);
        Scene welcomeScene = new Scene(layout, 800, 400);
        
        primaryStage.setScene(welcomeScene);
        primaryStage.setTitle("Welcome Page");
    }
}
package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import databasePart1.*;

public class WelcomeLoginPage {
    private final DatabaseHelper databaseHelper;

    public WelcomeLoginPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
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
                    new StudentHomePage(databaseHelper).show(primaryStage);
                    break;
                case "instructor":
                    new InstructorHomePage(databaseHelper).show(primaryStage);
                    break;
                case "staff":
                    new StaffHomePage(databaseHelper).show(primaryStage);
                    break;
                case "reviewer":
                    new ReviewerHomePage(databaseHelper).show(primaryStage);
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
package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminHomePage {
    private final DatabaseHelper databaseHelper;
    private final String adminUserName;

    public AdminHomePage(DatabaseHelper databaseHelper, String userName) {
        this.databaseHelper = databaseHelper;
        this.adminUserName = userName;
    }

    public void show(Stage primaryStage) {
        VBox layout = new VBox();
        
        // Button the let the user logout
        Button quitButton = new Button("Logout");
        quitButton.setOnAction(a -> {
            new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
        });
        
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        // Label to welcome the Admin
        Label adminLabel = new Label("Hello, Admin!");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Button to see the list view of users
        Button viewUserListButton = new Button("View User List");
        viewUserListButton.setOnAction(a -> {
            new ViewUserListPage().show(databaseHelper, primaryStage, adminUserName);
        });

        layout.getChildren().addAll(adminLabel, quitButton, viewUserListButton);
        Scene adminScene = new Scene(layout, 800, 400);

        primaryStage.setScene(adminScene);
        primaryStage.setTitle("Admin Page");
    }
    
}
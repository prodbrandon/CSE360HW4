package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class ViewUserListPage {
    private String selectedUserName; // Track the current user selected in the list view
    private Label errorLabel;
    private String currentUserName; // To track the logged-in admin's username
    
    public void show(DatabaseHelper databaseHelper, Stage primaryStage, String adminUserName) throws SQLException {
        // Update the username of the currently logged in user
        this.currentUserName = adminUserName;
        
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        // Label to welcome the admin
        Label adminLabel = new Label("Hello, Admin!");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        // List to hold all the users from the database
        List<User> users;
        try {
            users = databaseHelper.getUsers();
        } catch (SQLException ex) {
            errorLabel.setText("Error loading users: " + ex.getMessage());
            users = new ArrayList<>();
        }
        ObservableList<User> observableUsers = FXCollections.observableArrayList(users);
        
        // Display all the users from the database in the list view
        ListView<User> userListView = new ListView<>(observableUsers);
        userListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getUserName() + " : " + user.getPassword() + " : " + user.getRole());
                }
            }
        });
        userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // User to display the currently selected user 
        Label selectedUserLabel = new Label("Selected User: None");
        
        // Button to delete the currently selected user
        Button deleteUser = new Button("Delete User");
        deleteUser.setOnAction(e -> {
            User selectedUser = userListView.getSelectionModel().getSelectedItem();
            
            // Prevent deleting other admins
            if(selectedUser != null && selectedUser.getRole().toLowerCase().contains("admin")) {
                deleteUser.setDisable(true);
                errorLabel.setText("Can't delete other admins");
                return;
            }
            
            // Prevent deleting when there is no user selected
            if(selectedUserName == null || selectedUserName.isEmpty()) {
                errorLabel.setText("Choose a user first");
                return;
            }
            
            // Prevent deleting the currently logged in account
            if(selectedUserName.equals(currentUserName)) {
                errorLabel.setText("Can't delete your own account.");
                return;
            }
            
            // Prevent deleting an admin when there is only one left
            boolean isLastAdmin = false;
            isLastAdmin = databaseHelper.isLastAdmin(selectedUserName);
            
            if (isLastAdmin) {
                errorLabel.setText("Can't delete the last admin.");
                return;
            }
            
            // Require a confirmation to delete a user
            TextInputDialog confirmation = new TextInputDialog();
            confirmation.setTitle("Confirm this deletion");
            confirmation.setHeaderText("Are you sure you want to delete " + selectedUserName + "?");
            confirmation.setContentText("Put in 'Yes' to do so:");
            confirmation.showAndWait().ifPresent(response -> {
                if(response.equals("Yes")) {
                    databaseHelper.deleteUser(selectedUserName);
					try {
					    userListView.setItems(FXCollections.observableArrayList(databaseHelper.getUsers()));
					} catch (SQLException ex) {
					    errorLabel.setText("Error refreshing user list: " + ex.getMessage());
					    return;
					}
					errorLabel.setStyle("-fx-text-fill: green;");
					errorLabel.setText("User has been deleted");
					selectedUserLabel.setText("Selected User: None");
					selectedUserName = null;
                } else {
                    errorLabel.setText("Deleting user did not work");
                }
            });
        });
        
        // Checkboxes to display and edit roles
        CheckBox adminCheckBox = new CheckBox("Admin");
        CheckBox studentCheckBox = new CheckBox("Student");
        CheckBox reviewerCheckBox = new CheckBox("Reviewer");
        CheckBox instructorCheckBox = new CheckBox("Instructor");
        CheckBox staffCheckBox = new CheckBox("Staff");
        
        HBox rolesBox = new HBox(10);
        rolesBox.setStyle("-fx-alignment: center;");
        rolesBox.getChildren().addAll(adminCheckBox, studentCheckBox, reviewerCheckBox,
                instructorCheckBox, staffCheckBox);
        
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                
                // Update the selected user and the display 
                selectedUserName = newSelection.getUserName();
                selectedUserLabel.setText("Selected User: " + selectedUserName);
                errorLabel.setText("");
                
                // Get the roles for the selected user
                String currentRoles = newSelection.getRole();
                
                // Reset all checkboxes and delete user button
                adminCheckBox.setDisable(false);
                adminCheckBox.setSelected(false);
                studentCheckBox.setSelected(false);
                reviewerCheckBox.setSelected(false);
                instructorCheckBox.setSelected(false);
                staffCheckBox.setSelected(false);
                deleteUser.setDisable(false);
                
                // Set checkboxes based on current roles
                if (currentRoles != null) {
                    String[] roleArray = currentRoles.split(",");
                    for (String role : roleArray) {
                        switch (role.trim().toLowerCase()) {
                            case "admin":
                                // Prevent being able to change the admin role of an admin
                                adminCheckBox.setSelected(true);
                                adminCheckBox.setDisable(true);
                                break;
                            case "student":
                                studentCheckBox.setSelected(true);
                                break;
                            case "reviewer":
                                reviewerCheckBox.setSelected(true);
                                break;
                            case "instructor":
                                instructorCheckBox.setSelected(true);
                                break;
                            case "staff":
                                staffCheckBox.setSelected(true);
                                break;
                        }
                    }
                }
                
                // If this is the current admin user, disable the admin checkbox
                if (selectedUserName.equals(currentUserName)) {
                    adminCheckBox.setDisable(true);
                }
            }
        });
        
        // Button to change the selected user's roles
        Button changeRolesButton = new Button("Change Roles");
        changeRolesButton.setOnAction(e -> {
            
            // Prevent changing roles when no user selected
            if (selectedUserName == null || selectedUserName.isEmpty()) {
                errorLabel.setText("Please select a user first");
                return;
            }
            
            ArrayList<String> newRoles = new ArrayList<>();
            
            // Update the checkboxes based on the currently selected user's roles
            if (adminCheckBox.isSelected()) newRoles.add("admin");
            if (studentCheckBox.isSelected()) newRoles.add("student");
            if (reviewerCheckBox.isSelected()) newRoles.add("reviewer");
            if (instructorCheckBox.isSelected()) newRoles.add("instructor");
            if (staffCheckBox.isSelected()) newRoles.add("staff");
            
            // Make sure at least one role is selected 
            if (newRoles.isEmpty()) {
                errorLabel.setText("Please select at least one role");
                return;
            }
            
            // Check if trying to remove admin role from last admin
            boolean isLastAdmin = false;
            isLastAdmin = databaseHelper.isLastAdmin(selectedUserName);
            
            if (isLastAdmin && !newRoles.contains("admin")) {
                errorLabel.setText("Cannot remove admin role from the last admin user");
                return;
            }
            
            // Update the roles in the database 
            try {
                databaseHelper.updateUserRoles(selectedUserName, newRoles, currentUserName);
                
                // Update the list view with the changes made to roles
                List<User> updatedUsers = databaseHelper.getUsers();
                userListView.setItems(FXCollections.observableArrayList(updatedUsers));
                
                errorLabel.setStyle("-fx-text-fill: green;");
                errorLabel.setText("Roles updated successfully");
            } catch (SQLException ex) {
                errorLabel.setText("Error updating roles: " + ex.getMessage());
            }
        });
        
        // Label to display the one-time password
        Label oneTimePasswordLabel = new Label("");
        oneTimePasswordLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic;");
        
        // Button to generate a one-time password for a selected user
        Button oneTimePasswordButton = new Button("Generate OTP");
        oneTimePasswordButton.setOnAction(e -> {
            
            // Make sure a user is selected
            if (selectedUserName == null || selectedUserName.isEmpty()) {
                errorLabel.setText("Please select a user first");
                return;
            }
            
            // Generate a one-time password only if the selected user does not already have one
            String oneTimePassword = databaseHelper.generateOTP(selectedUserName);
            if (oneTimePassword.isEmpty()) {
                oneTimePasswordLabel.setText("User already has a one-time password");
            } else {
                oneTimePasswordLabel.setText("One-time password: " + oneTimePassword);
            }
        });
        
        // Button to let the user logout
        Button quitButton = new Button("Logout");
        quitButton.setOnAction(e -> {
            new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
        });
        
        HBox buttons = new HBox(10);
        buttons.setStyle("-fx-alignment: center;");
        buttons.getChildren().addAll(changeRolesButton, oneTimePasswordButton, quitButton, deleteUser);
        
        layout.getChildren().addAll(
            adminLabel,
            userListView,
            selectedUserLabel,
            rolesBox,
            buttons,
            errorLabel,
            oneTimePasswordLabel
        );

        Scene adminScene = new Scene(layout, 800, 600);
        primaryStage.setScene(adminScene);
        primaryStage.setTitle("User Management");
    }
}
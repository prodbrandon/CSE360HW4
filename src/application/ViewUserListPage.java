package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.util.List;
import java.util.ArrayList;

public class ViewUserListPage {
    private String selectedUserName;
    private Label errorLabel;
    private String currentUserName; // To track the logged-in admin's username
    
    public void show(DatabaseHelper databaseHelper, Stage primaryStage, String adminUserName) {
        this.currentUserName = adminUserName;
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        Label adminLabel = new Label("Hello, Admin!");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        List<User> users = databaseHelper.getUsers();
        ObservableList<User> observableUsers = FXCollections.observableArrayList(users);
        
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
        
        Label selectedUserLabel = new Label("Selected User: None");
        
        Button deleteUser = new Button("Delete User");
        deleteUser.setOnAction(e -> {
        	User selectedUser = userListView.getSelectionModel().getSelectedItem();
        	if(selectedUser != null && selectedUser.getRole().toLowerCase().contains("admin")) {
        		deleteUser.setDisable(true);
        		errorLabel.setText("Can't delete other admins");
        		return;
        	}
        	if(selectedUserName == null || selectedUserName.isEmpty()) {
        		errorLabel.setText("Choose a user first");
        		return;
        	}
        	if(selectedUserName.equals(currentUserName)) {
        		errorLabel.setText("Can't delete your own account.");
        		return;
        	}
        	if(databaseHelper.isLastAdmin(selectedUserName)) {
        		errorLabel.setText("Can't delete the last admin.");
        		return;
        	}
        	TextInputDialog confirmation = new TextInputDialog();
        	confirmation.setTitle("Confirm this deletion");
        	confirmation.setHeaderText("Are you sure you want to delete " + selectedUserName + "?");
        	confirmation.setContentText("Put in 'Yes' to do so:");
        	confirmation.showAndWait().ifPresent(response -> {
        		if(response.equals("Yes")) {
        			databaseHelper.deleteUser(selectedUserName);
        			userListView.setItems(FXCollections.observableArrayList(databaseHelper.getUsers()));
        			errorLabel.setStyle("-fx-text-fill: green;");
        			errorLabel.setText("User has been deleted");
        			selectedUserLabel.setText("Selected User: None");
        			selectedUserName = null;
        		}
        		else {
        			errorLabel.setText("Deleting user did not work");
        		}
        	});
        });
        
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
                selectedUserName = newSelection.getUserName();
                selectedUserLabel.setText("Selected User: " + selectedUserName);
                errorLabel.setText("");
                
                String currentRoles = newSelection.getRole();
                
                // Reset all checkboxes
                adminCheckBox.setDisable(false);
                adminCheckBox.setSelected(false);
                deleteUser.setDisable(false);
                studentCheckBox.setSelected(false);
                reviewerCheckBox.setSelected(false);
                instructorCheckBox.setSelected(false);
                staffCheckBox.setSelected(false);
                
                // Set checkboxes based on current roles
                if (currentRoles != null) {
                    String[] roleArray = currentRoles.split(",");
                    for (String role : roleArray) {
                        switch (role.trim().toLowerCase()) {
                            case "admin":
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
                } else {
                    //adminCheckBox.setDisable(false);
                }
            }
        });
        
        Button changeRolesButton = new Button("Change Roles");
        changeRolesButton.setOnAction(e -> {
            if (selectedUserName == null || selectedUserName.isEmpty()) {
                errorLabel.setText("Please select a user first");
                return;
            }
            
            ArrayList<String> newRoles = new ArrayList<>();
            
            if (adminCheckBox.isSelected()) newRoles.add("admin");
            if (studentCheckBox.isSelected()) newRoles.add("student");
            if (reviewerCheckBox.isSelected()) newRoles.add("reviewer");
            if (instructorCheckBox.isSelected()) newRoles.add("instructor");
            if (staffCheckBox.isSelected()) newRoles.add("staff");
            
            if (newRoles.isEmpty()) {
                errorLabel.setText("Please select at least one role");
                return;
            }
            
            // Check if trying to remove admin role from last admin
            if (databaseHelper.isLastAdmin(selectedUserName) && !newRoles.contains("admin")) {
                errorLabel.setText("Cannot remove admin role from the last admin user");
                return;
            }
            
            databaseHelper.updateUserRoles(selectedUserName, newRoles, currentUserName);
            
            List<User> updatedUsers = databaseHelper.getUsers();
            userListView.setItems(FXCollections.observableArrayList(updatedUsers));
            
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Roles updated successfully");
        });
        
        Label oneTimePasswordLabel = new Label("");
        oneTimePasswordLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic;");
        
        Button oneTimePasswordButton = new Button("Generate OTP");
        oneTimePasswordButton.setOnAction(e -> {
            if (selectedUserName == null || selectedUserName.isEmpty()) {
                errorLabel.setText("Please select a user first");
                return;
            }
            String oneTimePassword = databaseHelper.generateOTP(selectedUserName);
            if (oneTimePassword.isEmpty()) {
                oneTimePasswordLabel.setText("User already has a one-time password");
            } else {
                oneTimePasswordLabel.setText("One-time password: " + oneTimePassword);
            }
        });
        
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
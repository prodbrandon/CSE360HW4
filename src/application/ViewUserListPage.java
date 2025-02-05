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


/**
 * AdminPage class represents the user interface for the admin user.
 * This page displays a simple welcome message for the admin.
 */

public class ViewUserListPage {
	/**
     * Displays the admin page in the provided primary stage.
     * @param primaryStage The primary stage where the scene will be displayed.
     */
	
	private String selectedUserName;
	
    
    public void show(DatabaseHelper databaseHelper, Stage primaryStage) {
    	VBox layout = new VBox();
    	
	    layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
	    
	    // label to display the welcome message for the admin
	    Label adminLabel = new Label("Hello, Admin!");
	    
	    adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

	    layout.getChildren().add(adminLabel);
	    Scene adminScene = new Scene(layout, 800, 400);

	    // Set the scene to primary stage
	    primaryStage.setScene(adminScene);
	    primaryStage.setTitle("User Management");
	    
	    List<User> users = databaseHelper.getUsers();
	    ObservableList<User> observableUsers = FXCollections.observableArrayList(users);
	    
	    // Display the user's username, password, and role in the list 
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
	    
	    // Label to display currently selected user
	    Label selectedUserLabel = new Label("Selected User: None");
	    
	    // Update the selected user within the list view
	    userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
	    	if (newSelection != null) {
	    		selectedUserName = newSelection.getUserName();
	    		selectedUserLabel.setText("Selected User: " + selectedUserName);
	    	}
	    });
	    
	    // Check boxes for role adjustment 
	    // TODO start already checked if the user is that role
	    CheckBox adminCheckBox = new CheckBox("Admin");
	    CheckBox studentCheckBox = new CheckBox("Student");
	    CheckBox reviewerCheckBox = new CheckBox("Reviewer");
	    CheckBox instructorCheckBox = new CheckBox("Instructor");
	    CheckBox staffCheckBox = new CheckBox("Staff");
	    
	    // HBox to hold the check boxes at the same y-level
	    HBox roles = new HBox(10, adminCheckBox, studentCheckBox, reviewerCheckBox,
	    		instructorCheckBox, staffCheckBox);
	    
	    // Button to confirm the change roles for a user
	    Button changeRolesButton = new Button("Change Roles");
	    changeRolesButton.setOnAction(a -> {
	    	ArrayList<String> newRoles = new ArrayList<>();
	    	
	    	if (adminCheckBox.isSelected()) newRoles.add("Admin");
	    	if (studentCheckBox.isSelected()) newRoles.add("Student");
	    	if (reviewerCheckBox.isSelected()) newRoles.add("Reviewer");
	    	if (instructorCheckBox.isSelected()) newRoles.add("Instructor");
	    	if (staffCheckBox.isSelected()) newRoles.add("Staff");
	    	
	    	// Update the roles in the database 
	    	// TODO
	    	
	    });
	    
	    // Label to display generated one-time password
	    Label oneTimePasswordLabel = new Label(""); ;
	    oneTimePasswordLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic;");
	    
	    // Button to generate a one-time password
	    Button oneTimePasswordButton = new Button("Generate OTP");
	    oneTimePasswordButton.setOnAction(a -> {
	    	System.out.println(selectedUserName);
	    	String oneTimePassword = databaseHelper.generateOTP(selectedUserName);
	    	if (oneTimePassword == "") {
	    		oneTimePasswordLabel.setText("user already has a one-time password");
	    	} else {
	    		oneTimePasswordLabel.setText("one-time password: " + oneTimePassword);
	    	}
	    });
	    
	    // Button to log the admin out once done updating roles and generating passwords
        Button quitButton = new Button("Logout");
	    quitButton.setOnAction(a -> {
	    	new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
	    });
	    
	    // HBox to hold the buttons at the same y-level
	    HBox buttons = new HBox(10, changeRolesButton, oneTimePasswordButton, quitButton);
	    
	    // HBox to hold the roles and buttons
	    HBox rolesAndButtons = new HBox(20, roles, buttons);
	    
	    layout.getChildren().addAll(userListView, selectedUserLabel, rolesAndButtons, oneTimePasswordLabel);
    }
}
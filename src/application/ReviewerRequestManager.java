package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReviewerRequestManager {
    private final studentDatabase studentDb;
    private final String currentUser;
    private final String userRole;
    
    // UI components
    private Label statusLabel;
    private ListView<ReviewerRequest> requestsListView;
    private TextArea justificationArea;
    private TextArea commentsArea;
    
    public ReviewerRequestManager(studentDatabase studentDb, String currentUser, String userRole) {
        this.studentDb = studentDb;
        this.currentUser = currentUser;
        this.userRole = userRole;
    }
    
    public void show(Stage primaryStage) {
        if (userRole.contains("instructor")) {
            showInstructorInterface(primaryStage);
        } else {
            showStudentInterface(primaryStage);
        }
    }
    
    private void showStudentInterface(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        
        // Title
        Label titleLabel = new Label("Request Reviewer Role");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Justification input
        Label instructionLabel = new Label("Explain why you should be a reviewer:");
        TextArea justificationInput = new TextArea();
        justificationInput.setPromptText("Describe your qualifications...");
        justificationInput.setPrefRowCount(8);
        justificationInput.setWrapText(true);
        
        // Agreement checkbox
        CheckBox agreeCheckbox = new CheckBox("I understand reviewer responsibilities");
        
        // Status label
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");
        
        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button submitButton = new Button("Submit Request");
        submitButton.setOnAction(e -> handleStudentSubmission(
            justificationInput.getText().trim(),
            agreeCheckbox.isSelected()
        ));
        
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> returnToHome(stage));
        
        buttonBox.getChildren().addAll(submitButton, backButton);
        
        // Layout
        layout.getChildren().addAll(
            titleLabel,
            instructionLabel,
            justificationInput,
            agreeCheckbox,
            statusLabel,
            buttonBox
        );
        
        stage.setScene(new Scene(layout, 600, 500));
        stage.setTitle("Request Reviewer Role");
    }
    
    private void showInstructorInterface(Stage stage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        
        // Title
        Label titleLabel = new Label("Reviewer Requests");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Status label
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");
        
        // Split view
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35);
        
        // Left side - Request list
        VBox listBox = new VBox(10);
        Label listTitle = new Label("Pending Requests");
        requestsListView = new ListView<>();
        requestsListView.setCellFactory(lv -> new ListCell<ReviewerRequest>() {
            @Override
            protected void updateItem(ReviewerRequest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUserName() + " - " + 
                           item.getRequestDate().toLocalDateTime()
                              .format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                }
            }
        });
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadRequests());
        
        listBox.getChildren().addAll(listTitle, requestsListView, refreshButton);
        
        // Right side - Details
        VBox detailBox = new VBox(15);
        detailBox.setPadding(new Insets(0, 0, 0, 20));
        
        Label detailTitle = new Label("Request Details");
        detailTitle.setStyle("-fx-font-weight: bold;");
        
        justificationArea = new TextArea();
        justificationArea.setEditable(false);
        justificationArea.setWrapText(true);
        justificationArea.setPrefRowCount(5);
        
        Label commentsLabel = new Label("Your Comments:");
        commentsLabel.setStyle("-fx-font-weight: bold;");
        
        commentsArea = new TextArea();
        commentsArea.setPromptText("Required for rejections");
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(3);
        
        HBox actionButtons = new HBox(15);
        Button approveButton = new Button("Approve");
        approveButton.setOnAction(e -> handleApproval());
        
        Button denyButton = new Button("Deny");
        denyButton.setOnAction(e -> handleRejection());
        
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> returnToHome(stage));
        
        actionButtons.getChildren().addAll(approveButton, denyButton, backButton);
        
        detailBox.getChildren().addAll(
            detailTitle,
            new Label("Justification:"),
            justificationArea,
            commentsLabel,
            commentsArea,
            actionButtons
        );
        
        splitPane.getItems().addAll(listBox, detailBox);
        
        // Selection listener
        requestsListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateDetails(newVal));
        
        layout.getChildren().addAll(titleLabel, statusLabel, splitPane);
        stage.setScene(new Scene(layout, 900, 600));
        stage.setTitle("Reviewer Requests");
        
        loadRequests();
    }
    
    private void handleStudentSubmission(String justification, boolean agreed) {
        if (justification.isEmpty()) {
            showStatus("Justification cannot be empty", true);
            return;
        }
        
        if (!agreed) {
            showStatus("You must agree to the responsibilities", true);
            return;
        }
        
        try {
            int userId = studentDb.getUserId(currentUser);
            if (userId == -1) {
                showStatus("User not found", true);
                return;
            }
            
            studentDb.submitReviewerRequest(userId, justification);
            showStatus("Request submitted successfully!", false);
        } catch (SQLException e) {
            showStatus("Error submitting request: " + e.getMessage(), true);
        }
    }
    
    private void loadRequests() {
        try {
            List<ReviewerRequest> requests = studentDb.getPendingReviewerRequests();
            requestsListView.setItems(FXCollections.observableArrayList(requests));
            showStatus(requests.isEmpty() ? "No pending requests" : "", false);
        } catch (SQLException e) {
            showStatus("Error loading requests: " + e.getMessage(), true);
        }
    }
    
    private void updateDetails(ReviewerRequest request) {
        if (request == null) {
            justificationArea.clear();
            commentsArea.clear();
            return;
        }
        
        justificationArea.setText(request.getJustify());
        commentsArea.clear();
    }
    
    private void handleApproval() {
        ReviewerRequest selected = requestsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a request first", true);
            return;
        }
        
        try {
            studentDb.updateReviewerRequestStatus(selected.getId(), "APPROVED", 
                commentsArea.getText().trim());
            studentDb.addReviewerRole(selected.getUserId());
            showStatus("Request approved", false);
            loadRequests();
        } catch (SQLException e) {
            showStatus("Error approving: " + e.getMessage(), true);
        }
    }
    
    private void handleRejection() {
        ReviewerRequest selected = requestsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a request first", true);
            return;
        }
        
        String comments = commentsArea.getText().trim();
        if (comments.isEmpty()) {
            showStatus("Comments required for rejection", true);
            return;
        }
        
        try {
            studentDb.updateReviewerRequestStatus(selected.getId(), "REJECTED", comments);
            showStatus("Request rejected", false);
            loadRequests();
        } catch (SQLException e) {
            showStatus("Error rejecting: " + e.getMessage(), true);
        }
    }
    
    private void returnToHome(Stage stage) {
        try {
            if (userRole.contains("instructor")) {
                new InstructorHomePage(studentDb, currentUser).show(stage);
            } else {
                new StudentHomePage(studentDb).show(stage);
            }
        } catch (Exception e) {
            showStatus("Error returning: " + e.getMessage(), true);
        }
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        statusLabel.setVisible(!message.isEmpty());
    }
}
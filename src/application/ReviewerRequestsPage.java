// ReviewerRequestsPage.java
package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReviewerRequestsPage {
    private final DatabaseHelper databaseHelper;
    private final String instructorUserName;
    private ListView<ReviewerRequest> requestsListView;
    private TextArea justificationArea;
    private TextArea commentsArea;
    private Label studentInfoLabel;
    private Label statusLabel;
    
    public ReviewerRequestsPage(DatabaseHelper databaseHelper, String instructorUserName) {
        this.databaseHelper = databaseHelper;
        this.instructorUserName = instructorUserName;
    }
    
    public void show(Stage primaryStage) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20;");
        
        // Title
        Label titleLabel = new Label("Pending Reviewer Requests");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Status label for feedback
        statusLabel = new Label();
        statusLabel.setVisible(false);
        
        // Split pane for requests list and details
        SplitPane splitPane = new SplitPane();
        
        // Left side - Requests list
        VBox requestsBox = new VBox(10);
        Label requestsLabel = new Label("Pending Requests");
        requestsLabel.setStyle("-fx-font-weight: bold;");
        
        requestsListView = new ListView<>();
        requestsListView.setCellFactory(lv -> new ListCell<ReviewerRequest>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            
            @Override
            protected void updateItem(ReviewerRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setText(null);
                } else {
                    setText(request.getUserName() + " - " + 
                           request.getRequestDate().toLocalDateTime().format(formatter));
                }
            }
        });
        
        Button refreshButton = new Button("Refresh Requests");
        refreshButton.setOnAction(e -> loadPendingRequests());
        
        requestsBox.getChildren().addAll(requestsLabel, requestsListView, refreshButton);
        
        // Right side - Request details
        VBox detailsBox = new VBox(10);
        
        studentInfoLabel = new Label("Select a request to view details");
        
        Label justificationLabel = new Label("Justification:");
        justificationLabel.setStyle("-fx-font-weight: bold;");
        
        justificationArea = new TextArea();
        justificationArea.setEditable(false);
        justificationArea.setWrapText(true);
        justificationArea.setPrefRowCount(8);
        
        Label commentsLabel = new Label("Your Comments:");
        commentsLabel.setStyle("-fx-font-weight: bold;");
        
        commentsArea = new TextArea();
        commentsArea.setPromptText("Enter your comments here (required for rejection)");
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(4);
        
        HBox buttonBox = new HBox(10);
        
        Button approveButton = new Button("Approve");
        approveButton.setOnAction(e -> handleApproveRequest());
        
        Button rejectButton = new Button("Reject");
        rejectButton.setOnAction(e -> handleRejectRequest());
        
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            new InstructorHomePage(databaseHelper).show(primaryStage);
        });
        
        buttonBox.getChildren().addAll(approveButton, rejectButton, backButton);
        
        detailsBox.getChildren().addAll(
            studentInfoLabel,
            justificationLabel,
            justificationArea,
            commentsLabel,
            commentsArea,
            buttonBox
        );
        
        // Update details when a request is selected
        requestsListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateRequestDetails(newVal);
                } else {
                    clearRequestDetails();
                }
            });
        
        // Add boxes to split pane
        splitPane.getItems().addAll(requestsBox, detailsBox);
        splitPane.setDividerPositions(0.3);
        
        // Add components to main layout
        layout.getChildren().addAll(titleLabel, statusLabel, splitPane);
        
        // Create scene
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Review Pending Reviewer Requests");
        
        // Load pending requests
        loadPendingRequests();
    }
    
    private void loadPendingRequests() {
        try {
            List<ReviewerRequest> requests = databaseHelper.getPendingReviewerRequests();
            requestsListView.setItems(FXCollections.observableArrayList(requests));
            
            if (requests.isEmpty()) {
                showStatus("No pending requests found", false);
            } else {
                statusLabel.setVisible(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showStatus("Error loading requests: " + e.getMessage(), true);
        }
    }
    
    private void updateRequestDetails(ReviewerRequest request) {
        studentInfoLabel.setText("Student: " + request.getUserName());
        justificationArea.setText(request.getJustify());
        commentsArea.clear();
    }
    
    private void clearRequestDetails() {
        studentInfoLabel.setText("Select a request to view details");
        justificationArea.clear();
        commentsArea.clear();
    }
    
    private void handleApproveRequest() {
        ReviewerRequest selectedRequest = requestsListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            showStatus("Please select a request first", true);
            return;
        }
        
        String comments = commentsArea.getText().trim();
        
        try {
            // Update request status
            databaseHelper.updateReviewerRequestStatus(selectedRequest.getId(), "APPROVED", comments);
            
            // Add reviewer role to user
            databaseHelper.addUserRole(selectedRequest.getUserId(), "reviewer");
            
            showStatus("Request approved successfully!", false);
            loadPendingRequests();
            clearRequestDetails();
        } catch (SQLException e) {
            e.printStackTrace();
            showStatus("Error approving request: " + e.getMessage(), true);
        }
    }
    
    private void handleRejectRequest() {
        ReviewerRequest selectedRequest = requestsListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            showStatus("Please select a request first", true);
            return;
        }
        
        String comments = commentsArea.getText().trim();
        if (comments.isEmpty()) {
            showStatus("Please provide comments explaining the rejection", true);
            return;
        }
        
        try {
            databaseHelper.updateReviewerRequestStatus(selectedRequest.getId(), "REJECTED", comments);
            showStatus("Request rejected successfully!", false);
            loadPendingRequests();
            clearRequestDetails();
        } catch (SQLException e) {
            e.printStackTrace();
            showStatus("Error rejecting request: " + e.getMessage(), true);
        }
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        statusLabel.setVisible(true);
    }
}

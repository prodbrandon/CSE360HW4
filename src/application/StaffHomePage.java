package application;

import databasePart1.DatabaseHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * StaffHomePage provides a comprehensive interface for staff members to manage the Q&A system.
 * It allows staff to view, edit, and moderate all questions, answers, and reviews,
 * as well as communicate with other users in the system.
 */
public class StaffHomePage {
    
    private final DatabaseHelper databaseHelper;
    private final studentDatabase studentDatabaseHelper;
    private User currentUser;
    private Stage primaryStage;
    
    // UI components
    private TabPane tabPane;
    private ListView<QuestionData> questionListView;
    private ListView<AnswerData> answerListView;
    private ListView<ReviewData> reviewListView;
    private TextArea moderationTextArea;
    private TextField searchField;
    private Label statusLabel;
    private ComboBox<String> filterComboBox;
    
    // Data structures
    private QuestionData selectedQuestion = null;
    private AnswerData selectedAnswer = null;
    private ReviewData selectedReview = null;
    private ObservableList<QuestionData> questions = FXCollections.observableArrayList();
    private ObservableList<AnswerData> answers = FXCollections.observableArrayList();
    private ObservableList<ReviewData> reviews = FXCollections.observableArrayList();
    private ObservableList<ReviewerRecord> reviewers = FXCollections.observableArrayList();
    private ObservableList<MessageData> messages = FXCollections.observableArrayList();

    /**
     * Constructor initializes the database helpers and the current user
     */
    public StaffHomePage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.studentDatabaseHelper = new studentDatabase();
        try {
            this.studentDatabaseHelper.connectToDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the staff home page in the provided stage
     * @param primaryStage The primary stage to display the home page
     */
    public void show(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Try to get current user info
        try {
            int userId = studentDatabaseHelper.getUserId("testuser"); // In a real implementation, use actual username
            String userName = studentDatabaseHelper.getUserName(userId);
            this.currentUser = new User(userName, "", "staff");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Create main layout with TabPane
        tabPane = new TabPane();
        
        // Questions & Answers Tab
        Tab qaTab = new Tab("Questions & Answers Management");
        qaTab.setClosable(false);
        qaTab.setContent(createQATabContent());
        
        // Reviewers Management Tab
        Tab reviewersTab = new Tab("Reviewers Management");
        reviewersTab.setClosable(false);
        reviewersTab.setContent(createReviewersTabContent());
        
        // Messages Tab
        Tab messagesTab = new Tab("Messages");
        messagesTab.setClosable(false);
        messagesTab.setContent(createMessagesTabContent());
        
        // Reports Tab
        Tab reportsTab = new Tab("System Reports");
        reportsTab.setClosable(false);
        reportsTab.setContent(createReportsTabContent());
        
        // Add tabs to the TabPane
        tabPane.getTabs().addAll(qaTab, reviewersTab, messagesTab, reportsTab);
        
        // Create scene and set it on the stage
        Scene scene = new Scene(tabPane, 1200, 800);
        primaryStage.setTitle("Staff Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Store reference to primary stage
        this.primaryStage = primaryStage;
        
        // Load initial data
        loadAllQuestions();
        loadAllReviewers();
        loadAllMessages();
    }
    
    /**
     * Creates the content for the Questions & Answers tab
     * @return The VBox containing the Q&A tab content
     */
    private VBox createQATabContent() {
        VBox qaLayout = new VBox(10);
        qaLayout.setPadding(new Insets(10));
        
        // Status label for feedback
        statusLabel = new Label();
        statusLabel.setVisible(false);
        statusLabel.setMinHeight(20);
        
        // Search and filter section
        HBox searchFilterBox = createSearchFilterSection();
        
        // Main content split pane with three sections
        SplitPane splitPane = new SplitPane();
        // Set divider positions to create three equal sections
        splitPane.setDividerPositions(0.333, 0.667);
        
        // Create the three sections
        VBox questionsSection = createQuestionsSection();
        VBox answersSection = createAnswersSection();
        VBox reviewsSection = createReviewsSection();
        
        // Set equal size constraints for each section
        SplitPane.setResizableWithParent(questionsSection, true);
        SplitPane.setResizableWithParent(answersSection, true);
        SplitPane.setResizableWithParent(reviewsSection, true);
        
        // Add sections to the split pane
        splitPane.getItems().addAll(questionsSection, answersSection, reviewsSection);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        // Add all components to the main layout
        qaLayout.getChildren().addAll(statusLabel, searchFilterBox, splitPane);
        
        return qaLayout;
    }
    
    /**
     * Creates the search and filter section for the Q&A tab
     * @return HBox containing search field and filter dropdown
     */
    private HBox createSearchFilterSection() {
        HBox searchFilterBox = new HBox(10);
        searchFilterBox.setPadding(new Insets(0, 0, 10, 0));
        
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search questions, answers, or reviews...");
        searchField.setId("searchField");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            try {
                if (newText != null && !newText.trim().isEmpty()) {
                    questions.setAll(studentDatabaseHelper.searchQuestions(newText.trim()));
                } else {
                    loadAllQuestions();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error searching questions: " + e.getMessage());
            }
        });
        
        // Filter dropdown
        Label filterLabel = new Label("Filter:");
        filterComboBox = new ComboBox<>();
        filterComboBox.setItems(FXCollections.observableArrayList(
            "All Questions", "Resolved", "Unresolved", "Needs Moderation", "Reported Content"
        ));
        filterComboBox.setValue("All Questions");
        filterComboBox.setOnAction(e -> applyFilter(filterComboBox.getValue()));
        
        searchFilterBox.getChildren().addAll(searchField, filterLabel, filterComboBox);
        
        return searchFilterBox;
    }
    
    /**
     * Creates the questions section for the split pane
     * @return VBox containing the questions list and related controls
     */
    private VBox createQuestionsSection() {
        VBox questionsBox = new VBox(10);
        questionsBox.setPadding(new Insets(10));
        
        // Label for questions section
        Label questionsLabel = new Label("Questions");
        questionsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Question list view
        questionListView = new ListView<>(questions);
        questionListView.setId("questionListView");
        VBox.setVgrow(questionListView, Priority.ALWAYS);
        
        // Custom cell factory for question items
        questionListView.setCellFactory(createQuestionCellFactory());
        
        // Add context menu for questions
        questionListView.setContextMenu(createQuestionContextMenu());
        
        // Handle question selection
        questionListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedQuestion = newSelection;
                if (newSelection != null) {
                    try {
                        answers.setAll(studentDatabaseHelper.getAnswersForQuestion(newSelection.id));
                        reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(newSelection.id));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showError("Error loading answers or reviews: " + e.getMessage());
                    }
                } else {
                    answers.clear();
                    reviews.clear();
                }
            });
        
        // Moderation input area
        moderationTextArea = new TextArea();
        moderationTextArea.setPromptText("Enter moderation notes or edit question content here...");
        moderationTextArea.setPrefRowCount(3);
        moderationTextArea.setId("moderationTextArea");
        
        // Action buttons
        HBox buttonBox = new HBox(10);
        
        Button editButton = new Button("Edit Question");
        editButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editButton, Priority.ALWAYS);
        editButton.setOnAction(e -> handleEditQuestion());
        
        Button deleteButton = new Button("Delete Question");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteButton, Priority.ALWAYS);
        deleteButton.setOnAction(e -> handleDeleteQuestion());
        
        buttonBox.getChildren().addAll(editButton, deleteButton);
        
        // Add all components to the questions box
        questionsBox.getChildren().addAll(questionsLabel, questionListView, moderationTextArea, buttonBox);
        
        return questionsBox;
    }
    
    /**
     * Creates the answers section for the split pane
     * @return VBox containing the answers list and related controls
     */
    private VBox createAnswersSection() {
        VBox answersBox = new VBox(10);
        answersBox.setPadding(new Insets(10));
        
        // Label for answers section
        Label answersLabel = new Label("Answers");
        answersLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Answers list view
        answerListView = new ListView<>(answers);
        answerListView.setId("answerListView");
        VBox.setVgrow(answerListView, Priority.ALWAYS);
        
        // Custom cell factory for answer items
        answerListView.setCellFactory(createAnswerCellFactory());
        
        // Add context menu for answers
        answerListView.setContextMenu(createAnswerContextMenu());
        
        // Handle answer selection
        answerListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedAnswer = newSelection;
                if (newSelection != null) {
                    try {
                        reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(newSelection.id));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showError("Error loading reviews: " + e.getMessage());
                    }
                }
            });
        
        // Moderation input area for answers
        TextArea answerModerationTextArea = new TextArea();
        answerModerationTextArea.setPromptText("Enter moderation notes or edit answer content here...");
        answerModerationTextArea.setPrefRowCount(3);
        
        // Action buttons
        HBox buttonBox = new HBox(10);
        
        Button editAnswerButton = new Button("Edit Answer");
        editAnswerButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editAnswerButton, Priority.ALWAYS);
        editAnswerButton.setOnAction(e -> handleEditAnswer());
        
        Button deleteAnswerButton = new Button("Delete Answer");
        deleteAnswerButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteAnswerButton, Priority.ALWAYS);
        deleteAnswerButton.setOnAction(e -> handleDeleteAnswer());
        
        Button toggleClarificationButton = new Button("Toggle Clarification Flag");
        toggleClarificationButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(toggleClarificationButton, Priority.ALWAYS);
        toggleClarificationButton.setOnAction(e -> handleToggleAnswerClarification());
        
        buttonBox.getChildren().addAll(editAnswerButton, deleteAnswerButton, toggleClarificationButton);
        
        // Add all components to the answers box
        answersBox.getChildren().addAll(answersLabel, answerListView, answerModerationTextArea, buttonBox);
        
        return answersBox;
    }
    
    /**
     * Creates the reviews section for the split pane
     * @return VBox containing the reviews list and related controls
     */
    private VBox createReviewsSection() {
        VBox reviewsBox = new VBox(10);
        reviewsBox.setPadding(new Insets(10));
        
        // Label for reviews section
        Label reviewsLabel = new Label("Reviews");
        reviewsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Reviews list view
        reviewListView = new ListView<>(reviews);
        reviewListView.setId("reviewListView");
        VBox.setVgrow(reviewListView, Priority.ALWAYS);
        
        // Custom cell factory for review items
        reviewListView.setCellFactory(createReviewCellFactory());
        
        // Handle review selection
        reviewListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedReview = newSelection;
            });
        
        // Moderation input area for reviews
        TextArea reviewModerationTextArea = new TextArea();
        reviewModerationTextArea.setPromptText("Enter moderation notes or edit review content here...");
        reviewModerationTextArea.setPrefRowCount(3);
        
        // Action buttons
        HBox buttonBox = new HBox(10);
        
        Button editReviewButton = new Button("Edit Review");
        editReviewButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editReviewButton, Priority.ALWAYS);
        editReviewButton.setOnAction(e -> handleEditReview());
        
        Button deleteReviewButton = new Button("Delete Review");
        deleteReviewButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteReviewButton, Priority.ALWAYS);
        deleteReviewButton.setOnAction(e -> handleDeleteReview());
        
        Button contactReviewerButton = new Button("Contact Reviewer");
        contactReviewerButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contactReviewerButton, Priority.ALWAYS);
        contactReviewerButton.setOnAction(e -> handleContactReviewer());
        
        buttonBox.getChildren().addAll(editReviewButton, deleteReviewButton, contactReviewerButton);
        
        // Add all components to the reviews box
        reviewsBox.getChildren().addAll(reviewsLabel, reviewListView, reviewModerationTextArea, buttonBox);
        
        return reviewsBox;
    }
    
    /**
     * Creates the content for the Reviewers Management tab
     * @return VBox containing the reviewers management content
     */
    private VBox createReviewersTabContent() {
        VBox reviewersLayout = new VBox(10);
        reviewersLayout.setPadding(new Insets(10));
        
        // Label for reviewers management
        Label headerLabel = new Label("Reviewers Management");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Search field for reviewers
        TextField reviewerSearchField = new TextField();
        reviewerSearchField.setPromptText("Search reviewers by name...");
        
        // Split pane for reviewers list and details
        SplitPane splitPane = new SplitPane();
        
        // Left side: Reviewers list
        VBox reviewersListBox = new VBox(10);
        reviewersListBox.setPadding(new Insets(5));
        
        Label reviewersListLabel = new Label("Reviewers");
        reviewersListLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        ListView<ReviewerRecord> reviewersListView = new ListView<>(reviewers);
        VBox.setVgrow(reviewersListView, Priority.ALWAYS);
        
        // Custom cell factory for reviewers
        reviewersListView.setCellFactory(lv -> new ListCell<ReviewerRecord>() {
            @Override
            protected void updateItem(ReviewerRecord reviewer, boolean empty) {
                super.updateItem(reviewer, empty);
                
                if (empty || reviewer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(10);
                    container.setPadding(new Insets(5));
                    
                    VBox details = new VBox(3);
                    Label nameLabel = new Label(reviewer.userName);
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    Label weightLabel = new Label("Weight: " + reviewer.weight);
                    details.getChildren().addAll(nameLabel, weightLabel);
                    
                    container.getChildren().addAll(details);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        reviewersListBox.getChildren().addAll(reviewersListLabel, reviewersListView);
        
        // Right side: Reviewer details and actions
        VBox reviewerDetailsBox = new VBox(10);
        reviewerDetailsBox.setPadding(new Insets(5));
        
        Label detailsLabel = new Label("Reviewer Details");
        detailsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Fields for reviewer details
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        nameField.setEditable(false);
        
        Label weightLabel = new Label("Weight:");
        Slider weightSlider = new Slider(0.5, 2.0, 1.0);
        weightSlider.setShowTickLabels(true);
        weightSlider.setShowTickMarks(true);
        
        Label reviewsCountLabel = new Label("Total Reviews: 0");
        
        // Tabs for reviewer's content
        TabPane reviewerContentTabs = new TabPane();
        
        // Tab for reviewer's reviews
        Tab reviewsTab = new Tab("Reviews");
        reviewsTab.setClosable(false);
        
        ListView<ReviewData> reviewerReviewsListView = new ListView<>();
        reviewsTab.setContent(reviewerReviewsListView);
        
        // Tab for reviewer's profile
        Tab profileTab = new Tab("Profile");
        profileTab.setClosable(false);
        
        VBox profileBox = new VBox(10);
        profileBox.setPadding(new Insets(5));
        
        Label profileLabel = new Label("Reviewer Profile");
        profileLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea profileTextArea = new TextArea();
        profileTextArea.setPromptText("Reviewer's profile information or notes...");
        VBox.setVgrow(profileTextArea, Priority.ALWAYS);
        
        profileBox.getChildren().addAll(profileLabel, profileTextArea);
        profileTab.setContent(profileBox);
        
        reviewerContentTabs.getTabs().addAll(reviewsTab, profileTab);
        VBox.setVgrow(reviewerContentTabs, Priority.ALWAYS);
        
        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));
        
        Button saveButton = new Button("Save Changes");
        saveButton.setOnAction(e -> handleSaveReviewerChanges());
        
        Button messageButton = new Button("Message Reviewer");
        messageButton.setOnAction(e -> handleMessageReviewer());
        
        Button suspendButton = new Button("Suspend Reviewer");
        suspendButton.setOnAction(e -> handleSuspendReviewer());
        
        actionButtons.getChildren().addAll(saveButton, messageButton, suspendButton);
        
        reviewerDetailsBox.getChildren().addAll(
            detailsLabel,
            nameLabel, nameField,
            weightLabel, weightSlider,
            reviewsCountLabel,
            reviewerContentTabs,
            actionButtons
        );
        
        // Add both sides to the split pane
        splitPane.getItems().addAll(reviewersListBox, reviewerDetailsBox);
        splitPane.setDividerPositions(0.3);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        // Handle reviewer selection
        reviewersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                nameField.setText(newVal.userName);
                weightSlider.setValue(newVal.weight);
                
                try {
                    // Load reviewer's reviews
                    List<ReviewData> reviewerReviews = studentDatabaseHelper.getReviewsByReviewer(newVal.reviewerId);
                    reviewerReviewsListView.setItems(FXCollections.observableArrayList(reviewerReviews));
                    reviewsCountLabel.setText("Total Reviews: " + reviewerReviews.size());
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error loading reviewer's reviews: " + e.getMessage());
                }
            }
        });
        
        // Add everything to the main layout
        reviewersLayout.getChildren().addAll(headerLabel, reviewerSearchField, splitPane);
        
        return reviewersLayout;
    }
    
    /**
     * Creates the content for the Messages tab
     * @return VBox containing the messages tab content
     */
    private VBox createMessagesTabContent() {
        VBox messagesLayout = new VBox(10);
        messagesLayout.setPadding(new Insets(10));
        
        // Header label
        Label headerLabel = new Label("Staff Communication");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Split pane for messages list and content
        SplitPane splitPane = new SplitPane();
        
        // Left side: Messages list
        VBox messagesListBox = new VBox(10);
        messagesListBox.setPadding(new Insets(5));
        
        Label messagesLabel = new Label("Messages");
        messagesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Messages list
        ListView<MessageData> messagesListView = new ListView<>(messages);
        VBox.setVgrow(messagesListView, Priority.ALWAYS);
        
        // Custom cell factory for messages
        messagesListView.setCellFactory(lv -> new ListCell<MessageData>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
            @Override
            protected void updateItem(MessageData message, boolean empty) {
                super.updateItem(message, empty);
                
                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    container.setPadding(new Insets(5));
                    
                    // Message header with sender/receiver info
                    HBox headerBox = new HBox(5);
                    String displayName = message.senderName + " → " + message.receiverName;
                    Label fromToLabel = new Label(displayName);
                    fromToLabel.setStyle("-fx-font-weight: bold;");
                    
                    Label dateLabel = new Label(message.createDate.toLocalDateTime().format(formatter));
                    dateLabel.setStyle("-fx-font-size: 11px;");
                    
                    HBox.setHgrow(dateLabel, Priority.ALWAYS);
                    dateLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    
                    headerBox.getChildren().addAll(fromToLabel, dateLabel);
                    
                    // If unread, highlight it
                    if (!message.isRead) {
                        Label unreadLabel = new Label("[Unread]");
                        unreadLabel.setTextFill(Color.RED);
                        unreadLabel.setStyle("-fx-font-weight: bold;");
                        headerBox.getChildren().add(unreadLabel);
                        setStyle("-fx-background-color: #f0f8ff;"); // Light blue background for unread
                    } else {
                        setStyle("");
                    }
                    
                    // Message preview (first 50 chars)
                    String preview = message.content.length() > 50 ? 
                            message.content.substring(0, 47) + "..." : message.content;
                    Label previewLabel = new Label(preview);
                    previewLabel.setWrapText(true);
                    
                    container.getChildren().addAll(headerBox, previewLabel);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        // Filter buttons for messages
        HBox filterButtonsBox = new HBox(10);
        
        Button allMessagesButton = new Button("All");
        allMessagesButton.setOnAction(e -> loadAllMessages());
        
        Button inboxButton = new Button("Inbox");
        inboxButton.setOnAction(e -> loadInboxMessages());
        
        Button sentButton = new Button("Sent");
        sentButton.setOnAction(e -> loadSentMessages());
        
        Button unreadButton = new Button("Unread");
        unreadButton.setOnAction(e -> loadUnreadMessages());
        
        filterButtonsBox.getChildren().addAll(allMessagesButton, inboxButton, sentButton, unreadButton);
        
        messagesListBox.getChildren().addAll(messagesLabel, filterButtonsBox, messagesListView);
        
        // Right side: Message content and reply
        VBox messageContentBox = new VBox(10);
        messageContentBox.setPadding(new Insets(5));
        
        Label messageDetailLabel = new Label("Message Details");
        messageDetailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Message header info
        HBox messageHeaderBox = new HBox(10);
        Label fromLabel = new Label("From:");
        Label fromValueLabel = new Label();
        messageHeaderBox.getChildren().addAll(fromLabel, fromValueLabel);
        
        HBox messageHeaderBox2 = new HBox(10);
        Label toLabel = new Label("To:");
        Label toValueLabel = new Label();
        messageHeaderBox2.getChildren().addAll(toLabel, toValueLabel);
        
        HBox messageHeaderBox3 = new HBox(10);
        Label dateLabel = new Label("Date:");
        Label dateValueLabel = new Label();
        messageHeaderBox3.getChildren().addAll(dateLabel, dateValueLabel);
        
        // Message content area
        TextArea messageContentArea = new TextArea();
        messageContentArea.setEditable(false);
        messageContentArea.setWrapText(true);
        messageContentArea.setPrefHeight(200);
        VBox.setVgrow(messageContentArea, Priority.ALWAYS);
        
        // Reply section
        Label replyLabel = new Label("Reply");
        replyLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Type your reply here...");
        replyArea.setPrefHeight(100);
        
        // Recipient selection for new messages
        HBox newMessageBox = new HBox(10);
        Label newMessageLabel = new Label("New Message To:");
        ComboBox<String> recipientTypeComboBox = new ComboBox<>();
        recipientTypeComboBox.setItems(FXCollections.observableArrayList(
            "Staff", "Instructor", "Admin", "Reviewer", "Student"
        ));
        recipientTypeComboBox.setValue("Staff");
        
        TextField recipientField = new TextField();
        recipientField.setPromptText("Recipient username");
        HBox.setHgrow(recipientField, Priority.ALWAYS);
        
        newMessageBox.getChildren().addAll(newMessageLabel, recipientTypeComboBox, recipientField);
        
        // Action buttons
        HBox actionButtonsBox = new HBox(10);
        
        Button replyButton = new Button("Send Reply");
        replyButton.setOnAction(e -> handleSendReply(messageContentArea, replyArea));
        
        Button newMessageButton = new Button("Send New Message");
        newMessageButton.setOnAction(e -> handleSendNewMessage(recipientTypeComboBox, recipientField, replyArea));
        
        Button deleteMessageButton = new Button("Delete Message");
        deleteMessageButton.setOnAction(e -> handleDeleteMessage(messagesListView));
        
        actionButtonsBox.getChildren().addAll(replyButton, newMessageButton, deleteMessageButton);
        
        messageContentBox.getChildren().addAll(
            messageDetailLabel,
            messageHeaderBox, messageHeaderBox2, messageHeaderBox3,
            messageContentArea,
            newMessageBox,
            replyLabel, replyArea,
            actionButtonsBox
        );
        
        // Add both sides to the split pane
        splitPane.getItems().addAll(messagesListBox, messageContentBox);
        splitPane.setDividerPositions(0.4);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        // Handle message selection
        messagesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                messageContentArea.setText(newVal.content);
                fromValueLabel.setText(newVal.senderName);
                toValueLabel.setText(newVal.receiverName);
                dateValueLabel.setText(newVal.createDate.toString());
                
                // Mark message as read if it's incoming and unread
                try {
                    int currentUserId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
                    if (newVal.receiverId == currentUserId && !newVal.isRead) {
                        studentDatabaseHelper.markMessageAsRead(newVal.id, currentUserId);
                        loadAllMessages(); // Refresh the message list
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error marking message as read: " + e.getMessage());
                }
            }
        });
        
        // Add everything to the main layout
        messagesLayout.getChildren().addAll(headerLabel, splitPane);
        
        return messagesLayout;
    }
    
    /**
     * Creates the content for the System Reports tab
     * @return VBox containing the system reports content
     */
    private VBox createReportsTabContent() {
        VBox reportsLayout = new VBox(10);
        reportsLayout.setPadding(new Insets(10));
        
        // Header label
        Label headerLabel = new Label("System Reports and Statistics");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Create tabs for different report types
        TabPane reportsTabs = new TabPane();
        
        // Activity Summary Tab
        Tab activityTab = new Tab("Activity Summary");
        activityTab.setClosable(false);
        
        VBox activityContent = new VBox(10);
        activityContent.setPadding(new Insets(10));
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.setPadding(new Insets(10));
        
        // Placeholder statistics (would be populated from database in real implementation)
        Label totalQuestionsLabel = new Label("Total Questions:");
        Label totalQuestionsValue = new Label("0");
        statsGrid.add(totalQuestionsLabel, 0, 0);
        statsGrid.add(totalQuestionsValue, 1, 0);
        
        Label resolvedQuestionsLabel = new Label("Resolved Questions:");
        Label resolvedQuestionsValue = new Label("0");
        statsGrid.add(resolvedQuestionsLabel, 0, 1);
        statsGrid.add(resolvedQuestionsValue, 1, 1);
        
        Label totalAnswersLabel = new Label("Total Answers:");
        Label totalAnswersValue = new Label("0");
        statsGrid.add(totalAnswersLabel, 0, 2);
        statsGrid.add(totalAnswersValue, 1, 2);
        
        Label totalReviewsLabel = new Label("Total Reviews:");
        Label totalReviewsValue = new Label("0");
        statsGrid.add(totalReviewsLabel, 0, 3);
        statsGrid.add(totalReviewsValue, 1, 3);
        
        Label activeUsersLabel = new Label("Active Users:");
        Label activeUsersValue = new Label("0");
        statsGrid.add(activeUsersLabel, 2, 0);
        statsGrid.add(activeUsersValue, 3, 0);
        
        Label activeReviewersLabel = new Label("Active Reviewers:");
        Label activeReviewersValue = new Label("0");
        statsGrid.add(activeReviewersLabel, 2, 1);
        statsGrid.add(activeReviewersValue, 3, 1);
        
        Label avgResponseTimeLabel = new Label("Avg. Response Time:");
        Label avgResponseTimeValue = new Label("N/A");
        statsGrid.add(avgResponseTimeLabel, 2, 2);
        statsGrid.add(avgResponseTimeValue, 3, 2);
        
        Label avgResolutionTimeLabel = new Label("Avg. Resolution Time:");
        Label avgResolutionTimeValue = new Label("N/A");
        statsGrid.add(avgResolutionTimeLabel, 2, 3);
        statsGrid.add(avgResolutionTimeValue, 3, 3);
        
        // Buttons to refresh or export stats
        HBox activityButtonsBox = new HBox(10);
        Button refreshStatsButton = new Button("Refresh Statistics");
        refreshStatsButton.setOnAction(e -> loadSystemStatistics());
        
        Button exportStatsButton = new Button("Export Statistics");
        exportStatsButton.setOnAction(e -> handleExportStatistics());
        
        activityButtonsBox.getChildren().addAll(refreshStatsButton, exportStatsButton);
        
        activityContent.getChildren().addAll(statsGrid, activityButtonsBox);
        activityTab.setContent(activityContent);
        
        // User Activity Tab
        Tab userActivityTab = new Tab("User Activity");
        userActivityTab.setClosable(false);
        
        VBox userActivityContent = new VBox(10);
        userActivityContent.setPadding(new Insets(10));
        
        ListView<String> userActivityListView = new ListView<>();
        userActivityListView.setItems(FXCollections.observableArrayList(
            "User activities will be displayed here..."
        ));
        VBox.setVgrow(userActivityListView, Priority.ALWAYS);
        
        userActivityContent.getChildren().add(userActivityListView);
        userActivityTab.setContent(userActivityContent);
        
        // Content Quality Tab
        Tab contentQualityTab = new Tab("Content Quality");
        contentQualityTab.setClosable(false);
        
        VBox contentQualityContent = new VBox(10);
        contentQualityContent.setPadding(new Insets(10));
        
        ListView<String> contentQualityListView = new ListView<>();
        contentQualityListView.setItems(FXCollections.observableArrayList(
            "Content quality metrics will be displayed here..."
        ));
        VBox.setVgrow(contentQualityListView, Priority.ALWAYS);
        
        contentQualityContent.getChildren().add(contentQualityListView);
        contentQualityTab.setContent(contentQualityContent);
        
        // Add tabs to tabpane
        reportsTabs.getTabs().addAll(activityTab, userActivityTab, contentQualityTab);
        VBox.setVgrow(reportsTabs, Priority.ALWAYS);
        
        // Add logout button
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> handleLogout());
        
        // Add everything to the main layout
        reportsLayout.getChildren().addAll(headerLabel, reportsTabs, logoutButton);
        
        return reportsLayout;
    }
    
    /**
     * Creates a cell factory for question items in a list view
     * @return CellFactory for QuestionData
     */
    private Callback<ListView<QuestionData>, ListCell<QuestionData>> createQuestionCellFactory() {
        return lv -> new ListCell<QuestionData>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
            @Override
            protected void updateItem(QuestionData question, boolean empty) {
                super.updateItem(question, empty);
                
                if (empty || question == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    container.setPadding(new Insets(5));
                    
                    // Title with resolved status
                    HBox titleBox = new HBox(5);
                    Label titleLabel = new Label(question.title);
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    if (question.resolved) {
                        Label resolvedLabel = new Label("[Resolved]");
                        resolvedLabel.setTextFill(Color.GREEN);
                        resolvedLabel.setStyle("-fx-font-weight: bold;");
                        titleBox.getChildren().addAll(titleLabel, resolvedLabel);
                    } else {
                        titleBox.getChildren().add(titleLabel);
                    }
                    
                    // Metadata line
                    HBox metaBox = new HBox(10);
                    Label userLabel = new Label("By: " + question.userName);
                    userLabel.setStyle("-fx-font-style: italic;");
                    
                    Label dateLabel = new Label(question.createDate.toLocalDateTime().format(formatter));
                    dateLabel.setStyle("-fx-font-size: 11px;");
                    
                    Label answerCountLabel = new Label("Answers: " + question.answerCount);
                    
                    metaBox.getChildren().addAll(userLabel, dateLabel, answerCountLabel);
                    
                    container.getChildren().addAll(titleBox, metaBox);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    /**
     * Creates a cell factory for answer items in a list view
     * @return CellFactory for AnswerData
     */
    private Callback<ListView<AnswerData>, ListCell<AnswerData>> createAnswerCellFactory() {
        return lv -> new ListCell<AnswerData>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
            @Override
            protected void updateItem(AnswerData answer, boolean empty) {
                super.updateItem(answer, empty);
                
                if (empty || answer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    container.setPadding(new Insets(5));
                    
                    // Content
                    Label contentLabel = new Label(answer.content);
                    contentLabel.setWrapText(true);
                    
                    // Metadata line
                    HBox metaBox = new HBox(10);
                    Label userLabel = new Label("By: " + answer.userName);
                    userLabel.setStyle("-fx-font-style: italic;");
                    
                    Label dateLabel = new Label(answer.createDate.toLocalDateTime().format(formatter));
                    dateLabel.setStyle("-fx-font-size: 11px;");
                    
                    metaBox.getChildren().addAll(userLabel, dateLabel);
                    
                    // Status flags
                    if (answer.needsClarification) {
                        Label clarificationLabel = new Label("[Needs Clarification]");
                        clarificationLabel.setTextFill(Color.ORANGE);
                        clarificationLabel.setStyle("-fx-font-weight: bold;");
                        metaBox.getChildren().add(clarificationLabel);
                    }
                    
                    container.getChildren().addAll(contentLabel, metaBox);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    /**
     * Creates a cell factory for review items in a list view
     * @return CellFactory for ReviewData
     */
    private Callback<ListView<ReviewData>, ListCell<ReviewData>> createReviewCellFactory() {
        return lv -> new ListCell<ReviewData>() {
            @Override
            protected void updateItem(ReviewData review, boolean empty) {
                super.updateItem(review, empty);
                
                if (empty || review == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    container.setPadding(new Insets(5));
                    
                    // Review content
                    Label contentLabel = new Label(review.content);
                    contentLabel.setWrapText(true);
                    
                    // Reviewer info
                    HBox metaBox = new HBox(10);
                    
                    // Try to get reviewer name
                    String reviewerName = "Unknown Reviewer";
                    try {
                        reviewerName = studentDatabaseHelper.getReviewerName(review.reviewerId);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    
                    Label reviewerLabel = new Label("By: " + reviewerName);
                    reviewerLabel.setStyle("-fx-font-style: italic;");
                    
                    metaBox.getChildren().add(reviewerLabel);
                    
                    container.getChildren().addAll(contentLabel, metaBox);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    /**
     * Creates a context menu for the question list view
     * @return ContextMenu with question-related actions
     */
    private ContextMenu createQuestionContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Question");
        editItem.setOnAction(e -> handleEditQuestion());
        
        MenuItem deleteItem = new MenuItem("Delete Question");
        deleteItem.setOnAction(e -> handleDeleteQuestion());
        
        MenuItem resolveItem = new MenuItem("Toggle Resolved Status");
        resolveItem.setOnAction(e -> handleToggleResolved());
        
        MenuItem messageAuthorItem = new MenuItem("Message Author");
        messageAuthorItem.setOnAction(e -> handleMessageQuestionAuthor());
        
        contextMenu.getItems().addAll(editItem, deleteItem, resolveItem, messageAuthorItem);
        
        return contextMenu;
    }
    
    /**
     * Creates a context menu for the answer list view
     * @return ContextMenu with answer-related actions
     */
    private ContextMenu createAnswerContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Answer");
        editItem.setOnAction(e -> handleEditAnswer());
        
        MenuItem deleteItem = new MenuItem("Delete Answer");
        deleteItem.setOnAction(e -> handleDeleteAnswer());
        
        MenuItem clarificationItem = new MenuItem("Toggle Clarification Flag");
        clarificationItem.setOnAction(e -> handleToggleAnswerClarification());
        
        MenuItem markResolvedItem = new MenuItem("Mark as Solution");
        markResolvedItem.setOnAction(e -> handleMarkAnswerAsResolved());
        
        MenuItem messageAuthorItem = new MenuItem("Message Author");
        messageAuthorItem.setOnAction(e -> handleMessageAnswerAuthor());
        
        contextMenu.getItems().addAll(editItem, deleteItem, clarificationItem, markResolvedItem, messageAuthorItem);
        
        return contextMenu;
    }
    
    /**
     * Applies the selected filter to the questions list
     * @param filterValue The selected filter value
     */
    private void applyFilter(String filterValue) {
        try {
            switch (filterValue) {
                case "All Questions":
                    loadAllQuestions();
                    break;
                case "Resolved":
                    List<QuestionData> allQuestions = studentDatabaseHelper.getQuestions();
                    questions.setAll(allQuestions.stream()
                        .filter(q -> q.resolved)
                        .collect(java.util.stream.Collectors.toList()));
                    break;
                case "Unresolved":
                    List<QuestionData> allUnresolvedQuestions = studentDatabaseHelper.getQuestions();
                    questions.setAll(allUnresolvedQuestions.stream()
                        .filter(q -> !q.resolved)
                        .collect(java.util.stream.Collectors.toList()));
                    break;
                case "Needs Moderation":
                    // This would require additional database functionality to flag content for moderation
                    showInfo("Moderation filtering would be implemented in a full system");
                    loadAllQuestions();
                    break;
                case "Reported Content":
                    // This would require additional database functionality to track reported content
                    showInfo("Reported content filtering would be implemented in a full system");
                    loadAllQuestions();
                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error applying filter: " + e.getMessage());
        }
    }
    
    /**
     * Loads all questions from the database
     */
    private void loadAllQuestions() {
        try {
            questions.setAll(studentDatabaseHelper.getQuestions());
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading questions: " + e.getMessage());
        }
    }
    
    /**
     * Loads all reviewers from the database
     */
    private void loadAllReviewers() {
        try {
            // Note: This is a placeholder as the current database doesn't have a direct method to get all reviewers
            // In a real implementation, this would use a method like studentDatabaseHelper.getAllReviewers()
            showInfo("Reviewer data would be loaded in a full implementation");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading reviewers: " + e.getMessage());
        }
    }
    
    /**
     * Loads all messages for the current user
     */
    private void loadAllMessages() {
        try {
            int userId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
            List<MessageData> allMessages = studentDatabaseHelper.getMessagesForUser(userId);
            messages.setAll(allMessages);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading messages: " + e.getMessage());
        }
    }
    
    /**
     * Loads inbox messages (received messages)
     */
    private void loadInboxMessages() {
        try {
            int userId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
            List<MessageData> inboxMessages = studentDatabaseHelper.getMessagesForUser(userId).stream()
                .filter(m -> m.receiverId == userId)
                .collect(java.util.stream.Collectors.toList());
            messages.setAll(inboxMessages);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading inbox messages: " + e.getMessage());
        }
    }
    
    /**
     * Loads sent messages
     */
    private void loadSentMessages() {
        try {
            int userId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
            List<MessageData> sentMessages = studentDatabaseHelper.getMessagesForUser(userId).stream()
                .filter(m -> m.senderId == userId)
                .collect(java.util.stream.Collectors.toList());
            messages.setAll(sentMessages);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading sent messages: " + e.getMessage());
        }
    }
    
    /**
     * Loads unread messages
     */
    private void loadUnreadMessages() {
        try {
            int userId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
            List<MessageData> unreadMessages = studentDatabaseHelper.getUnreadMessagesForUser(userId);
            messages.setAll(unreadMessages);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading unread messages: " + e.getMessage());
        }
    }
    
    /**
     * Loads system statistics
     */
    private void loadSystemStatistics() {
        // This would connect to the database and load actual statistics in a real implementation
        showInfo("System statistics would be loaded in a full implementation");
    }
    
    /**
     * Handles the action of editing a question
     */
    private void handleEditQuestion() {
        if (selectedQuestion == null) {
            showError("Please select a question to edit");
            return;
        }
        
        // Use the text from moderationTextArea if it's not empty
        String editedText = moderationTextArea.getText().trim();
        if (editedText.isEmpty()) {
            showError("Please enter the edited question content in the moderation text area");
            return;
        }
        
        try {
            studentDatabaseHelper.updateQuestion(selectedQuestion.id, editedText, editedText);
            loadAllQuestions();
            moderationTextArea.clear();
            showSuccess("Question updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error updating question: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of deleting a question
     */
    private void handleDeleteQuestion() {
        if (selectedQuestion == null) {
            showError("Please select a question to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Question");
        confirmation.setHeaderText("Delete Question");
        confirmation.setContentText("Are you sure you want to delete this question? This will also delete all its answers and reviews.");
        
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    studentDatabaseHelper.deleteQuestion(selectedQuestion.id);
                    loadAllQuestions();
                    answers.clear();
                    reviews.clear();
                    selectedQuestion = null;
                    moderationTextArea.clear();
                    showSuccess("Question deleted successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error deleting question: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handles the action of toggling a question's resolved status
     */
    private void handleToggleResolved() {
        if (selectedQuestion == null) {
            showError("Please select a question");
            return;
        }
        
        try {
            if (selectedQuestion.resolved) {
                studentDatabaseHelper.unmarkResolved(selectedQuestion.id);
                showSuccess("Question is no longer marked as resolved.");
            } else {
                // If no answer is selected, prompt the user
                if (selectedAnswer == null) {
                    if (answers.isEmpty()) {
                        showError("This question has no answers to mark as resolved");
                        return;
                    }
                    
                    // Ask the user if they want to mark as resolved without selecting an answer
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Mark as Resolved");
                    alert.setHeaderText("No Answer Selected");
                    alert.setContentText("Do you want to mark this question as resolved without selecting an answer?");
                    
                    alert.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                            try {
                                // Mark as resolved with the first answer
                                studentDatabaseHelper.markAnswerAsResolved(selectedQuestion.id, answers.get(0).id);
                                loadAllQuestions();
                                showSuccess("Question marked as resolved with the first answer!");
                            } catch (SQLException e) {
                                e.printStackTrace();
                                showError("Error marking question as resolved: " + e.getMessage());
                            }
                        }
                    });
                    return;
                }
                
                studentDatabaseHelper.markAnswerAsResolved(selectedQuestion.id, selectedAnswer.id);
                showSuccess("Question marked as resolved with the selected answer!");
            }
            loadAllQuestions();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error updating resolution status: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of editing an answer
     */
    private void handleEditAnswer() {
        if (selectedAnswer == null) {
            showError("Please select an answer to edit");
            return;
        }
        
        // Open a dialog to edit the answer
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Answer");
        dialog.setHeaderText("Edit Answer Content");
        
        // Create a text area for editing
        TextArea textArea = new TextArea(selectedAnswer.content);
        textArea.setWrapText(true);
        textArea.setPrefHeight(200);
        textArea.setPrefWidth(400);
        dialog.getDialogPane().setContent(textArea);
        
        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText();
            }
            return null;
        });
        
        // Process the result
        dialog.showAndWait().ifPresent(result -> {
            if (result != null && !result.trim().isEmpty()) {
                try {
                    studentDatabaseHelper.updateAnswer(selectedAnswer.id, result.trim());
                    if (selectedQuestion != null) {
                        answers.setAll(studentDatabaseHelper.getAnswersForQuestion(selectedQuestion.id));
                    }
                    showSuccess("Answer updated successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error updating answer: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handles the action of deleting an answer
     */
    private void handleDeleteAnswer() {
        if (selectedAnswer == null) {
            showError("Please select an answer to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Answer");
        confirmation.setHeaderText("Delete Answer");
        confirmation.setContentText("Are you sure you want to delete this answer? This will also delete all its reviews.");
        
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    studentDatabaseHelper.deleteAnswer(selectedAnswer.id);
                    if (selectedQuestion != null) {
                        answers.setAll(studentDatabaseHelper.getAnswersForQuestion(selectedQuestion.id));
                    }
                    reviews.clear();
                    selectedAnswer = null;
                    showSuccess("Answer deleted successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error deleting answer: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handles the action of toggling the 'needs clarification' flag for an answer
     */
    private void handleToggleAnswerClarification() {
        if (selectedAnswer == null) {
            showError("Please select an answer");
            return;
        }
        
        try {
            // Toggle the needsClarification status
            boolean newStatus = !selectedAnswer.needsClarification;
            studentDatabaseHelper.markAnswerNeedsClarification(selectedAnswer.id, newStatus);
            
            // Refresh the answers list
            if (selectedQuestion != null) {
                answers.setAll(studentDatabaseHelper.getAnswersForQuestion(selectedQuestion.id));
            }
            
            showSuccess(newStatus ? 
                "Answer marked as needing clarification." : 
                "Clarification flag removed from answer.");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error updating clarification status: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of marking an answer as the solution
     */
    private void handleMarkAnswerAsResolved() {
        if (selectedQuestion == null) {
            showError("Please select a question");
            return;
        }
        
        if (selectedAnswer == null) {
            showError("Please select an answer to mark as resolved");
            return;
        }
        
        try {
            studentDatabaseHelper.markAnswerAsResolved(selectedQuestion.id, selectedAnswer.id);
            loadAllQuestions();
            showSuccess("Question marked as resolved with the selected answer!");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error marking answer as resolved: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of editing a review
     */
    private void handleEditReview() {
        if (selectedReview == null) {
            showError("Please select a review to edit");
            return;
        }
        
        // Open a dialog to edit the review
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Review");
        dialog.setHeaderText("Edit Review Content");
        
        // Create a text area for editing
        TextArea textArea = new TextArea(selectedReview.content);
        textArea.setWrapText(true);
        textArea.setPrefHeight(200);
        textArea.setPrefWidth(400);
        dialog.getDialogPane().setContent(textArea);
        
        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText();
            }
            return null;
        });
        
        // Process the result
        dialog.showAndWait().ifPresent(result -> {
            if (result != null && !result.trim().isEmpty()) {
                try {
                    studentDatabaseHelper.updateReview(
                        selectedReview.id, 
                        selectedReview.reviewerId, 
                        selectedReview.questionId, 
                        selectedReview.answerId,
                        result.trim());
                    
                    // Refresh reviews
                    if (selectedQuestion != null && selectedReview.questionId != -1) {
                        reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(selectedQuestion.id));
                    } else if (selectedAnswer != null && selectedReview.answerId != -1) {
                        reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(selectedAnswer.id));
                    }
                    
                    showSuccess("Review updated successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error updating review: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handles the action of deleting a review
     */
    private void handleDeleteReview() {
        if (selectedReview == null) {
            showError("Please select a review to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Review");
        confirmation.setHeaderText("Delete Review");
        confirmation.setContentText("Are you sure you want to delete this review?");
        
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    studentDatabaseHelper.deleteReview(
                        selectedReview.id, 
                        selectedReview.reviewerId, 
                        selectedReview.questionId, 
                        selectedReview.answerId);
                    
                    // Refresh reviews
                    if (selectedQuestion != null && selectedReview.questionId != -1) {
                        reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(selectedQuestion.id));
                    } else if (selectedAnswer != null && selectedReview.answerId != -1) {
                        reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(selectedAnswer.id));
                    } else {
                        reviews.clear();
                    }
                    
                    selectedReview = null;
                    showSuccess("Review deleted successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error deleting review: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handles the action of contacting a reviewer
     */
    private void handleContactReviewer() {
        if (selectedReview == null) {
            showError("Please select a review first");
            return;
        }
        
        int reviewerId = selectedReview.reviewerId;
        
        try {
            // Get reviewer user ID
            int reviewerUserId = -1;
            // Note: This would need to be implemented in studentDatabase class:
            // reviewerUserId = studentDatabaseHelper.getReviewerUserId(reviewerId);
            
            if (reviewerUserId == -1) {
                showError("Could not find the reviewer user ID");
                return;
            }
            
            // Open message dialog
            openMessageDialog(reviewerUserId, "Reviewer");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error contacting reviewer: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of messaging a question author
     */
    private void handleMessageQuestionAuthor() {
        if (selectedQuestion == null) {
            showError("Please select a question first");
            return;
        }
        
        try {
            int authorId = studentDatabaseHelper.getQuestionOwnerId(selectedQuestion.id);
            if (authorId == -1) {
                showError("Could not find the question author");
                return;
            }
            
            openMessageDialog(authorId, "Question Author");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error messaging question author: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of messaging an answer author
     */
    private void handleMessageAnswerAuthor() {
        if (selectedAnswer == null) {
            showError("Please select an answer first");
            return;
        }
        
        try {
            int authorId = studentDatabaseHelper.getAnswerOwnerId(selectedAnswer.id);
            if (authorId == -1) {
                showError("Could not find the answer author");
                return;
            }
            
            openMessageDialog(authorId, "Answer Author");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error messaging answer author: " + e.getMessage());
        }
    }
    
    /**
     * Opens a dialog to send a message to a user
     * @param recipientId The ID of the recipient
     * @param recipientType A descriptive string for the recipient type
     */
    private void openMessageDialog(int recipientId, String recipientType) {
        try {
            String recipientName = studentDatabaseHelper.getUserName(recipientId);
            
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Send Message");
            dialog.setHeaderText("Send Message to " + recipientName + " (" + recipientType + ")");
            
            // Create the dialog content
            VBox dialogContent = new VBox(10);
            dialogContent.setPadding(new Insets(10));
            
            Label recipientLabel = new Label("To: " + recipientName);
            recipientLabel.setStyle("-fx-font-weight: bold;");
            
            TextArea messageArea = new TextArea();
            messageArea.setPromptText("Type your message here...");
            messageArea.setPrefRowCount(8);
            messageArea.setWrapText(true);
            
            dialogContent.getChildren().addAll(recipientLabel, messageArea);
            dialog.getDialogPane().setContent(dialogContent);
            
            // Add buttons
            ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);
            
            // Set the result converter
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == sendButtonType) {
                    return messageArea.getText();
                }
                return null;
            });
            
            // Handle the result
            dialog.showAndWait().ifPresent(messageText -> {
                if (!messageText.trim().isEmpty()) {
                    try {
                        int senderId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
                        int messageId = studentDatabaseHelper.sendMessage(
                                senderId, 
                                recipientId, 
                                selectedQuestion != null ? selectedQuestion.id : -1, 
                                selectedAnswer != null ? selectedAnswer.id : -1, 
                                messageText.trim());
                        
                        if (messageId != -1) {
                            showSuccess("Message sent successfully!");
                        } else {
                            showError("Failed to send message");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showError("Error sending message: " + e.getMessage());
                    }
                } else {
                    showError("Message cannot be empty");
                }
            });
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error preparing message: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of saving changes to a reviewer
     */
    private void handleSaveReviewerChanges() {
        showInfo("Saving reviewer changes would be implemented in a full system");
    }
    
    /**
     * Handles the action of messaging a reviewer
     */
    private void handleMessageReviewer() {
        showInfo("Messaging reviewer would be implemented in a full system");
    }
    
    /**
     * Handles the action of suspending a reviewer
     */
    private void handleSuspendReviewer() {
        showInfo("Suspending reviewer would be implemented in a full system");
    }
    
    /**
     * Handles the action of sending a reply to a message
     */
    private void handleSendReply(TextArea messageContentArea, TextArea replyArea) {
        // Find the messages ListView in the Messages tab
        Tab messagesTab = tabPane.getTabs().stream()
                .filter(tab -> tab.getText().equals("Messages"))
                .findFirst()
                .orElse(null);
        
        if (messagesTab == null) {
            showError("Messages tab not found");
            return;
        }
        
        // Navigate to the messages ListView inside the tab content hierarchy
        VBox messagesLayout = (VBox) messagesTab.getContent();
        SplitPane splitPane = (SplitPane) messagesLayout.getChildren().get(1);
        VBox messagesListBox = (VBox) splitPane.getItems().get(0);
        ListView<MessageData> messagesListView = null;
        
        // Find the ListView in the messagesListBox children
        for (javafx.scene.Node node : messagesListBox.getChildren()) {
            if (node instanceof ListView) {
                messagesListView = (ListView<MessageData>) node;
                break;
            }
        }
        
        if (messagesListView == null || messagesListView.getSelectionModel().getSelectedItem() == null) {
            showError("Please select a message to reply to");
            return;
        }
        
        String replyText = replyArea.getText().trim();
        if (replyText.isEmpty()) {
            showError("Reply message cannot be empty");
            return;
        }
        
        MessageData selectedMessage = messagesListView.getSelectionModel().getSelectedItem();
        
        try {
            int currentUserId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
            int recipientId = selectedMessage.senderId == currentUserId ? selectedMessage.receiverId : selectedMessage.senderId;
            
            int messageId = studentDatabaseHelper.sendMessage(
                    currentUserId, 
                    recipientId, 
                    selectedMessage.relatedQuestionId, 
                    selectedMessage.relatedAnswerId, 
                    replyText);
            
            if (messageId != -1) {
                replyArea.clear();
                showSuccess("Reply sent successfully!");
                loadAllMessages();
            } else {
                showError("Failed to send reply");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error sending reply: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of sending a new message
     */
    private void handleSendNewMessage(ComboBox<String> recipientTypeComboBox, TextField recipientField, TextArea replyArea) {
        String recipientName = recipientField.getText().trim();
        if (recipientName.isEmpty()) {
            showError("Please enter a recipient username");
            return;
        }
        
        String messageText = replyArea.getText().trim();
        if (messageText.isEmpty()) {
            showError("Message cannot be empty");
            return;
        }
        
        try {
            int recipientId = studentDatabaseHelper.getUserId(recipientName);
            if (recipientId == -1) {
                showError("Recipient not found: " + recipientName);
                return;
            }
            
            int senderId = studentDatabaseHelper.getUserId("testuser"); // Change to actual username in real implementation
            int messageId = studentDatabaseHelper.sendMessage(
                    senderId, 
                    recipientId, 
                    -1, // No related question
                    -1, // No related answer
                    messageText);
            
            if (messageId != -1) {
                replyArea.clear();
                recipientField.clear();
                showSuccess("Message sent successfully!");
                loadAllMessages();
            } else {
                showError("Failed to send message");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error sending message: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of deleting a message
     */
    private void handleDeleteMessage(ListView<MessageData> messagesListView) {
        MessageData selectedMessage = messagesListView.getSelectionModel().getSelectedItem();
        if (selectedMessage == null) {
            showError("Please select a message to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Message");
        confirmation.setHeaderText("Delete Message");
        confirmation.setContentText("Are you sure you want to delete this message?");
        
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Note: Message deletion would need to be implemented in studentDatabase class
                showInfo("Message deletion would be implemented in a full system");
                loadAllMessages();
            }
        });
    }
    
    /**
     * Handles the action of exporting statistics
     */
    private void handleExportStatistics() {
        showInfo("Exporting statistics would be implemented in a full system");
    }
    
    /**
     * Handles the action of logging out
     */
    private void handleLogout() {
        new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
    }
    
    /**
     * Shows an informational message to the user
     * @param message The information message to display
     */
    private void showInfo(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.BLUE);
        statusLabel.setVisible(true);
    }
    
    /**
     * Shows an error message to the user
     * @param message The error message to display
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.RED);
        statusLabel.setVisible(true);
        
        // Display error alert for serious errors
        if (message.startsWith("Error")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred");
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
    
    /**
     * Shows a success message to the user
     * @param message The success message to display
     */
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setVisible(true);
    }
}
package application;

import javafx.scene.Scene;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import databasePart1.DatabaseHelper;

/**
 * StudentHomePage provides the main interface for students to interact with the Q&A system.
 * It allows students to ask questions, view existing questions, search for questions,
 * provide answers, mark questions as resolved, and manage their content.
 */
public class StudentHomePage {
    // Database helper for database operations
    private final studentDatabase studentDatabaseHelper;
    private Stage primaryStage;
    
    // UI components
    private TabPane tabPane;
    private ListView<QuestionData> questionListView;
    private ListView<AnswerData> answerListView;
    private TextArea questionTextArea;
    private TextArea answerTextArea;
    private TextField searchField;
    private Label statusLabel;
    private ComboBox<String> filterComboBox;
    
    // Data structures
    private QuestionData selectedQuestion = null;
    private AnswerData selectedAnswer = null;
    private ObservableList<QuestionData> questions = FXCollections.observableArrayList();
    private ObservableList<AnswerData> answers = FXCollections.observableArrayList();
    
    private ListView<ReviewData> reviewListView;
    private ObservableList<ReviewData> reviews = FXCollections.observableArrayList();
    
    private int currentUserId; // Store the ID of the current user
    private CheckBox showTrustedOnlyCheckBox; // Checkbox to filter reviews
    private ObservableList<ReviewData> allReviews = FXCollections.observableArrayList(); // All reviews
    private List<Integer> trustedReviewerIds = new ArrayList<>(); // List of trusted reviewer IDs

    
    /**
     * Constructor initializes the student database helper
     * @param studentDatabaseHelper The database helper for database operations
     */
 // Update the constructor to initialize the user ID
    public StudentHomePage(studentDatabase studentDatabaseHelper) {
        this.studentDatabaseHelper = studentDatabaseHelper;
        
        // Get the current user ID (using test user)
        try {
            this.currentUserId = studentDatabaseHelper.getUserId("testuser");
        } catch (SQLException e) {
            e.printStackTrace();
            this.currentUserId = -1;
        }
    }

    
    /**
     * Shows the student home page in the provided stage
     * @param primaryStage The primary stage to display the home page
     */
    public void show(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Create main layout with TabPane
        tabPane = new TabPane();
        
        // Questions & Answers Tab
        Tab qaTab = new Tab("Questions & Answers");
        qaTab.setClosable(false);
        qaTab.setContent(createQATabContent());
        
        // My Activity Tab
        Tab myActivityTab = new Tab("My Activity");
        myActivityTab.setClosable(false);
        myActivityTab.setContent(createMyActivityTabContent());
        
        // Trusted Reviewers Tab
        Tab trustedReviewersTab = new Tab("Trusted Reviewers");
        trustedReviewersTab.setClosable(false);
        trustedReviewersTab.setContent(createTrustedReviewersTabContent());
        
        // Add tabs to the TabPane
        tabPane.getTabs().addAll(qaTab, myActivityTab, trustedReviewersTab);
        
        // Create scene and set it on the stage
        Scene scene = new Scene(tabPane, 1000, 700);
        primaryStage.setTitle("Student Q&A System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Load initial data
        loadAllQuestions();
        loadTrustedReviewerIds();
    }
    
    private void loadTrustedReviewerIds() {
        try {
            trustedReviewerIds = studentDatabaseHelper.getTrustedReviewerIds(currentUserId);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading trusted reviewers: " + e.getMessage());
        }
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
        
        // Main content split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.4);
        VBox questionsSection = createQuestionsSection();
        VBox answersWithReviewsSection = createAnswersSection();
        splitPane.getItems().addAll(questionsSection, answersWithReviewsSection);
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
        searchField.setPromptText("Search questions...");
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
            "All Questions", "My Questions", "Resolved", "Unresolved"
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
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showError("Error loading answers: " + e.getMessage());
                    }
                } else {
                    answers.clear();
                }
            });
        
        // Question input area
        questionTextArea = new TextArea();
        questionTextArea.setPromptText("Ask a new question...");
        questionTextArea.setPrefRowCount(3);
        questionTextArea.setId("questionTextArea");
        
        // Ask button
        Button askButton = new Button("Ask Question");
        askButton.setId("askButton");
        askButton.setMaxWidth(Double.MAX_VALUE);
        askButton.setOnAction(e -> handleAskQuestion());
        
        // Add all components to the questions box
        questionsBox.getChildren().addAll(questionsLabel, questionListView, questionTextArea, askButton);
        
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
        
        // Create a split pane to show answers and their reviews
        SplitPane answerReviewSplitPane = new SplitPane();
        
        // Answers list view
        answerListView = new ListView<>(answers);
        answerListView.setId("answerListView");
        VBox.setVgrow(answerListView, Priority.ALWAYS);
        
        // Custom cell factory for answer items
        answerListView.setCellFactory(createAnswerCellFactory());
        
        // Add context menu for answers
        answerListView.setContextMenu(createAnswerContextMenu());
        
        // Add the review section
        VBox reviewsBox = createReviewsSection();
        
        answerReviewSplitPane.getItems().addAll(answerListView, reviewsBox);
        answerReviewSplitPane.setOrientation(Orientation.VERTICAL);
        answerReviewSplitPane.setDividerPositions(0.7);
        VBox.setVgrow(answerReviewSplitPane, Priority.ALWAYS);
        
        // Handle answer selection
        answerListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedAnswer = newSelection;
                if (newSelection != null) {
                    try {
                        // Load reviews for this answer
                        reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(newSelection.id));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showError("Error loading reviews: " + e.getMessage());
                    }
                } else {
                    reviews.clear();
                }
            });
        
        // Answer input area
        answerTextArea = new TextArea();
        answerTextArea.setPromptText("Write an answer...");
        answerTextArea.setPrefRowCount(3);
        answerTextArea.setId("answerTextArea");
        
        // Buttons for answer actions
        HBox buttonBox = new HBox(10);
        
        Button submitButton = new Button("Submit Answer");
        submitButton.setId("submitButton");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(submitButton, Priority.ALWAYS);
        submitButton.setOnAction(e -> handleSubmitAnswer());
        
        Button resolveButton = new Button("Mark as Resolved");
        resolveButton.setId("resolveButton");
        resolveButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resolveButton, Priority.ALWAYS);
        resolveButton.setOnAction(e -> handleToggleResolved());
        
        Button clarificationButton = new Button("Needs Clarification");
        clarificationButton.setId("clarificationButton");
        clarificationButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(clarificationButton, Priority.ALWAYS);
        clarificationButton.setOnAction(e -> handleToggleAnswerClarification());
        
        // Update clarification button text based on answer selection
        answerListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    clarificationButton.setText(newSelection.needsClarification ? 
                        "Remove Clarification Tag" : "Needs Clarification");
                } else {
                    clarificationButton.setText("Needs Clarification");
                }
            });
        
        buttonBox.getChildren().addAll(submitButton, resolveButton, clarificationButton);
        
        // Add all components to the answers box
        answersBox.getChildren().addAll(answersLabel, answerReviewSplitPane, answerTextArea, buttonBox);
        
        return answersBox;
    }

 // Modify the createReviewsSection method to include trust filter
    private VBox createReviewsSection() {
        VBox reviewsBox = new VBox(10);
        reviewsBox.setPadding(new Insets(10));
        
        // Label for reviews section
        Label reviewsLabel = new Label("Reviews");
        reviewsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Add trusted reviewers filter checkbox
        showTrustedOnlyCheckBox = new CheckBox("Show Trusted Reviewers Only");
        showTrustedOnlyCheckBox.setOnAction(e -> applyTrustFilter());
        
        // Reviews list view
        reviewListView = new ListView<>(reviews);
        reviewListView.setId("reviewListView");
        VBox.setVgrow(reviewListView, Priority.ALWAYS);
        
        // Store all reviews
        allReviews = reviews;
        
        // Custom cell factory for review items with trust buttons
        reviewListView.setCellFactory(lv -> new ListCell<ReviewData>() {
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
                    
                    // Try to get reviewer name
                    String reviewerName = "Reviewer";
                    boolean isTrusted = false;
                    
                    try {
                        reviewerName = studentDatabaseHelper.getReviewerName(review.reviewerId);
                        isTrusted = trustedReviewerIds.contains(review.reviewerId);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        reviewerName = "Reviewer #" + review.reviewerId;
                    }
                    
                    // Metadata line
                    HBox metaBox = new HBox(10);
                    Label reviewerLabel = new Label("By: " + reviewerName);
                    reviewerLabel.setStyle("-fx-font-style: italic;");
                    
                    // Trust status indicator
                    Label trustLabel = new Label(isTrusted ? "[Trusted]" : "");
                    trustLabel.setTextFill(Color.GREEN);
                    trustLabel.setStyle("-fx-font-weight: bold;");
                    
                    // Trust/Untrust button
                    Button trustButton = new Button(isTrusted ? "Untrust Reviewer" : "Trust Reviewer");
                    trustButton.setOnAction(e -> toggleTrustStatus(review.reviewerId, trustButton, trustLabel));
                    
                    metaBox.getChildren().addAll(reviewerLabel, trustLabel, trustButton);
                    
                    container.getChildren().addAll(contentLabel, metaBox);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        reviewsBox.getChildren().addAll(reviewsLabel, showTrustedOnlyCheckBox, reviewListView);
        
        return reviewsBox;
    }
    
    
 // Add method to toggle trust status
    private void toggleTrustStatus(int reviewerId, Button trustButton, Label trustLabel) {
        try {
            boolean currentlyTrusted = trustedReviewerIds.contains(reviewerId);
            
            if (currentlyTrusted) {
                // Remove trust
                studentDatabaseHelper.removeTrustedReviewer(currentUserId, reviewerId);
                trustButton.setText("Trust Reviewer");
                trustLabel.setText("");
                trustedReviewerIds.remove(Integer.valueOf(reviewerId));
                showSuccess("Reviewer removed from trusted list");
            } else {
                // Add trust
                studentDatabaseHelper.addTrustedReviewer(currentUserId, reviewerId);
                trustButton.setText("Untrust Reviewer");
                trustLabel.setText("[Trusted]");
                trustedReviewerIds.add(reviewerId);
                showSuccess("Reviewer added to trusted list");
            }
            
            // Reapply filter if active
            if (showTrustedOnlyCheckBox.isSelected()) {
                applyTrustFilter();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error updating trust status: " + e.getMessage());
        }
    }
    
    private void applyTrustFilter() {
        if (showTrustedOnlyCheckBox.isSelected()) {
            // Filter reviews to only show trusted reviewers
            List<ReviewData> filteredReviews = allReviews.stream()
                .filter(review -> trustedReviewerIds.contains(review.reviewerId))
                .collect(java.util.stream.Collectors.toList());
            
            reviewListView.setItems(FXCollections.observableArrayList(filteredReviews));
            
            if (filteredReviews.isEmpty() && !allReviews.isEmpty()) {
                showInfo("No reviews from trusted reviewers. Consider adding more trusted reviewers.");
            }
        } else {
            // Show all reviews
            reviewListView.setItems(allReviews);
        }
    }
    
 // Add method to create trusted reviewers tab content
    private VBox createTrustedReviewersTabContent() {
        VBox trustedReviewersLayout = new VBox(10);
        trustedReviewersLayout.setPadding(new Insets(10));
        
        Label headerLabel = new Label("Your Trusted Reviewers");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Create list view for trusted reviewers
        ListView<ReviewerRecord> trustedReviewersListView = new ListView<>();
        VBox.setVgrow(trustedReviewersListView, Priority.ALWAYS);
        
        // Load trusted reviewers
        try {
            List<ReviewerRecord> trustedReviewers = studentDatabaseHelper.getTrustedReviewers(currentUserId);
            trustedReviewersListView.setItems(FXCollections.observableArrayList(trustedReviewers));
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading trusted reviewers: " + e.getMessage());
        }
        
        // Custom cell factory for trusted reviewers
        trustedReviewersListView.setCellFactory(lv -> new ListCell<ReviewerRecord>() {
            @Override
            protected void updateItem(ReviewerRecord reviewer, boolean empty) {
                super.updateItem(reviewer, empty);
                
                if (empty || reviewer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(10);
                    container.setPadding(new Insets(5));
                    
                    Label nameLabel = new Label(reviewer.userName);
                    nameLabel.setPrefWidth(200);
                    
                    Button removeButton = new Button("Remove Trust");
                    removeButton.setOnAction(e -> {
                        try {
                            studentDatabaseHelper.removeTrustedReviewer(currentUserId, reviewer.reviewerId);
                            showSuccess("Reviewer removed from trusted list");
                            
                            // Remove from trusted IDs list
                            trustedReviewerIds.remove(Integer.valueOf(reviewer.reviewerId));
                            
                            // Refresh the list
                            List<ReviewerRecord> updatedList = studentDatabaseHelper.getTrustedReviewers(currentUserId);
                            trustedReviewersListView.setItems(FXCollections.observableArrayList(updatedList));
                            
                            // Reapply the filter if active
                            if (showTrustedOnlyCheckBox != null && showTrustedOnlyCheckBox.isSelected()) {
                                applyTrustFilter();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showError("Error removing trusted reviewer: " + ex.getMessage());
                        }
                    });
                    
                    container.getChildren().addAll(nameLabel, removeButton);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        // Add explanation text
        Label explanationLabel = new Label(
            "Trusted reviewers are those whose opinions you value highly. " +
            "Their reviews will be highlighted and can be filtered to show only their feedback."
        );
        explanationLabel.setWrapText(true);
        
        trustedReviewersLayout.getChildren().addAll(headerLabel, explanationLabel, trustedReviewersListView);
        
        return trustedReviewersLayout;
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
                    
                    // Try to get reviewer name
                    String reviewerName = "Reviewer";
                    try {
                        reviewerName = "Reviewer #" + review.reviewerId;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    Label reviewerLabel = new Label("By: " + reviewerName);
                    reviewerLabel.setStyle("-fx-font-style: italic;");
                    
                    container.getChildren().addAll(contentLabel, reviewerLabel);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    /**
     * Shows an informational message to the user
     * @param message The info message to display
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
        
        // Make alert for more serious errors
        if (message.startsWith("Error")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
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
    
    /**
     * Creates the content for the My Activity tab
     * @return VBox containing the My Activity tab content
     */
    private VBox createMyActivityTabContent() {
        VBox myActivityLayout = new VBox(10);
        myActivityLayout.setPadding(new Insets(10));
        
        // Create tabs for different types of activities
        TabPane activityTabPane = new TabPane();
        
        // My Questions tab
        Tab myQuestionsTab = new Tab("My Questions");
        myQuestionsTab.setClosable(false);
        
        ListView<QuestionData> myQuestionsListView = new ListView<>();
        myQuestionsListView.setCellFactory(createQuestionCellFactory());
        
        myQuestionsTab.setContent(myQuestionsListView);
        
        // My Answers tab
        Tab myAnswersTab = new Tab("My Answers");
        myAnswersTab.setClosable(false);
        
        ListView<AnswerData> myAnswersListView = new ListView<>();
        myAnswersListView.setCellFactory(createAnswerCellFactory());
        
        myAnswersTab.setContent(myAnswersListView);
        
        // Reviews on My Answers tab
        Tab reviewsOnMyAnswersTab = new Tab("Reviews on My Answers");
        reviewsOnMyAnswersTab.setClosable(false);
        
        ListView<ReviewData> reviewsOnMyAnswersListView = new ListView<>();
        reviewsOnMyAnswersListView.setCellFactory(createReviewCellFactory());
        
        reviewsOnMyAnswersTab.setContent(reviewsOnMyAnswersListView);
        
        // Trusted Reviews tab
        Tab trustedReviewsTab = new Tab("Trusted Reviews");
        trustedReviewsTab.setClosable(false);
        
        ListView<ReviewData> trustedReviewsListView = new ListView<>();
        trustedReviewsListView.setCellFactory(createReviewCellFactory());
        
        trustedReviewsTab.setContent(trustedReviewsListView);
        
        // Load questions when tab is selected
        myQuestionsTab.setOnSelectionChanged(e -> {
            if (myQuestionsTab.isSelected()) {
                try {
                    int userId = studentDatabaseHelper.getUserId("testuser");
                    List<QuestionData> myQuestions = studentDatabaseHelper.getQuestionsForUser(userId);
                    myQuestionsListView.setItems(FXCollections.observableArrayList(myQuestions));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading your questions: " + ex.getMessage());
                }
            }
        });
        
        // Load my answers when tab is selected
        myAnswersTab.setOnSelectionChanged(e -> {
            if (myAnswersTab.isSelected()) {
                try {
                    int userId = studentDatabaseHelper.getUserId("testuser");
                    List<AnswerData> myAnswers = studentDatabaseHelper.getAnswersForUser(userId);
                    myAnswersListView.setItems(FXCollections.observableArrayList(myAnswers));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading your answers: " + ex.getMessage());
                }
            }
        });
        
        // Load reviews on my answers when tab is selected
        reviewsOnMyAnswersTab.setOnSelectionChanged(e -> {
            if (reviewsOnMyAnswersTab.isSelected()) {
                try {
                    int userId = studentDatabaseHelper.getUserId("testuser");
                    List<ReviewData> myAnswerReviews = studentDatabaseHelper.getReviewsForUserAnswers(userId);
                    reviewsOnMyAnswersListView.setItems(FXCollections.observableArrayList(myAnswerReviews));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading reviews for your answers: " + ex.getMessage());
                }
            }
        });
        
        // Load trusted reviews when tab is selected
        trustedReviewsTab.setOnSelectionChanged(e -> {
            if (trustedReviewsTab.isSelected()) {
                try {
                    List<ReviewData> trustedReviews = studentDatabaseHelper.getTrustedReviews(currentUserId, -1);
                    trustedReviewsListView.setItems(FXCollections.observableArrayList(trustedReviews));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading trusted reviews: " + ex.getMessage());
                }
            }
        });
        
        // Add tabs to the activity TabPane
        activityTabPane.getTabs().addAll(myQuestionsTab, myAnswersTab, reviewsOnMyAnswersTab, trustedReviewsTab);
        VBox.setVgrow(activityTabPane, Priority.ALWAYS);
        
        // Add Request Reviewer Role button
        Button requestReviewerButton = new Button("Request to Become a Reviewer");
        requestReviewerButton.setOnAction(e -> {
            try {
                // Get the database helper for the request page
                DatabaseHelper dbHelper = new DatabaseHelper();
                dbHelper.connectToDatabase();
                
                // Navigate to the request page with the current username
                new RequestReviewerRolePage(dbHelper, "testuser").show(primaryStage);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showError("Error opening reviewer request page: " + ex.getMessage());
            }
        });
        
        myActivityLayout.getChildren().addAll(activityTabPane, requestReviewerButton);
        
        return myActivityLayout;
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
                    
                    // If this answer is the one that resolved the question
                    if (selectedQuestion != null && selectedQuestion.resolved) {
                        try {
                            boolean isResolvedAnswer = false; // placeholder
                            if (isResolvedAnswer) {
                                Label resolvedAnswerLabel = new Label("[Selected Solution]");
                                resolvedAnswerLabel.setTextFill(Color.GREEN);
                                resolvedAnswerLabel.setStyle("-fx-font-weight: bold;");
                                metaBox.getChildren().add(resolvedAnswerLabel);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
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
        
        contextMenu.getItems().addAll(editItem, deleteItem);
        
        // Dynamic menu items
        questionListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                contextMenu.getItems().removeIf(item -> 
                    item.getText().equals("Mark as Resolved") || 
                    item.getText().equals("Unmark as Resolved"));
                
                if (newSelection != null) {
                    MenuItem resolveItem;
                    if (newSelection.resolved) {
                        resolveItem = new MenuItem("Unmark as Resolved");
                        resolveItem.setOnAction(e -> handleUnresolve());
                    } else {
                        resolveItem = new MenuItem("Mark as Resolved");
                        resolveItem.setOnAction(e -> handleMarkAsResolved());
                    }
                    contextMenu.getItems().add(resolveItem);
                }
            });
        
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
        
        MenuItem clarificationItem = new MenuItem("Needs Clarification");
        clarificationItem.setOnAction(e -> handleToggleAnswerClarification());
        
        MenuItem markResolvedItem = new MenuItem("Mark as Solution");
        markResolvedItem.setOnAction(e -> handleMarkAsResolved());
        
        contextMenu.getItems().addAll(editItem, deleteItem, clarificationItem, markResolvedItem);
        
        // Update the clarification menu item text
        answerListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    // Updating clarification update item
                    for (MenuItem item : contextMenu.getItems()) {
                        if (item.getText().equals("Needs Clarification") ||
                            item.getText().equals("Remove Clarification Tag")) {
                            
                            item.setText(newSelection.needsClarification ? 
                                "Remove Clarification Tag" : "Needs Clarification");
                            break;
                        }
                    }
                }
            });
        
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
                case "My Questions":
                    int userId = studentDatabaseHelper.getUserId("testuser");
                    List<QuestionData> myQuestions = studentDatabaseHelper.getQuestionsForUser(userId);
                    questions.setAll(myQuestions);
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
     * Handles the action of asking a new question
     */
    private void handleAskQuestion() {
        String questionText = questionTextArea.getText().trim();
        if (questionText.isEmpty()) {
            showError("Question field cannot be empty. Please enter your question.");
            return;
        }
        
        try {
            int userId = studentDatabaseHelper.getUserId("testuser");
            if (userId != -1) {
                System.out.println("Got user ID: " + userId);
                int questionId = studentDatabaseHelper.addQuestion(questionText, questionText, userId);
                System.out.println("Got question ID: " + questionId);
                if (questionId != -1) {
                    loadAllQuestions();
                    questionTextArea.clear();
                    showSuccess("Question posted successfully!");
                } else {
                    showError("Failed to add question");
                }
            } else {
                showError("Test user not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error adding question: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of submitting a new answer
     */
    private void handleSubmitAnswer() {
        if (selectedQuestion == null) {
            showError("Please select a question to answer");
            return;
        }
        
        String answerText = answerTextArea.getText().trim();
        if (answerText.isEmpty()) {
            showError("Answer field cannot be empty. Please enter your answer.");
            return;
        }
        
        try {
            int userId = studentDatabaseHelper.getUserId("testuser");
            if (userId != -1) {
                System.out.println("Got user ID for answer: " + userId);
                int answerId = studentDatabaseHelper.addAnswer(answerText, selectedQuestion.id, userId);
                System.out.println("Got answer ID: " + answerId);
                if (answerId != -1) {
                    answers.setAll(studentDatabaseHelper.getAnswersForQuestion(selectedQuestion.id));
                    answerTextArea.clear();
                    loadAllQuestions();
                    
                    // Check if any reviews for similar answers
                    List<ReviewData> questionReviews = studentDatabaseHelper.getReviewsForQuestion(selectedQuestion.id);
                    if (!questionReviews.isEmpty()) {
                        showInfo("This question has " + questionReviews.size() + 
                                 " review(s). They may contain helpful information for your answer.");
                    }
                    
                    showSuccess("Answer submitted successfully!");
                } else {
                    showError("Failed to add answer");
                }
            } else {
                showError("Test user not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error adding answer: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of marking an answer as the solution
     */
    private void handleMarkAsResolved() {
        if (selectedQuestion == null) {
            showError("Please select a question");
            return;
        }
        
        AnswerData selectedAnswer = answerListView.getSelectionModel().getSelectedItem();
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
                AnswerData selectedAnswer = answerListView.getSelectionModel().getSelectedItem();
                if (selectedAnswer == null) {
                    showError("Please select an answer to mark as resolved");
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
     * Handles the action of unmarking a question as resolved
     */
    private void handleUnresolve() {
        if (selectedQuestion == null) {
            showError("Please select a question");
            return;
        }
        
        if (!selectedQuestion.resolved) {
            showError("This question is not marked as resolved");
            return;
        }
        
        try {
            studentDatabaseHelper.unmarkResolved(selectedQuestion.id);
            loadAllQuestions();
            showSuccess("Question is no longer marked as resolved.");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error unmarking question as resolved: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of editing a question
     */
    private void handleEditQuestion() {
        QuestionData selectedQuestion = questionListView.getSelectionModel().getSelectedItem();
        if (selectedQuestion == null) {
            showError("Please select a question to edit");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Question");
        dialog.setHeaderText("Edit your question");

        // Create the dialog content
        TextArea editArea = new TextArea(selectedQuestion.title);
        editArea.setPrefRowCount(5);
        editArea.setPrefColumnCount(40);
        editArea.setWrapText(true);
        dialog.getDialogPane().setContent(editArea);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Set the result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return editArea.getText();
            }
            return null;
        });

        // Handle the result
        dialog.showAndWait().ifPresent(editedText -> {
            if (!editedText.trim().isEmpty()) {
                try {
                    studentDatabaseHelper.updateQuestion(selectedQuestion.id, editedText.trim(), editedText.trim());
                    loadAllQuestions();
                    showSuccess("Question updated successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error updating question: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Handles the action of editing an answer
     */
    private void handleEditAnswer() {
        AnswerData selectedAnswer = answerListView.getSelectionModel().getSelectedItem();
        if (selectedAnswer == null) {
            showError("Please select an answer to edit");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Answer");
        dialog.setHeaderText("Edit your answer");

        // Create the dialog content
        TextArea editArea = new TextArea(selectedAnswer.content);
        editArea.setPrefRowCount(5);
        editArea.setPrefColumnCount(40);
        editArea.setWrapText(true);
        dialog.getDialogPane().setContent(editArea);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Set the result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return editArea.getText();
            }
            return null;
        });

        // Handle the result
        dialog.showAndWait().ifPresent(editedText -> {
            if (!editedText.trim().isEmpty()) {
                try {
                    studentDatabaseHelper.updateAnswer(selectedAnswer.id, editedText.trim());
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
     * Handles the action of deleting a question
     */
    private void handleDeleteQuestion() {
        QuestionData selectedQuestion = questionListView.getSelectionModel().getSelectedItem();
        if (selectedQuestion == null) {
            showError("Please select a question to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Question");
        confirmation.setHeaderText("Delete Question");
        confirmation.setContentText("Are you sure you want to delete this question? This will also delete all its answers.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                studentDatabaseHelper.deleteQuestion(selectedQuestion.id);
                loadAllQuestions();
                answers.clear();
                this.selectedQuestion = null;
                showSuccess("Question deleted successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error deleting question: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles the action of deleting an answer
     */
    private void handleDeleteAnswer() {
        AnswerData selectedAnswer = answerListView.getSelectionModel().getSelectedItem();
        if (selectedAnswer == null) {
            showError("Please select an answer to delete");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Answer");
        confirmation.setHeaderText("Delete Answer");
        confirmation.setContentText("Are you sure you want to delete this answer?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                studentDatabaseHelper.deleteAnswer(selectedAnswer.id);
                if (selectedQuestion != null) {
                    answers.setAll(studentDatabaseHelper.getAnswersForQuestion(selectedQuestion.id));
                }
                loadAllQuestions();
                showSuccess("Answer deleted successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error deleting answer: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles the action of toggling the 'needs clarification' flag for an answer
     */
    private void handleToggleAnswerClarification() {
        AnswerData selectedAnswer = answerListView.getSelectionModel().getSelectedItem();
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
}

package application;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import databasePart1.DatabaseHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * This page displays a simple welcome message for the user.
 */

public class ReviewerHomePage {
	
	//private final DatabaseHelper databaseHelper;
	private final studentDatabase studentDatabaseHelper;
	private User user;
	
	// UI components
    private TabPane tabPane;
    private ListView<QuestionData> questionListView;
    private ListView<AnswerData> answerListView;
    private ListView<ReviewData> reviewListView;
    private TextArea reviewTextArea;
    private TextArea answerReviewTextArea;
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

    public ReviewerHomePage(studentDatabase studentDatabaseHelper, User user) {
        this.studentDatabaseHelper = studentDatabaseHelper;
        this.user = user;
    }

    public void show(Stage primaryStage) {
        // Create main layout with TabPane
        tabPane = new TabPane();
        
        // Questions & Answers Tab
        Tab qaTab = new Tab("Questions & Answers");
        qaTab.setClosable(false);
        qaTab.setContent(createQATabContent());
        
        // My Reviews Tab
        Tab myReviewsTab = new Tab("My Reviews");
        myReviewsTab.setClosable(false);
        myReviewsTab.setContent(createMyActivityTabContent());
        
        // Messages Tab
        Tab messagesTab = new Tab("Messages");
        messagesTab.setClosable(false);
        messagesTab.setContent(createMessagesTabContent());
        
        // Add tabs to the TabPane
        tabPane.getTabs().addAll(qaTab, myReviewsTab, messagesTab);
        
        // Create scene and set it on the stage
        Scene scene = new Scene(tabPane, 1000, 700);
        primaryStage.setTitle("Reviewer Q&A System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Load initial data
        loadAllQuestions();
        
        // Check for unread messages and update UI
        checkForUnreadMessages();
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
        
        // Main content split pane with three equal sections
        SplitPane splitPane = new SplitPane();
        // Set divider positions to create three equal sections (0.33, 0.67)
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
        
        // Review input area
        reviewTextArea = new TextArea();
        reviewTextArea.setPromptText("Add a new question review");
        reviewTextArea.setPrefRowCount(3);
        reviewTextArea.setId("reviewTextArea");
        
        // Add Review button
        Button addReviewButton = new Button("Add Review");
        addReviewButton.setId("addReview");
        addReviewButton.setMaxWidth(Double.MAX_VALUE);
        addReviewButton.setOnAction(e -> handleAddReview(selectedQuestion, null));
        
        // Add all components to the questions box
        questionsBox.getChildren().addAll(questionsLabel, questionListView, reviewTextArea, addReviewButton);
        
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
                } else {
                    answers.clear();
                    reviews.clear();
                }
            });
        
        // Review input area
        answerReviewTextArea = new TextArea();
        answerReviewTextArea.setPromptText("Add a new answer review");
        answerReviewTextArea.setPrefRowCount(3);
        answerReviewTextArea.setId("answerReviewTextArea");
        
        // Buttons for review actions
        HBox buttonBox = new HBox(10);
        
        Button submitReviewButton = new Button("Submit Review");
        submitReviewButton.setId("submitReviewButton");
        submitReviewButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(submitReviewButton, Priority.ALWAYS);
        submitReviewButton.setOnAction(e -> handleAddReview(null, selectedAnswer));
//        
//        Button resolveButton = new Button("Mark as Resolved");
//        resolveButton.setId("resolveButton");
//        resolveButton.setMaxWidth(Double.MAX_VALUE);
//        HBox.setHgrow(resolveButton, Priority.ALWAYS);
//        resolveButton.setOnAction(e -> handleToggleResolved());
//        
//        Button clarificationButton = new Button("Needs Clarification");
//        clarificationButton.setId("clarificationButton");
//        clarificationButton.setMaxWidth(Double.MAX_VALUE);
//        HBox.setHgrow(clarificationButton, Priority.ALWAYS);
//        clarificationButton.setOnAction(e -> handleToggleAnswerClarification());
        
        // Update clarification button text based on answer selection
//        answerListView.getSelectionModel().selectedItemProperty().addListener(
//            (obs, oldSelection, newSelection) -> {
//                if (newSelection != null) {
//                    clarificationButton.setText(newSelection.needsClarification ? 
//                        "Remove Clarification Tag" : "Needs Clarification");
//                } else {
//                    clarificationButton.setText("Needs Clarification");
//                }
//            });
        
        buttonBox.getChildren().addAll(submitReviewButton);
        
        // Add all components to the answers box
        answersBox.getChildren().addAll(answersLabel, answerListView, answerReviewTextArea, buttonBox);
        
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
        
        // Add context menu for reviews
        //reviewListView.setContextMenu(createQuestionContextMenu());
        
        // Handle review selection
        reviewListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedReview = newSelection;
//                if (newSelection != null) {
//                    try {
//                        //answers.setAll(studentDatabaseHelper.getAnswersForQuestion(newSelection.id));
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                        showError("Error loading answers: " + e.getMessage());
//                    }
//                } else {
//                    answers.clear();
//                }
            });
        
        
        // Buttons for review actions
        HBox buttonBox = new HBox(10);
        
        Button editReviewButton = new Button("Edit Review");
        editReviewButton.setOnAction(e -> handleEditReview());
        Button deleteReviewButton = new Button("Delete Review");
        deleteReviewButton.setOnAction(e -> handleDeleteReview());
        
        buttonBox.getChildren().addAll(editReviewButton, deleteReviewButton);
        
        // Add all components to the questions box
        reviewsBox.getChildren().addAll(reviewsLabel, reviewListView, buttonBox);
        
        return reviewsBox;
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
        
        // My Reviews tab
        Tab myReviewsTab = new Tab("My Reviews");
        myReviewsTab.setClosable(false);
        
        // Creating a ListView for my reviews
        ListView<ReviewData> myReviewsListView = new ListView<>();
        myReviewsListView.setCellFactory(createReviewCellFactory());
        
        // Add context menu for my reviews to enable edit/delete functionality
        ContextMenu reviewContextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Review");
        editItem.setOnAction(e -> {
            ReviewData selectedReview = myReviewsListView.getSelectionModel().getSelectedItem();
            if (selectedReview != null) {
                handleEditReview(selectedReview, myReviewsListView);
            } else {
                showError("Please select a review to edit");
            }
        });
        
        MenuItem deleteItem = new MenuItem("Delete Review");
        deleteItem.setOnAction(e -> {
            ReviewData selectedReview = myReviewsListView.getSelectionModel().getSelectedItem();
            if (selectedReview != null) {
                handleDeleteReview(selectedReview, myReviewsListView);
            } else {
                showError("Please select a review to delete");
            }
        });
        
        reviewContextMenu.getItems().addAll(editItem, deleteItem);
        myReviewsListView.setContextMenu(reviewContextMenu);
        
        myReviewsTab.setContent(myReviewsListView);
        
        // My Questions tab (keeping existing code)
        Tab myQuestionsTab = new Tab("My Questions");
        myQuestionsTab.setClosable(false);
        
        ListView<QuestionData> myQuestionsListView = new ListView<>();
        myQuestionsListView.setCellFactory(createQuestionCellFactory());
        
        myQuestionsTab.setContent(myQuestionsListView);
        
        // My Answers tab (keeping existing code)
        Tab myAnswersTab = new Tab("My Answers");
        myAnswersTab.setClosable(false);
        
        ListView<AnswerData> myAnswersListView = new ListView<>();
        myAnswersListView.setCellFactory(createAnswerCellFactory());
        
        myAnswersTab.setContent(myAnswersListView);
        
        // Load my reviews when the tab is selected
        myReviewsTab.setOnSelectionChanged(e -> {
            if (myReviewsTab.isSelected()) {
                try {
                    int userId = studentDatabaseHelper.getUserId(user.getUserName());
                    int reviewerId = studentDatabaseHelper.getReviewerId(userId);
                    if (reviewerId != -1) {
                        // Fetch all reviews by this reviewer
                        List<ReviewData> myReviews = studentDatabaseHelper.getReviewsByReviewer(reviewerId);
                        myReviewsListView.setItems(FXCollections.observableArrayList(myReviews));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading your reviews: " + ex.getMessage());
                }
            }
        });
        
        // Load my questions when tab is selected (keeping existing code)
        myQuestionsTab.setOnSelectionChanged(e -> {
            if (myQuestionsTab.isSelected()) {
                try {
                    int userId = studentDatabaseHelper.getUserId(user.getUserName());
                    // This would need to be implemented in studentDatabase.java:
                    // myQuestionsListView.setItems(FXCollections.observableArrayList(
                    //     studentDatabaseHelper.getQuestionsForUser(userId)));
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
                    int userId = studentDatabaseHelper.getUserId(user.getUserName());
                    // This would need to be implemented in studentDatabase.java:
                    // myAnswersListView.setItems(FXCollections.observableArrayList(
                    //     studentDatabaseHelper.getAnswersForUser(userId)));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading your answers: " + ex.getMessage());
                }
            }
        });
        
        // Add tabs to the activity TabPane - putting My Reviews first
        activityTabPane.getTabs().addAll(myReviewsTab, myQuestionsTab, myAnswersTab);
        VBox.setVgrow(activityTabPane, Priority.ALWAYS);
        
        myActivityLayout.getChildren().add(activityTabPane);
        
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
                            // This would need additional database method to check
                            // if this answer is the resolved one for the question
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
                    
                    if (review.questionId != -1) {
                        try {
                            // Create a final copy of the question
                            final QuestionData question = studentDatabaseHelper.getQuestionById(review.questionId);
                            
                            if (question != null) {
                                // Title with resolved status
                                HBox titleBox = new HBox(5);
                                Label titleLabel = new Label("Review of: " + question.title);
                                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                                titleBox.getChildren().addAll(titleLabel);
                                
                                // Metadata line
                                HBox metaBox = new HBox(10);
                                Label reviewContent = new Label(review.content);
                                
                                metaBox.getChildren().addAll(reviewContent);
                                
                                // Add message button
                                Button messageButton = new Button("Message Author");
                                messageButton.setOnAction(e -> openMessageDialog(question, null));
                                
                                container.getChildren().addAll(titleBox, metaBox, messageButton);
                                setGraphic(container);
                                setText(null);
                            } else {
                                setText("Review of a deleted question");
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            setText("Error loading question: " + e.getMessage());
                        }
                    } else {
                        try {
                            // Create a final copy of the answer
                            final AnswerData answer = studentDatabaseHelper.getAnswerById(review.answerId);
                            
                            if (answer != null) {
                                // Title with resolved status
                                HBox titleBox = new HBox(5);
                                Label titleLabel = new Label("Review of: " + answer.content);
                                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                                titleBox.getChildren().addAll(titleLabel);
                                
                                // Metadata line
                                HBox metaBox = new HBox(10);
                                Label reviewContent = new Label(review.content);
                                
                                metaBox.getChildren().addAll(reviewContent);
                                
                                // Add message button
                                Button messageButton = new Button("Message Author");
                                messageButton.setOnAction(e -> openMessageDialog(null, answer));
                                
                                container.getChildren().addAll(titleBox, metaBox, messageButton);
                                setGraphic(container);
                                setText(null);
                            } else {
                                setText("Review of a deleted answer");
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            setText("Error loading answer: " + e.getMessage());
                        }
                    }
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
        
        MenuItem messageItem = new MenuItem("Message Author");
        messageItem.setOnAction(e -> {
            if (selectedQuestion != null) {
                openMessageDialog(selectedQuestion, null);
            }
        });
        
        contextMenu.getItems().addAll(messageItem);
        
        return contextMenu;
    }
    
    /**
     * Creates a context menu for the answer list view
     * @return ContextMenu with answer-related actions
     */
    private ContextMenu createAnswerContextMenu() {
    	ContextMenu contextMenu = new ContextMenu();
        
        MenuItem messageItem = new MenuItem("Message Author");
        messageItem.setOnAction(e -> {
            if (selectedAnswer != null) {
                openMessageDialog(null, selectedAnswer);
            }
        });
        
        contextMenu.getItems().addAll(messageItem);
        
        // Update the clarification menu item text based on the selected answer
        answerListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    // Find the clarification menu item and update it
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
     * Opens a dialog to send a direct message to a user
     * @param question The question whose author will receive the message, or null
     * @param answer The answer whose author will receive the message, or null
     */
    private void openMessageDialog(QuestionData question, AnswerData answer) {
        try {
            // Determine recipient ID
            int recipientId = -1;
            int relatedQuestionId = -1;
            int relatedAnswerId = -1;
            String recipientName = "";
            String contentPreview = "";
            
            if (question != null) {
                recipientId = studentDatabaseHelper.getQuestionOwnerId(question.id);
                relatedQuestionId = question.id;
                recipientName = question.userName;
                contentPreview = "Question: " + question.title;
            } else if (answer != null) {
                recipientId = studentDatabaseHelper.getAnswerOwnerId(answer.id);
                relatedAnswerId = answer.id;
                recipientName = answer.userName;
                contentPreview = "Answer: " + (answer.content.length() > 30 ? 
                        answer.content.substring(0, 27) + "..." : answer.content);
            }
            
            if (recipientId == -1) {
                showError("Could not identify the recipient");
                return;
            }
            
            // Create the message dialog
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Send Message");
            dialog.setHeaderText("Send a message to " + recipientName + " about their " + 
                    (question != null ? "question" : "answer"));
            
            // Create the dialog content
            VBox dialogContent = new VBox(10);
            dialogContent.setPadding(new Insets(10));
            
            Label previewLabel = new Label("Regarding: " + contentPreview);
            previewLabel.setStyle("-fx-font-style: italic;");
            
            TextArea messageArea = new TextArea();
            messageArea.setPromptText("Type your message here...");
            messageArea.setPrefRowCount(8);
            messageArea.setWrapText(true);
            
            dialogContent.getChildren().addAll(previewLabel, messageArea);
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
            
            // Final variables for use in lambda
            final int finalRecipientId = recipientId;
            final int finalRelatedQuestionId = relatedQuestionId;
            final int finalRelatedAnswerId = relatedAnswerId;
            
            // Handle the result
            dialog.showAndWait().ifPresent(messageText -> {
                if (!messageText.trim().isEmpty()) {
                    try {
                        int senderId = studentDatabaseHelper.getUserId(user.getUserName());
                        int messageId = studentDatabaseHelper.sendMessage(
                                senderId, 
                                finalRecipientId, 
                                finalRelatedQuestionId, 
                                finalRelatedAnswerId, 
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
                    // This would need to be implemented in studentDatabase.java:
                    // questions.setAll(studentDatabaseHelper.getQuestionsForUser(userId));
                    break;
                case "Resolved":
                    // This would need to be implemented in studentDatabase.java:
                    // questions.setAll(studentDatabaseHelper.getQuestionsByStatus(true));
                    break;
                case "Unresolved":
                    // This would need to be implemented in studentDatabase.java:
                    // questions.setAll(studentDatabaseHelper.getQuestionsByStatus(false));
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
     * Handles the action of adding a new review to a question
     */
    private void handleAddReview(QuestionData question, AnswerData answer) {
    	String responseText;
    	if (question != null) {
    		responseText = reviewTextArea.getText().trim();
            if (responseText.isEmpty()) {
                showError("Review field cannot be empty. Please enter your Review.");
                return;
            }
    	} else {
    		responseText = answerReviewTextArea.getText().trim();
            if (responseText.isEmpty()) {
                showError("Review field cannot be empty. Please enter your Review.");
                return;
            }
    	}
        
        try {
            int userId = studentDatabaseHelper.getUserId(user.getUserName());
            int reviewerId = studentDatabaseHelper.getReviewerId(userId);
            if (userId != -1 && reviewerId != -1) {
                System.out.println("Got user ID: " + userId);
                System.out.println("Got reviewer ID: " + reviewerId);
                
                int reviewId = -1;
                if (answer == null) {
                	reviewId = studentDatabaseHelper.addReview(reviewerId, question.id, -1, responseText);
                	System.out.println("Got Review ID: " + reviewId);
                    if (reviewId != -1) {
                        reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(question.id));
                        reviewTextArea.clear();
                        showSuccess("Review posted successfully!");
                    } else {
                        showError("Failed to add review");
                    }
                } else if (question == null) {
                	reviewId = studentDatabaseHelper.addReview(reviewerId, -1, answer.id, responseText);
                	if (reviewId != -1) {
                		reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(answer.id));
                        answerReviewTextArea.clear();
                        showSuccess("Review posted successfully!");
                    } else {
                        showError("Failed to add review");
                    }
                }
            } else {
                showError("user not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error adding question: " + e.getMessage());
        }
    }
    
    /**
     * Handles the action of editing a selected review
     * @param reviewParam The review to edit (can be from either tab)
     * @param listView The ListView to refresh after editing (can be null if using main tab)
     */
    private void handleEditReview(ReviewData reviewParam, ListView<ReviewData> listView) {
        // Create a final copy of the review to use in the lambda
        final ReviewData review = (reviewParam == null) ? selectedReview : reviewParam;
        
        if (review != null) {
            try {
                int userId = studentDatabaseHelper.getUserId(user.getUserName());
                int reviewerId = studentDatabaseHelper.getReviewerId(userId);
                
                if (review.reviewerId == reviewerId) {
                    Dialog<String> dialog = new Dialog<>();
                    dialog.setTitle("Edit review");
                    dialog.setHeaderText("Edit your review");
                    
                    // Create the dialog content
                    TextArea editArea = new TextArea(review.content);
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
                    
                    // Store these values before the lambda to make them effectively final
                    final int reviewId = review.id;
                    final int questionId = review.questionId;
                    final int answerId = review.answerId;
                    
                    // Handle the result
                    dialog.showAndWait().ifPresent(editedText -> {
                        if (!editedText.trim().isEmpty()) {
                            try {
                                studentDatabaseHelper.updateReview(reviewId, reviewerId, questionId, answerId, editedText.trim());
                                
                                // Refresh the appropriate list view
                                if (listView != null) {
                                    // We're in the My Reviews tab, refresh that list
                                    refreshMyReviewsList(listView, reviewerId);
                                } else {
                                    // We're in the main tab, refresh that list
                                    if (questionId != -1) {
                                        reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(questionId));
                                    } else {
                                        reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(answerId));
                                    }
                                    selectedReview = null;
                                }
                                
                                showSuccess("Review updated successfully!");
                            } catch (SQLException e) {
                                e.printStackTrace();
                                showError("Error updating review: " + e.getMessage());
                            }
                        }
                    });
                } else {
                    showError("You must be the reviewer of the review to edit");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error: " + e.getMessage());
            }
        } else {
            showError("Please select a review to edit");
        }
    }

    /**
     * Handles the action of deleting a selected review
     * @param reviewParam The review to delete (can be from either tab)
     * @param listView The ListView to refresh after deleting (can be null if using main tab)
     */
    private void handleDeleteReview(ReviewData reviewParam, ListView<ReviewData> listView) {
        // Create a final copy of the review to use in the lambda
        final ReviewData review = (reviewParam == null) ? selectedReview : reviewParam;
        
        if (review != null) {
            try {
                int userId = studentDatabaseHelper.getUserId(user.getUserName());
                int reviewerId = studentDatabaseHelper.getReviewerId(userId);
                
                if (review.reviewerId == reviewerId) {
                    // Confirm deletion
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Delete");
                    confirmAlert.setHeaderText("Delete Review");
                    confirmAlert.setContentText("Are you sure you want to delete this review?");
                    
                    // Store these values before the lambda to make them effectively final
                    final int reviewId = review.id;
                    final int questionId = review.questionId;
                    final int answerId = review.answerId;
                    
                    confirmAlert.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                            try {
                                studentDatabaseHelper.deleteReview(reviewId, reviewerId, questionId, answerId);
                                
                                // Refresh the appropriate list view
                                if (listView != null) {
                                    // We're in the My Reviews tab, refresh that list
                                    refreshMyReviewsList(listView, reviewerId);
                                } else {
                                    // We're in the main tab, refresh that list
                                    if (questionId != -1) {
                                        reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(questionId));
                                    } else {
                                        reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(answerId));
                                    }
                                    selectedReview = null;
                                }
                                
                                showSuccess("Review deleted successfully!");
                            } catch (SQLException e) {
                                e.printStackTrace();
                                showError("Error deleting review: " + e.getMessage());
                            }
                        }
                    });
                } else {
                    showError("Must be the reviewer of the review to delete.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error: " + e.getMessage());
            }
        } else {
            showError("Must have a review selected");
        }
    }

    /**
     * Helper method to refresh the My Reviews list
     * @param listView The ListView to refresh
     * @param reviewerId The reviewer ID to filter by
     * @throws SQLException
     */
    private void refreshMyReviewsList(ListView<ReviewData> listView, int reviewerId) throws SQLException {
        List<ReviewData> myReviews = studentDatabaseHelper.getReviewsByReviewer(reviewerId);
        listView.setItems(FXCollections.observableArrayList(myReviews));
    }

    /**
     * Update the original handleEditReview and handleDeleteReview methods to call the new methods
     */
    private void handleEditReview() {
        handleEditReview(null, null);
    }

    private void handleDeleteReview() {
        handleDeleteReview(null, null);
    }
    
    /**
     * Creates the content for the Messages tab
     * @return VBox containing the Messages tab content
     */
    private VBox createMessagesTabContent() {
        VBox messagesLayout = new VBox(10);
        messagesLayout.setPadding(new Insets(10));
        
        // Create a split pane to divide messages list and message content
        SplitPane splitPane = new SplitPane();
        
        // Left side: Messages list
        VBox messagesListBox = new VBox(10);
        messagesListBox.setPadding(new Insets(5));
        
        Label messagesLabel = new Label("Messages");
        messagesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Messages list
        ListView<MessageData> messagesListView = new ListView<>();
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
                    boolean isIncoming = false;
					try {
						isIncoming = message.receiverId == studentDatabaseHelper.getUserId(user.getUserName());
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    
                    Label fromToLabel = new Label(isIncoming ? "From: " + message.senderName : "To: " + message.receiverName);
                    fromToLabel.setStyle("-fx-font-weight: bold;");
                    
                    Label dateLabel = new Label(message.createDate.toLocalDateTime().format(formatter));
                    dateLabel.setStyle("-fx-font-size: 11px;");
                    
                    HBox.setHgrow(dateLabel, Priority.ALWAYS);
                    dateLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    
                    headerBox.getChildren().addAll(fromToLabel, dateLabel);
                    
                    // If unread and an incoming message, highlight it
                    if (!message.isRead && isIncoming) {
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
                    
                    // Related content info
                    String relatedInfo = "";
                    try {
                        if (message.relatedQuestionId != -1) {
                            QuestionData question = studentDatabaseHelper.getQuestionById(message.relatedQuestionId);
                            if (question != null) {
                                relatedInfo = "Re: Question \"" + question.title + "\"";
                            }
                        } else if (message.relatedAnswerId != -1) {
                            AnswerData answer = studentDatabaseHelper.getAnswerById(message.relatedAnswerId);
                            if (answer != null) {
                                relatedInfo = "Re: Answer to a question";
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    
                    if (!relatedInfo.isEmpty()) {
                        Label relatedLabel = new Label(relatedInfo);
                        relatedLabel.setStyle("-fx-font-style: italic; -fx-font-size: 11px;");
                        container.getChildren().addAll(headerBox, previewLabel, relatedLabel);
                    } else {
                        container.getChildren().addAll(headerBox, previewLabel);
                    }
                    
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        messagesListBox.getChildren().addAll(messagesLabel, messagesListView);
        
        // Right side: Message content and reply
        VBox messageContentBox = new VBox(10);
        messageContentBox.setPadding(new Insets(5));
        
        Label messageDetailLabel = new Label("Message Details");
        messageDetailLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
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
        
        Button sendReplyButton = new Button("Send Reply");
        sendReplyButton.setMaxWidth(Double.MAX_VALUE);
        
        messageContentBox.getChildren().addAll(messageDetailLabel, messageContentArea, replyLabel, replyArea, sendReplyButton);
        
        // Add both sides to the split pane
        splitPane.getItems().addAll(messagesListBox, messageContentBox);
        splitPane.setDividerPositions(0.4);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        // Handle message selection
        messagesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                messageContentArea.setText(newVal.content);
                
                // If this is an incoming message and it's unread, mark it as read
                try {
                    int currentUserId = studentDatabaseHelper.getUserId(user.getUserName());
                    if (newVal.receiverId == currentUserId && !newVal.isRead) {
                        studentDatabaseHelper.markMessageAsRead(newVal.id, currentUserId);
                        
                        // Refresh the list to update the unread status
                        refreshMessagesList(messagesListView);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error marking message as read: " + e.getMessage());
                }
            } else {
                messageContentArea.clear();
            }
        });
        
        // Handle reply button
        sendReplyButton.setOnAction(e -> {
            MessageData selectedMessage = messagesListView.getSelectionModel().getSelectedItem();
            if (selectedMessage != null && !replyArea.getText().trim().isEmpty()) {
                try {
                    int currentUserId = studentDatabaseHelper.getUserId(user.getUserName());
                    int recipientId = (selectedMessage.senderId == currentUserId) ? 
                            selectedMessage.receiverId : selectedMessage.senderId;
                    
                    int messageId = studentDatabaseHelper.sendMessage(
                            currentUserId, 
                            recipientId, 
                            selectedMessage.relatedQuestionId, 
                            selectedMessage.relatedAnswerId, 
                            replyArea.getText().trim());
                    
                    if (messageId != -1) {
                        replyArea.clear();
                        showSuccess("Reply sent successfully!");
                        
                        // Refresh the messages list
                        refreshMessagesList(messagesListView);
                    } else {
                        showError("Failed to send reply");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error sending reply: " + ex.getMessage());
                }
            } else if (replyArea.getText().trim().isEmpty()) {
                showError("Reply cannot be empty");
            } else {
                showError("Please select a message to reply to");
            }
        });
        
        // Add a refresh button
        Button refreshButton = new Button("Refresh Messages");
        refreshButton.setOnAction(e -> refreshMessagesList(messagesListView));
        
        messagesLayout.getChildren().addAll(refreshButton, splitPane);
        
        // Initial loading of messages
        try {
            int userId = studentDatabaseHelper.getUserId(user.getUserName());
            List<MessageData> messages = studentDatabaseHelper.getMessagesForUser(userId);
            messagesListView.setItems(FXCollections.observableArrayList(messages));
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading messages: " + e.getMessage());
        }
        
        return messagesLayout;
    }
    
    /**
     * Helper method to refresh the messages list
     */
    private void refreshMessagesList(ListView<MessageData> messagesListView) {
        try {
            int userId = studentDatabaseHelper.getUserId(user.getUserName());
            List<MessageData> messages = studentDatabaseHelper.getMessagesForUser(userId);
            messagesListView.setItems(FXCollections.observableArrayList(messages));
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error refreshing messages: " + e.getMessage());
        }
    }
    
    /**
     * Check for unread messages and update UI indicators
     */
    private void checkForUnreadMessages() {
        try {
            int userId = studentDatabaseHelper.getUserId(user.getUserName());
            List<MessageData> unreadMessages = studentDatabaseHelper.getUnreadMessagesForUser(userId);
            
            if (!unreadMessages.isEmpty()) {
                // Update the Messages tab to indicate unread messages
                Tab messagesTab = tabPane.getTabs().stream()
                        .filter(tab -> tab.getText().startsWith("Messages"))
                        .findFirst()
                        .orElse(null);
                
                if (messagesTab != null) {
                    messagesTab.setText("Messages (" + unreadMessages.size() + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error checking for unread messages: " + e.getMessage());
        }
    }
}
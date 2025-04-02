package application;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

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
        
        // Add tabs to the TabPane
        tabPane.getTabs().addAll(qaTab, myReviewsTab);
        
        // Create scene and set it on the stage
        Scene scene = new Scene(tabPane, 1000, 700);
        primaryStage.setTitle("Reviewer Q&A System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Load initial data
        loadAllQuestions();
    	
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
        splitPane.getItems().addAll(createQuestionsSection(), createAnswersSection(), createReviewsSection());
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
        
        // Load my questions when tab is selected
        myQuestionsTab.setOnSelectionChanged(e -> {
            if (myQuestionsTab.isSelected()) {
                try {
                    int userId = studentDatabaseHelper.getUserId("testuser");
                    // This would need to be implemented in studentDatabase.java:
                    // myQuestionsListView.setItems(FXCollections.observableArrayList(
                    //     studentDatabaseHelper.getQuestionsForUser(userId)));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error loading your questions: " + ex.getMessage());
                }
            }
        });
        
        // Add tabs to the activity TabPane
        activityTabPane.getTabs().addAll(myQuestionsTab, myAnswersTab);
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
            //private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
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
                    	QuestionData question;
						try {
							question = studentDatabaseHelper.getQuestionById(review.questionId);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							question = null;
						}
						
                    	// Title with resolved status
                        HBox titleBox = new HBox(5);
                        Label titleLabel = new Label("Review of: " + question.title);
                        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                        titleBox.getChildren().addAll(titleLabel);
                        
                        // Metadata line
                        HBox metaBox = new HBox(10);
                        Label reviewContent = new Label(review.content);
                        //reviewContent.setStyle("-fx-font-style: italic;");
                        
                        metaBox.getChildren().addAll(reviewContent);
                        
                        container.getChildren().addAll(titleBox, metaBox);
                        setGraphic(container);
                        setText(null);
                    } else {
                    	AnswerData answer;
						try {
							answer = studentDatabaseHelper.getAnswerById(review.answerId);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							answer = null;
						}
                    	// Title with resolved status
                        HBox titleBox = new HBox(5);
                        Label titleLabel = new Label("Review of: " + answer.content);
                        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                        titleBox.getChildren().addAll(titleLabel);
                        
                        // Metadata line
                        HBox metaBox = new HBox(10);
                        Label reviewContent = new Label(review.content);
                        //reviewContent.setStyle("-fx-font-style: italic;");
                        
                        metaBox.getChildren().addAll(reviewContent);
                        
                        container.getChildren().addAll(titleBox, metaBox);
                        setGraphic(container);
                        setText(null);
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
        
//        MenuItem editItem = new MenuItem("Edit Question");
//        editItem.setOnAction(e -> handleEditQuestion());
//        
//        MenuItem deleteItem = new MenuItem("Delete Question");
//        deleteItem.setOnAction(e -> handleDeleteQuestion());
        
        //contextMenu.getItems().addAll(editItem, deleteItem);
        
        // Add dynamic menu items based on question selection
//        questionListView.getSelectionModel().selectedItemProperty().addListener(
//            (obs, oldSelection, newSelection) -> {
//                // Remove any existing resolve/unresolve menu items
//                contextMenu.getItems().removeIf(item -> 
//                    item.getText().equals("Mark as Resolved") || 
//                    item.getText().equals("Unmark as Resolved"));
//                
//                if (newSelection != null) {
//                    MenuItem resolveItem;
//                    if (newSelection.resolved) {
//                        resolveItem = new MenuItem("Unmark as Resolved");
//                        resolveItem.setOnAction(e -> handleUnresolve());
//                    } else {
//                        resolveItem = new MenuItem("Mark as Resolved");
//                        resolveItem.setOnAction(e -> handleMarkAsResolved());
//                    }
//                    contextMenu.getItems().add(resolveItem);
//                }
//            });
        
        return contextMenu;
    }
    
    /**
     * Creates a context menu for the answer list view
     * @return ContextMenu with answer-related actions
     */
    private ContextMenu createAnswerContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
//        MenuItem editItem = new MenuItem("Edit Answer");
//        editItem.setOnAction(e -> handleEditAnswer());
//        
//        MenuItem deleteItem = new MenuItem("Delete Answer");
//        deleteItem.setOnAction(e -> handleDeleteAnswer());
//        
//        MenuItem clarificationItem = new MenuItem("Needs Clarification");
//        clarificationItem.setOnAction(e -> handleToggleAnswerClarification());
//        
//        MenuItem markResolvedItem = new MenuItem("Mark as Solution");
//        markResolvedItem.setOnAction(e -> handleMarkAsResolved());
        
        //contextMenu.getItems().addAll(editItem, deleteItem, clarificationItem, markResolvedItem);
        
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
     */
    private void handleEditReview() {
    	if (selectedReview != null) {
    		try {
    			int userId = studentDatabaseHelper.getUserId(user.getUserName());
    			int reviewerId = studentDatabaseHelper.getReviewerId(userId);
    			if (selectedReview.reviewerId == reviewerId) {
    				Dialog<String> dialog = new Dialog<>();
    		    	dialog.setTitle("Edit review");
    		    	dialog.setHeaderText("Edit your answer");
    		    	
    		    	// Create the dialog content
    		        TextArea editArea = new TextArea(selectedReview.content);
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
    		                    studentDatabaseHelper.updateReview(selectedReview.id, reviewerId, selectedReview.questionId, selectedReview.answerId, editedText.trim());
    		                    if (selectedReview.questionId != -1) {
    		    					reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(selectedReview.questionId));
    		    				} else {
    		    					reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(selectedReview.answerId));
    		    				}
    		    				this.selectedReview = null;
    		                    showSuccess("Review updated successfully!");
    		                } catch (SQLException e) {
    		                    e.printStackTrace();
    		                    showError("Error updating review: " + e.getMessage());
    		                }
    		            }
    		        });
    			} else {
    				showError("Can Must be the reivwer of the reviwer to delete");
    			}
    			
    		} catch (SQLException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	} else {
    		showError("Please select a review to edit");
    		return;
    	}
    }
    
    /**
     * Handles the action of deleting a selected review
     */
    private void handleDeleteReview() {
    	if (selectedReview != null) {
    		try {
    			int userId = studentDatabaseHelper.getUserId(user.getUserName());
    			int reviewerId = studentDatabaseHelper.getReviewerId(userId);
    			
    			if (selectedReview.reviewerId == reviewerId) {
    				studentDatabaseHelper.deleteReview(selectedReview.id, reviewerId, selectedReview.questionId, selectedReview.answerId);
    				if (selectedReview.questionId != -1) {
    					reviews.setAll(studentDatabaseHelper.getReviewsForQuestion(selectedReview.questionId));
    				} else {
    					reviews.setAll(studentDatabaseHelper.getReviewsForAnswer(selectedReview.answerId));
    				}
    				this.selectedReview = null;
    				showSuccess("Review deleted successfully!");
    			} else {
    				showError("Must be the reviewer of the review to delete.");
    			}

    			
    		} catch (SQLException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	} else {
    		showError("Must have a review selected");
            return;
    	}
    }
}
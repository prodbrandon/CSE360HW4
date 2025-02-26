package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class StudentHomePage {
    private final studentDatabase studentDatabaseHelper;
    private ListView<QuestionData> questionListView;
    private ListView<AnswerData> answerListView;
    private TextArea questionTextArea;
    private TextArea answerTextArea;
    private TextField searchField;
    private QuestionData selectedQuestion = null;
    private ObservableList<QuestionData> questions = FXCollections.observableArrayList();
    private ObservableList<AnswerData> answers = FXCollections.observableArrayList();
    
    public StudentHomePage(studentDatabase studentDatabaseHelper) {
        this.studentDatabaseHelper = studentDatabaseHelper;
    }
    
    public void show(Stage primaryStage) {
        TabPane tabPane = new TabPane();
        
        // Questions & Answers Tab
        Tab qaTab = new Tab("Questions & Answers");
        qaTab.setClosable(false);
        
        VBox qaLayout = new VBox(10);
        qaLayout.setStyle("-fx-padding: 10;");
        
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search questions...");
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
        
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(createQuestionsSection(), createAnswersSection());
        
        qaLayout.getChildren().addAll(searchField, splitPane);
        qaTab.setContent(qaLayout);
        
        tabPane.getTabs().add(qaTab);
        
        Scene scene = new Scene(tabPane, 800, 600);
        primaryStage.setTitle("Student Q&A System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Load initial data
        loadAllQuestions();
    }
    
    private VBox createQuestionsSection() {
        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 10;");
        
        Label label = new Label("Questions");
        
        questionListView = new ListView<>(questions);
        questionListView.setCellFactory(lv -> new ListCell<QuestionData>() {
            @Override
            protected void updateItem(QuestionData question, boolean empty) {
                super.updateItem(question, empty);
                if (empty || question == null) {
                    setText(null);
                } else {
                    setText(question.title + "\nBy: " + question.userName + 
                          (question.resolved ? " [Resolved]" : "") +
                          "\nAnswers: " + question.answerCount);
                }
            }
        });
        
        // Add context menu
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
        
        questionTextArea = new TextArea();
        questionTextArea.setPromptText("Ask a new question...");
        questionTextArea.setPrefRowCount(3);
        
        Button askButton = new Button("Ask Question");
        askButton.setMaxWidth(Double.MAX_VALUE);
        askButton.setOnAction(e -> handleAskQuestion());
        
        box.getChildren().addAll(label, questionListView, questionTextArea, askButton);
        return box;
    }
    
    private ContextMenu createQuestionContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Question");
        editItem.setOnAction(e -> handleEditQuestion());
        
        MenuItem deleteItem = new MenuItem("Delete Question");
        deleteItem.setOnAction(e -> handleDeleteQuestion());
        
        // We'll create this MenuItem on-demand when a question is selected
        // instead of creating it once at initialization
        
        contextMenu.getItems().addAll(editItem, deleteItem);
        
        // Add listener to selection model to update context menu dynamically
        questionListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                // Remove any existing resolve/unresolve menu items
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
    
    private VBox createAnswersSection() {
        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 10;");
        
        Label label = new Label("Answers");
        
        answerListView = new ListView<>(answers);
        answerListView.setCellFactory(lv -> new ListCell<AnswerData>() {
            @Override
            protected void updateItem(AnswerData answer, boolean empty) {
                super.updateItem(answer, empty);
                if (empty || answer == null) {
                    setText(null);
                } else {
                    setText(answer.content + "\nBy: " + answer.userName);
                }
            }
        });
        
        // Add context menu
        answerListView.setContextMenu(createAnswerContextMenu());
        
        answerTextArea = new TextArea();
        answerTextArea.setPromptText("Write an answer...");
        answerTextArea.setPrefRowCount(3);
        
        HBox buttonBox = new HBox(10);
        Button submitButton = new Button("Submit Answer");
        Button resolveButton = new Button("Mark as Resolved");
        
        submitButton.setMaxWidth(Double.MAX_VALUE);
        resolveButton.setMaxWidth(Double.MAX_VALUE);
        
        HBox.setHgrow(submitButton, Priority.ALWAYS);
        HBox.setHgrow(resolveButton, Priority.ALWAYS);
        
        submitButton.setOnAction(e -> handleSubmitAnswer());
        
        // Update button text based on question status
        questionListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    resolveButton.setText(newSelection.resolved ? "Unmark as Resolved" : "Mark as Resolved");
                }
            });
        
        // Update action handler
        resolveButton.setOnAction(e -> {
            if (selectedQuestion == null) {
                showError("Please select a question");
                return;
            }
            
            // Toggle resolved status
            try {
                if (selectedQuestion.resolved) {
                    studentDatabaseHelper.unmarkResolved(selectedQuestion.id);
                } else {
                    AnswerData selectedAnswer = answerListView.getSelectionModel().getSelectedItem();
                    if (selectedAnswer == null) {
                        showError("Please select an answer to mark as resolved");
                        return;
                    }
                    studentDatabaseHelper.markAnswerAsResolved(selectedQuestion.id, selectedAnswer.id);
                }
                loadAllQuestions();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showError("Error updating question resolution status: " + ex.getMessage());
            }
        });
        
        buttonBox.getChildren().addAll(submitButton, resolveButton);
        box.getChildren().addAll(label, answerListView, answerTextArea, buttonBox);
        return box;
    }
    
    private ContextMenu createAnswerContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Answer");
        editItem.setOnAction(e -> handleEditAnswer());
        
        MenuItem deleteItem = new MenuItem("Delete Answer");
        deleteItem.setOnAction(e -> handleDeleteAnswer());
        
        contextMenu.getItems().addAll(editItem, deleteItem);
        return contextMenu;
    }
    
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
    
    private void handleSubmitAnswer() {
        if (selectedQuestion == null) {
            showError("Please select a question to answer");
            return;
        }
        
        String answerText = answerTextArea.getText().trim();
        if (!answerText.isEmpty()) {
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
    }
    
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
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error marking answer as resolved: " + e.getMessage());
        }
    }
    
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
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    studentDatabaseHelper.deleteQuestion(selectedQuestion.id);
                    loadAllQuestions();
                    answers.clear();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error deleting question: " + ex.getMessage());
                }
            }
        });
    }
    
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
        editArea.setPrefRowCount(3);
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
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error updating question: " + e.getMessage());
                }
            }
        });
    }

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
        editArea.setPrefRowCount(3);
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
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error updating answer: " + e.getMessage());
                }
            }
        });
    }
    
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
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    studentDatabaseHelper.deleteAnswer(selectedAnswer.id);
                    if (selectedQuestion != null) {
                        answers.setAll(studentDatabaseHelper.getAnswersForQuestion(selectedQuestion.id));
                    }
                    loadAllQuestions();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Error deleting answer: " + ex.getMessage());
                }
            }
        });
    }
    
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
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error unmarking question as resolved: " + e.getMessage());
        }
    }
    
    private void loadAllQuestions() {
        try {
            questions.setAll(studentDatabaseHelper.getQuestions());
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading questions: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
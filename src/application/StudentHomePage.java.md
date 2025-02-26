# Student Q&A System: Internal Code Documentation

[Linked Table of Contents](#toc)

## <a name="toc"></a>Table of Contents

* [1. Overview](#overview)
* [2. Class `StudentHomePage`](#class-studenthomepage)
    * [2.1 Constructor `StudentHomePage(studentDatabase studentDatabaseHelper)`](#constructor-studenthomepage-studentdatabasestudentdatabasehelper)
    * [2.2 Method `show(Stage primaryStage)`](#method-showstage-primarystage)
    * [2.3 Method `createQuestionsSection()`](#method-createquestionssection)
        * [2.3.1  `questionListView` Cell Factory and Context Menu](#questionlistview-cell-factory-and-context-menu)
        * [2.3.2 Handling Question Selection](#handling-question-selection)
        * [2.3.3  `handleAskQuestion()`](#handleaskquestion)
    * [2.4 Method `createAnswerSection()`](#method-createanswerssection)
        * [2.4.1 `answerListView` Cell Factory and Context Menu](#answerlistview-cell-factory-and-context-menu)
        * [2.4.2 Handling Answer Submission and Resolution](#handling-answer-submission-and-resolution)
        * [2.4.3 `handleSubmitAnswer()`](#handlesubmitanswer)
    * [2.5 Method `createQuestionContextMenu()`](#method-createquestioncontextmenu)
    * [2.6 Method `createAnswerContextMenu()`](#method-createanswercontextmenu)
    * [2.7 Methods for Handling User Actions](#methods-for-handling-user-actions)
        * [2.7.1 `handleMarkAsResolved()`](#handlemarkasresolved)
        * [2.7.2 `handleDeleteQuestion()`](#handledeletequestion)
        * [2.7.3 `handleEditQuestion()`](#handleeditquestion)
        * [2.7.4 `handleEditAnswer()`](#handleeditanswer)
        * [2.7.5 `handleDeleteAnswer()`](#handledeleteanswer)
        * [2.7.6 `handleUnresolve()`](#handleunresolve)
    * [2.8 Method `loadAllQuestions()`](#method-loadallquestions)
    * [2.9 Method `showError(String message)`](#method-showerrorstring-message)


## <a name="overview"></a>1. Overview

This document details the implementation of the `StudentHomePage` class, which is responsible for creating and managing the user interface of a student question and answer system.  The system uses JavaFX for the GUI and interacts with a database (via the `studentDatabase` helper class) to persist and retrieve questions and answers.

## <a name="class-studenthomepage"></a>2. Class `StudentHomePage`

This class manages the main UI for students, displaying questions, answers, and providing functionalities to interact with them.

### <a name="constructor-studenthomepage-studentdatabasestudentdatabasehelper"></a>2.1 Constructor `StudentHomePage(studentDatabase studentDatabaseHelper)`

The constructor initializes the `StudentHomePage` object with a reference to the `studentDatabase` helper class, which is used for all database interactions.

```java
public StudentHomePage(studentDatabase studentDatabaseHelper) {
    this.studentDatabaseHelper = studentDatabaseHelper;
}
```

### <a name="method-showstage-primarystage"></a>2.2 Method `show(Stage primaryStage)`

This method is responsible for creating and displaying the main JavaFX stage.  It sets up a `TabPane` containing a "Questions & Answers" tab.  The method also initializes the search functionality and loads all initial questions.

```java
public void show(Stage primaryStage) {
    // ... JavaFX UI setup ...
    
    // Load initial data
    loadAllQuestions();
}
```

### <a name="method-createquestionssection"></a>2.3 Method `createQuestionsSection()`

This method creates the UI section for displaying and managing questions.  It includes:

* A search field (`searchField`) that dynamically filters the displayed questions based on user input. The search uses the `studentDatabaseHelper.searchQuestions()` method. If the search field is empty, it calls `loadAllQuestions()` to display all questions.
* A `ListView` (`questionListView`) to display questions.
* A `TextArea` (`questionTextArea`) for composing new questions.
* An "Ask Question" button to submit new questions.

#### <a name="questionlistview-cell-factory-and-context-menu"></a>2.3.1  `questionListView` Cell Factory and Context Menu

A custom cell factory is used to format the display of each question in the `questionListView`.  It shows the question title, author, resolution status, and answer count.  A context menu provides options to edit, delete, and resolve/unresolve questions.  The context menu is dynamically updated based on the selected question.


#### <a name="handling-question-selection"></a>2.3.2 Handling Question Selection

A listener is attached to the `questionListView`'s selection model. When a question is selected, the corresponding answers are loaded from the database using `studentDatabaseHelper.getAnswersForQuestion()`, and displayed in the `answerListView`. If no question is selected, the answers list is cleared.


#### <a name="handleaskquestion"></a>2.3.3  `handleAskQuestion()`

This method handles the logic for submitting a new question.  It retrieves user input from `questionTextArea`, validates it, and uses `studentDatabaseHelper.addQuestion()` to add the question to the database.  It then updates the UI and displays any errors.  It uses a hardcoded "testuser" for simplicity.  A more robust implementation would incorporate proper user authentication and authorization.


```java
private void handleAskQuestion() {
    // ... Input validation and database interaction ...
}
```


### <a name="method-createanswerssection"></a>2.4 Method `createAnswersSection()`

This method creates the UI section for displaying and managing answers.  It includes:

* A `ListView` (`answerListView`) to display answers.
* A `TextArea` (`answerTextArea`) for composing new answers.
* "Submit Answer" and "Mark as Resolved" buttons.  The "Mark as Resolved" button dynamically updates its text and action depending on the selected question's resolution status.

#### <a name="answerlistview-cell-factory-and-context-menu"></a>2.4.1 `answerListView` Cell Factory and Context Menu

Similar to `questionListView`, `answerListView` uses a custom cell factory to format answer display and a context menu for editing and deleting answers.

#### <a name="handling-answer-submission-and-resolution"></a>2.4.2 Handling Answer Submission and Resolution

This section handles user interactions for submitting answers and marking answers/questions as resolved.  It uses the `studentDatabaseHelper` to interact with the database and updates the UI accordingly.  The `handleMarkAsResolved` function is used to set a selected answer as resolved for a selected question.  If there is no selected answer, an error message is displayed.

#### <a name="handlesubmitanswer"></a>2.4.3 `handleSubmitAnswer()`

This method handles the logic for submitting a new answer. It retrieves the answer text, validates that a question is selected, and uses `studentDatabaseHelper.addAnswer()` to add the answer to the database. It then updates the UI and displays errors if any.  It uses a hardcoded "testuser" for simplicity, similar to `handleAskQuestion`.


```java
private void handleSubmitAnswer() {
    // ... Input validation and database interaction ...
}
```

### <a name="method-createquestioncontextmenu"></a>2.5 Method `createQuestionContextMenu()`

This method creates the context menu for the `questionListView`.  It dynamically adds or removes a "Mark as Resolved" / "Unmark as Resolved" item based on the selected question's status.

### <a name="method-createanswercontextmenu"></a>2.6 Method `createAnswerContextMenu()`

This method creates the context menu for the `answerListView`.

### <a name="methods-for-handling-user-actions"></a>2.7 Methods for Handling User Actions

These methods handle actions triggered by user interactions (button clicks, menu selections). They perform the appropriate database operations via `studentDatabaseHelper` and update the UI.

#### <a name="handlemarkasresolved"></a>2.7.1 `handleMarkAsResolved()`

Marks the selected answer as resolved for the selected question.

#### <a name="handledeletequestion"></a>2.7.2 `handleDeleteQuestion()`

Deletes the selected question and its associated answers after confirmation from the user.

#### <a name="handleeditquestion"></a>2.7.3 `handleEditQuestion()`

Opens a dialog to edit the selected question and updates the database. Uses a Dialog to allow the user to edit the question text.

#### <a name="handleeditanswer"></a>2.7.4 `handleEditAnswer()`

Opens a dialog to edit the selected answer and updates the database. Uses a Dialog to allow the user to edit the answer text.

#### <a name="handledeleteanswer"></a>2.7.5 `handleDeleteAnswer()`

Deletes the selected answer after confirmation from the user.

#### <a name="handleunresolve"></a>2.7.6 `handleUnresolve()`

Unmarks the selected question as resolved.

### <a name="method-loadallquestions"></a>2.8 Method `loadAllQuestions()`

Loads all questions from the database using `studentDatabaseHelper.getQuestions()` and updates the `questions` ObservableList.

### <a name="method-showerrorstring-message"></a>2.9 Method `showError(String message)`

Displays an error alert with the given message.

# ReviewerHomePage Class Documentation

[TOC]

## 1. Introduction

The `ReviewerHomePage` class is responsible for displaying a welcome message and providing a logout functionality for reviewers.  It utilizes JavaFX for the user interface and interacts with a database via the `DatabaseHelper` class.

## 2. Class Overview

| Feature          | Description                                                                 |
|-----------------|-----------------------------------------------------------------------------|
| **Purpose**      | Displays a welcome screen for reviewers and allows logout.                   |
| **Dependencies** | `databasePart1.DatabaseHelper`, `javafx.application.Platform`, `javafx.scene.*`, `javafx.stage.*` |
| **Data Members** | `databaseHelper`: An instance of `DatabaseHelper` for database interaction. |


## 3. Constructor

```java
public ReviewerHomePage(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
}
```

The constructor initializes the `ReviewerHomePage` object with a `DatabaseHelper` instance. This instance is used for any necessary database operations, although none are explicitly performed within this class.  The dependency injection approach allows for easy testing and swapping of database implementations.


## 4. `show()` Method

```java
public void show(Stage primaryStage) {
    VBox layout = new VBox();
    layout.setStyle("-fx-alignment: center; -fx-padding: 20;");

    // Button to let the user logout
    Button quitButton = new Button("Logout");
    quitButton.setOnAction(a -> {
        new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
    });

    // Label to display Hello user
    Label userLabel = new Label("Hello, Reviewer!");
    userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    layout.getChildren().addAll(userLabel, quitButton);
    Scene userScene = new Scene(layout, 800, 400);

    // Set the scene to primary stage
    primaryStage.setScene(userScene);
    primaryStage.setTitle("Reviewer Page");
}
```

The `show()` method is the primary method of this class. It constructs the JavaFX scene and sets it on the provided `primaryStage`.

* **Layout Creation:** A `VBox` is created to arrange the UI elements vertically.  Styling is applied to center the content and add padding.
* **Logout Button:** A `Button` labeled "Logout" is created.  Its `setOnAction` method is used to handle the button click event.  Clicking this button creates a new `SetupLoginSelectionPage` instance (passing the `databaseHelper` for consistency) and calls its `show()` method, effectively navigating to the login selection page.
* **Welcome Label:** A `Label` displaying "Hello, Reviewer!" is created and styled with a larger, bold font.
* **Scene Creation:** The `Label` and `Button` are added to the `VBox`, which is then used to create a `Scene` with dimensions 800x400 pixels.
* **Stage Setup:** Finally, the `Scene` is set to the provided `primaryStage`, and the title of the stage is set to "Reviewer Page".  This makes the welcome page visible to the user.


## 5. Algorithm Details

The `show()` method follows a straightforward algorithm:

1. Create a vertical layout (`VBox`).
2. Create a logout button and associate a handler to navigate to the login selection page.
3. Create a welcome label.
4. Add the button and label to the layout.
5. Create a scene using the layout.
6. Set the scene and title of the provided stage.


No complex algorithms are employed within this class. The functionality is primarily focused on simple UI construction and event handling.

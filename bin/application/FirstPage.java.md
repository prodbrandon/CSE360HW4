# FirstPage Class Documentation

[Linked Table of Contents](#linked-table-of-contents)

## Linked Table of Contents

* [1. Introduction](#1-introduction)
* [2. Class Overview](#2-class-overview)
* [3. Constructor Details](#3-constructor-details)
* [4. `show()` Method Details](#4-show-method-details)


## 1. Introduction

This document provides internal code documentation for the `FirstPage` class within the `application` package.  This class is responsible for displaying the initial screen to the first user of the application.  It guides the user to set up administrator access by navigating to the `AdminSetupPage`.


## 2. Class Overview

The `FirstPage` class uses JavaFX to create a simple graphical user interface (GUI). It consists of:

* A welcome message informing the user that they are the first user and prompting them to set up administrator access.
* A "Continue" button that triggers navigation to the `AdminSetupPage`.

The class interacts with a database through a `DatabaseHelper` object, passed to it during initialization.  This interaction is handled by the `AdminSetupPage` which is launched when the "Continue" button is pressed.


## 3. Constructor Details

```java
public FirstPage(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
}
```

The constructor for the `FirstPage` class takes a single argument:

| Parameter | Type             | Description                                      |
|-----------|-----------------|--------------------------------------------------|
| `databaseHelper` | `DatabaseHelper` | An instance of `DatabaseHelper` used for database interactions. |

The constructor initializes the `databaseHelper` instance variable with the provided `DatabaseHelper` object. This object is then used by the `AdminSetupPage`  to interact with the database for administrator setup.


## 4. `show()` Method Details

```java
public void show(Stage primaryStage) {
    VBox layout = new VBox(5);
    layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
    Label userLabel = new Label("Hello..You are the first person here. \nPlease select continue to setup administrator access");
    userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    Button continueButton = new Button("Continue");
    continueButton.setOnAction(a -> {
        new AdminSetupPage(databaseHelper).show(primaryStage);
    });

    layout.getChildren().addAll(userLabel, continueButton);
    Scene firstPageScene = new Scene(layout, 800, 400);
    primaryStage.setScene(firstPageScene);
    primaryStage.setTitle("First Page");
    primaryStage.show();
}
```

The `show()` method is responsible for displaying the first page GUI.  It performs the following actions:

1. **Creates a layout:** A `VBox` is created to arrange the UI elements vertically with 5 pixels spacing between them.  The style is set for centering and adding padding.

2. **Creates a welcome label:** A `Label` displays the welcome message instructing the user to set up administrator access. The style sets font size and weight.

3. **Creates a continue button:** A `Button` with the text "Continue" is created.  An `setOnAction` event handler is attached which creates a new `AdminSetupPage` instance and calls its `show()` method, passing in the provided `primaryStage`. This effectively navigates to the administrator setup screen.

4. **Adds elements to layout and creates a scene:** The label and button are added to the `VBox`. A `Scene` is created using the `VBox` layout, with dimensions of 800x400 pixels.

5. **Sets the scene and displays the stage:** The created `Scene` is set to the provided `primaryStage`, the title is set, and finally, the stage is displayed to the user.  This makes the initial GUI visible.

The algorithm is straightforward: create UI elements, arrange them, handle button click to navigate, and display the resulting scene.  No complex algorithms are involved.

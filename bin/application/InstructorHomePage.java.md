# InstructorHomePage Class Documentation

[Linked Table of Contents](#linked-table-of-contents)

## Linked Table of Contents

* [1. Overview](#1-overview)
* [2. Class Structure](#2-class-structure)
* [3. `show()` Method Details](#3-show-method-details)


## 1. Overview

The `InstructorHomePage` class is responsible for displaying a welcome message and providing a logout functionality for instructors within the application.  It leverages JavaFX for the user interface and interacts with a database via the `DatabaseHelper` class.


## 2. Class Structure

The `InstructorHomePage` class utilizes a simple structure:

| Member        | Type                 | Description                                                                 |
|----------------|----------------------|-----------------------------------------------------------------------------|
| `databaseHelper` | `DatabaseHelper`     | Instance of `DatabaseHelper` for database interactions.                   |


**Constructor:**

* `InstructorHomePage(DatabaseHelper databaseHelper)`:  Initializes the `InstructorHomePage` object with a provided `DatabaseHelper` instance. This dependency injection ensures proper database access.

## 3. `show()` Method Details

The `show()` method is the core functionality of this class, responsible for constructing and displaying the instructor's home page.

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
    Label userLabel = new Label("Hello, Instructor!");
    userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    layout.getChildren().addAll(userLabel, quitButton);
    Scene userScene = new Scene(layout, 800, 400);

    // Set the scene to primary stage
    primaryStage.setScene(userScene);
    primaryStage.setTitle("Instructor Page");
}
```

**Algorithm:**

1. **Layout Creation:** A `VBox` is created to hold the page elements.  The `-fx-alignment: center; -fx-padding: 20;` style is applied for visual centering and padding.

2. **Logout Button:** A `Button` labeled "Logout" is created. Its `setOnAction` method is configured to handle the logout action.  Upon clicking, a new instance of `SetupLoginSelectionPage` is created, passing the `databaseHelper`, and its `show()` method is called, effectively navigating to the login selection page.

3. **Welcome Label:** A `Label` displaying "Hello, Instructor!" is created and styled with a larger, bold font.

4. **Scene Construction:** Both the label and button are added to the `VBox`. A `Scene` object is then created using the `VBox` as its root and dimensions of 800x400 pixels.

5. **Scene Setting:** Finally, the `Scene` is set to the provided `primaryStage` (the main application window), and the window title is set to "Instructor Page".  This makes the instructor home page visible to the user.

The algorithm is straightforward, focusing on creating and displaying a simple yet functional user interface element.  No complex algorithms are involved.

# SetupLoginSelectionPage Class Documentation

[Linked Table of Contents](#table-of-contents)

## Table of Contents <a name="table-of-contents"></a>

* [1. Overview](#overview)
* [2. Class Structure](#class-structure)
* [3. `show(Stage primaryStage)` Method](#show-method)


## 1. Overview <a name="overview"></a>

The `SetupLoginSelectionPage` class provides a JavaFX-based user interface (UI) allowing users to choose between creating a new account ("Setup") or logging into an existing one ("Login").  It acts as a gateway to the account creation (`SetupAccountPage`) and login (`UserLoginPage`) functionalities. The class relies on a `DatabaseHelper` object for database interactions (presumably for user authentication and account management).


## 2. Class Structure <a name="class-structure"></a>

The `SetupLoginSelectionPage` class is structured as follows:

| Member          | Type                | Description                                                                        |
|-----------------|---------------------|------------------------------------------------------------------------------------|
| `databaseHelper` | `DatabaseHelper`    | Instance of the `DatabaseHelper` class, used for database operations.            |


**Constructor:**

* `SetupLoginSelectionPage(DatabaseHelper databaseHelper)`:  The constructor initializes the `databaseHelper` instance variable. This dependency injection ensures the page has access to the database.


## 3. `show(Stage primaryStage)` Method <a name="show-method"></a>

The `show(Stage primaryStage)` method is responsible for creating and displaying the UI.  The method performs the following steps:


1. **Button Creation:** Two buttons, `setupButton` ("SetUp") and `loginButton` ("Login"), are created using JavaFX's `Button` class.

2. **Action Handling:**  Event handlers are attached to each button using `setOnAction`.
    * When the `setupButton` is clicked, a new instance of `SetupAccountPage` is created, passing the `databaseHelper` object, and its `show()` method is called, displaying the account setup page.
    * Similarly, clicking `loginButton` creates a new `UserLoginPage` instance (with the `databaseHelper`), displays the login page via its `show()` method.

3. **Layout Creation:** A `VBox` layout is used to vertically arrange the buttons with a 10-pixel spacing.  The layout's style is set to add padding and center the buttons.

4. **Scene and Stage Setup:**  A new `Scene` is created using the `VBox` layout, and it's set to the `primaryStage` along with the title "Account Setup". Finally, `primaryStage.show()` displays the window.

**Algorithm:** The algorithm is straightforward. It involves creating UI elements, attaching event handlers to trigger navigation to other pages based on user selection, and setting up the scene for display. No complex algorithms are involved.  The core logic lies in the event handling and the dependency injection of the `DatabaseHelper` object, enabling database interaction from other pages.

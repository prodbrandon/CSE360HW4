# AdminHomePage Class Documentation

[Linked Table of Contents](#linked-table-of-contents)

## Linked Table of Contents

* [1. Overview](#1-overview)
* [2. Class Structure](#2-class-structure)
* [3. Constructor: `AdminHomePage(DatabaseHelper, String)`](#3-constructor-adminhomepage-databasehelper-string)
* [4. Method: `show(Stage)`](#4-method-showstage)


## 1. Overview

The `AdminHomePage` class is responsible for creating and displaying the graphical user interface (GUI) for the administrator's home page within the application.  It utilizes JavaFX for GUI elements and interacts with a database via the `DatabaseHelper` class. The page provides options for logging out and viewing a list of users.

## 2. Class Structure

The `AdminHomePage` class has the following structure:

| Member          | Type                 | Description                                                                 |
|-----------------|----------------------|-----------------------------------------------------------------------------|
| `databaseHelper` | `DatabaseHelper`     | Instance of `DatabaseHelper` for database interactions.                     |
| `adminUserName`  | `String`             | Stores the username of the currently logged-in administrator.             |


## 3. Constructor: `AdminHomePage(DatabaseHelper, String)`

The constructor initializes the `AdminHomePage` object.

| Parameter        | Type                 | Description                                                              |
|-----------------|----------------------|--------------------------------------------------------------------------|
| `databaseHelper` | `DatabaseHelper`     | An instance of the `DatabaseHelper` class used to interact with the database. |
| `userName`       | `String`             | The username of the administrator.                                      |

**Functionality:** The constructor simply assigns the provided `DatabaseHelper` and `userName` to the respective class member variables.  No complex logic is involved.

## 4. Method: `show(Stage)`

This method is responsible for creating and displaying the admin home page.

**Parameters:**

| Parameter    | Type     | Description                                           |
|-------------|----------|-------------------------------------------------------|
| `primaryStage` | `Stage` | The JavaFX primary stage on which to display the scene. |


**Functionality:**

1. **Layout Creation:** A `VBox` layout is created to hold the GUI elements.  The layout is styled for center alignment and padding.

2. **Logout Button:** A "Logout" button is added. When clicked, it creates a new instance of `SetupLoginSelectionPage` and displays it, effectively logging the administrator out.  The `setOnAction` method handles the button click event.

3. **Welcome Label:** A label displaying "Hello, Admin!" is added with styling for font size and weight.

4. **View User List Button:** A "View User List" button is added. When clicked, it creates a new instance of `ViewUserListPage`, passing the `databaseHelper`, `primaryStage`, and `adminUserName` as parameters. This allows the `ViewUserListPage` to interact with the database and display user information.  The `setOnAction` method handles the button click event.

5. **Scene Creation:** All elements (`adminLabel`, `quitButton`, `viewUserListButton`) are added to the `VBox`. A new `Scene` is created using the `VBox` layout and dimensions 800x400 pixels.

6. **Scene Display:**  The `Scene` is set to the `primaryStage`, and the stage title is set to "Admin Page". This makes the admin home page visible to the user.

**Algorithm:** The method follows a straightforward approach of creating GUI elements, adding event handlers, creating a scene, and displaying it on the stage. No sophisticated algorithms are employed.  The core logic lies in the event handlers, which instantiate other pages to handle their respective actions.

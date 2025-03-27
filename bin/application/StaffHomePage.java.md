# StaffHomePage Class Documentation

[Linked Table of Contents](#table-of-contents)

## Table of Contents <a name="table-of-contents"></a>

* [1. Overview](#overview)
* [2. Class Structure](#class-structure)
* [3. `show(Stage primaryStage)` Method](#show-method)


## 1. Overview <a name="overview"></a>

The `StaffHomePage` class is responsible for displaying a simple welcome message to staff users upon successful login.  It provides a logout button that transitions the user back to the login selection page. The class utilizes JavaFX for its graphical user interface (GUI).


## 2. Class Structure <a name="class-structure"></a>

The `StaffHomePage` class has the following structure:

| Member          | Type                | Description                                                                |
|-----------------|---------------------|----------------------------------------------------------------------------|
| `databaseHelper` | `DatabaseHelper`    | Instance of `DatabaseHelper` for database interactions.                     |
| `show(Stage)`   | `void`               | Displays the Staff Home Page GUI.                                         |


The class takes a `DatabaseHelper` object in its constructor, enabling it to interact with the application's database if needed (e.g., for retrieving user-specific information).


## 3. `show(Stage primaryStage)` Method <a name="show-method"></a>

The `show` method is responsible for creating and displaying the JavaFX GUI for the Staff Home Page.  It performs the following steps:

1. **Layout Creation:** A `VBox` layout is created to vertically arrange the GUI elements.  The `setStyle` method is used to center the content and add padding.

2. **Logout Button:** A `Button` labeled "Logout" is created.  Its `setOnAction` method is used to define the action performed when the button is clicked.  This action instantiates a new `SetupLoginSelectionPage` object (passing the `databaseHelper` object) and calls its `show` method, effectively navigating the user back to the login selection page.

3. **Welcome Label:** A `Label` displaying "Hello, Staff!" is created and styled with a larger font size and bold font weight.

4. **Layout Population:** The `Label` and `Button` are added to the `VBox` layout using `getChildren().addAll()`.

5. **Scene Creation:** A `Scene` is created using the `VBox` layout, specifying its dimensions (800x400 pixels).

6. **Scene Setting and Title:** The created `Scene` is set to the provided `primaryStage` using `primaryStage.setScene()`. The title of the stage is set to "Staff Page" using `primaryStage.setTitle()`.


The algorithm behind this method is straightforward: it uses a declarative approach to build the GUI elements, sets their properties, and then arranges them within a `VBox`.  The event handling for the logout button uses a lambda expression for concise code.  No complex algorithms or data structures are employed.

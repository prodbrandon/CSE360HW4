# StartCSE360 Application Documentation

[Linked Table of Contents](#table-of-contents)

## Table of Contents <a name="table-of-contents"></a>

* [1. Overview](#overview)
* [2. Class `StartCSE360`](#class-startcse360)
    * [2.1 `main` Method](#main-method)
    * [2.2 `start` Method](#start-method)
* [3. Database Interaction](#database-interaction)


## 1. Overview <a name="overview"></a>

This document provides internal code documentation for the `StartCSE360` application.  The application serves as an entry point, determining the initial user interface based on the state of the underlying database.  It leverages JavaFX for the UI and interacts with a database using the `DatabaseHelper` class (defined elsewhere).

## 2. Class `StartCSE360` <a name="class-startcse360"></a>

The `StartCSE360` class extends `javafx.application.Application` and serves as the main application class. It initializes a `DatabaseHelper` object and uses it to determine the initial UI to display.

### 2.1 `main` Method <a name="main-method"></a>

```java
public static void main( String[] args ) {
    launch(args);
}
```

The `main` method simply launches the JavaFX application using the `launch` method inherited from `Application`. This delegates the application's initialization and execution to the JavaFX framework.


### 2.2 `start` Method <a name="start-method"></a>

```java
@Override
public void start(Stage primaryStage) {
    try {
        databaseHelper.connectToDatabase(); // Connect to the database
        if (databaseHelper.isDatabaseEmpty()) {

            new FirstPage(databaseHelper).show(primaryStage);
        } else {
            new SetupLoginSelectionPage(databaseHelper).show(primaryStage);

        }
    } catch (SQLException e) {
        System.out.println(e.getMessage());
    }
}
```

The `start` method is the entry point for the JavaFX application lifecycle. It performs the following actions:

1. **Database Connection:** It attempts to connect to the database using `databaseHelper.connectToDatabase()`.  This method (defined in `DatabaseHelper`) handles the specifics of establishing the database connection.

2. **Database State Check:** It checks if the database is empty using `databaseHelper.isDatabaseEmpty()`. This method likely queries the database to determine if any relevant data exists.  The algorithm behind this is likely a simple count of rows in a key table.

3. **Conditional UI Loading:** Based on the database state:
    * **Empty Database:** If the database is empty, a `FirstPage` object is created and displayed using `show(primaryStage)`. This presumably presents a setup or initial configuration interface.
    * **Non-Empty Database:** If the database is not empty, a `SetupLoginSelectionPage` object is created and displayed. This suggests a login or user selection screen is presented.  This implies the application is designed to handle multiple users or accounts.

4. **Error Handling:** A `try-catch` block handles potential `SQLException` exceptions that might occur during database interaction.  The error message is printed to the console for debugging purposes.  More robust error handling (e.g., displaying a user-friendly error message) might be desirable in a production environment.


## 3. Database Interaction <a name="database-interaction"></a>

The application heavily relies on the `DatabaseHelper` class (not shown in the provided code).  This class is responsible for all database interactions, including connection establishment, data retrieval, and potentially data manipulation.  The specifics of the database interactions and the algorithms used within `DatabaseHelper` are not included in this snippet, and would require separate documentation.  Understanding the `DatabaseHelper` class is crucial for a complete understanding of the application's functionality.

The interaction is summarized in the following table:

| Method Call              | Purpose                                          | Algorithm (High-Level)                                   |
|--------------------------|------------------------------------------------------|-----------------------------------------------------------|
| `connectToDatabase()`    | Establishes a connection to the database.            | Uses JDBC or similar to connect; specifics are external. |
| `isDatabaseEmpty()`      | Checks if the database contains any relevant data.  | Likely counts rows in a key table.                        |


The success or failure of the database operations directly determines the flow of the application, controlling which UI is presented to the user.

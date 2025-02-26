# SetupAccountPage Class Documentation

## Table of Contents

* [1. Introduction](#1-introduction)
* [2. Class Overview](#2-class-overview)
* [3. Constructor `SetupAccountPage(DatabaseHelper databaseHelper)`](#3-constructor-setupaccountpagedatabasehelper-databasehelper)
* [4. Method `show(Stage primaryStage)`](#4-method-showstage-primarystage)
    * [4.1 UI Element Creation](#41-ui-element-creation)
    * [4.2 Button Action Handlers](#42-button-action-handlers)
    * [4.3 User Input Validation and Registration](#43-user-input-validation-and-registration)
* [5. Algorithm Details](#5-algorithm-details)


## 1. Introduction

This document provides internal code documentation for the `SetupAccountPage` class, which handles the account creation process for new users within the application.  The class utilizes JavaFX for the user interface and interacts with a database via the `DatabaseHelper` class.


## 2. Class Overview

The `SetupAccountPage` class is responsible for displaying a registration form and processing user inputs to create new accounts. It validates user input (username and password) using external validator classes and checks for the existence of the username and validity of an invitation code in the database. Upon successful validation and registration, it navigates the user to a welcome login page.


## 3. Constructor `SetupAccountPage(DatabaseHelper databaseHelper)`

This constructor initializes the `SetupAccountPage` object with a `DatabaseHelper` object.  The `DatabaseHelper` object is used for all database interactions, promoting modularity and reusability.

```java
private final DatabaseHelper databaseHelper; 
public SetupAccountPage(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
}
```

## 4. Method `show(Stage primaryStage)`

The `show` method is responsible for creating and displaying the account setup page within the provided `primaryStage`.

### 4.1 UI Element Creation

The method creates the following JavaFX UI elements:

*   **RadioButtons:** `admin`, `student`, `instructor`, `staff`, `reviewer` for role selection.  These are added to a `ToggleGroup` to ensure only one role can be selected.
*   **TextFields:** `userNameField`, `passwordField`, `inviteCodeField` for user input.
*   **Labels:** `errorLabel`, `roleErrLabel`, `usernameErrorLabel`, `passwordErrorLabel`, `usernameSuccessLabel`, `passwordSuccessLabel` for displaying messages (errors and success notifications).
*   **Button:** `setupButton` to trigger the account creation process.


### 4.2 Button Action Handlers

Each RadioButton has an `setOnAction` handler to update the `buttonVal` String variable which stores the selected role.

```java
admin.setOnAction(e -> { if (admin.isSelected()) { buttonVal = "admin"; } });
// Similar handlers for student, instructor, staff, and reviewer RadioButtons
```

The `setupButton`'s `setOnAction` handler orchestrates the account creation process, encompassing:

1.  **Input Retrieval:** Retrieves the username, password, and invitation code from the respective TextFields.

2.  **Username Validation:** Calls `UserNameRecognizer.checkForValidUserName(userName)` to validate the username format.  If invalid, displays an error and returns, halting further processing.

3.  **Password Validation:** Calls `PasswordEvaluator.evaluatePassword(password)` to validate the password strength. If invalid, displays an error and returns.

4.  **Role Selection Check:** Verifies that a role has been selected using the `ToggleGroup`. If not, an error is displayed and processing stops.

5.  **Database Interaction:**
    * Attempts to check if the username already exists using `databaseHelper.doesUserExist(userName)`. If the username exists, shows an error.
    * If username is unique, it checks the invitation code validity using `databaseHelper.validateInvitationCode(code)`.
    * If the invitation code is valid, a new `User` object is created, and the user is registered using `databaseHelper.register(user)`.
    * Navigates to the `WelcomeLoginPage` using `new WelcomeLoginPage(databaseHelper).show(primaryStage,user);`.

6.  **Error Handling:** Catches `SQLExceptions` that might occur during database operations and prints an error message to the console.


### 4.3 User Input Validation and Registration

This section outlines the steps involved in validating user inputs and registering the new account.  The process is sequential, and any validation failure halts the registration process. The algorithm relies on external validation methods provided by `UserNameRecognizer` and `PasswordEvaluator` classes (not shown in the provided code but implicitly used).

The core logic resides within the `setupButton`'s `setOnAction` handler.


## 5. Algorithm Details

The registration algorithm can be summarized as follows:

1.  **Gather User Input:** Obtain username, password, invitation code, and selected role from the UI.

2.  **Validate Username:** Use the `UserNameRecognizer` class to check if the username meets the defined criteria. Display an error if invalid.

3.  **Validate Password:** Use the `PasswordEvaluator` class to check if the password meets the minimum strength requirements. Display an error if invalid.

4.  **Validate Role Selection:** Check if the user has selected a role. Display an error if no role is selected.

5.  **Check for Existing User:** Query the database to check if a user with the entered username already exists. Display an error if the username exists.

6.  **Validate Invitation Code:** If the username is unique, verify the validity of the provided invitation code against the database. Display an error if the code is invalid.

7.  **Register User:** If all validations pass, create a new user object with the gathered information and register it in the database.

8.  **Navigate to Welcome Page:** After successful registration, navigate the user to the welcome login page.

9.  **Error Handling:** Handle potential database errors gracefully and provide informative error messages to the user.  The error handling includes displaying UI messages for various issues and logging exceptions to the console for debugging purposes.

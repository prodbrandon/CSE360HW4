# AdminSetupPage Class Documentation

## Table of Contents

* [1. Overview](#1-overview)
* [2. Class Structure](#2-class-structure)
* [3. `show()` Method Detailed Explanation](#3-show-method-detailed-explanation)
    * [3.1 User Input Acquisition](#31-user-input-acquisition)
    * [3.2 Username Validation](#32-username-validation)
    * [3.3 Password Validation](#33-password-validation)
    * [3.4 Database Registration and Navigation](#34-database-registration-and-navigation)


## 1. Overview

The `AdminSetupPage` class facilitates the creation of the initial administrator account.  This is a crucial step in the application's setup, ensuring secure initialization with defined admin credentials. The class interacts with the database to store the new administrator's information.

## 2. Class Structure

The `AdminSetupPage` class utilizes the following components:

| Component           | Description                                                                 |
|-----------------------|-----------------------------------------------------------------------------|
| `databaseHelper`     | An instance of `DatabaseHelper` for database interactions.                     |


## 3. `show()` Method Detailed Explanation

The `show()` method is responsible for creating and displaying the administrator setup interface and handling the setup process. It uses JavaFX components to build the graphical user interface (GUI).

### 3.1 User Input Acquisition

The method first creates JavaFX input fields (`userNameField`, `passwordField`) for the administrator's username and password, prompting the user with placeholders.


### 3.2 Username Validation

After the "Setup" button is pressed, the method retrieves the entered username and password. It then calls `UserNameRecognizer.checkForValidUserName(userName)` to validate the username.  This external method (presumably defined in the `userNameRecognizerTestbed` package) enforces username constraints. If the validation fails (a non-empty error message is returned), an error message is displayed, and the process stops.  Successful validation clears the error message and displays a success message.


### 3.3 Password Validation

Similarly, password validation is performed using `PasswordEvaluator.evaluatePassword(password)`.  This method (from the `passwordEvaluationTestbed` package) assesses the password's strength or adherence to security policies.  A non-empty return value signifies failure, resulting in an error message and termination of the process. A successful validation clears the error message and shows a success message.


### 3.4 Database Registration and Navigation

If both username and password validations pass, a `User` object is created with the provided credentials and the "admin" role. The `databaseHelper.register(user)` method attempts to register this new user in the database.  A successful registration prints a confirmation message.  Any `SQLException` during database interaction is caught, an error message is printed to the console, and the stack trace is displayed for debugging. Finally, the application navigates to the `UserLoginPage` using the same `databaseHelper` instance.  This allows the newly created administrator to log in.

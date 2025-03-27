# InvitationPage Class Documentation

## Table of Contents

* [1. Introduction](#1-introduction)
* [2. Class Overview: `InvitationPage`](#2-class-overview-invitationpage)
* [3. Method Details: `show()`](#3-method-details-show)


<a name="1-introduction"></a>
## 1. Introduction

This document provides internal code documentation for the `InvitationPage` class,  responsible for displaying a user interface to generate invitation codes.  The page interacts with a database to generate unique codes.


<a name="2-class-overview-invitationpage"></a>
## 2. Class Overview: `InvitationPage`

The `InvitationPage` class creates and displays a JavaFX scene allowing an administrator to generate invitation codes.  The generated codes are obtained from a database via the `DatabaseHelper` class.  The UI consists of a title label, a button to trigger code generation, and a label to display the resulting code.

| Element       | Description                                                                 |
|---------------|-----------------------------------------------------------------------------|
| `userLabel`   | Label displaying the page title ("Invite").                                   |
| `showCodeButton` | Button that triggers invitation code generation.                               |
| `inviteCodeLabel`| Label displaying the generated invitation code.                              |
| `layout`      | A `VBox` containing all UI elements, centered and padded.                    |


<a name="3-method-details-show"></a>
## 3. Method Details: `show()`

The `show()` method is the primary method of the `InvitationPage` class. It's responsible for creating and displaying the invitation code generation UI.

**Method Signature:**

```java
public void show(DatabaseHelper databaseHelper, Stage primaryStage) 
```

**Parameters:**

| Parameter         | Type               | Description                                                              |
|-------------------|--------------------|--------------------------------------------------------------------------|
| `databaseHelper` | `DatabaseHelper`   | An instance of the `DatabaseHelper` class used to interact with the database for code generation. |
| `primaryStage`    | `Stage`             | The JavaFX `Stage` object where the scene will be displayed.           |


**Functionality:**

1. **UI Creation:** A `VBox` layout is created and styled for centering and padding.  Labels for the title and invitation code, and a button to generate the code are added to this layout.  The label displaying the generated code is initially empty.

2. **Button Action:** An `setOnAction` event handler is attached to the `showCodeButton`. When the button is clicked:

    * The `generateInvitationCode()` method of the `databaseHelper` object is called to obtain a new invitation code from the database.  The implementation details of this method are outside the scope of this class, but it is assumed to handle database interaction and code uniqueness.
    * The generated code (a String) is then set as the text of the `inviteCodeLabel`.

3. **Scene Setup:** A new JavaFX `Scene` is created using the `VBox` layout.

4. **Stage Setup:** The created scene is set to the provided `primaryStage`, and the stage title is set to "Invite Page".  The resulting scene is then displayed to the user.

**Algorithm:** The `show()` method follows a straightforward procedural approach.  It constructs the UI, attaches an event handler to the button, and then displays the resulting scene. The core logic of invitation code generation is delegated to the `databaseHelper.generateInvitationCode()` method.  No specific algorithm is implemented within this method itself.

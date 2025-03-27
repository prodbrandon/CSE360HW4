module CSE360group {
	requires javafx.controls;
	requires java.sql;
	requires junit;
	
	opens application to javafx.graphics, javafx.fxml;
}

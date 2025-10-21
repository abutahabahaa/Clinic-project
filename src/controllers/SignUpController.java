package controllers;

import dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.MD5Util;

public class SignUpController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button createAcount;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleSignUp(ActionEvent event) {
        String firstName = firstNameField.getText();
        String lastName  = lastNameField.getText();
        String email     = emailField.getText();
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            alert(Alert.AlertType.ERROR, "All fields are required.");
            return;
        }
        if (!password.equals(confirm)) {
            alert(Alert.AlertType.ERROR, "Passwords do not match.");
            return;
        }

        try {
            if (userDAO.emailExists(email)) {
                alert(Alert.AlertType.ERROR, "Email already exists.");
                return;
            }
            String md5 = MD5Util.generateMD5(password);
            userDAO.insert(firstName, lastName, email, md5);

            alert(Alert.AlertType.INFORMATION, "You have successfully signed up.");
            goToLogin(event);
        } catch (RuntimeException e) {
            alert(Alert.AlertType.ERROR, "DB error:\n" + e.getMessage());
        }
    }

    @FXML
    private void navigateToLogin(ActionEvent event) {
        goToLogin(event);
    }

    private void goToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Navigation Error:\n" + e.getMessage());
        }
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setTitle(type == Alert.AlertType.ERROR ? "Sign Up Failed" : "Sign Up Successful");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

package controllers;

import dao.AppointmentDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;      // <<< مهم
import javafx.scene.control.Label;
import javafx.scene.Node;               // إن لم يكن موجودًا
import javafx.stage.Stage;

import java.net.URL;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label patientsCountLabel;
    @FXML private Label doctorsCountLabel;
    @FXML private Label appointmentsCountLabel;

    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO   = new DoctorDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    @FXML
    private void initialize() {
        updateCounts();
    }

    public void setWelcomeMessage(String firstName) {
        welcomeLabel.setText("مرحبا, " + firstName + "!");
    }

    private void updateCounts() {
        try {
            patientsCountLabel.setText(String.valueOf(patientDAO.count()));
            doctorsCountLabel.setText(String.valueOf(doctorDAO.count()));
            appointmentsCountLabel.setText(String.valueOf(appointmentDAO.count()));
        } catch (RuntimeException e) {
            patientsCountLabel.setText("-");
            doctorsCountLabel.setText("-");
            appointmentsCountLabel.setText("-");
            System.err.println("DB count error: " + e.getMessage());
        }
    }

    @FXML private void navigateToPatients(ActionEvent event) { swap("/views/Patients.fxml", event); }
    @FXML private void navigateToDoctors(ActionEvent event)  { swap("/views/Doctors.fxml", event); }
    @FXML private void navigateToAppointments(ActionEvent event) { swap("/views/Appointments.fxml", event); }
    @FXML private void navigateToSchedules(ActionEvent event) { swap("/views/Schedules.fxml", event); }
    @FXML private void logout(ActionEvent event) { swap("/views/Login.fxml", event); }

   private void swap(String fxmlPath, ActionEvent event) {
    try {
        URL url = getClass().getResource(fxmlPath);
        if (url == null) throw new IllegalStateException("FXML not found: " + fxmlPath);
        Parent root = FXMLLoader.load(url);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();
    } catch (Exception e) {
        StringBuilder msg = new StringBuilder(e.getClass().getSimpleName() + ": " + e.getMessage());
        Throwable c = e.getCause();
        int depth = 0;
        while (c != null && depth++ < 5) {
            msg.append("\n-> ").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
            c = c.getCause();
        }
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Navigation Error");
        a.setContentText(msg.toString());
        a.showAndWait();
        e.printStackTrace();
    }
}

}

package controllers;

import dao.PatientDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Patient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatientsController {
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TableView<Patient> patientsTable;
    @FXML private TableColumn<Patient, String> idColumn;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, String> phoneColumn;
    @FXML private TableColumn<Patient, String> emailColumn;

    // Phase 2 UI
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;

    // Data
    private final List<Patient> patientsList = new ArrayList<>(); // source
    private final ObservableList<Patient> patientList = FXCollections.observableArrayList(); // view

    private final PatientDAO patientDAO = new PatientDAO();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadPatients();

        if (sortComboBox != null) {
            sortComboBox.setItems(FXCollections.observableArrayList("الاسم"));
            sortComboBox.setOnAction(e -> applyPatientFilters());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyPatientFilters());
        }

        // مزامنة الحقول مع الصف المحدد
        patientsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            if (sel != null) {
                nameField.setText(sel.getName());
                phoneField.setText(sel.getPhone());
                emailField.setText(sel.getEmail());
            }
        });
    }

    private void loadPatients() {
        try {
            patientsList.clear();
            patientsList.addAll(patientDAO.findAll());
            patientList.setAll(patientsList);
            patientsTable.setItems(patientList);
        } catch (RuntimeException e) {
            alert(Alert.AlertType.ERROR, "Database error while loading patients:\n" + e.getMessage());
        }
    }

    // فلترة + فرز + بحث حي
    private void applyPatientFilters() {
        String kw = (searchField == null ? "" : searchField.getText()).trim().toLowerCase();

        List<Patient> filtered = patientsList.stream()
                .filter(p -> kw.isEmpty() || (p.getName() != null && p.getName().toLowerCase().contains(kw)))
                .collect(Collectors.toList());

        String sort = (sortComboBox == null) ? null : sortComboBox.getValue();
        if ("الاسم".equals(sort)) {
            java.text.Collator ar = java.text.Collator.getInstance(new java.util.Locale("ar"));
            ar.setStrength(java.text.Collator.PRIMARY);
            filtered.sort((a, b) -> ar.compare(
                    a.getName() == null ? "" : a.getName(),
                    b.getName() == null ? "" : b.getName()
            ));
        }

        patientList.setAll(filtered);
    }
@FXML
private void handleSearchPatient(javafx.event.ActionEvent e) {
    applyPatientFilters();
}

@FXML
private void handleSortPatients(javafx.event.ActionEvent e) {
    applyPatientFilters();
}

    @FXML
    private void handleAddPatient() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            alert(Alert.AlertType.ERROR, "All fields are required."); return;
        }
        boolean exists = patientsList.stream()
                .anyMatch(p -> name.equals(p.getName()) && phone.equals(p.getPhone()));
        if (exists) { alert(Alert.AlertType.ERROR, "Patient already exists."); return; }

        try {
            Patient p = new Patient("0", name, phone, email);
            int newId = patientDAO.insert(p);
            p.setId(String.valueOf(newId));
            patientsList.add(p);
            applyPatientFilters();
            alert(Alert.AlertType.INFORMATION, "Patient added successfully.");
            clearFields(null);
        } catch (RuntimeException e) {
            alert(Alert.AlertType.ERROR, "DB insert failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdatePatient(ActionEvent event) {
        Patient sel = patientsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.ERROR, "اختر مريضًا من الجدول."); return; }

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            alert(Alert.AlertType.ERROR, "كل الحقول مطلوبة."); return;
        }
        if (name.equals(sel.getName()) && phone.equals(sel.getPhone()) && email.equals(sel.getEmail())) {
            alert(Alert.AlertType.INFORMATION, "مافي تغييرات."); return;
        }

        boolean duplicate = patientsList.stream()
                .filter(p -> p != sel)
                .anyMatch(p -> p.getName().equals(name) && p.getPhone().equals(phone));
        if (duplicate) { alert(Alert.AlertType.ERROR, "مريض بنفس الاسم/الهاتف موجود."); return; }

        sel.setName(name);
        sel.setPhone(phone);
        sel.setEmail(email);

        try {
            patientDAO.update(sel);
            patientsTable.refresh();
            applyPatientFilters();
            alert(Alert.AlertType.INFORMATION, "تم حفظ التعديل.");
        } catch (RuntimeException e) {
            alert(Alert.AlertType.ERROR, "DB update failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeletePatient(ActionEvent event) {
        Patient sel = patientsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.ERROR, "Select a patient to delete."); return; }

        try {
            patientDAO.delete(Integer.parseInt(sel.getId()));
            patientsList.remove(sel);
            applyPatientFilters();
            alert(Alert.AlertType.INFORMATION, "Patient deleted.");
            clearFields(null);
        } catch (RuntimeException e) {
            alert(Alert.AlertType.ERROR, "DB delete failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    private void clearFields(ActionEvent event) {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        patientsTable.getSelectionModel().clearSelection();

        if (searchField != null) searchField.clear();
        if (sortComboBox != null) sortComboBox.setValue(null);

        patientList.setAll(patientsList);
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

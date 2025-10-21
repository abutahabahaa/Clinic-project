package controllers;

import dao.DoctorDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import models.Doctor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DoctorsController {

    @FXML private TextField nameField;
    @FXML private TextField specialtyField;
    @FXML private TableView<Doctor> doctorsTable;
    @FXML private TableColumn<Doctor, String> idColumn;
    @FXML private TableColumn<Doctor, String> nameColumn;
    @FXML private TableColumn<Doctor, String> specialtyColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;

    private final List<Doctor> doctorsList = new ArrayList<>();
    private final ObservableList<Doctor> doctorList = FXCollections.observableArrayList();

    private static final java.text.Collator AR = java.text.Collator.getInstance(new java.util.Locale("ar"));
    static { AR.setStrength(java.text.Collator.PRIMARY); }

    private final DoctorDAO doctorDAO = new DoctorDAO();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        specialtyColumn.setCellValueFactory(new PropertyValueFactory<>("specialty"));

        loadDoctors();

        if (sortComboBox != null) {
            sortComboBox.setItems(FXCollections.observableArrayList("الاسم", "التخصص"));
            sortComboBox.setOnAction(e -> applyDoctorFilters());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyDoctorFilters());
        }

        doctorsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            if (sel != null) {
                nameField.setText(sel.getName());
                specialtyField.setText(sel.getSpecialty());
            }
        });
    }

    private void loadDoctors() {
        try {
            doctorsList.clear();
            doctorsList.addAll(doctorDAO.findAll());
            doctorList.setAll(doctorsList);
            doctorsTable.setItems(doctorList);
        } catch (RuntimeException e) {
            alert("Database error while loading doctors:\n" + e.getMessage());
        }
    }

    private void applyDoctorFilters() {
        String kw = (searchField == null ? "" : searchField.getText()).trim().toLowerCase();

        List<Doctor> filtered = doctorsList.stream()
                .filter(d -> {
                    if (kw.isEmpty()) return true;
                    String n = d.getName() == null ? "" : d.getName().toLowerCase();
                    String s = d.getSpecialty() == null ? "" : d.getSpecialty().toLowerCase();
                    return n.contains(kw) || s.contains(kw);
                })
                .collect(Collectors.toList());

        String sort = (sortComboBox == null) ? null : sortComboBox.getValue();
        if ("الاسم".equals(sort)) {
            filtered.sort((a, b) -> AR.compare(
                    a.getName() == null ? "" : a.getName(),
                    b.getName() == null ? "" : b.getName()
            ));
        } else if ("التخصص".equals(sort)) {
            filtered.sort((a, b) -> AR.compare(
                    a.getSpecialty() == null ? "" : a.getSpecialty(),
                    b.getSpecialty() == null ? "" : b.getSpecialty()
            ));
        }

        doctorList.setAll(filtered);
    }
@FXML
private void handleSearchDoctor(javafx.event.ActionEvent e) {
    applyDoctorFilters();
}

@FXML
private void handleSortDoctors(javafx.event.ActionEvent e) {
    applyDoctorFilters();
}

    @FXML
    private void handleAddDoctor() {
        String name = nameField.getText().trim();
        String specialty = specialtyField.getText().trim();

        if (name.isEmpty() || specialty.isEmpty()) { alert("All fields are required."); return; }
        boolean exists = doctorsList.stream().anyMatch(d -> d.getName().equals(name) && d.getSpecialty().equals(specialty));
        if (exists) { alert("Doctor already exists."); return; }

        try {
            Doctor d = new Doctor("0", name, specialty);
            int newId = doctorDAO.insert(d);
            d.setId(String.valueOf(newId));
            doctorsList.add(d);
            applyDoctorFilters();
            info("Doctor added.");
            clearFields();
        } catch (RuntimeException e) {
            alert("DB insert failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateDoctor() {
        Doctor sel = doctorsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Select a doctor to update."); return; }

        String name = nameField.getText().trim();
        String specialty = specialtyField.getText().trim();
        if (name.isEmpty() || specialty.isEmpty()) { alert("All fields are required."); return; }
        if (name.equals(sel.getName()) && specialty.equals(sel.getSpecialty())) { info("No changes detected."); return; }

        boolean duplicate = doctorsList.stream()
                .filter(d -> d != sel)
                .anyMatch(d -> d.getName().equals(name) && d.getSpecialty().equals(specialty));
        if (duplicate) { alert("Another doctor with same name & specialty exists."); return; }

        sel.setName(name);
        sel.setSpecialty(specialty);

        try {
            doctorDAO.update(sel);
            applyDoctorFilters();
            info("Doctor updated.");
        } catch (RuntimeException e) {
            alert("DB update failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteDoctor() {
        Doctor sel = doctorsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Select a doctor to delete."); return; }

        try {
            doctorDAO.delete(Integer.parseInt(sel.getId()));
            doctorsList.remove(sel);
            applyDoctorFilters();
            info("Doctor deleted.");
            clearFields();
        } catch (RuntimeException e) {
            alert("DB delete failed:\n" + e.getMessage());
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
    private void clearFields() {
        nameField.clear();
        specialtyField.clear();
        doctorsTable.getSelectionModel().clearSelection();

        if (searchField != null) searchField.clear();
        if (sortComboBox != null) sortComboBox.setValue(null);

        doctorList.setAll(doctorsList);
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

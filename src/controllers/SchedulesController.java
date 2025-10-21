package controllers;

import dao.AppointmentDAO;
import dao.DoctorDAO;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.Appointment;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// للتنقّل
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SchedulesController {

    @FXML private ComboBox<String> doctorCombo;
    @FXML private DatePicker fromDate, toDate;
    @FXML private Button generateBtn, cancelBtn;
    @FXML private ProgressIndicator progress;
    @FXML private Label statusLabel;
    @FXML private TextArea output;

    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private Map<String,Integer> doctorNameToId = new HashMap<>();
    private Task<?> currentTask;

    @FXML
    private void initialize() {
        try {
            doctorNameToId = doctorDAO.nameToIdMap();
            doctorCombo.setItems(
                    FXCollections.observableArrayList(new TreeSet<>(doctorNameToId.keySet())));
        } catch (RuntimeException e) {
            alert(Alert.AlertType.ERROR, "DB error loading doctors:\n" + e.getMessage());
        }

        // حالة مبدئية لعناصر الواجهة
        progress.setVisible(false);
        progress.managedProperty().bind(progress.visibleProperty());
        statusLabel.setText("");
        cancelBtn.setDisable(true);

        if (output != null) {
            output.setEditable(false);
            output.setPromptText("اختر طبيبًا وحدّد المدى الزمني ثم اضغط توليد.");
        }
    }

    @FXML
    private void handleGenerate() {
        String docName = doctorCombo.getValue();
        LocalDate f = fromDate.getValue();
        LocalDate t = toDate.getValue();

        // تحقق المدخلات حسب متطلبات Phase 5
        if (docName == null) { alert(Alert.AlertType.ERROR, "اختر الطبيب."); return; }
        if (f == null || t == null) { alert(Alert.AlertType.ERROR, "حدد المدى الزمني (من/إلى)."); return; }
        if (f.isAfter(t)) { alert(Alert.AlertType.ERROR, "المدى غير صحيح (من > إلى)."); return; }
        if (f.plusDays(31).isBefore(t)) { alert(Alert.AlertType.ERROR, "المدى الأقصى 31 يومًا."); return; }

        int did = doctorNameToId.get(docName);

        Task<List<Appointment>> task = new Task<>() {
            @Override protected List<Appointment> call() {
                updateMessage("Loading...");
                List<Appointment> list = appointmentDAO.findByRange(did, f, t);
                int n = list.size();
             for (int i = 0; i < n; i++) {
                    if (isCancelled()) break;
                    if (i % 50 == 0) {
                        updateProgress(i + 1, n);
                        updateMessage("Processed " + (i + 1) + "/" + n);
                    }
                }
                return list;
            }
        };

        bind(task);

        task.setOnSucceeded(e -> {
            unbind();
            List<Appointment> list = task.getValue();
            if (list == null || list.isEmpty()) {
                output.setText("No appointments found for the selected filters.");
            } else {
                // مهم: العرض عبر خصائص النص المنسق من الـ Entity
                String txt = list.stream()
                        .map(a -> a.getDateString() + " " + a.getTimeString() + " — " + a.getPatientName())
                        .collect(Collectors.joining("\n"));
                output.setText(txt);
            }
        });

        task.setOnFailed(e -> {
            unbind();
            alert(Alert.AlertType.ERROR,
                    "Failed to generate schedule: " + task.getException().getMessage());
        });

        new Thread(task, "schedule-generate").start();
    }

    @FXML
    private void handleCancel() {
        if (currentTask != null) {
            currentTask.cancel();
            unbind();
            alert(Alert.AlertType.INFORMATION, "Schedule generation canceled.");
        }
    }

    private void bind(Task<?> t) {
        unbind(); // تنظيف أي ربط سابق
        currentTask = t;

        progress.visibleProperty().bind(t.runningProperty());
        cancelBtn.disableProperty().bind(t.runningProperty().not());
        statusLabel.textProperty().bind(t.messageProperty());

        // تعطيل المدخلات أثناء التشغيل
        generateBtn.disableProperty().bind(t.runningProperty());
        doctorCombo.disableProperty().bind(t.runningProperty());
        fromDate.disableProperty().bind(t.runningProperty());
        toDate.disableProperty().bind(t.runningProperty());
    }

    private void unbind() {
        if (currentTask == null) {
            // إعادة الحالة الافتراضية
            progress.setVisible(false);
            cancelBtn.setDisable(true);
            statusLabel.setText("");
            return;
        }

        // فك الربط
        if (progress.visibleProperty().isBound()) progress.visibleProperty().unbind();
        if (cancelBtn.disableProperty().isBound()) cancelBtn.disableProperty().unbind();
        if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind();
        if (generateBtn.disableProperty().isBound()) generateBtn.disableProperty().unbind();
        if (doctorCombo.disableProperty().isBound()) doctorCombo.disableProperty().unbind();
        if (fromDate.disableProperty().isBound()) fromDate.disableProperty().unbind();
        if (toDate.disableProperty().isBound()) toDate.disableProperty().unbind();

        // إعادة القيم
        progress.setVisible(false);
        cancelBtn.setDisable(true);
        statusLabel.setText("");
        currentTask = null;
    }

    private void alert(Alert.AlertType t, String m) {
        Alert a = new Alert(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }

    // ====== الرجوع للوحة التحكم ======
    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Navigation Error:\n" + e.getMessage());
        }
    }
}

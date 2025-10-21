package controllers;

import dao.AppointmentDAO;
import dao.DoctorDAO;
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
import models.Appointment;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AppointmentsController {

    // ========= UI (الحجز) =========
    @FXML private ComboBox<String> patientComboBox;
    @FXML private ComboBox<String> doctorComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;

    // ========= الجدول =========
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> idColumn;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> dateColumn; // سنعرض dateString
    @FXML private TableColumn<Appointment, String> timeColumn; // سنعرض timeString

    // ========= البحث/الفرز =========
    @FXML private TextField  searchField;        // اسم مريض/طبيب
    @FXML private DatePicker searchDatePicker;   // فلترة بالتاريخ
    @FXML private ComboBox<String> sortComboBox; // "التاريخ" أو "الوقت"

    // ========= عناصر التقدم/الإلغاء/الحالة + التذكيرات =========
    @FXML private ProgressIndicator progress;    // fx:id="progress"
    @FXML private Label statusLabel;             // fx:id="statusLabel"
    @FXML private Button cancelBtn;              // fx:id="cancelBtn"
    @FXML private TextArea remindersArea;        // fx:id="remindersArea"

    // ========= Data =========
    private final ObservableList<Appointment> appointmentListView = FXCollections.observableArrayList();
    private final List<Appointment> appointmentsAll = new ArrayList<>();

    // Maps (اسم → ID) من DB
    private Map<String,Integer> patientNameToId = new HashMap<>();
    private Map<String,Integer> doctorNameToId  = new HashMap<>();

    // DAO
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    // لمهام الخلفية
    private javafx.concurrent.Task<?> currentTask;

    // ========= تهيئة =========
    @FXML
    private void initialize() {
        // أعمدة الجدول
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        doctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        // IMPORTANT: نعرض النص المنسق وليس النوع الفعلي
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateString"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeString"));

        // تعبئة الكومبو من DB
        try {
            patientNameToId = patientDAO.nameToIdMap();
            doctorNameToId  = doctorDAO.nameToIdMap();
            patientComboBox.getItems().setAll(new TreeSet<>(patientNameToId.keySet()));
            doctorComboBox.getItems().setAll(new TreeSet<>(doctorNameToId.keySet()));
        } catch (RuntimeException e) {
            error("DB error loading names:\n" + e.getMessage());
        }

        // تحميل المواعيد من DB — الآن عبر Task (خلفية)
        refreshInBackground(false);

        // البحث/الفرز
        if (sortComboBox != null) {
            sortComboBox.setItems(FXCollections.observableArrayList("التاريخ", "الوقت"));
            sortComboBox.setOnAction(e -> applyAppointmentFilters());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyAppointmentFilters());
        }
        if (searchDatePicker != null) {
            searchDatePicker.valueProperty().addListener((obs, o, n) -> applyAppointmentFilters());
        }

        // --- فك أي ربط ثم ضبط القيم الابتدائية لعناصر الحالة ---
        if (progress != null) {
            if (progress.visibleProperty().isBound()) progress.visibleProperty().unbind();
            progress.setVisible(false);
            progress.managedProperty().bind(progress.visibleProperty());
        }
        if (statusLabel != null) {
            if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind();
            statusLabel.setText("");
        }
        if (cancelBtn != null) {
            if (cancelBtn.disableProperty().isBound()) cancelBtn.disableProperty().unbind();
            cancelBtn.setDisable(true);
        }
        if (remindersArea != null) {
            remindersArea.setEditable(false);
            remindersArea.setPromptText("لن تظهر أي تنبيهات حتى تضغط \"توليد التذكيرات (48h)\".");
        }
    }

    // ========= تحميل/فلترة =========
    private void refreshInBackground(boolean showInfoOnDone) {
        javafx.concurrent.Task<List<Appointment>> task = new javafx.concurrent.Task<>() {
            @Override protected List<Appointment> call() {
                updateMessage("Loading appointments...");
                List<Appointment> data = appointmentDAO.findAllWithNames();
                int n = data.size();
                for (int i = 0; i < n; i++) {
                    if (isCancelled()) break;
                    if (i % 100 == 0) {
                        updateProgress(i + 1, n);
                        updateMessage("Processed " + (i + 1) + "/" + n);
                    }
                }
                return data;
            }
        };

        bind(task);

        task.setOnSucceeded(e -> {
            unbind();
            appointmentsAll.clear();
            appointmentsAll.addAll(task.getValue());
            appointmentListView.setAll(appointmentsAll);
            appointmentsTable.setItems(appointmentListView);
            applyAppointmentFilters();
            if (showInfoOnDone) info("تم تحديث قائمة المواعيد.");
        });

        task.setOnFailed(e -> {
            unbind();
            error("Database error while loading appointments:\n" + task.getException().getMessage());
        });

        new Thread(task, "appt-refresh").start();
    }

    private void applyAppointmentFilters() {
        String kw = (searchField == null ? "" : searchField.getText()).trim().toLowerCase();
        LocalDate dt = (searchDatePicker == null) ? null : searchDatePicker.getValue();

        List<Appointment> filtered = appointmentsAll.stream()
                .filter(a -> kw.isEmpty()
                        || (a.getPatientName() != null && a.getPatientName().toLowerCase().contains(kw))
                        || (a.getDoctorName()  != null && a.getDoctorName().toLowerCase().contains(kw)))
                .filter(a -> dt == null || dt.equals(a.getDate()))
                .collect(Collectors.toList());

        String sort = (sortComboBox == null) ? null : sortComboBox.getValue();
        if ("التاريخ".equals(sort)) {
            filtered.sort(Comparator.comparing(Appointment::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("الوقت".equals(sort)) {
            filtered.sort(Comparator.comparing(Appointment::getTime, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        appointmentListView.setAll(filtered);
    }

    private boolean isValidHHmm(String s) {
        return s != null && Pattern.matches("([01]\\d|2[0-3]):[0-5]\\d", s);
    }

    private boolean isWithinClinicHours(LocalTime t) {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end   = LocalTime.of(17, 0);
        return !t.isBefore(start) && !t.isAfter(end);
    }

    private boolean isThirtyMinuteSlot(LocalTime t) {
        int m = t.getMinute();
        return m == 0 || m == 30;
    }

    // ========= أزرار CRUD =========
    @FXML
    private void handleSchedule() {
        String patientName = patientComboBox.getValue();
        String doctorName  = doctorComboBox.getValue();
        LocalDate date     = datePicker.getValue();
        String timeTxt     = (timeField.getText() == null) ? "" : timeField.getText().trim();

        if (patientName == null || doctorName == null || date == null || timeTxt.isEmpty()) {
            error("كل الحقول مطلوبة."); return;
        }
        if (!isValidHHmm(timeTxt)) { error("صيغة الوقت غير صحيحة. HH:MM"); return; }

        LocalTime time = LocalTime.parse(timeTxt);
        if (!isWithinClinicHours(time)) { error("الوقت خارج ساعات العيادة (09:00–17:00)."); return; }
        if (!isThirtyMinuteSlot(time)) { error("الموعد يجب أن يكون بفاصل 30 دقيقة (مثال 09:00, 09:30, 10:00)."); return; }
        if (date.isBefore(LocalDate.now())) { error("التاريخ يجب أن يكون مستقبلي."); return; }

        try {
            Integer pid = patientNameToId.get(patientName);
            Integer did = doctorNameToId.get(doctorName);
            if (pid == null || did == null) { error("تعذّر إيجاد المريض/الطبيب المختار."); return; }

            // 1) كشف التعارض قبل الحفظ
            if (appointmentDAO.existsConflict(did, date, time)) {
                error("Time conflict with Dr. " + doctorName + " at " + time);
                return;
            }

            // 2) الحفظ
            int newId = appointmentDAO.insert(pid, did, date, time);

            // 3) إضافة للسورس وتطبيق الفلاتر
            Appointment a = new Appointment(String.valueOf(newId), patientName, doctorName, date.toString(), time.toString());
            a.setPatient(null); // أسماء العرض تأتي من السطور أعلاه؛ سنجدد بالـ refresh لضمان العلاقات
            a.setDoctor(null);
            appointmentsAll.add(a);
            applyAppointmentFilters();

            info("The appointment has been booked.");
            clearFields();
        } catch (RuntimeException e) {
            error("DB insert failed:\n" + e.getMessage());
        }
    }

    // ========= أزرار الشريط =========
    @FXML
    private void handleRefresh() {
        unbind();
        refreshInBackground(true);
    }

    @FXML
    private void handleGenerateReminders() {
        // نفذها عبر DAO باستعلام زمني ضمن Task
        javafx.concurrent.Task<List<String>> task = new javafx.concurrent.Task<>() {
            @Override protected List<String> call() {
                updateMessage("Generating reminders...");
                List<Appointment> appts = appointmentDAO.findUpcomingWithinHours(48);
                int n = appts.size();
                List<String> lines = new ArrayList<>(n);
       for (int i = 0; i < n; i++) {
                    if (isCancelled()) break;
                    Appointment a = appts.get(i);
                    lines.add(a.getDateString() + " " + a.getTimeString()
                            + " — " + a.getPatientName() + " مع " + a.getDoctorName());
                    if (i % 20 == 0) {
                        updateProgress(i + 1, n);
                        updateMessage("Processed " + (i + 1) + "/" + n);
                    }
                }
                Collections.sort(lines);
                return lines;
            }
        };

        bind(task);

        task.setOnSucceeded(e -> {
            unbind();
            List<String> lines = task.getValue();
            if (remindersArea != null) {
                remindersArea.setText(
                        lines.isEmpty() ? "No upcoming appointments."
                                        : String.join("\n", lines)
                );
            }
            info("تم توليد التذكيرات.");
        });

        task.setOnFailed(e -> {
            unbind();
            error("فشل توليد التذكيرات: " + task.getException().getMessage());
        });

        new Thread(task, "appt-reminders").start();
    }

    @FXML
    private void handleCancelTask() {
        if (currentTask != null) {
            currentTask.cancel();
            unbind();
            info("تم إلغاء العملية.");
        }
    }

    // ========= الربط/فك الربط لعناصر الحالة =========
    private void bind(javafx.concurrent.Task<?> t) {
        unbind(); // فك أي ارتباطات سابقة أولًا
        currentTask = t;

        if (progress != null)  progress.visibleProperty().bind(t.runningProperty());
        if (cancelBtn != null) cancelBtn.disableProperty().bind(t.runningProperty().not());
        if (statusLabel != null) statusLabel.textProperty().bind(t.messageProperty());

        // تعطيل عناصر البحث/الأزرار أثناء التشغيل
        if (searchField != null)       searchField.disableProperty().bind(t.runningProperty());
        if (searchDatePicker != null)  searchDatePicker.disableProperty().bind(t.runningProperty());
        if (sortComboBox != null)      sortComboBox.disableProperty().bind(t.runningProperty());
    }

    private void unbind() {
        if (progress != null) {
            if (progress.visibleProperty().isBound()) progress.visibleProperty().unbind();
            progress.setVisible(false);
        }
        if (cancelBtn != null) {
            if (cancelBtn.disableProperty().isBound()) cancelBtn.disableProperty().unbind();
            cancelBtn.setDisable(true);
        }
        if (statusLabel != null) {
            if (statusLabel.textProperty().isBound()) statusLabel.textProperty().unbind();
            statusLabel.setText("");
        }
        if (searchField != null && searchField.disableProperty().isBound()) searchField.disableProperty().unbind();
        if (searchDatePicker != null && searchDatePicker.disableProperty().isBound()) searchDatePicker.disableProperty().unbind();
        if (sortComboBox != null && sortComboBox.disableProperty().isBound()) sortComboBox.disableProperty().unbind();

        currentTask = null;
    }

    // ========= تنقّل =========
    @FXML
    private void handleBackToDashboard(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();
    }

    // ========= أدوات مساعدة =========
    @FXML
    private void clearFields() {
        patientComboBox.setValue(null);
        doctorComboBox.setValue(null);
        datePicker.setValue(null);
        timeField.clear();

        if (searchField != null) searchField.clear();
        if (searchDatePicker != null) searchDatePicker.setValue(null);
        if (sortComboBox != null) sortComboBox.setValue(null);

        appointmentListView.setAll(appointmentsAll);
    }

    private void error(String msg) {
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

    // handlers موجودة في الـFXML (توجيه للفلترة)
    @FXML private void handleSearchAppointments(ActionEvent e) { applyAppointmentFilters(); }
    @FXML private void handleSortAppointments(ActionEvent e)   { applyAppointmentFilters(); }
}

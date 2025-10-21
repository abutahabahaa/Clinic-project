package models;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    // تُخزَّن كأنواع زمنية فعلية
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    public Appointment() { }

    // مُنشئ للتوافق مع TableView القديمة (لن نستخدمه للحفظ)
    public Appointment(String id, String patientName, String doctorName, String date, String time) {
        this.id = (id == null || id.isEmpty()) ? 0 : Integer.parseInt(id);
        this.date = (date == null || date.isEmpty()) ? null : LocalDate.parse(date);
        this.time = (time == null || time.isEmpty()) ? null : LocalTime.parse(time);
    }

    // ========== ID ==========
    public String getId() { return id == 0 ? null : String.valueOf(id); }
    public void setId(String id) { this.id = (id == null || id.isEmpty()) ? 0 : Integer.parseInt(id); }
    public int getIdAsInt() { return id; }

    // ========== Relations ==========
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    // ========== Date/Time (أنواع فعلية) ==========
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate d) { this.date = d; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime t) { this.time = t; }

    // ========== Helpers for TableView display ==========
    @Transient public String getPatientName() { return patient == null ? null : patient.getName(); }
    @Transient public String getDoctorName()  { return doctor  == null ? null : doctor.getName(); }
    @Transient public String getDateString()  { return date == null ? null : date.toString(); }
    @Transient public String getTimeString()  { return time == null ? null : time.toString(); }
}

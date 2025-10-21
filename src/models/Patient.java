package models;

import javax.persistence.*;

@Entity
@Table(name = "Patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String phone;
    @Column(nullable = false) private String email;

    public Patient() { }

    // مُنشئ للتوافق مع الكود الموجود
    public Patient(String id, String name, String phone, String email) {
        this.id = (id == null || id.isEmpty()) ? 0 : Integer.parseInt(id);
        this.name = name; this.phone = phone; this.email = email;
    }

    // TableView يتوقع String للمعرف
    public String getId() { return id == 0 ? null : String.valueOf(id); }
    public void setId(String id) { this.id = (id == null || id.isEmpty()) ? 0 : Integer.parseInt(id); }
    public int getIdAsInt() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

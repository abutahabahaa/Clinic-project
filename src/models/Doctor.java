package models;

import javax.persistence.*;

@Entity
@Table(name = "Doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String specialty;

    public Doctor() { }

    public Doctor(String id, String name, String specialty) {
        this.id = (id == null || id.isEmpty()) ? 0 : Integer.parseInt(id);
        this.name = name; this.specialty = specialty;
    }

    public String getId() { return id == 0 ? null : String.valueOf(id); }
    public void setId(String id) { this.id = (id == null || id.isEmpty()) ? 0 : Integer.parseInt(id); }
    public int getIdAsInt() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
}

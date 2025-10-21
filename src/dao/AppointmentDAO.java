package dao;

import models.Appointment;
import models.Doctor;
import models.Patient;
import utils.JPAUtil;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AppointmentDAO {

    /** جميع المواعيد مع أسماء المريض والطبيب */
    public List<Appointment> findAllWithNames() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT a FROM Appointment a " +
                "JOIN FETCH a.patient " +
                "JOIN FETCH a.doctor " +
                "ORDER BY a.date, a.time, a.id", Appointment.class
            ).getResultList();
        } finally {
            em.close();
        }
    }

    /** فحص التعارض: نفس الطبيب/التاريخ/الوقت (أنواع زمنية فعلية) */
    public boolean existsConflict(int doctorId, LocalDate date, LocalTime time) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long c = em.createQuery(
                "SELECT COUNT(a) FROM Appointment a " +
                "WHERE a.doctor.id = :did AND a.date = :d AND a.time = :t", Long.class)
                .setParameter("did", doctorId)
                .setParameter("d", date)
                .setParameter("t", time)
                .getSingleResult();
            return c != null && c > 0;
        } finally {
            em.close();
        }
    }

    /** مواعيد طبيب بمدى زمني (أنواع زمنية فعلية) */
    public List<Appointment> findByRange(int doctorId, LocalDate from, LocalDate to) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT a FROM Appointment a " +
                "JOIN FETCH a.patient " +
                "JOIN FETCH a.doctor " +
                "WHERE a.doctor.id = :did " +
                "AND a.date BETWEEN :f AND :t " +
                "ORDER BY a.date, a.time, a.id", Appointment.class)
                .setParameter("did", doctorId)
                .setParameter("f", from)
                .setParameter("t", to)
                .getResultList();
        } finally {
            em.close();
        }
    }

    /** المواعيد القادمة خلال N ساعة (أنواع زمنية فعلية) */
    public List<Appointment> findUpcomingWithinHours(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(hours);

        LocalDate fromDate = now.toLocalDate();
        LocalDate toDate   = end.toLocalDate();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Appointment> base = em.createQuery(
                "SELECT a FROM Appointment a " +
                "JOIN FETCH a.patient " +
                "JOIN FETCH a.doctor " +
                "WHERE a.date BETWEEN :f AND :t " +
                "ORDER BY a.date, a.time, a.id", Appointment.class)
                .setParameter("f", fromDate)
                .setParameter("t", toDate)
                .getResultList();

            return base.stream().filter(a -> {
                LocalDate d = a.getDate();
                LocalTime t = a.getTime();
                if (d == null || t == null) return false;
                LocalDateTime ts = LocalDateTime.of(d, t);
                return !ts.isBefore(now) && !ts.isAfter(end);
            }).collect(Collectors.toList());
        } finally {
            em.close();
        }
    }

    /** إضافة موعد وإرجاع الـID الجديد (أنواع زمنية فعلية) */
    public int insert(int patientId, int doctorId, LocalDate date, LocalTime time) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Patient p = em.find(Patient.class, patientId);
            Doctor  d = em.find(Doctor.class, doctorId);

            Appointment a = new Appointment();
            a.setPatient(p);
            a.setDoctor(d);
            a.setDate(date);
            a.setTime(time);

            em.persist(a);
            em.getTransaction().commit();
            return a.getIdAsInt();
        } finally {
            em.close();
        }
    }

    /** تحديث موعد */
    public void update(int id, int patientId, int doctorId, LocalDate date, LocalTime time) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Appointment a = em.find(Appointment.class, id);
            if (a != null) {
                a.setPatient(em.find(Patient.class, patientId));
                a.setDoctor(em.find(Doctor.class, doctorId));
                a.setDate(date);
                a.setTime(time);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /** حذف موعد */
    public void delete(int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Appointment a = em.find(Appointment.class, id);
            if (a != null) em.remove(a);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /** عدد المواعيد */
    public int count() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long c = em.createQuery("SELECT COUNT(a) FROM Appointment a", Long.class).getSingleResult();
            return c.intValue();
        } finally {
            em.close();
        }
    }
}

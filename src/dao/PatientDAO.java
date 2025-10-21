package dao;

import models.Patient;
import utils.JPAUtil;

import javax.persistence.EntityManager;
import java.util.*;

public class PatientDAO {

    public List<Patient> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Patient p ORDER BY p.id", Patient.class)
                     .getResultList();
        } finally { em.close(); }
    }

    public int insert(Patient p) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(p);
            em.getTransaction().commit();
            return p.getIdAsInt();
        } finally { em.close(); }
    }

    public void update(Patient p) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(p);
            em.getTransaction().commit();
        } finally { em.close(); }
    }

    /** يمنع الحذف إذا كان للمريض مواعيد مرتبطة. */
    public void delete(int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Long apptCount = em.createQuery(
                    "SELECT COUNT(a) FROM models.Appointment a WHERE a.patient.id = :pid",
                    Long.class)
                    .setParameter("pid", id)
                    .getSingleResult();

            if (apptCount != null && apptCount > 0) {
                em.getTransaction().rollback();
                throw new IllegalStateException(
                        "لا يمكن حذف المريض لوجود " + apptCount + " موعد/مواعيد مرتبطة به. "
                      + "احذف المواعيد أولاً أو غيّر ارتباطها.");
            }

            Patient ref = em.find(Patient.class, id);
            if (ref != null) em.remove(ref);

            em.getTransaction().commit();
        } finally { em.close(); }
    }

    public int count() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long c = em.createQuery("SELECT COUNT(p) FROM Patient p", Long.class).getSingleResult();
            return c.intValue();
        } finally { em.close(); }
    }

    public Map<String,Integer> nameToIdMap() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createQuery(
                "SELECT p.name, p.id FROM Patient p", Object[].class).getResultList();
            Map<String,Integer> map = new HashMap<>();
            for (Object[] r : rows) map.put((String) r[0], (Integer) r[1]);
            return map;
        } finally { em.close(); }
    }
}

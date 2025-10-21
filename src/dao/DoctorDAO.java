package dao;

import models.Doctor;
import utils.JPAUtil;

import javax.persistence.EntityManager;
import java.util.*;

public class DoctorDAO {

    public List<Doctor> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT d FROM Doctor d ORDER BY d.id", Doctor.class)
                     .getResultList();
        } finally { em.close(); }
    }

    public int insert(Doctor d) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(d);
            em.getTransaction().commit();
            return d.getIdAsInt();
        } finally { em.close(); }
    }

    public void update(Doctor d) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(d);
            em.getTransaction().commit();
        } finally { em.close(); }
    }

    /** يمنع الحذف إذا كان للطبيب مواعيد مرتبطة. */
    public void delete(int id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Long apptCount = em.createQuery(
                    "SELECT COUNT(a) FROM models.Appointment a WHERE a.doctor.id = :did",
                    Long.class)
                    .setParameter("did", id)
                    .getSingleResult();

            if (apptCount != null && apptCount > 0) {
                em.getTransaction().rollback();
                throw new IllegalStateException(
                        "لا يمكن حذف الطبيب لوجود " + apptCount + " موعد/مواعيد مرتبطة به. "
                      + "احذف المواعيد أولاً أو غيّر ارتباطها.");
            }

            Doctor ref = em.find(Doctor.class, id);
            if (ref != null) em.remove(ref);

            em.getTransaction().commit();
        } finally { em.close(); }
    }

    public int count() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long c = em.createQuery("SELECT COUNT(d) FROM Doctor d", Long.class).getSingleResult();
            return c.intValue();
        } finally { em.close(); }
    }

    public Map<String,Integer> nameToIdMap() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createQuery(
                "SELECT d.name, d.id FROM Doctor d", Object[].class).getResultList();
            Map<String,Integer> map = new HashMap<>();
            for (Object[] r : rows) map.put((String) r[0], (Integer) r[1]);
            return map;
        } finally { em.close(); }
    }
}

package dao;

import models.User;
import utils.JPAUtil;

import javax.persistence.EntityManager;

public class UserDAO {

    public String findFirstNameByEmailAndHash(String email, String md5Hash) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User u = em.find(User.class, email);
            return (u != null && md5Hash.equals(u.getPassword())) ? u.getFirstName() : null;
        } finally {
            em.close();
        }
    }

    public boolean emailExists(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(User.class, email) != null;
        } finally {
            em.close();
        }
    }

    public void insert(String firstName, String lastName, String email, String md5Hash) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(new User(email, md5Hash, firstName, lastName));
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}

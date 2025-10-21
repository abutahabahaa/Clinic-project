package utils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAUtil {

    // لا ننشئ الـ EMF فورًا حتى لا يكسر تحميل الـFXML
    private static EntityManagerFactory emf;

    private static synchronized void init() {
        if (emf == null) {
            try {
                emf = Persistence.createEntityManagerFactory("clinicPU");
            } catch (Exception e) {
                // رسالة مفيدة ستظهر في الـ Alert/Output بدل InvocationTargetException null
                throw new RuntimeException("Failed to initialize JPA (clinicPU). "
                        + "Check persistence.xml and MySQL driver on classpath. Cause: "
                        + e.getClass().getSimpleName() + " - " + String.valueOf(e.getMessage()), e);
            }
        }
    }

    public static EntityManager getEntityManager() {
        init();
        return emf.createEntityManager();
    }
}

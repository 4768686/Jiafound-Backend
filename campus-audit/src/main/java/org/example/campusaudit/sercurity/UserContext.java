package org.example.campusaudit.sercurity;

public class UserContext {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();

    public static void set(String id, String role) {
        USER_ID.set(id);
        USER_ROLE.set(role);
    }

    public static String getUserId() { return USER_ID.get(); }
    public static boolean isAdmin() { return "Admin".equalsIgnoreCase(USER_ROLE.get()); }

    public static void clear() {
        USER_ID.remove();
        USER_ROLE.remove();
    }
}

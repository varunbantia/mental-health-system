package com.vanaksh.manomitra.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.dashboard.DashboardActivity;

public class RoleManager {
    public static final String ROLE_USER = "user";
    public static final String ROLE_VOLUNTEER = "volunteer";
    public static final String ROLE_COUNSELLOR = "counsellor";
    public static final String ROLE_MODERATOR = "moderator";
    public static final String ROLE_ADMIN = "admin";

    // SharedPreferences constants
    private static final String PREFS_NAME = "RolePrefs";
    private static final String KEY_CACHED_ROLE = "cached_role";

    /**
     * Returns the display name for a given role.
     */
    public static String getRoleDisplayName(String role) {
        if (role == null)
            return "Student";
        switch (role.toLowerCase()) {
            case ROLE_VOLUNTEER:
                return "Volunteer";
            case ROLE_COUNSELLOR:
                return "Counsellor";
            case ROLE_MODERATOR:
                return "Moderator";
            case ROLE_ADMIN:
                return "Administrator";
            case ROLE_USER:
            default:
                return "Student";
        }
    }

    /**
     * Returns the menu resource ID for the bottom navigation based on the user's
     * role.
     */
    public static int getMenuResId(String role) {
        if (role == null)
            return R.menu.menu_dashboard_user;
        switch (role.toLowerCase()) {
            case ROLE_VOLUNTEER:
                return R.menu.menu_dashboard_volunteer;
            case ROLE_COUNSELLOR:
                return R.menu.menu_dashboard_counsellor;
            case ROLE_MODERATOR:
                return R.menu.menu_dashboard_moderator;
            case ROLE_ADMIN:
                return R.menu.menu_dashboard_admin;
            case ROLE_USER:
            default:
                return R.menu.menu_dashboard_user;
        }
    }

    /**
     * Returns the target Activity class for a given role.
     */
    public static Class<?> getTargetActivity(String role) {
        if (role == null)
            return DashboardActivity.class;
        switch (role.toLowerCase()) {
            case ROLE_VOLUNTEER:
                try {
                    return Class.forName("com.vanaksh.manomitra.ui.roles.VolunteerActivity");
                } catch (ClassNotFoundException e) {
                    return DashboardActivity.class;
                }
            case ROLE_COUNSELLOR:
                try {
                    return Class.forName("com.vanaksh.manomitra.ui.roles.CounsellorActivity");
                } catch (ClassNotFoundException e) {
                    return DashboardActivity.class;
                }
            case ROLE_MODERATOR:
                try {
                    return Class.forName("com.vanaksh.manomitra.ui.roles.ModeratorActivity");
                } catch (ClassNotFoundException e) {
                    return DashboardActivity.class;
                }
            case ROLE_ADMIN:
                try {
                    return Class.forName("com.vanaksh.manomitra.ui.roles.AdminActivity");
                } catch (ClassNotFoundException e) {
                    return DashboardActivity.class;
                }
            case ROLE_USER:
            default:
                return DashboardActivity.class;
        }
    }

    /**
     * Returns true if the given role is allowed for anonymous users.
     */
    public static boolean isAnonymousAllowed(String role) {
        return ROLE_USER.equalsIgnoreCase(role);
    }

    /**
     * Returns true if the role has admin privileges.
     */
    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    /**
     * Returns true if the role is a mental health professional.
     */
    public static boolean isProfessional(String role) {
        return ROLE_COUNSELLOR.equalsIgnoreCase(role);
    }

    // ==========================================
    // SHARED PREFERENCES CACHING
    // ==========================================

    /**
     * Save role to SharedPreferences for fast access on app restart.
     */
    public static void saveRole(Context context, String role) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CACHED_ROLE, role).apply();
    }

    /**
     * Get cached role from SharedPreferences.
     * Returns null if no role is cached.
     */
    public static String getCachedRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CACHED_ROLE, null);
    }

    /**
     * Clear cached role from SharedPreferences.
     * Called on logout.
     */
    public static void clearCachedRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CACHED_ROLE).apply();
    }

    /**
     * Perform full logout: clear cached role, sign out Firebase.
     * Call this from any Activity that needs logout functionality.
     */
    public static void performLogout(Context context) {
        clearCachedRole(context);
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
    }
}

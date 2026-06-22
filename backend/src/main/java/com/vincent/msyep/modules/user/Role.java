package com.vincent.msyep.modules.user;

/**
 * MSYEP login tiers. Visibility (highest → lowest):
 * SUPER_ADMIN  → everything
 * ADMIN        → zone, center, student, staff, finance
 * ZONE         → own centers + students (orange portal)
 * CENTER       → own students (pink portal)
 * STAFF        → staff-management scope
 * FINANCE      → finance wing scope
 * STUDENT      → own record only
 */
public enum Role {
    SUPER_ADMIN,
    ADMIN,
    ZONE,
    CENTER,
    STAFF,
    FINANCE,
    STUDENT
}

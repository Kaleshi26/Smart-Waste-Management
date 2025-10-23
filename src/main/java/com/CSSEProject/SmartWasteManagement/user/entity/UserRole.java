package com.CSSEProject.SmartWasteManagement.user.entity;

public enum UserRole {
    ROLE_RESIDENT,      // Households/Businesses
    ROLE_STAFF,         // Waste collection staff
    ROLE_ADMIN,         // System administrators
    ROLE_CITY_MANAGER   // Can configure billing models per city
}
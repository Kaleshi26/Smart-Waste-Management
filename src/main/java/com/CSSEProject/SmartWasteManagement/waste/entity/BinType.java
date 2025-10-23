package com.CSSEProject.SmartWasteManagement.waste.entity;

public enum BinType {
    GENERAL_WASTE,      // Mixed waste
    RECYCLABLE_PLASTIC, // For PAYT rebates
    RECYCLABLE_PAPER,
    ORGANIC_WASTE,
    E_WASTE,           // Higher paybacks
    HAZARDOUS_WASTE    // Special handling
}
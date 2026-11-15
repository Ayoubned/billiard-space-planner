package fr.billiardplanner.shared.config;

public record SystemStatusResponse(
    String status,
    String application
) {}
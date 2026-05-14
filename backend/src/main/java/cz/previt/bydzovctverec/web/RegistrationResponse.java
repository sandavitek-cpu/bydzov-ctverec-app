package cz.previt.bydzovctverec.web;

public record RegistrationResponse(
    Long id,
    String teamName,
    String email,
    String phone,
    String vehicleCategory,
    String vehiclePlate,
    Integer vehicleYear,
    Integer crewCount,
    Integer startNumber,
    Integer startFee,
    String status) {}

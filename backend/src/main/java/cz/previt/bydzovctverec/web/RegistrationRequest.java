package cz.previt.bydzovctverec.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(
    @NotBlank String teamName,
    @NotBlank @Email String email,
    @NotBlank String phone,
    @NotBlank String vehicleCategory,
    @NotBlank String vehiclePlate,
    @NotNull Integer vehicleYear,
    @NotNull @Min(1) Integer crewCount,
    @NotBlank String variant) {}

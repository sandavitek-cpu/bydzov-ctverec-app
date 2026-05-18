package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.CrewMember;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import java.time.Instant;
import java.util.List;

public record AdminRegistrationResponse(
    Long id,
    String teamName,
    String email,
    String phone,
    String vehicleCategory,
    String vehicleMake,
    String vehiclePlate,
    Integer vehicleYear,
    Integer crewCount,
    Integer startNumber,
    Integer startFee,
    String status,
    String variant,
    String firstName,
    String lastName,
    Boolean firstTime,
    String gender,
    Integer driverAge,
    String club,
    String address,
    Integer youngestAge,
    String youngestName,
    Integer engineDisplacement,
    Integer power,
    Integer maxSpeed,
    String vehicleNotes,
    String notes,
    Boolean contacted,
    Boolean properlyRegistered,
    Boolean arrived,
    Boolean consent,
    Boolean approved,
    Instant createdAt,
    List<CrewMemberInfo> crewMembers) {

  public record CrewMemberInfo(String firstName, String lastName, String email,
      Integer driverAge, String gender, String address, Boolean clubMember, String clubName, Boolean firstTime) {}

  public static AdminRegistrationResponse from(RacerRegistration r, List<CrewMember> crewMembers) {
    List<CrewMemberInfo> cmList = crewMembers == null ? List.of()
        : crewMembers.stream().map(cm -> new CrewMemberInfo(cm.getFirstName(), cm.getLastName(), cm.getEmail(),
            cm.getDriverAge(), cm.getGender(), cm.getAddress(), cm.getClubMember(), cm.getClubName(), cm.getFirstTime())).toList();
    return new AdminRegistrationResponse(
        r.getId(), r.getTeamName(), r.getEmail(), r.getPhone(),
        r.getVehicleCategory(), r.getVehicleMake(), r.getVehiclePlate(),
        r.getVehicleYear(), r.getCrewCount(), r.getStartNumber(),
        r.getStartFee(), r.getStatus(), r.getVariant(), r.getFirstName(),
        r.getLastName(), r.getFirstTime(), r.getGender(), r.getDriverAge(),
        r.getClub(), r.getAddress(), r.getYoungestAge(), r.getYoungestName(),
        r.getEngineDisplacement(), r.getPower(), r.getMaxSpeed(),
        r.getVehicleNotes(), r.getNotes(), r.getContacted(),
        r.getProperlyRegistered(), r.getArrived(), r.getConsent(),
        r.getApproved(),
        r.getCreatedAt(), cmList);
  }
}

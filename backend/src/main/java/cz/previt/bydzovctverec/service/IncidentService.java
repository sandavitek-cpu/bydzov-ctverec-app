package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.domain.Incident;
import cz.previt.bydzovctverec.domain.IncidentAssignee;
import cz.previt.bydzovctverec.domain.IncidentAssigneeRepository;
import cz.previt.bydzovctverec.domain.IncidentRepository;
import cz.previt.bydzovctverec.domain.Notification;
import cz.previt.bydzovctverec.domain.NotificationRepository;
import cz.previt.bydzovctverec.domain.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

  private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

  private final IncidentRepository incidentRepository;
  private final IncidentAssigneeRepository incidentAssigneeRepository;
  private final NotificationRepository notificationRepository;
  private final EmailService emailService;

  public IncidentService(IncidentRepository incidentRepository,
      IncidentAssigneeRepository incidentAssigneeRepository,
      NotificationRepository notificationRepository,
      EmailService emailService) {
    this.incidentRepository = incidentRepository;
    this.incidentAssigneeRepository = incidentAssigneeRepository;
    this.notificationRepository = notificationRepository;
    this.emailService = emailService;
  }

  @Transactional
  public Incident createIncident(String title, String description, java.time.LocalDate dueDate,
      User createdBy, List<User> assignees) {
    Incident incident = new Incident(title, description, dueDate, createdBy);
    List<IncidentAssignee> assigneeList = new ArrayList<>();
    for (User user : assignees) {
      assigneeList.add(new IncidentAssignee(incident, user));
    }
    incident.setAssignees(assigneeList);
    incidentRepository.save(incident);

    String link = "/zavodnik/ukoly";
    List<Notification> notifications = new ArrayList<>();
    for (User assignee : assignees) {
      notifications.add(new Notification(assignee, "Přiřazen nový úkol",
          "Byl Vám přiřazen úkol: " + title, "INFO", link));
      emailService.send(assignee.getEmail(),
          "Nový úkol: " + title,
          ("Dobrý den %s,\n\nbyl Vám přiřazen nový úkol:\n\n" +
           "  Název: %s\n  Popis: %s\n  Termín: %s\n\n" +
           "Pro více informací se přihlaste do aplikace:\n" +
           "https://app.bydzov-ctverec.cz\n\nS pozdravem\nTým Novobydžovského čtverce")
              .formatted(assignee.getName(), title,
                  description != null ? description : "—",
                  dueDate != null ? dueDate.toString() : "bez termínu"));
    }
    if (!notifications.isEmpty()) {
      notificationRepository.saveAll(notifications);
    }

    log.info("Incident '{}' created by {} with {} assignees", title, createdBy.getName(), assignees.size());
    return incident;
  }

  @Transactional
  public void updateAssigneeStatus(Long assigneeId, User user, String newStatus) {
    IncidentAssignee assignee = incidentAssigneeRepository.findById(assigneeId).orElse(null);
    if (assignee == null || !assignee.getUser().getId().equals(user.getId())) {
      return;
    }
    assignee.setStatus(newStatus);
    incidentAssigneeRepository.save(assignee);

    Incident incident = assignee.getIncident();
    incident.setUpdatedAt(Instant.now());

    String allDone = incident.getAssignees().stream()
        .allMatch(a -> "DONE".equals(a.getStatus())) ? " (všichni splnili)" : "";
    if ("DONE".equals(newStatus)) {
      boolean everyoneDone = incident.getAssignees().stream()
          .allMatch(a -> "DONE".equals(a.getStatus()));
      if (everyoneDone) {
        incident.setStatus("DONE");
      }
    } else if ("IN_PROGRESS".equals(newStatus) && !"DONE".equals(incident.getStatus())) {
      incident.setStatus("IN_PROGRESS");
    }
    incidentRepository.save(incident);

    User creator = incident.getCreatedBy();
    String statusLabel = switch (newStatus) {
      case "IN_PROGRESS" -> "rozpracován";
      case "DONE" -> "splněn";
      default -> newStatus;
    };
    String message = "Úkol '" + incident.getTitle() + "' změnil stav na " + statusLabel
        + " (uživatel " + user.getName() + ")" + allDone;
    var notification = new Notification(creator, "Stav úkolu změněn",
        message, "INFO", "/admin/incidenty");
    notificationRepository.save(notification);

    emailService.send(creator.getEmail(),
        "Stav úkolu: " + incident.getTitle(),
        ("Dobrý den %s,\n\nuživatel %s změnil stav úkolu '%s' na '%s'.%s\n\n" +
         "Pro více informací se přihlaste do aplikace:\n" +
         "https://app.bydzov-ctverec.cz\n\nS pozdravem\nTým Novobydžovského čtverce")
            .formatted(creator.getName(), user.getName(), incident.getTitle(),
                statusLabel, allDone));
  }

  public List<Incident> getAllIncidents() {
    return incidentRepository.findAllByOrderByCreatedAtDesc();
  }

  public List<IncidentAssignee> getUserAssignments(Long userId) {
    return incidentAssigneeRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }
}

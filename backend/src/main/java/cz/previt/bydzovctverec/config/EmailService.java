package cz.previt.bydzovctverec.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private final JavaMailSender mailSender;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void send(String to, String subject, String body) {
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setTo(to);
      msg.setSubject(subject);
      msg.setText(body);
      msg.setFrom("noreply@bydzov-ctverec.cz");
      mailSender.send(msg);
      log.info("Email sent to {}", to);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
    }
  }

  public void sendCredentials(String to, String personName, String login, String password, Integer startNumber, Integer startFee) {
    String body = """
Dobrý den %s,

byl Vám vytvořen účet do závodnického portálu Novobydžovského čtverce 2026.

Vaše přihlašovací údaje:
  Přihlašovací jméno: %s
  Heslo: %s

Startovní číslo Vaší posádky: %d
Startovné: %d Kč

Po přihlášení uvidíte itinerář, mapu a stav Vaší přihlášky:
https://app.bydzov-ctverec.cz

S pozdravem
Tým Novobydžovského čtverce
""".formatted(personName, login, password, startNumber, startFee);
    send(to, "Novobydžovský čtverec 2026 – vytvořen účet", body);
  }
}

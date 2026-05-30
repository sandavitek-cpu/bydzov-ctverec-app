package cz.previt.bydzovctverec.config;

import java.security.SecureRandom;

public final class ErrorCodeGenerator {

  private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int CODE_LENGTH = 16;

  private ErrorCodeGenerator() {}

  public static String generate() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
    }
    return "LID: " + sb;
  }
}

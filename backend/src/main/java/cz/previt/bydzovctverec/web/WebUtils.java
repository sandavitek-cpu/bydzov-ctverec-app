package cz.previt.bydzovctverec.web;

final class WebUtils {

  private WebUtils() {}

  static Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
  }

  static String toString(Object v) {
    if (v == null) return null;
    if (v instanceof String s) return s;
    return v.toString();
  }

  static Boolean toBoolean(Object v) {
    if (v == null) return null;
    if (v instanceof Boolean b) return b;
    return "true".equalsIgnoreCase(v.toString());
  }
}

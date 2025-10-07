package auth;

import app.AppConfig;

public class AuthService {
  public boolean login(String u, String p) {
    if (u == null || p == null) return false;
    return u.trim().equalsIgnoreCase(AppConfig.HARD_USER) && p.equals(AppConfig.HARD_PASS);
  }
}

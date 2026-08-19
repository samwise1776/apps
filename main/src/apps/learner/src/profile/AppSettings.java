package profile;

public final class AppSettings {
  private int fontSize = 18;
  private boolean animation = true;
  private boolean sound = false;

  public int fontSize() {
    return fontSize;
  }

  public boolean animation() {
    return animation;
  }

  public boolean sound() {
    return sound;
  }

  public void setFontSize(int value) {
    fontSize = Math.max(14, Math.min(28, value));
  }

  public void setAnimation(boolean value) {
    animation = value;
  }

  public void setSound(boolean value) {
    sound = value;
  }
}

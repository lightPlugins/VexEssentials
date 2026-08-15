package dev.vexsoft.essentials.paper.teleport.data;


/** JSON-persisted teleport values kept separate from the player-facing container. */
public final class TeleportData {

  private boolean acceptsRequests = true;

  private String backServer;
  private String backWorld;
  private double backX;
  private double backY;
  private double backZ;
  private float backYaw;
  private float backPitch;

  public boolean isAcceptsRequests() {
    return acceptsRequests;
  }

  public void setAcceptsRequests(final boolean acceptsRequests) {
    this.acceptsRequests = acceptsRequests;
  }

  public String getBackServer() {
    return backServer;
  }

  public void setBackServer(final String backServer) {
    this.backServer = backServer;
  }

  public String getBackWorld() {
    return backWorld;
  }

  public void setBackWorld(final String backWorld) {
    this.backWorld = backWorld;
  }

  public double getBackX() {
    return backX;
  }

  public void setBackX(final double backX) {
    this.backX = backX;
  }

  public double getBackY() {
    return backY;
  }

  public void setBackY(final double backY) {
    this.backY = backY;
  }

  public double getBackZ() {
    return backZ;
  }

  public void setBackZ(final double backZ) {
    this.backZ = backZ;
  }

  public float getBackYaw() {
    return backYaw;
  }

  public void setBackYaw(final float backYaw) {
    this.backYaw = backYaw;
  }

  public float getBackPitch() {
    return backPitch;
  }

  public void setBackPitch(final float backPitch) {
    this.backPitch = backPitch;
  }

  /** Returns whether a complete back position is currently stored. */
  public boolean hasBackPosition() {
    return backServer != null && !backServer.isBlank()
        && backWorld != null && !backWorld.isBlank();
  }

  /** Removes all values belonging to the back position. */
  public void clearBackPosition() {
    backServer = null;
    backWorld = null;
    backX = 0;
    backY = 0;
    backZ = 0;
    backYaw = 0;
    backPitch = 0;
  }
}

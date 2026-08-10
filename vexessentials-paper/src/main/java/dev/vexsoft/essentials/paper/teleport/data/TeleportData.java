package dev.vexsoft.essentials.paper.teleport.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JSON-persisted teleport values kept separate from the player-facing container. */
@Getter
@Setter
@NoArgsConstructor
public final class TeleportData {

  private String backServer;
  private String backWorld;
  private double backX;
  private double backY;
  private double backZ;
  private float backYaw;
  private float backPitch;

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

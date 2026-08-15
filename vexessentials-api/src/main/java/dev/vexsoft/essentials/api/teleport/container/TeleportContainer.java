package dev.vexsoft.essentials.api.teleport.container;

import dev.vexsoft.core.api.player.PlayerContainer;
import dev.vexsoft.core.api.world.ServerPosition;
import java.util.Optional;

/** Exposes the persistent teleport state belonging to one loaded Vex player. */
public interface TeleportContainer extends PlayerContainer {

  /** Returns whether this player currently accepts teleport requests. */
  boolean acceptsRequests();

  /** Changes whether this player accepts teleport requests. */
  void setAcceptsRequests(boolean acceptsRequests);

  /** Returns the last position stored for the back command. */
  Optional<ServerPosition> getBackPosition();

  /** Replaces the position used by the back command. */
  void setBackPosition(ServerPosition position);

  /** Removes the currently stored back position. */
  void clearBackPosition();
}

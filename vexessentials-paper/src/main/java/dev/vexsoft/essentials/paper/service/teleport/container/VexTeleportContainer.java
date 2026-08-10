package dev.vexsoft.essentials.paper.service.teleport.container;

import dev.vexsoft.core.api.network.ServerId;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.essentials.api.teleport.container.TeleportContainer;
import dev.vexsoft.essentials.paper.teleport.data.TeleportData;
import dev.vexsoft.essentials.paper.teleport.data.VexEssentialsPlayerData;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Default player-bound facade for persistent teleport state. */
public final class VexTeleportContainer implements TeleportContainer {

  private final VexPlayer player;
  private final Logger logger;

  /** Creates a teleport container for one loaded player. */
  public VexTeleportContainer(final VexPlayer player, final Logger logger) {
    this.player = Objects.requireNonNull(player, "player");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public Optional<ServerPosition> getBackPosition() {
    return player.read(VexEssentialsPlayerData.TELEPORT, data -> readPosition(data));
  }

  @Override
  public void setBackPosition(final ServerPosition position) {
    ServerPosition checked = Objects.requireNonNull(position, "position");
    player.update(
        VexEssentialsPlayerData.TELEPORT,
        (Consumer<TeleportData>) data -> writePosition(data, checked)
    );
  }

  @Override
  public void clearBackPosition() {
    player.update(VexEssentialsPlayerData.TELEPORT, TeleportData::clearBackPosition);
  }

  private Optional<ServerPosition> readPosition(final TeleportData data) {
    if (!data.hasBackPosition()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new ServerPosition(
          new ServerId(data.getBackServer()),
          WorldKey.parse(data.getBackWorld()),
          data.getBackX(),
          data.getBackY(),
          data.getBackZ(),
          data.getBackYaw(),
          data.getBackPitch()
      ));
    } catch (IllegalArgumentException exception) {
      logger.warning(
          "The saved back position for player '" + player.getName() + "' is invalid and "
              + "cannot be used. The stored value was kept unchanged. Reason: "
              + exception.getMessage()
      );
      return Optional.empty();
    }
  }

  private void writePosition(final TeleportData data, final ServerPosition position) {
    data.setBackServer(position.server().value());
    data.setBackWorld(position.world().asString());
    data.setBackX(position.x());
    data.setBackY(position.y());
    data.setBackZ(position.z());
    data.setBackYaw(position.yaw());
    data.setBackPitch(position.pitch());
  }
}

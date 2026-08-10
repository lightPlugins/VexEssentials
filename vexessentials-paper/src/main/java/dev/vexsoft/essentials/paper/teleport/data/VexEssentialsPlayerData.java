package dev.vexsoft.essentials.paper.teleport.data;

import dev.vexsoft.core.api.player.DataContainerKey;
import dev.vexsoft.core.api.player.DataContainerRegistry;
import dev.vexsoft.core.api.player.PlayerDataDefinition;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import java.util.Objects;

/** Registers the persistent player data owned by VexEssentials. */
@Dependencies
public final class VexEssentialsPlayerData implements PlayerDataDefinition {

  public static final DataContainerKey<TeleportData> TELEPORT = DataContainerKey.of(
      "teleport",
      TeleportData.class,
      TeleportData::new
  );

  /** Creates the data definition through VexCore's class factory. */
  public VexEssentialsPlayerData(final VexServiceRegistry services) {
    Objects.requireNonNull(services, "services");
  }

  @Override
  public void register(final DataContainerRegistry registry) {
    registry.register(TELEPORT);
  }
}

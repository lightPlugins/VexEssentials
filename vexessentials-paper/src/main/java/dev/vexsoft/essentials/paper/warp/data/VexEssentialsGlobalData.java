package dev.vexsoft.essentials.paper.warp.data;

import dev.vexsoft.core.api.globaldata.GlobalDataDefinition;
import dev.vexsoft.core.api.globaldata.GlobalDataKey;
import dev.vexsoft.core.api.globaldata.GlobalDataRegistry;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;

import java.util.Objects;

/** Declares VexEssentials global values shared by every backend server. */
@Dependencies
public final class VexEssentialsGlobalData implements GlobalDataDefinition {

  public VexEssentialsGlobalData(final VexServiceRegistry services) {
    Objects.requireNonNull(services, "services");
  }

  public static final GlobalDataKey<WarpRegistry> WARPS = GlobalDataKey.of(
      "warps",
      WarpRegistry.class,
      WarpRegistry::empty
  );

  @Override
  public void register(final GlobalDataRegistry registry) {
    registry.register(WARPS);
  }
}

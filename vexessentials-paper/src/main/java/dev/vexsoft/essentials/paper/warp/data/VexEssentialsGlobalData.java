package dev.vexsoft.essentials.paper.warp.data;

import dev.vexsoft.core.api.globaldata.GlobalDataDefinition;
import dev.vexsoft.core.api.globaldata.GlobalDataKey;
import dev.vexsoft.core.api.globaldata.GlobalDataRegistry;

/** Declares VexEssentials global values shared by every backend server. */
public final class VexEssentialsGlobalData implements GlobalDataDefinition {

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

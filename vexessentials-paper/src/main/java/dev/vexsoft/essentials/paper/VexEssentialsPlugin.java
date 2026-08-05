package dev.vexsoft.essentials.paper;

import dev.vexsoft.core.paper.plugin.VexPlugin;

/**
 * Starts VexEssentials and connects it to the shared VexCore infrastructure
 */
public final class VexEssentialsPlugin extends VexPlugin {

  @Override
  protected void onVexEnable() {
    getLogger().info("VexEssentials successfully enabled");
  }

  @Override
  protected String getConsolePrefix() {
    return "<dark_gray>[<gradient:#8A2BE2:#00BFFF>VexEssentials</gradient>]</dark_gray> ";
  }
}

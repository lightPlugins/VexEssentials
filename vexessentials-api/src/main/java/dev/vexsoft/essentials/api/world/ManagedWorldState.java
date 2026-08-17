package dev.vexsoft.essentials.api.world;

/** Runtime state of a world managed by VexEssentials. */
public enum ManagedWorldState {
  LOADED,
  UNLOADED,
  LOADING,
  UNLOADING,
  FAILED
}

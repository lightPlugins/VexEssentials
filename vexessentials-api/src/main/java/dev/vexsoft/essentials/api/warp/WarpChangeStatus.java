package dev.vexsoft.essentials.api.warp;

/** Describes the outcome of one persistent warp registry mutation. */
public enum WarpChangeStatus {
  CREATED,
  UPDATED,
  DELETED,
  ALREADY_EXISTS,
  NOT_FOUND
}

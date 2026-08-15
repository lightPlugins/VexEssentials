package dev.vexsoft.essentials.paper.teleport.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Covers request-toggle persistence and compatibility with existing player data. */
final class TeleportDataTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void existingDataDefaultsToAcceptingRequests() throws Exception {
    TeleportData restored = mapper.readValue("{}", TeleportData.class);

    assertTrue(restored.isAcceptsRequests());
  }

  @Test
  void disabledRequestSettingRoundTrips() throws Exception {
    TeleportData original = new TeleportData();
    original.setAcceptsRequests(false);

    TeleportData restored = mapper.readValue(
        mapper.writeValueAsString(original),
        TeleportData.class
    );

    assertFalse(restored.isAcceptsRequests());
  }
}

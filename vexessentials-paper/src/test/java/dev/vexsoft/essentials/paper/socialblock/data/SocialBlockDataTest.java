package dev.vexsoft.essentials.paper.socialblock.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies the persistent representation of general player blocks. */
final class SocialBlockDataTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void roundTripsBlockedPlayerIds() throws Exception {
    UUID blockedId = UUID.randomUUID();
    SocialBlockData original = new SocialBlockData();

    assertTrue(original.block(blockedId));
    SocialBlockData restored = mapper.readValue(
        mapper.writeValueAsString(original),
        SocialBlockData.class
    );

    assertTrue(restored.hasBlocked(blockedId));
    assertEquals(1, restored.getBlockedPlayers().size());
    assertFalse(restored.block(blockedId));
  }
}

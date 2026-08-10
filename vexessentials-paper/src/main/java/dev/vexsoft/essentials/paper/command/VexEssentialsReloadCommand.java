package dev.vexsoft.essentials.paper.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.service.messages.SendMessageService;
import dev.vexsoft.essentials.paper.service.reload.EssentialsReloadService;
import java.util.Objects;

/** Administrative command for reloading VexEssentials at runtime. */
@CommandRoot(name = "vexessentials", description = "Manages VexEssentials")
@Dependencies({EssentialsReloadService.class, SendMessageService.class})
public final class VexEssentialsReloadCommand {

  private final EssentialsReloadService reloads;
  private final SendMessageService messages;

  /** Creates the VexEssentials reload command. */
  public VexEssentialsReloadCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    reloads = checked.require(EssentialsReloadService.class);
    messages = checked.require(SendMessageService.class);
  }

  /** Reloads VexEssentials configuration and language files. */
  @Command(value = "reload", permission = "vexessentials.command.reload")
  public int reload(final VexCommandSource source) {
    boolean successful = reloads.reload();
    messages.send(
        source.getSender(),
        successful
            ? "commands.vexessentials.reload.success"
            : "commands.vexessentials.reload.failed",
        true
    );
    return successful ? 1 : 0;
  }
}

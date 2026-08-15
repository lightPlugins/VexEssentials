package dev.vexsoft.essentials.paper.service.teleport.request;

import dev.vexsoft.core.api.messaging.DeliveryResult;
import dev.vexsoft.core.api.messaging.MessageTarget;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.player.identity.PlayerIdentity;
import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.network.PlayerDirectoryService;
import dev.vexsoft.core.api.service.player.PlayerIdentityService;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.cache.CacheService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.cache.VexCache;
import dev.vexsoft.core.cache.VexCacheOptions;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockContainer;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.api.teleport.container.TeleportContainer;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestRejectionReason;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestState;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import dev.vexsoft.essentials.paper.service.teleport.presentation.TeleportPresentationService;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestCompletion;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestAdmission;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestDecision;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestExecution;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestOffer;
import dev.vexsoft.essentials.paper.teleport.request.TeleportRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/** TPA coordinator supporting local and cross-server participants. */
@Dependencies({
    CacheService.class,
    PlayerService.class,
    PlayerIdentityService.class,
    PlayerDirectoryService.class,
    MessagingService.class,
    ScheduleService.class,
    TeleportConfigurationService.class,
    TeleportPositionService.class,
    TeleportPresentationService.class,
    EssentialsTeleportService.class
})
public final class VexTeleportRequestService implements TeleportRequestService, AutoCloseable {

  private final PlayerService players;
  private final PlayerIdentityService identities;
  private final PlayerDirectoryService directory;
  private final MessagingService messages;
  private final ScheduleService scheduler;
  private final TeleportConfigurationService configuration;
  private final TeleportPositionService positions;
  private final TeleportPresentationService presentation;
  private final EssentialsTeleportService teleports;
  private final Logger logger;
  private final VexCache<UUID, TeleportRequest> incoming;
  private final VexCache<UUID, TeleportRequest> outgoing;
  private final VexCache<UUID, Instant> cooldowns;
  private final Map<UUID, ConcurrentLinkedDeque<UUID>> incomingByTarget =
      new ConcurrentHashMap<>();
  private final Map<UUID, ConcurrentLinkedDeque<UUID>> outgoingByRequester =
      new ConcurrentHashMap<>();
  private final Map<UUID, CompletableFuture<Boolean>> pendingAdmissions =
      new ConcurrentHashMap<>();

  /** Creates the request coordinator and its bounded runtime caches. */
  public VexTeleportRequestService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    identities = checked.require(PlayerIdentityService.class);
    directory = checked.require(PlayerDirectoryService.class);
    messages = checked.require(MessagingService.class);
    scheduler = checked.require(ScheduleService.class);
    configuration = checked.require(TeleportConfigurationService.class);
    positions = checked.require(TeleportPositionService.class);
    presentation = checked.require(TeleportPresentationService.class);
    teleports = checked.require(EssentialsTeleportService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
    CacheService caches = checked.require(CacheService.class);
    long maximum = configuration.maximumRequests();
    Duration retention = configuration.requestExpiration().plusSeconds(30);
    incoming = caches.create(
        "teleport-request-incoming",
        cacheOptions(maximum, retention)
    );
    outgoing = caches.create(
        "teleport-request-outgoing",
        cacheOptions(maximum, retention)
    );
    cooldowns = caches.create(
        "teleport-request-cooldowns",
        cacheOptions(maximum, Duration.ofMinutes(5))
    );
  }

  @Override
  public CompletableFuture<Boolean> send(
      final VexPlayer requester,
      final String targetName,
      final TeleportRequestType type
  ) {
    Objects.requireNonNull(requester, "requester");
    String checkedTarget = Objects.requireNonNull(targetName, "targetName").trim();
    if (checkedTarget.equalsIgnoreCase(requester.getName())) {
      presentation.send(requester, "teleport.request.self", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    Instant now = Instant.now();
    Instant cooldown = cooldowns.getIfPresent(requester.getUniqueId()).orElse(null);
    if (cooldown != null && now.isBefore(cooldown)) {
      presentation.send(
          requester,
          "teleport.request.cooldown",
          Map.of("remaining_seconds", remainingSeconds(Duration.between(now, cooldown))),
          "teleport-failed"
      );
      return CompletableFuture.completedFuture(false);
    }

    return identities.find(checkedTarget).thenCompose(identity -> identity
        .map(found -> createOnlineRequest(requester, found, type, now))
        .orElseGet(() -> {
          presentation.send(
              requester,
              "teleport.player-not-found",
              Map.of("player", checkedTarget),
              "teleport-failed"
          );
          return CompletableFuture.completedFuture(false);
        })).exceptionally(throwable -> {
          reportFailure("look up player '" + checkedTarget + "' for a teleport request", throwable);
          presentation.send(requester, "teleport.error.unavailable", Map.of(), "teleport-failed");
          return false;
        });
  }

  private CompletableFuture<Boolean> createOnlineRequest(
      final VexPlayer requester,
      final PlayerIdentity target,
      final TeleportRequestType type,
      final Instant now
  ) {
    if (requester.getContainer(SocialBlockContainer.class).hasBlocked(target.uniqueId())) {
      presentation.send(requester, "teleport.request.blocked", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    if (players.find(target.uniqueId()).isPresent()) {
      return registerRequest(requester, target, type, now);
    }
    return directory.find(target.uniqueId()).thenCompose(networkPlayer -> {
      if (networkPlayer.isEmpty()) {
        presentation.send(
            requester,
            "teleport.player-offline",
            Map.of("player", target.name()),
            "teleport-failed"
        );
        return CompletableFuture.completedFuture(false);
      }
      return registerRequest(requester, target, type, now);
    });
  }

  private CompletableFuture<Boolean> registerRequest(
      final VexPlayer requester,
      final PlayerIdentity target,
      final TeleportRequestType type,
      final Instant now
  ) {
    Instant expiresAt = now.plus(configuration.requestExpiration());
    TeleportRequest request = new TeleportRequest(
        UUID.randomUUID(), requester.getUniqueId(), requester.getName(), target.uniqueId(),
        target.name(), type, now, expiresAt, TeleportRequestState.AWAITING_ADMISSION
    );
    store(outgoing, outgoingByRequester, request.requesterId(), request);
    CompletableFuture<Boolean> admission = new CompletableFuture<>();
    pendingAdmissions.put(request.requestId(), admission);
    if (!deliverOffer(request.targetId(), offer(request))) {
      request.transition(TeleportRequestState.AWAITING_ADMISSION, TeleportRequestState.FAILED);
      pendingAdmissions.remove(request.requestId());
      presentation.send(
          requester,
          "teleport.request.delivery-failed",
          Map.of("player", target.name()),
          "teleport-failed"
      );
      return CompletableFuture.completedFuture(false);
    }
    scheduler.runAsyncLater(configuration.networkTimeout(), () -> {
      CompletableFuture<Boolean> waiting = pendingAdmissions.remove(request.requestId());
      if (waiting == null) {
        return;
      }
      request.transition(TeleportRequestState.AWAITING_ADMISSION, TeleportRequestState.FAILED);
      players.find(request.requesterId()).ifPresent(player -> presentation.send(
          player,
          "teleport.request.delivery-failed",
          Map.of("player", request.targetName()),
          "teleport-failed"
      ));
      waiting.complete(false);
    });
    return admission;
  }

  @Override
  public CompletableFuture<Boolean> review(final VexPlayer target, final String selector) {
    Optional<TeleportRequest> selected = findIncoming(target.getUniqueId(), selector);
    if (selected.isEmpty()) {
      presentation.send(target, "teleport.request.none", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    TeleportRequest request = selected.get();
    if (!isPending(request)) {
      presentation.send(target, "teleport.request.expired", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    Duration remaining = Duration.between(Instant.now(), request.expiresAt());
    return presentation.openRequestDialog(
        target,
        request.requesterName(),
        request.type(),
        Duration.between(request.createdAt(), request.expiresAt()),
        remaining
    ).thenCompose(choice -> switch (choice) {
      case ACCEPT -> acceptRequest(target, request);
      case DENY -> CompletableFuture.completedFuture(deny(target, request));
      case CLOSED -> CompletableFuture.completedFuture(false);
      case UNAVAILABLE -> {
        presentation.send(target, "teleport.error.dialog", Map.of(), "teleport-failed");
        yield CompletableFuture.completedFuture(false);
      }
    });
  }

  @Override
  public boolean deny(final VexPlayer target, final String selector) {
    Optional<TeleportRequest> request = findIncoming(target.getUniqueId(), selector);
    if (request.isEmpty()) {
      presentation.send(target, "teleport.request.none", Map.of(), "teleport-failed");
      return false;
    }
    return deny(target, request.get());
  }

  private boolean deny(final VexPlayer target, final TeleportRequest request) {
    if (!request.transition(TeleportRequestState.PENDING, TeleportRequestState.DENIED)) {
      presentation.send(target, "teleport.request.already-handled", Map.of(), "teleport-failed");
      return false;
    }
    presentation.send(
        target,
        "teleport.request.denied-target",
        Map.of("player", request.requesterName()),
        "request-denied"
    );
    deliverDecision(
        request.requesterId(),
        new TeleportRequestDecision(request.requestId(), TeleportRequestState.DENIED, null)
    );
    return true;
  }

  @Override
  public boolean cancel(final VexPlayer requester, final String selector) {
    Optional<TeleportRequest> selected = latest(
        outgoing,
        outgoingByRequester.get(requester.getUniqueId()),
        selector
    );
    if (selected.isEmpty()
        || !selected.get().transition(
            TeleportRequestState.PENDING,
            TeleportRequestState.CANCELLED
        )) {
      presentation.send(requester, "teleport.request.no-outgoing", Map.of(), "teleport-failed");
      return false;
    }
    TeleportRequest request = selected.get();
    presentation.send(
        requester,
        "teleport.request.cancelled-requester",
        Map.of("player", request.targetName()),
        "request-denied"
    );
    deliverDecision(
        request.targetId(),
        new TeleportRequestDecision(request.requestId(), TeleportRequestState.CANCELLED, null)
    );
    return true;
  }

  @Override
  public CompletableFuture<Boolean> accept(final VexPlayer target, final String selector) {
    Optional<TeleportRequest> request = findIncoming(target.getUniqueId(), selector);
    if (request.isEmpty()) {
      presentation.send(target, "teleport.request.none", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    return acceptRequest(target, request.get());
  }

  @Override
  public boolean toggle(final VexPlayer player, final Boolean enabled) {
    TeleportContainer teleports = player.getContainer(TeleportContainer.class);
    boolean next = enabled == null ? !teleports.acceptsRequests() : enabled;
    teleports.setAcceptsRequests(next);
    presentation.send(
        player,
        next ? "teleport.request.toggle-enabled" : "teleport.request.toggle-disabled",
        Map.of(),
        null
    );
    return next;
  }

  @Override
  public List<String> getIncomingSuggestions(final UUID targetId) {
    ConcurrentLinkedDeque<UUID> index = incomingByTarget.get(
        Objects.requireNonNull(targetId, "targetId")
    );
    if (index == null) {
      return List.of();
    }
    Instant now = Instant.now();
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (UUID requestId : index) {
      incoming.getIfPresent(requestId)
          .filter(request -> request.state() == TeleportRequestState.PENDING)
          .filter(request -> !request.expired(now))
          .map(TeleportRequest::requesterName)
          .ifPresent(names::add);
    }
    return List.copyOf(names);
  }

  @Override
  public void receive(final TeleportRequestOffer offer) {
    players.find(offer.targetId()).ifPresentOrElse(target -> {
      TeleportRequestRejectionReason rejection = admissionRejection(target, offer);
      if (rejection != TeleportRequestRejectionReason.NONE) {
        deliverAdmission(
            offer.requesterId(),
            new TeleportRequestAdmission(offer.requestId(), false, rejection)
        );
        return;
      }
      TeleportRequest request = new TeleportRequest(
          offer.requestId(), offer.requesterId(), offer.requesterName(), offer.targetId(),
          offer.targetName(), offer.type(), Instant.ofEpochMilli(offer.createdAt()),
          Instant.ofEpochMilli(offer.expiresAt()),
          TeleportRequestState.PENDING
      );
      if (request.expired(Instant.now())) {
        deliverAdmission(
            offer.requesterId(),
            new TeleportRequestAdmission(
                offer.requestId(),
                false,
                TeleportRequestRejectionReason.TARGET_UNAVAILABLE
            )
        );
        return;
      }
      store(incoming, incomingByTarget, request.targetId(), request);
      if (!deliverAdmission(
          offer.requesterId(),
          new TeleportRequestAdmission(
              offer.requestId(),
              true,
              TeleportRequestRejectionReason.NONE
          )
      )) {
        request.transition(TeleportRequestState.PENDING, TeleportRequestState.FAILED);
        return;
      }
      presentation.sendInteractiveRequest(
          target,
          request.type(),
          request.requesterName(),
          Duration.between(request.createdAt(), request.expiresAt()),
          Duration.between(Instant.now(), request.expiresAt()),
          () -> review(target, request.requestId().toString())
      );
      scheduleExpiration(request, false);
    }, () -> deliverAdmission(
        offer.requesterId(),
        new TeleportRequestAdmission(
            offer.requestId(),
            false,
            TeleportRequestRejectionReason.TARGET_UNAVAILABLE
        )
    ));
  }

  @Override
  public List<String> getOutgoingSuggestions(final UUID requesterId) {
    ConcurrentLinkedDeque<UUID> index = outgoingByRequester.get(
        Objects.requireNonNull(requesterId, "requesterId")
    );
    if (index == null) {
      return List.of();
    }
    Instant now = Instant.now();
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (UUID requestId : index) {
      outgoing.getIfPresent(requestId)
          .filter(request -> request.state() == TeleportRequestState.PENDING)
          .filter(request -> !request.expired(now))
          .map(TeleportRequest::targetName)
          .ifPresent(names::add);
    }
    return List.copyOf(names);
  }

  @Override
  public void receive(final TeleportRequestAdmission admission) {
    TeleportRequest request = outgoing.getIfPresent(admission.requestId()).orElse(null);
    CompletableFuture<Boolean> waiting = pendingAdmissions.remove(admission.requestId());
    if (request == null || waiting == null
        || request.state() != TeleportRequestState.AWAITING_ADMISSION) {
      return;
    }
    if (!admission.accepted()) {
      request.transition(TeleportRequestState.AWAITING_ADMISSION, TeleportRequestState.FAILED);
      players.find(request.requesterId()).ifPresent(player -> presentation.send(
          player,
          admissionMessage(admission.reason()),
          Map.of("player", request.targetName()),
          "teleport-failed"
      ));
      waiting.complete(false);
      return;
    }
    if (!request.transition(
        TeleportRequestState.AWAITING_ADMISSION,
        TeleportRequestState.PENDING
    )) {
      waiting.complete(false);
      return;
    }
    cooldowns.put(
        request.requesterId(),
        Instant.now().plus(configuration.requestCooldown())
    );
    players.find(request.requesterId()).ifPresent(player -> presentation.sendWithHover(
        player,
        "teleport.request.sent.message",
        "teleport.request.sent.hover-text",
        Map.of(
            "player", request.targetName(),
            "request_expiration_seconds",
            Long.toString(configuration.requestExpiration().toSeconds())
        ),
        "request-sent"
    ));
    scheduleExpiration(request, true);
    waiting.complete(true);
  }

  @Override
  public void receive(final TeleportRequestDecision decision) {
    TeleportRequest request = outgoing.getIfPresent(decision.requestId()).orElse(null);
    if (request != null) {
      receiveForRequester(request, decision);
      return;
    }
    TeleportRequest incomingRequest = incoming.getIfPresent(decision.requestId()).orElse(null);
    if (incomingRequest != null && decision.state() == TeleportRequestState.CANCELLED
        && incomingRequest.transition(
            TeleportRequestState.PENDING,
            TeleportRequestState.CANCELLED
        )) {
      players.find(incomingRequest.targetId()).ifPresent(target -> presentation.send(
          target,
          "teleport.request.cancelled-target",
          Map.of("player", incomingRequest.requesterName()),
          "request-denied"
      ));
    }
  }

  private void receiveForRequester(
      final TeleportRequest request,
      final TeleportRequestDecision decision
  ) {
    Optional<VexPlayer> requester = players.find(request.requesterId());
    if (decision.state() == TeleportRequestState.DENIED
        && request.transition(TeleportRequestState.PENDING, TeleportRequestState.DENIED)) {
      requester.ifPresent(player -> presentation.send(
          player,
          "teleport.request.denied-requester",
          Map.of("player", request.targetName()),
          "request-denied"
      ));
      return;
    }
    if (decision.state() != TeleportRequestState.ACCEPTING) {
      return;
    }
    if (!isPending(request)) {
      if (request.state() != TeleportRequestState.ACCEPTING
          && request.state() != TeleportRequestState.EXECUTING) {
        sendCompletion(
            request.targetId(),
            request.requestId(),
            false,
            "The teleport request is no longer pending"
        );
      }
      return;
    }
    if (!request.transition(TeleportRequestState.PENDING, TeleportRequestState.ACCEPTING)) {
      return;
    }
    if (requester.isEmpty()) {
      request.transition(TeleportRequestState.ACCEPTING, TeleportRequestState.FAILED);
      sendCompletion(
          request.targetId(),
          request.requestId(),
          false,
          "The requesting player is no longer on this server"
      );
      return;
    }
    if (request.type() == TeleportRequestType.TO_TARGET) {
      if (decision.targetPosition() == null) {
        finishRequesterFailure(request, "The target position was unavailable");
        return;
      }
      if (!startExecution(request)) {
        return;
      }
      execute(request, requester.get(), decision.targetPosition(), request.targetId());
      return;
    }
    positions.capture(requester.get()).thenAccept(position -> {
      if (position.isEmpty() || !deliverExecution(
          request.targetId(),
          new TeleportRequestExecution(request.requestId(), position.get())
      )) {
        finishRequesterFailure(request, "The teleport destination could not be delivered");
      }
    });
  }

  @Override
  public void receive(final TeleportRequestExecution execution) {
    TeleportRequest request = incoming.getIfPresent(execution.requestId()).orElse(null);
    if (request == null || request.type() != TeleportRequestType.TARGET_HERE
        || !startExecution(request)) {
      return;
    }
    players.find(request.targetId()).ifPresentOrElse(
        target -> execute(request, target, execution.destination(), request.requesterId()),
        () -> sendCompletion(request.requesterId(), request.requestId(), false,
            "The target player is no longer online")
    );
  }

  @Override
  public void receive(final TeleportRequestCompletion completion) {
    TeleportRequest request = outgoing.getIfPresent(completion.requestId()).orElse(null);
    if (request != null && request.type() == TeleportRequestType.TARGET_HERE) {
      complete(request, request.requesterId(), completion);
      return;
    }
    request = incoming.getIfPresent(completion.requestId()).orElse(null);
    if (request != null && request.type() == TeleportRequestType.TO_TARGET) {
      complete(request, request.targetId(), completion);
    }
  }

  private CompletableFuture<Boolean> acceptRequest(
      final VexPlayer target,
      final TeleportRequest request
  ) {
    if (!target.getContainer(TeleportContainer.class).acceptsRequests()
        || target.getContainer(SocialBlockContainer.class).hasBlocked(request.requesterId())) {
      if (request.transition(TeleportRequestState.PENDING, TeleportRequestState.DENIED)) {
        deliverDecision(
            request.requesterId(),
            new TeleportRequestDecision(request.requestId(), TeleportRequestState.DENIED, null)
        );
      }
      presentation.send(target, "teleport.request.blocked", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    if (!request.transition(TeleportRequestState.PENDING, TeleportRequestState.ACCEPTING)) {
      presentation.send(target, "teleport.request.already-handled", Map.of(), "teleport-failed");
      return CompletableFuture.completedFuture(false);
    }
    CompletableFuture<Optional<ServerPosition>> targetPosition =
        request.type() == TeleportRequestType.TO_TARGET
            ? positions.capture(target)
            : CompletableFuture.completedFuture(Optional.empty());
    return targetPosition.thenApply(position -> {
      if (request.type() == TeleportRequestType.TO_TARGET && position.isEmpty()) {
        request.transition(TeleportRequestState.ACCEPTING, TeleportRequestState.FAILED);
        presentation.send(target, "teleport.error.position", Map.of(), "teleport-failed");
        return false;
      }
      boolean delivered = deliverDecision(
          request.requesterId(),
          new TeleportRequestDecision(
              request.requestId(),
              TeleportRequestState.ACCEPTING,
              position.orElse(null)
          )
      );
      if (!delivered) {
        request.transition(TeleportRequestState.ACCEPTING, TeleportRequestState.FAILED);
        presentation.send(target, "teleport.error.unavailable", Map.of(), "teleport-failed");
        return false;
      }
      presentation.send(
          target,
          "teleport.request.accepted-target",
          Map.of("player", request.requesterName()),
          "request-accepted"
      );
      return true;
    });
  }

  private void execute(
      final TeleportRequest request,
      final VexPlayer movingPlayer,
      final ServerPosition destination,
      final UUID completionTarget
  ) {
    teleports.teleport(movingPlayer.getUniqueId(), destination, TeleportOptions.defaults())
        .thenAccept(outcome -> {
          TeleportRequestCompletion completion = new TeleportRequestCompletion(
              request.requestId(),
              outcome.successful(),
              outcome.detail()
          );
          if (complete(request, movingPlayer.getUniqueId(), completion)) {
            sendCompletion(
                completionTarget,
                completion.requestId(),
                completion.successful(),
                completion.detail()
            );
          }
        });
  }

  private boolean complete(
      final TeleportRequest request,
      final UUID localPlayerId,
      final TeleportRequestCompletion completion
  ) {
    TeleportRequestState finalState = completion.successful()
        ? TeleportRequestState.ACCEPTED
        : TeleportRequestState.FAILED;
    boolean transitioned = request.transition(TeleportRequestState.EXECUTING, finalState)
        || request.transition(TeleportRequestState.ACCEPTING, finalState);
    if (!transitioned) {
      return false;
    }
    players.find(localPlayerId).ifPresent(player -> presentation.send(
        player,
        completion.successful()
            ? "teleport.request.completed"
            : "teleport.request.failed",
        Map.of("player", otherName(request, localPlayerId)),
        completion.successful() ? "teleport-success" : "teleport-failed"
    ));
    return true;
  }

  private void finishRequesterFailure(final TeleportRequest request, final String detail) {
    boolean completed = complete(
        request,
        request.requesterId(),
        new TeleportRequestCompletion(request.requestId(), false, detail)
    );
    if (completed) {
      sendCompletion(request.targetId(), request.requestId(), false, detail);
    }
  }

  private void sendCompletion(
      final UUID playerId,
      final UUID requestId,
      final boolean successful,
      final String detail
  ) {
    TeleportRequestCompletion completion = new TeleportRequestCompletion(
        requestId,
        successful,
        Objects.requireNonNullElse(detail, "")
    );
    if (players.find(playerId).isPresent()) {
      receive(completion);
      return;
    }
    sendNetwork(playerId, TeleportMessages.REQUEST_COMPLETION, completion);
  }

  private boolean deliverOffer(final UUID targetId, final TeleportRequestOffer offer) {
    if (players.find(targetId).isPresent()) {
      receive(offer);
      return true;
    }
    return sendNetwork(targetId, TeleportMessages.REQUEST_OFFER, offer);
  }

  private boolean deliverAdmission(
      final UUID requesterId,
      final TeleportRequestAdmission admission
  ) {
    if (players.find(requesterId).isPresent()) {
      receive(admission);
      return true;
    }
    return sendNetwork(requesterId, TeleportMessages.REQUEST_ADMISSION, admission);
  }

  private boolean deliverDecision(final UUID playerId, final TeleportRequestDecision decision) {
    if (players.find(playerId).isPresent()) {
      receive(decision);
      return true;
    }
    return sendNetwork(playerId, TeleportMessages.REQUEST_DECISION, decision);
  }

  private boolean deliverExecution(final UUID playerId, final TeleportRequestExecution execution) {
    if (players.find(playerId).isPresent()) {
      receive(execution);
      return true;
    }
    return sendNetwork(playerId, TeleportMessages.REQUEST_EXECUTION, execution);
  }

  private <T> boolean sendNetwork(
      final UUID playerId,
      final MessageType<T> type,
      final T payload
  ) {
    try {
      DeliveryResult result = messages.send(MessageTarget.player(playerId), type, payload);
      return result == DeliveryResult.SENT || result == DeliveryResult.QUEUED;
    } catch (RuntimeException exception) {
      reportFailure("deliver a cross-server teleport message", exception);
      return false;
    }
  }

  private TeleportRequestOffer offer(final TeleportRequest request) {
    return new TeleportRequestOffer(
        request.requestId(), request.requesterId(), request.requesterName(), request.targetId(),
        request.targetName(), request.type(), request.createdAt().toEpochMilli(),
        request.expiresAt().toEpochMilli()
    );
  }

  private Optional<TeleportRequest> findIncoming(final UUID targetId, final String selector) {
    return latest(incoming, incomingByTarget.get(targetId), selector);
  }

  private Optional<TeleportRequest> latest(
      final VexCache<UUID, TeleportRequest> cache,
      final ConcurrentLinkedDeque<UUID> index,
      final String selector
  ) {
    if (index == null) {
      return Optional.empty();
    }
    UUID exactId = parseUuid(selector);
    for (UUID requestId : index) {
      TeleportRequest request = cache.getIfPresent(requestId).orElse(null);
      if (request == null || request.state() != TeleportRequestState.PENDING) {
        continue;
      }
      if (selector == null || selector.isBlank() || requestId.equals(exactId)
          || request.requesterName().equalsIgnoreCase(selector)
          || request.targetName().equalsIgnoreCase(selector)) {
        return Optional.of(request);
      }
    }
    return Optional.empty();
  }

  private void store(
      final VexCache<UUID, TeleportRequest> cache,
      final Map<UUID, ConcurrentLinkedDeque<UUID>> indexes,
      final UUID playerId,
      final TeleportRequest request
  ) {
    cache.put(request.requestId(), request);
    ConcurrentLinkedDeque<UUID> index = indexes.computeIfAbsent(
        playerId,
        ignored -> new ConcurrentLinkedDeque<>()
    );
    index.addFirst(request.requestId());
    while (index.size() > 32) {
      index.pollLast();
    }
  }

  private void scheduleExpiration(final TeleportRequest request, final boolean requesterSide) {
    Duration delay = Duration.between(Instant.now(), request.expiresAt());
    if (delay.isNegative()) {
      delay = Duration.ZERO;
    }
    scheduler.runAsyncLater(delay, () -> {
      if (!request.transition(TeleportRequestState.PENDING, TeleportRequestState.EXPIRED)) {
        return;
      }
      UUID localId = requesterSide ? request.requesterId() : request.targetId();
      players.find(localId).ifPresent(player -> presentation.send(
          player,
          "teleport.request.expired-notice",
          Map.of("player", otherName(request, localId)),
          "request-denied"
      ));
    });
  }

  private boolean isPending(final TeleportRequest request) {
    if (!request.expired(Instant.now())) {
      return request.state() == TeleportRequestState.PENDING;
    }
    request.transition(TeleportRequestState.PENDING, TeleportRequestState.EXPIRED);
    return false;
  }

  private boolean startExecution(final TeleportRequest request) {
    return request.transition(TeleportRequestState.ACCEPTING, TeleportRequestState.EXECUTING);
  }

  private String otherName(final TeleportRequest request, final UUID localPlayerId) {
    return request.requesterId().equals(localPlayerId)
        ? request.targetName()
        : request.requesterName();
  }

  private TeleportRequestRejectionReason admissionRejection(
      final VexPlayer target,
      final TeleportRequestOffer offer
  ) {
    if (!target.getContainer(TeleportContainer.class).acceptsRequests()) {
      return TeleportRequestRejectionReason.REQUESTS_DISABLED;
    }
    if (target.getContainer(SocialBlockContainer.class).hasBlocked(offer.requesterId())) {
      return TeleportRequestRejectionReason.BLOCKED;
    }
    ConcurrentLinkedDeque<UUID> index = incomingByTarget.get(target.getUniqueId());
    if (index != null) {
      for (UUID requestId : index) {
        TeleportRequest request = incoming.getIfPresent(requestId).orElse(null);
        if (request != null && request.state() == TeleportRequestState.PENDING
            && request.requesterId().equals(offer.requesterId())
            && request.type() == offer.type()) {
          return TeleportRequestRejectionReason.DUPLICATE;
        }
      }
    }
    return TeleportRequestRejectionReason.NONE;
  }

  private String admissionMessage(final TeleportRequestRejectionReason reason) {
    return switch (reason) {
      case BLOCKED -> "teleport.request.blocked";
      case REQUESTS_DISABLED -> "teleport.request.target-disabled";
      case DUPLICATE -> "teleport.request.duplicate";
      case TARGET_UNAVAILABLE, NONE -> "teleport.request.delivery-failed";
    };
  }

  private UUID parseUuid(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private String remainingSeconds(final Duration duration) {
    return Long.toString(Math.max(0, duration.toSeconds()));
  }

  private VexCacheOptions cacheOptions(final long maximum, final Duration expiration) {
    return VexCacheOptions.builder()
        .maximumSize(maximum)
        .expireAfterWrite(expiration)
        .build();
  }

  private void reportFailure(final String action, final Throwable throwable) {
    logger.log(
        Level.WARNING,
        "VexEssentials could not " + action + ". The request was stopped without moving any "
            + "player.",
        throwable
    );
  }

  @Override
  public void close() {
    pendingAdmissions.values().forEach(future -> future.complete(false));
    pendingAdmissions.clear();
    incoming.invalidateAll();
    outgoing.invalidateAll();
    cooldowns.invalidateAll();
    incomingByTarget.clear();
    outgoingByRequester.clear();
  }
}

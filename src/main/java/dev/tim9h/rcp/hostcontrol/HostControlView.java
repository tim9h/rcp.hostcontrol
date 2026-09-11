package dev.tim9h.rcp.hostcontrol;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

import dev.tim9h.rcp.event.CcEvent;
import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.hostcontrol.service.HostControlService;
import dev.tim9h.rcp.hostcontrol.utils.TimeUtils;
import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.spi.CommandBuilder;
import dev.tim9h.rcp.spi.CommandNode;
import dev.tim9h.rcp.spi.Plugin;

public class HostControlView implements Plugin {

	@InjectLogger
	private Logger logger;

	@Inject
	private EventManager eventManager;

	@Inject
	private HostControlService service;

	@Override
	public String getName() {
		return "Host Controller";
	}

	@Override
	public String getId() {
		return "hostcontrol";
	}

	@Override
	public Optional<CommandNode> getCommands() {
		return new CommandBuilder().command("shutdown", true, _ -> {
			shutdown();
		}).argumentAction(time -> {
			var ldt = service.shutdown(time, this::shutdown);
			if (ldt == null) {
				eventManager.echo("Unable to parse shutdown time. Use examples like '10 min', '1h30', or '23:15'.");
			} else {
				eventManager.echo("Shutdown scheduled", TimeUtils.getAbsoluteAndRelativeTimeString(ldt));
			}
		}).child("cancel", _ -> {
			eventManager.showWaitingIndicator();
			CompletableFuture.supplyAsync(service::cancelShutdown).thenAccept(canceled -> {
				if (canceled.booleanValue()) {
					eventManager.echo("Scheduled shutdown canceled");
				} else {
					eventManager.echo("No shutdown scheduled");
				}
			});
		}).up().child("when", _ -> {
			eventManager.showWaitingIndicator();
			CompletableFuture.supplyAsync(service::getScheduledShutdown).thenAccept(ldt -> {
				if (ldt == null) {
					eventManager.echo("No shutdown scheduled");
				} else {
					eventManager.echo("Scheduled shutdown", TimeUtils.getAbsoluteAndRelativeTimeString(ldt));
				}
			});
		}).up().command("lock", _ -> {
			service.lock();
		}).build();
	}

	private void shutdown() {
		logger.info(() -> "Shutting down workstation");
		eventManager.post(new CcEvent("exitimmediately"));
		eventManager.echo("kthxbye.");
		eventManager.post(new CcEvent(CcEvent.EVENT_CLOSING));
		eventManager.listen(CcEvent.EVENT_CLOSING_FINISHED, _ -> service.shutdown());
	}

}

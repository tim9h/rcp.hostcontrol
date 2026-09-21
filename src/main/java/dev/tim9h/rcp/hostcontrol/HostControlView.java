package dev.tim9h.rcp.hostcontrol;

import java.util.Optional;

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
		//@formatter:off
		return new CommandBuilder()
			.command("shutdown", true, _ -> shutdownPluginsAndHost())
				.argumentAction(time -> {
					var shutdownTime = service.shutdownHost(time);
					if (shutdownTime == null) {
						eventManager.echo("Unable to parse shutdown time. " + "Use examples like '10 min', '1h30', or '23:15'.");
					} else {
						eventManager.echo("Shutdown scheduled", TimeUtils.getAbsoluteAndRelativeTimeString(shutdownTime));
					}
				})
				.child("cancel", _ -> cancelShutdown()).up()
				.child("when", _ -> showScheduledShutdown()).up()
			.command("lock", _ -> service.lock())
			.build();
		//@formatter:on
	}

	private void cancelShutdown() {
		if (service.cancelShutdown()) {
			eventManager.echo("Scheduled shutdown canceled");
		} else {
			eventManager.echo("No shutdown scheduled");
		}
	}

	private void showScheduledShutdown() {
		var shutdownTime = service.getScheduledShutdown();

		if (shutdownTime == null) {
			eventManager.echo("No shutdown scheduled");
		} else {
			eventManager.echo("Scheduled shutdown", TimeUtils.getAbsoluteAndRelativeTimeString(shutdownTime));
		}
	}

	private void shutdownPluginsAndHost() {
		eventManager.listen(CcEvent.EVENT_CLOSING_FINISHED, _ -> {
			eventManager.unsubscribe(CcEvent.EVENT_CLOSING_FINISHED);
			service.shutdownHost();
		});
		eventManager.post("exit", "cleanup");
	}

}
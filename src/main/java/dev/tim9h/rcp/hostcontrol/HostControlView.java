package dev.tim9h.rcp.hostcontrol;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

import dev.tim9h.rcp.event.CcEvent;
import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.hostcontrol.service.HostControlService;
import dev.tim9h.rcp.hostcontrol.utils.TimeUtils;
import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.spi.Plugin;
import dev.tim9h.rcp.spi.StringNode;
import dev.tim9h.rcp.spi.TreeNode;

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
	public Optional<TreeNode<String>> getModelessCommands() {
		var tree = new StringNode();
		tree.add("shutdown").add("cancel", "when");
		tree.add("lock");
		return Optional.of(tree);
	}

	@Override
	public void initBus(EventManager em) {
		Plugin.super.initBus(eventManager);

		em.listen("shutdown", data -> {
			var time = StringUtils.join(data, StringUtils.SPACE);
			if ("cancel".equals(time)) {
				em.showWaitingIndicator();
				CompletableFuture.supplyAsync(service::cancelShutdown).thenAccept(canceled -> {
					if (canceled.booleanValue()) {
						em.echo("Scheduled shutdown canceled");
					} else {
						em.echo("No shutdown scheduled");
					}
				});
			} else if ("when".equals(time)) {
				em.showWaitingIndicator();
				CompletableFuture.supplyAsync(service::getScheduledShutdown).thenAccept(ldt -> {
					if (ldt == null) {
						eventManager.echo("No shutdown scheduled");
					} else {
						eventManager.echo("Scheduled shutdown: " + TimeUtils.getAbsoluteAndRelativeTimeString(ldt));
					}
				});
			} else if (StringUtils.isBlank(time)) {
				shutdown();

			} else {
				var ldt = service.shutdown(time, this::shutdown);
				if (ldt == null) {
					em.echo("Unable to parse shutdown time. Use examples like '10 min', '1h30', or '23:15'.");
				} else {
					em.echo("Shutdown scheduled for ", TimeUtils.getAbsoluteAndRelativeTimeString(ldt));
				}
			}
		});
		em.listen("lock", _ -> service.lock());
	}

	private void shutdown() {
		logger.info(() -> "Shutting down workstation");
		eventManager.post(new CcEvent("exitimmediately"));
		eventManager.echo("kthxbye.");
		eventManager.post(new CcEvent(CcEvent.EVENT_CLOSING));
		eventManager.listen(CcEvent.EVENT_CLOSING_FINISHED, _ -> service.shutdown());
	}

}

package dev.tim9h.rcp.hostcontrol;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

import dev.tim9h.rcp.event.CcEvent;
import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.hostcontrol.service.HostControlService;
import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.spi.CCard;
import dev.tim9h.rcp.spi.TreeNode;

public class HostControlView implements CCard {

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
	public Optional<TreeNode<String>> getModelessCommands() {
		var tree = new TreeNode<>(StringUtils.EMPTY);
		tree.add("shutdown").add("cancel");
		tree.add("lock");
		return Optional.of(tree);
	}

	@Override
	public void initBus(EventManager em) {
		CCard.super.initBus(eventManager);

		em.listen("shutdown", data -> {
			var time = StringUtils.join(data, StringUtils.SPACE);
			if ("cancel".equals(time)) {
				em.showWaitingIndicator();
				CompletableFuture.supplyAsync(service::cancelShutdown).thenAccept(canceled -> {
					if (canceled.booleanValue()) {
						em.echoAsync("Scheduled shutdown canceled");
					} else {
						em.echoAsync("No shutdown scheduled");
					}
				});

			} else if (StringUtils.isBlank(time)) {
				shutdown();

			} else {
				var ldt = service.shutdown(time, this::shutdown);
				em.echo("Shutdown scheduled for", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
			}
		});
		em.listen("lock", data -> service.lock());
	}

	private void shutdown() {
		logger.info(() -> "Shutting down workstation");
		eventManager.post(new CcEvent("exitimmediately"));
		eventManager.echoAsync("kthxbye.");
		eventManager.post(new CcEvent(CcEvent.EVENT_CLOSING));
		eventManager.listen(CcEvent.EVENT_CLOSING_FINISHED, event -> service.shutdown());
	}

}

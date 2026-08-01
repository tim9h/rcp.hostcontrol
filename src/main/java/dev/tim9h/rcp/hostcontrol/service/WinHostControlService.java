package dev.tim9h.rcp.hostcontrol.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import dev.tim9h.rcp.hostcontrol.utils.TimeUtils;
import dev.tim9h.rcp.logging.InjectLogger;

public class WinHostControlService implements HostControlService {

	@InjectLogger
	private Logger logger;

	private ScheduledExecutorService scheduler;

	private ScheduledFuture<?> scheduledFuture;

	private volatile LocalDateTime scheduledShutdownTime;

	@Override
	public void shutdown() {
		try {
			Runtime.getRuntime().exec(new String[] { "shutdown", "-s", "-t", "0" });
			logger.debug(() -> "Shutting down now");
		} catch (IOException e) {
			logger.error(() -> "Unable to shutdown workstation", e);
		}
	}

	@Override
	public LocalDateTime shutdown(String time, Runnable shutdown) {
		if (scheduler == null) {
			scheduler = Executors.newScheduledThreadPool(1);
		}
		var seconds = TimeUtils.getSecondsByInput(time);
		logger.info(() -> time + " parsed into " + seconds + " seconds");
		if (seconds < 0) {
			return null;
		}
		scheduledShutdownTime = LocalDateTime.now().plusSeconds(seconds);
		scheduledFuture = scheduler.schedule(shutdown, seconds, TimeUnit.SECONDS);
		return scheduledShutdownTime;
	}

	@Override
	public void lock() {
		logger.info(() -> "Locking workstation");
		try {
			Runtime.getRuntime()
					.exec(new String[] { "C:\\Windows\\System32\\rundll32.exe", "user32.dll,LockWorkStation" });
		} catch (IOException e) {
			logger.error(() -> "Unable to lock workstation", e);
		}
	}

	@Override
	public boolean cancelShutdown() {
		if (scheduler == null || scheduledFuture == null) {
			return false;
		}
		logger.info(() -> "Cancelling shutdown timer");
		var cancelled = scheduledFuture.cancel(false);
		scheduledFuture = null;
		scheduledShutdownTime = null;
		return cancelled;
	}

	@Override
	public LocalDateTime getScheduledShutdown() {
		return scheduledShutdownTime;
	}

}

package dev.tim9h.rcp.hostcontrol.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.hostcontrol.utils.TimeUtils;
import dev.tim9h.rcp.logging.InjectLogger;

public class WinHostControlService implements HostControlService {

	private static final String SHUTDOWN_COMMAND = "shutdown";

	private volatile LocalDateTime scheduledShutdownTime;

	@InjectLogger
	private Logger logger;

	@Inject
	private EventManager em;

	@Override
	public void shutdownHost() {
		logger.debug(() -> "Shutting down workstation now");

		if (!executeShutdownCommand("-s", "-t", "0")) {
			logger.error(() -> "Unable to shut down workstation");
			em.post("exit", "force");
		}
	}

	@Override
	public LocalDateTime shutdownHost(String time) {
		var seconds = TimeUtils.getSecondsByInput(time);
		logger.info(() -> time + " parsed into " + seconds + " seconds");
		if (seconds < 0) {
			return null;
		}
		var shutdownTime = LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(seconds);
		if (!executeShutdownCommand("-s", "-t", String.valueOf(seconds))) {
			return null;
		}
		scheduledShutdownTime = shutdownTime;
		return shutdownTime;
	}

	@Override
	public boolean cancelShutdown() {
		if (scheduledShutdownTime == null) {
			return false;
		}
		logger.info(() -> "Cancelling scheduled workstation shutdown");
		if (!executeShutdownCommand("-a")) {
			return false;
		}
		scheduledShutdownTime = null;
		return true;
	}

	@Override
	public LocalDateTime getScheduledShutdown() {
		return scheduledShutdownTime;
	}

	private boolean executeShutdownCommand(String... arguments) {
		try {
			var command = new String[arguments.length + 1];
			command[0] = SHUTDOWN_COMMAND;
			System.arraycopy(arguments, 0, command, 1, arguments.length);
			new ProcessBuilder(command).start();
			return true;
		} catch (IOException e) {
			logger.error(() -> "Unable to execute Windows shutdown command", e);
			return false;
		}
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

}
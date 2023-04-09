package dev.tim9h.rcp.hostcontrol.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.joda.time.format.PeriodFormatterBuilder;

import dev.tim9h.rcp.logging.InjectLogger;

public class WinHostControlService implements HostControlService {

	@InjectLogger
	private Logger logger;

	private ScheduledExecutorService scheduler;

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
		var seconds = getSecondsByInput(time);
		logger.info(() -> time + " parsed into " + seconds + " seconds");
		scheduler.schedule(shutdown::run, seconds, TimeUnit.SECONDS);
		return LocalDateTime.now().plusSeconds(seconds);
	}

	private static long getSecondsByInput(String time) {
		//@formatter:off
		var formatter = new PeriodFormatterBuilder()
				.appendDays().appendSuffix("d ")
				.appendHours().appendSuffix("h ")
				.appendMinutes().appendSuffix("min")
				.toFormatter();
		//@formatter:on
		try {
			return formatter.parsePeriod(time).toStandardSeconds().getSeconds();
		} catch (IllegalArgumentException e) {
			//
		}
		try {
			var lt = LocalTime.parse(time);
			Instant instant;
			if (lt.isBefore(LocalTime.now())) {
				var ldt = LocalDateTime.of(LocalDate.now().plusDays(1), lt);
				instant = ldt.toInstant(OffsetDateTime.now().getOffset());
			} else {
				var ldt = LocalDateTime.of(LocalDate.now(), lt);
				instant = ldt.toInstant(OffsetDateTime.now().getOffset());
			}
			return Duration.between(Instant.now(), instant).toSeconds();
		} catch (DateTimeParseException e) {
			return -1;
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

	@Override
	public boolean cancelShutdown() {
		if (scheduler == null) {
			return false;
		}
		logger.info(() -> "Cancelling shutdown timer");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			logger.error(() -> "Unable to cancel shutdown", e);
			Thread.currentThread().interrupt();
		}
		scheduler = null;
		return true;
	}

}

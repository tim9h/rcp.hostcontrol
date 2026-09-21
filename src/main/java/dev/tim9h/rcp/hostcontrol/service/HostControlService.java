package dev.tim9h.rcp.hostcontrol.service;

import java.time.LocalDateTime;

import com.google.inject.ImplementedBy;

@ImplementedBy(WinHostControlService.class)
public interface HostControlService {

	void shutdownHost();

	LocalDateTime shutdownHost(String time);

	boolean cancelShutdown();

	LocalDateTime getScheduledShutdown();

	void lock();

}

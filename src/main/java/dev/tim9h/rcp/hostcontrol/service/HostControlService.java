package dev.tim9h.rcp.hostcontrol.service;

import java.time.LocalDateTime;

import com.google.inject.ImplementedBy;

@ImplementedBy(WinHostControlService.class)
public interface HostControlService {

	public void shutdown();

	public LocalDateTime shutdown(String time, Runnable shutdown);

	public boolean cancelShutdown();

	public void lock();

}

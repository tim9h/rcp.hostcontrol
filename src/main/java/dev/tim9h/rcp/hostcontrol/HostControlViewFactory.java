package dev.tim9h.rcp.hostcontrol;

import java.util.Collections;
import java.util.Map;

import com.google.inject.Inject;

import dev.tim9h.rcp.spi.Plugin;
import dev.tim9h.rcp.spi.PluginFactory;

public class HostControlViewFactory implements PluginFactory {

	@Inject
	private HostControlView view;

	@Override
	public String getId() {
		return "hostcontrol";
	}

	@Override
	public Plugin create() {
		return view;
	}

	@Override
	public Map<String, String> getSettingsContributions() {
		return Collections.emptyMap();
	}

}

package dev.tim9h.rcp.hostcontrol;

import java.util.Collections;
import java.util.Map;

import com.google.inject.Inject;

import dev.tim9h.rcp.spi.CCard;
import dev.tim9h.rcp.spi.CCardFactory;

public class HostControlViewFactory implements CCardFactory {

	@Inject
	private HostControlView view;

	@Override
	public String getId() {
		return "hostcontrol";
	}

	@Override
	public CCard createCCard() {
		return view;
	}

	@Override
	public Map<String, String> getSettingsContributions() {
		return Collections.emptyMap();
	}

}

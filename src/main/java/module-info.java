module rcp.hostcontrol {
	exports dev.tim9h.rcp.hostcontrol;

	requires transitive rcp.api;
	requires com.google.guice;
	requires org.apache.logging.log4j;
	requires transitive javafx.controls;
	requires org.apache.commons.lang3;
	requires org.joda.time;
}
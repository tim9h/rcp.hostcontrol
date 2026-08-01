package dev.tim9h.rcp.hostcontrol.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.joda.time.format.PeriodFormatterBuilder;

public class TimeUtils {

	private TimeUtils() {
		// hide implicit public constructor
	}

	public static long getSecondsByInput(String time) {
		if (time == null) {
			return -1;
		}
		var input = time.trim().toLowerCase();
		if (input.isEmpty()) {
			return -1;
		}

		// Try simple numeric + unit patterns, e.g. "10m", "10 min", "2h", "1d"
		var simplePattern = Pattern.compile(
				"^(\\d+)\\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days)?$");
		var m = simplePattern.matcher(input);
		if (m.matches()) {
			var value = Long.parseLong(m.group(1));
			var unit = m.group(2);
			if (unit == null || unit.isEmpty()) {
				// treat plain number as minutes (user-friendly)
				return TimeUnit.MINUTES.toSeconds(value);
			}
			switch (unit) {
			case "s":
			case "sec":
			case "secs":
			case "second":
			case "seconds":
				return value;
			case "m":
			case "min":
			case "mins":
			case "minute":
			case "minutes":
				return TimeUnit.MINUTES.toSeconds(value);
			case "h":
			case "hr":
			case "hrs":
			case "hour":
			case "hours":
				return TimeUnit.HOURS.toSeconds(value);
			case "d":
			case "day":
			case "days":
				return TimeUnit.DAYS.toSeconds(value);
			}
		}

		// Try patterns with hours and minutes like "1h30", "1h 30m", "1:30"
		try {
			// handle HH:MM or H:MM (treat as hours:minutes from now if <24:00)
			if (input.contains(":")) {
				var parts = input.split(":");
				if (parts.length == 2) {
					var h = Integer.parseInt(parts[0].trim());
					var mm = Integer.parseInt(parts[1].trim());
					var lt = LocalTime.of(h, mm);
					var targetZdt = ZonedDateTime.of(LocalDate.now(), lt, ZoneId.systemDefault());
					if (targetZdt.isBefore(ZonedDateTime.now())) {
						targetZdt = targetZdt.plusDays(1);
					}
					return Duration.between(Instant.now(), targetZdt.toInstant()).getSeconds();
				}
			}
		} catch (Exception e) {
			// fall through to other parsers
		}

		// Try Joda PeriodFormatter fallback (existing behavior) - accept inputs like
		// "1d 2h 10min"
		//@formatter:off
		var formatter = new PeriodFormatterBuilder()
				.appendDays().appendSuffix("d ")
				.appendHours().appendSuffix("h ")
				.appendMinutes().appendSuffix("min")
				.toFormatter();
		//@formatter:on
		try {
			return formatter.parsePeriod(input).toStandardSeconds().getSeconds();
		} catch (IllegalArgumentException e) {
			// ignore and continue
		}

		// Try parsing pure LocalTime (e.g. "23:15" or "07:00") using Java parser as
		// last resort
		try {
			var lt = LocalTime.parse(input);
			var targetZdt = ZonedDateTime.of(LocalDate.now(), lt, ZoneId.systemDefault());
			if (targetZdt.isBefore(ZonedDateTime.now())) {
				targetZdt = targetZdt.plusDays(1);
			}
			return Duration.between(Instant.now(), targetZdt.toInstant()).getSeconds();
		} catch (DateTimeParseException e) {
			return -1;
		}
	}

	public static String getAbsoluteAndRelativeTimeString(LocalDateTime ldt) {
		var absFmt = DateTimeFormatter.ofPattern("HH:mm");
		var absolute = ldt.format(absFmt);
		var now = java.time.LocalDateTime.now();
		var dur = java.time.Duration.between(now, ldt);
		String relative;
		if (dur.isZero() || dur.isNegative()) {
			relative = "now";
		} else {
			var seconds = dur.getSeconds();
			var days = seconds / 86400;
			var hours = (seconds % 86400) / 3600;
			var minutes = (seconds % 3600) / 60;
			if (days > 0) {
				relative = String.format("in %dd %dh %dm", days, hours, minutes);
			} else if (hours > 0) {
				relative = String.format("in %dh %dm", hours, minutes);
			} else if (minutes > 0) {
				relative = String.format("in %dm", minutes);
			} else {
				relative = String.format("in %ds", seconds);
			}
		}
		return String.format("%s (%s)", absolute, relative);
	}

}

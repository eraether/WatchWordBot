package com.raether.watchwordbot;

import java.util.concurrent.TimeUnit;

public class CompetitiveTime {
	private long remainingTime;
	private long remainingOvertime;
	private TimeUnit measurementUnit;

	public CompetitiveTime(long remainingOvertime, long remainingTime,
			TimeUnit unit) {
		this.remainingOvertime = remainingOvertime;
		this.remainingTime = remainingTime;
		this.measurementUnit = unit;
	}

	public long getTime(TimeUnit unit) {
		return unit.convert(this.remainingTime, measurementUnit);
	}

	public long getOvertime(TimeUnit unit) {
		return unit.convert(this.remainingOvertime, measurementUnit);
	}

	public long getTotalTime(TimeUnit unit) {
		return getTime(unit) + getOvertime(unit);
	}
}
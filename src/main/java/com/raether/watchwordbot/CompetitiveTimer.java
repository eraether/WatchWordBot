package com.raether.watchwordbot;

import java.util.concurrent.TimeUnit;

public class CompetitiveTimer {
	private long initialOvertime = 0;
	private CompetitiveTime remainingTime;
	private static TimeUnit measurementUnit = TimeUnit.MILLISECONDS;

	public CompetitiveTimer(long remainingOvertime, long initialOvertime,
			long remainingTime, TimeUnit unit) {
		setCompetitiveTime(new CompetitiveTime(remainingOvertime,
				remainingTime, unit));
		setInitialOvertime(initialOvertime, unit);
	}

	public CompetitiveTimer(long remainingOvertime, TimeUnit unit) {
		this(remainingOvertime, remainingOvertime, 0, unit);
	}

	private void setCompetitiveTime(CompetitiveTime time) {
		this.remainingTime = time;
	}

	private void setInitialOvertime(long number, TimeUnit unit) {
		this.initialOvertime = measurementUnit.convert(number, unit);
	}

	public long getInitialOvertime(TimeUnit unit) {
		return unit.convert(this.initialOvertime, measurementUnit);
	}

	public void resetOvertime() {
		setCompetitiveTime(new CompetitiveTime(initialOvertime, getTime(
				measurementUnit).getTime(measurementUnit), measurementUnit));
	}

	public void setRemainingTime(long number, TimeUnit unit) {
		setCompetitiveTime(new CompetitiveTime(getTime(measurementUnit)
				.getOvertime(measurementUnit), measurementUnit.convert(number,
				unit), measurementUnit));
	}

	public CompetitiveTime getTime(TimeUnit unit) {
		return remainingTime;
	}

	public void reduceTimeBy(long amount, TimeUnit unit) {
		setCompetitiveTime(reduceTime(getTime(measurementUnit), amount, unit));
	}

	private static CompetitiveTime reduceTime(CompetitiveTime original,
			long amount, TimeUnit unit) {
		long remainingTime = original.getTime(measurementUnit);
		long remainingOvertime = original.getOvertime(measurementUnit);
		long timeBeingSubtracted = measurementUnit.convert(amount, unit);

		remainingTime = remainingTime - timeBeingSubtracted;
		if (remainingTime < 0) {
			remainingOvertime += remainingTime;
			remainingTime = 0;
		}

		if (remainingOvertime < 0) {
			remainingOvertime = 0;
		}
		return new CompetitiveTime(remainingOvertime, remainingTime,
				measurementUnit);
	}

	public CompetitiveTime getTimeAfter(long duration, TimeUnit unit) {
		return reduceTime(getTime(measurementUnit), duration, unit);
	}
}

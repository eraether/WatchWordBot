package com.raether.watchwordbot.ranking;

import javax.persistence.Embeddable;

import jskills.Rating;

@Embeddable
public class RatingValue {

	private double mean;
	private double standardDeviation;

	public RatingValue() {

	}

	public RatingValue(Rating ranking) {
		setMean(ranking.getMean());
		setStandardDeviation(ranking.getStandardDeviation());
	}

	public double getMean() {
		return mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	public void setStandardDeviation(double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public Rating createRating() {
		return new Rating(getMean(), getStandardDeviation());
	}

}

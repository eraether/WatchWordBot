package com.raether.watchwordbot.ranking;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import jskills.Rating;

@Entity
public class RatingValue {
	@Id
	@GeneratedValue
	private int id;
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

	public void update(Rating newRating) {
		setMean(newRating.getMean());
		setStandardDeviation(newRating.getStandardDeviation());
	}

}

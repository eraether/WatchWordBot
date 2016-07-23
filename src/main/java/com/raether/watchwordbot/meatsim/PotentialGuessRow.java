package com.raether.watchwordbot.meatsim;

public class PotentialGuessRow {
	private String clue;
	private String card;
	private double certainty;
	private double weightedCertainty;

	public PotentialGuessRow(String clue, String card, double certainty,
			double weightedCertainty) {
		this.clue = clue;
		this.card = card;
		this.certainty = certainty;
		this.weightedCertainty = weightedCertainty;
	}

	public String getClue() {
		return this.clue;
	}

	public String getCard() {
		return this.card;
	}

	public double getCertainty() {
		return this.certainty;
	}

	public double getWeightedCertainty() {
		return this.weightedCertainty;
	}
}
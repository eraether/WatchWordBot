package com.raether.watchwordbot.meatsim;

import java.util.ArrayList;
import java.util.List;

public class PotentialGuess {
	private String card;
	private List<PotentialGuessRow> positiveGuessResults;
	private List<PotentialGuessRow> negativeGuessResults;

	public PotentialGuess(String card,
			List<PotentialGuessRow> positiveGuessResults,
			List<PotentialGuessRow> negativeGuessResults) {
		this.card = card;
		this.positiveGuessResults = positiveGuessResults;
		this.negativeGuessResults = negativeGuessResults;
	}

	public List<PotentialGuessRow> getAllGuessRows() {
		List<PotentialGuessRow> allGuessRows = new ArrayList<PotentialGuessRow>();
		allGuessRows.addAll(getPositiveGuessResults());
		allGuessRows.addAll(getNegativeGuessResults());
		return allGuessRows;
	}

	public List<PotentialGuessRow> getPositiveGuessResults() {
		return this.positiveGuessResults;
	}

	public List<PotentialGuessRow> getNegativeGuessResults() {
		return this.negativeGuessResults;
	}

	public String getWord() {
		return this.card;
	}

	public double getWeightedCertainty() {
		double totalWeight = 0;
		for (PotentialGuessRow row : getAllGuessRows()) {
			totalWeight += row.getWeightedCertainty();
		}
		return totalWeight;
	}

	public void rethinkGuess(double positiveExponentScale,
			double negativeExponentScale) {
		for (PotentialGuessRow positiveGuessRow : getPositiveGuessResults()) {
			positiveGuessRow.setWeightedCertainty(Math.pow(
					positiveGuessRow.getWeightedCertainty(),
					positiveExponentScale));
		}
		for (PotentialGuessRow negativeGuessRow : getNegativeGuessResults()) {
			negativeGuessRow.setWeightedCertainty(Math.pow(
					negativeGuessRow.getWeightedCertainty(),
					negativeExponentScale));
		}
	}
}
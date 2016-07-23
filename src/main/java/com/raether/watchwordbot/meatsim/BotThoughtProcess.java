package com.raether.watchwordbot.meatsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BotThoughtProcess {
	private List<PotentialGuess> guessResults;
	private DesiredBotAction action;

	public BotThoughtProcess(DesiredBotAction action) {
		this(action, new ArrayList<PotentialGuess>());
	}

	public BotThoughtProcess(DesiredBotAction action,
			List<PotentialGuess> guessResults) {
		this.guessResults = guessResults;
		this.action = action;
	}

	public List<PotentialGuess> getSortedGuessList() {
		List<PotentialGuess> allGuesses = new ArrayList<PotentialGuess>();
		allGuesses.addAll(guessResults);
		Collections.sort(allGuesses, new Comparator<PotentialGuess>() {

			@Override
			public int compare(PotentialGuess o1, PotentialGuess o2) {
				Double certainty1 = o1.getWeightedCertainty();
				Double certainty2 = o2.getWeightedCertainty();
				return certainty1.compareTo(certainty2);
			}
		});
		return allGuesses;
	}

	public PotentialGuess getBestGuess() {
		List<PotentialGuess> guessResults = getSortedGuessList();
		return guessResults.get(guessResults.size() - 1);
	}

	public DesiredBotAction getDesiredAction() {
		return this.action;
	}
}
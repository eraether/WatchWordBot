package com.raether.watchwordbot.meatsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.raether.watchwordbot.Faction;
import com.raether.watchwordbot.TurnOrder;
import com.raether.watchwordbot.WatchWordClue;
import com.raether.watchwordbot.WatchWordGrid;
import com.raether.watchwordbot.WordTile;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

public class AIPlayer {
	private static Optional<ILexicalDatabase> singletonDB = Optional.absent();

	public AIPlayer() {

	}

	public static Optional<ILexicalDatabase> getDatabase() {
		if (singletonDB.isPresent()) {
			return singletonDB;
		}
		try {
			singletonDB = Optional.of(new NictWordNet());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return singletonDB;
	}

	public static boolean canBeCreated() {
		return getDatabase().isPresent();
	}

	private void log(String s) {
		System.out.println("AIPlayer:" + s);
	}

	/*
	 * @currentFactionClues - Clues in order from oldest to newest
	 * 
	 * @otherFactionClues - Clues in order from oldest to newest
	 */

	public BotThoughtProcess makeGuess(List<WatchWordClue> currentFactionClues,
			List<WatchWordClue> otherFactionClues, WatchWordGrid grid,
			TurnOrder order) {
		int remainingTilesForCurrentFaction = grid
				.getUnrevealedTilesForFaction(order.getCurrentTurn()).size();
		int remainingTilesForOpponents = 0;
		for (Faction opponent : order.getAllTurnsExceptCurrent()) {
			remainingTilesForOpponents += grid.getUnrevealedTilesForFaction(
					opponent).size();
		}

		List<WordTile> allUnrevealedTiles = grid.getUnrevealedTiles();
		List<String> unrevealedWords = new ArrayList<String>();
		for (WordTile tile : allUnrevealedTiles) {
			unrevealedWords.add(tile.getWord());
		}
		List<String> positiveClues = new ArrayList<String>();
		int danglingPositiveGuessCount = 0;
		List<String> negativeClues = new ArrayList<String>();
		int danglingNegativeGuessCount = 0;
		for (WatchWordClue clue : currentFactionClues) {
			positiveClues.add(clue.getWord());
			danglingPositiveGuessCount += clue.getAmount() - 1;
		}
		for (WatchWordClue clue : otherFactionClues) {
			negativeClues.add(clue.getWord());
			danglingNegativeGuessCount += clue.getAmount() - 1;
		}

		Collections.reverse(positiveClues);
		Collections.reverse(negativeClues);

		if (danglingPositiveGuessCount <= 0) {
			return new BotThoughtProcess(DesiredBotAction.END_TURN);
		}

		log("Attempting to make a guess.  Positive clues:" + positiveClues
				+ ", Negative clues:" + negativeClues
				+ ", Remaining Tiles For Us:" + remainingTilesForCurrentFaction
				+ ", Remaining Tiles for Them:" + remainingTilesForOpponents);

		WS4JConfiguration.getInstance().setMFS(false);
		RelatednessCalculator relatednessCalculator = new WuPalmer(
				getDatabase().get());

		double[][] similarityMatrixPositive = relatednessCalculator
				.getNormalizedSimilarityMatrix(
						positiveClues.toArray(new String[] {}),
						unrevealedWords.toArray(new String[] {}));
		log("Completed positive similarity matrix");
		double[][] similarityMatrixNegative = relatednessCalculator
				.getNormalizedSimilarityMatrix(
						negativeClues.toArray(new String[] {}),
						unrevealedWords.toArray(new String[] {}));
		log("Completed negative similarity matrix");

		List<PotentialGuess> allPotentialGuesses = new ArrayList<PotentialGuess>();
		for (int currentWordIndex = 0; currentWordIndex < unrevealedWords
				.size(); currentWordIndex++) {
			String currentWord = unrevealedWords.get(currentWordIndex);
			List<PotentialGuessRow> positiveGuessRows = new ArrayList<PotentialGuessRow>();
			for (int currentPositiveClueIndex = 0; currentPositiveClueIndex < positiveClues
					.size(); currentPositiveClueIndex++) {
				String currentClue = positiveClues
						.get(currentPositiveClueIndex);
				double computedConfidence = similarityMatrixPositive[currentPositiveClueIndex][currentWordIndex];
				double weightedConfidence = Math.pow(computedConfidence,
						currentPositiveClueIndex + 1);// x, x^2, x^3, etc...
				positiveGuessRows.add(new PotentialGuessRow(currentClue,
						currentWord, computedConfidence, weightedConfidence));
			}

			List<PotentialGuessRow> negativeGuessRows = new ArrayList<PotentialGuessRow>();
			for (int currentNegativeClueIndex = 0; currentNegativeClueIndex < negativeClues
					.size(); currentNegativeClueIndex++) {
				String currentClue = negativeClues
						.get(currentNegativeClueIndex);
				double computedConfidence = similarityMatrixNegative[currentNegativeClueIndex][currentWordIndex];
				double weightedConfidence = -1
						* Math.pow(computedConfidence,
								(currentNegativeClueIndex + 1) * 2);// -x^2,
																	// -x^4,
																	// -x^6,
																	// etc...
				negativeGuessRows.add(new PotentialGuessRow(currentClue,
						currentWord, computedConfidence, weightedConfidence));
			}

			PotentialGuess guess = new PotentialGuess(currentWord,
					positiveGuessRows, negativeGuessRows);
			allPotentialGuesses.add(guess);

		}

		log("Returning thought process.");

		return new BotThoughtProcess(DesiredBotAction.GUESS,
				allPotentialGuesses);
	}
}
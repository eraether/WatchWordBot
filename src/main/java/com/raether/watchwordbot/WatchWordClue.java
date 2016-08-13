package com.raether.watchwordbot;

public class WatchWordClue {
	private String word;
	private int amount;
	private boolean unlimitedGuesses;
	private boolean zeroClue;
	private int guessesMade = 0;

	public WatchWordClue(String word, int amount, boolean unlimitedGuesses,
			boolean zeroClue) {
		this.word = word;
		this.amount = amount;
		this.unlimitedGuesses = unlimitedGuesses;
		this.zeroClue = zeroClue;

		if (isUnlimited() || isZero()) {
			this.amount = 1000000;
		}
	}

	public String getWord() {
		return this.word;
	}

	public int getOriginalAmount() {
		return this.amount;
	}

	public int getRemainingGuesses() {
		return getOriginalAmount() - getGuessesMade();
	}

	public void setGuessesMade(int guessesMade) {
		this.guessesMade = guessesMade;
	}

	public int getGuessesMade() {
		return this.guessesMade;
	}

	public boolean isUnlimited() {
		return this.unlimitedGuesses;
	}

	public boolean isZero() {
		return this.zeroClue;
	}

	public boolean hasGuessed() {
		return getGuessesMade() != 0;
	}
}

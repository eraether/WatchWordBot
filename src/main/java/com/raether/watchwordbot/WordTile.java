package com.raether.watchwordbot;

public class WordTile {
	private String word;
	private Faction faction;
	private boolean revealed;

	public WordTile(String word, Faction name, boolean revealed) {
		this.word = word;
		this.faction = name;
		this.revealed = revealed;
	}

	public String getWord() {
		return this.word;
	}

	public boolean isRevealed() {
		return this.revealed;
	}

	public Faction getFaction() {
		return this.faction;
	}

	public void setRevealed(boolean revealed) {
		this.revealed = revealed;
	}

	@Override
	public String toString() {
		return getWord() + " " + getFaction().getName().substring(0, 1) + " "
				+ (isRevealed() ? "R" : "H");
	}
}
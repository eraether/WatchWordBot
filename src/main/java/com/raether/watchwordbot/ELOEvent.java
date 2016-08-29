package com.raether.watchwordbot;

public enum ELOEvent {
	GUESSER_SENT_PLAINTEXT_MESSAGE(1), CLUE_GIVEN(8), CLUE_GUESSED(8);

	private int weight;

	private ELOEvent(int weight) {
		this.weight = weight;
	}

	public int getWeight() {
		return weight;
	}
}

package com.raether.watchwordbot;

public class Player {
	private boolean isAIControlled;

	public Player() {
		this(false);
	}

	public Player(boolean isAIControlled) {
		this.isAIControlled = isAIControlled;
	}

	public boolean isAIControlled() {
		return this.isAIControlled;
	}
}
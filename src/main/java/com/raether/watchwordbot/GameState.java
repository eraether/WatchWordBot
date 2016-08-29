package com.raether.watchwordbot;

public enum GameState {
	IDLE, LOBBY, GAME;

	public static GameState[] getAllStates() {
		return GameState.values();
	}
}
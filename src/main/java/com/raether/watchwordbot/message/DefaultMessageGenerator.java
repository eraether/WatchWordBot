package com.raether.watchwordbot.message;

public class DefaultMessageGenerator implements MessageGenerator {
	
	public String getGameStartMessage() {
		return "Starting the game...";
	}
	
	public String getCorrectPickMessage() {
		return "Nice!";
	}

	public String getIncorrectPickMessage() {
		return "Dang!";
	}

	public String getAssassinPickMessage() {
		return "Ouch!";
	}

	public String getWinMessage() {
		return "Game over!";
	}
}

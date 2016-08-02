package com.raether.watchwordbot.message;

public interface MessageGenerator {
	public String getCorrectPickMessage();
	public String getIncorrectPickMessage();
	public String getAssassinPickMessage();
	public String getWinMessage();
}

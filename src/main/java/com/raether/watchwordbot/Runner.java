package com.raether.watchwordbot;

public class Runner {

	public static void main(String[] args) {
		WatchWordBot bot = new WatchWordBot(
				"xoxb-47186420693-Ham8g4SWPiR8CSPYgMEhlujK");
		try {
			bot.loadWordList();
			bot.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package com.raether.watchwordbot;

public class Runner {

	public static void main(String[] args) {
		if(args.length == 0){
			System.err.println("Please pass in your slack token code as an argument.");
			System.exit(1);
		}
		WatchWordBot bot = new WatchWordBot(args[0]);
		try {
			bot.loadWordList();
			bot.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

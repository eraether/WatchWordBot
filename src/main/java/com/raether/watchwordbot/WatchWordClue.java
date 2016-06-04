package com.raether.watchwordbot;

public class WatchWordClue {
	private String word;
	private int amount;

	public WatchWordClue(String word, int amount) {
		this.word = word;
		this.amount = amount;
	}
	
	public String getWord(){
		return this.word;
	}
	
	public int getAmount(){
		return this.amount;
	}
}

package com.raether.watchwordbot.lex;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

public class BanishedWord {
	@Id
	@GeneratedValue
	private int id;

	public String banishedWord;

	public BanishedWord(String word) {
		setBanishedWord(word);
	}

	public void setBanishedWord(String word) {
		this.banishedWord = word;
	}

	public String getBanishedWord() {
		return this.banishedWord;
	}
}

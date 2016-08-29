package com.raether.watchwordbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

public class WordGenerator {

	private List<String> wordList = new ArrayList<String>();
	private List<String> banishedWordList = new ArrayList<String>();

	public WordGenerator() {
	}

	public void loadWordList() throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("wordlist.txt");
		System.out.println(in);
		List<String> words = IOUtils.readLines(in, "UTF-8");
		System.out.println(words.size());
		Set<String> uniqueWords = new TreeSet<String>();
		uniqueWords.addAll(words);
		wordList = new ArrayList<String>();
		for (String uniqueWord : uniqueWords) {
			String formattedUniqueWord = uniqueWord.replaceAll("[\\s]+", "_");
			if (!formattedUniqueWord.equals(uniqueWord)) {
				System.out.println("Transformed " + uniqueWord + "=>"
						+ formattedUniqueWord);
			}
			wordList.add(formattedUniqueWord);
		}
	}

	public List<String> generateWords(int count, Random random) {
		Set<String> words = new HashSet<String>();
		while (words.size() < count) {
			words.add(getWatchWord(random));
			words.removeAll(banishedWordList);
		}
		List<String> outputWords = new ArrayList<String>();
		outputWords.addAll(words);
		return outputWords;
	}

	private String getWatchWord(Random random) {
		return wordList.get((int) (wordList.size() * random.nextDouble()));
	}

	public boolean isWordPresent(String targetWord) {
		for (String word : wordList) {
			if (word.equalsIgnoreCase(targetWord)) {
				return true;
			}
		}
		return false;
	}

}

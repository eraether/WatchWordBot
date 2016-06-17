package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class BuildableWatchWordGrid {
	private List<String> words;
	private List<Faction> owners;
	private int width;
	private int height;

	BuildableWatchWordGrid(List<String> words, int width, int height) {
		setWords(words);
		this.width = width;
		this.height = height;
	}

	private BuildableWatchWordGrid setWords(List<String> words) {
		this.words = words;
		this.owners = new ArrayList<>();
		for (int x = 0; x < words.size(); x++) {
			this.owners.add(null);
		}
		return this;
	}

	void randomlyAssign(Faction faction, int count,
			Random rand) {
		List<Integer> unassignedTileIndicies = getUnassignedTileIndicies();
		for (int i = 0; i < count; i++) {
			int randomIndex = rand.nextInt(unassignedTileIndicies.size());
			int index = unassignedTileIndicies.get(randomIndex);
			unassignedTileIndicies.remove(randomIndex);
			owners.set(index, faction);
		}
	}

	private List<Integer> getUnassignedTileIndicies() {
		List<Integer> ints = new ArrayList<>();
		for (int x = 0; x < owners.size(); x++) {
			if (owners.get(x) == null) {
				ints.add(x);
			}
		}

		return ints;
	}

	BuildableWatchWordGrid fillRemainder(Faction faction) {
		for (Integer index : getUnassignedTileIndicies()) {
			owners.set(index, faction);
		}
		return this;
	}

	WatchWordGrid build() {
		List<WordTile> wordTiles = new ArrayList<WordTile>();

		for (int x = 0; x < words.size(); x++) {
			String word = words.get(x);
			Faction faction = owners.get(x);
			WordTile tile = new WordTile(word, faction, false);
			wordTiles.add(tile);
		}

		return new WatchWordGrid(wordTiles, this.width, this.height);
	}
}
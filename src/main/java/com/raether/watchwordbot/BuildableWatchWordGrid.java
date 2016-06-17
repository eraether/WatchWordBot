package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BuildableWatchWordGrid {
	private List<String> words;
	private List<Faction> owners;
	private int width;
	private int height;

	public BuildableWatchWordGrid(List<String> words, int width, int height) {
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

	public void randomlyAssign(Faction faction, int count,
			Random rand) {

		List<Integer> unassignedTileIndices = getUnassignedTileIndices();
		if (unassignedTileIndices.size() < count) {
			fillRemainder(faction);
		}

		for (int i = 0; i < count; i++) {
			int randomIndex = rand.nextInt(unassignedTileIndices.size());
			int index = unassignedTileIndices.get(randomIndex);
			unassignedTileIndices.remove(randomIndex);
			owners.set(index, faction);
		}
	}

	private List<Integer> getUnassignedTileIndices() {
		List<Integer> ints = new ArrayList<>();
		for (int x = 0; x < owners.size(); x++) {
			if (owners.get(x) == null) {
				ints.add(x);
			}
		}

		return ints;
	}

	public BuildableWatchWordGrid fillRemainder(Faction faction) {
		for (Integer index : getUnassignedTileIndices()) {
			owners.set(index, faction);
		}
		return this;
	}

	public WatchWordGrid build() {
		List<WordTile> wordTiles = new ArrayList<>();

		for (int x = 0; x < words.size(); x++) {
			String word = words.get(x);
			Faction faction = owners.get(x);
			WordTile tile = new WordTile(word, faction, false);
			wordTiles.add(tile);
		}

		return new WatchWordGrid(wordTiles, this.width, this.height);
	}
}
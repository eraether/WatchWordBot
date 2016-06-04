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
		this.owners = new ArrayList<Faction>();
		for (int x = 0; x < words.size(); x++) {
			this.owners.add(null);
		}
		return this;
	}

	public BuildableWatchWordGrid randomlyAssign(Faction faction, int count,
			Random rand) {
		List<Integer> unassignedTileIndicies = getUnassignedTileIndicies();

		if (unassignedTileIndicies.size() < count) {
			return fillRemainder(faction);
		}

		int assignedIndicies = 0;
		while (assignedIndicies++ < count) {
			int index = unassignedTileIndicies
					.get((int) (unassignedTileIndicies.size() * rand
							.nextDouble()));
			owners.set(index, faction);
		}

		return this;
	}

	private List<Integer> getUnassignedTileIndicies() {
		List<Integer> ints = new ArrayList<Integer>();
		for (int x = 0; x < owners.size(); x++) {
			if (owners.get(x) == null) {
				ints.add(x);
			}
		}

		return ints;
	}

	public BuildableWatchWordGrid fillRemainder(Faction faction) {
		for (Integer index : getUnassignedTileIndicies()) {
			owners.set(index, faction);
		}
		return this;
	}

	public WatchWordGrid build() {
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
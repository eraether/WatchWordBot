package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.List;

public class WatchWordGrid {
	private List<WordTile> words;
	private int width;
	private int height;

	public WatchWordGrid(List<WordTile> words, int width, int height) {
		this.words = words;
		this.width = width;
		this.height = height;
	}

	public WordTile getTileAt(int row, int col) {
		int index = row * getWidth() + col;
		if (index < 0 || index >= words.size()) {
			return null;
		}

		return words.get(index);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public List<WordTile> getRevealedTilesForFaction(Faction faction) {
		List<WordTile> tiles = getTilesForFaction(faction);
		tiles.removeAll(getUnrevealedTilesForFaction(faction));
		return tiles;
	}

	public List<WordTile> getUnrevealedTilesForFaction(Faction faction) {
		List<WordTile> allWordTilesForFaction = getTilesForFaction(faction);
		List<WordTile> unrevealedTiles = new ArrayList<WordTile>();
		for (WordTile tile : allWordTilesForFaction) {
			if (tile.isRevealed() == false) {
				unrevealedTiles.add(tile);
			}
		}
		return unrevealedTiles;
	}

	public List<WordTile> getTilesForFaction(Faction faction) {
		List<WordTile> tiles = new ArrayList<WordTile>();
		for (WordTile tile : this.words) {
			if (tile.getFaction().equals(faction)) {
				tiles.add(tile);
			}
		}
		return tiles;
	}

	public List<WordTile> getTilesForWord(String wordBeingGuessed) {
		List<WordTile> exactMatchingTiles = new ArrayList<WordTile>();
		List<WordTile> partialMatchingTiles = new ArrayList<WordTile>();
		for (WordTile tile : this.words) {
			if(tile.isRevealed())
			{
				continue;
			}
			
			if (tile.getWord().equals(wordBeingGuessed)) {
				exactMatchingTiles.add(tile);
			}

			if (tile.getWord().toLowerCase()
					.startsWith(wordBeingGuessed.toLowerCase())) {
				partialMatchingTiles.add(tile);
			}
		}

		if (!exactMatchingTiles.isEmpty()) {
			return exactMatchingTiles;
		}

		return partialMatchingTiles;
	}

	public List<String> getAllWords() {
		List<String> words = new ArrayList<String>();
		for(WordTile tile : this.words){
			words.add(tile.getWord());
		}
		return words;
	}
}
package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class BuildableWatchWordGrid {
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

class TurnOrder {
	private List<Faction> allFactions = new ArrayList<Faction>();
	private int currentTurn = 0;

	public TurnOrder() {

	}

	public TurnOrder(List<Faction> factions) {
		this.allFactions = factions;
	}

	public List<Faction> getAllFactions() {
		return allFactions;
	}

	public void addFaction(Faction faction) {
		recomputeTurnOrder();
		allFactions.add(faction);
	}

	public void removeFaction(Faction faction) {
		recomputeTurnOrder();
		allFactions.remove(faction);
	}

	public Faction getCurrentTurn() {
		return getFactionAtTurn(currentTurn);
	}

	public Faction getNextTurn() {
		return getFactionAtTurn(currentTurn + 1);
	}

	private void recomputeTurnOrder() {
		List<Faction> simplifiedTurnOrder = new ArrayList<Faction>();
		for (int x = 0; x < allFactions.size(); x++) {
			int turn = currentTurn + x;
			simplifiedTurnOrder.add(getFactionAtTurn(turn));
		}

		this.allFactions = simplifiedTurnOrder;
		this.currentTurn = 0;
	}

	private Integer getTurnForFaction(Faction faction) {
		for (int x = currentTurn; x < currentTurn + allFactions.size(); x++) {
			if (getFactionAtTurn(x) == faction) {
				return x;
			}
		}
		return null;
	}

	private Faction getFactionAtTurn(int turn) {
		int circularTurn = turn % allFactions.size();
		return allFactions.get(circularTurn);
	}

	public void shuffle(Random rand) {
		Collections.shuffle(allFactions, rand);
	}

	public void nextTurn() {
		currentTurn += 1;
	}

	public Faction getLeastFullFaction() {
		if (allFactions.isEmpty()) {
			return null;
		}

		Faction leastFullFaction = allFactions.get(0);
		for (Faction faction : allFactions) {
			if (faction.getAllPlayers().size() < leastFullFaction
					.getAllPlayers().size()) {
				leastFullFaction = faction;
			}
		}
		return leastFullFaction;
	}

	public boolean removePlayer(Player player) {
		Faction faction = getFactionFor(player);
		if (faction == null) {
			return false;
		}
		return faction.removePlayer(player);
	}

	public Faction getFactionFor(Player player) {
		for (Faction faction : allFactions) {
			if (faction.getAllPlayers().contains(player)) {
				return faction;
			}
		}
		return null;
	}

	public Faction swapFactions(Player player) {
		Integer currentFactionIndex = getTurnForFaction(getFactionFor(player));
		if (currentFactionIndex == null) {
			return null;
		}

		Faction currentFaction = getFactionAtTurn(currentFactionIndex);
		Faction newFaction = getFactionAtTurn(currentFactionIndex + 1);

		currentFaction.removePlayer(player);
		newFaction.addPlayer(player);
		return newFaction;
	}

	public boolean swapPlayers(Player firstPlayer, Player secondPlayer) {
		Faction firstPlayerFaction = getFactionFor(firstPlayer);
		Faction secondPlayerFaction = getFactionFor(secondPlayer);

		Integer firstPlayerIndex = firstPlayerFaction
				.getPlayerIndex(firstPlayer);
		Integer secondPlayerIndex = secondPlayerFaction
				.getPlayerIndex(secondPlayer);

		if (firstPlayerIndex == null || secondPlayerIndex == null) {
			return false;
		}

		firstPlayerFaction.getAllPlayers().set(firstPlayerIndex, secondPlayer);
		secondPlayerFaction.getAllPlayers().set(secondPlayerIndex, firstPlayer);

		return true;
	}

	public void shuffleFactionLeaders() {
		for (Faction faction : allFactions) {
			faction.shiftMembership();
		}
	}

	public Faction getFactionAfter(Faction faction) {
		return getFactionAtTurn(getTurnForFaction(faction)+1);
	}
}


class WatchWordGame {
	private WatchWordGrid grid;
	private TurnOrder turnOrder;
	private Faction neutralFaction;
	private Faction assassinFaction;
	private WatchWordClue currentClue;
	private int totalGuessesMadeThisTurn = 0;

	public WatchWordGame(WatchWordGrid grid, TurnOrder playerFactions,
			Faction neutralFaction, Faction assassinFaction) {
		this.grid = grid;
		this.turnOrder = playerFactions;
		this.neutralFaction = neutralFaction;
		this.assassinFaction = assassinFaction;
	}

	public WatchWordGrid getGrid() {
		return this.grid;
	}

	public TurnOrder getTurnOrder() {
		return turnOrder;
	}

	public Faction getNeutralFaction() {
		return this.neutralFaction;
	}

	public Faction getAssassinFaction() {
		return this.assassinFaction;
	}

	public Faction getFactionForPlayer(Player player) {
		for (Faction faction : turnOrder.getAllFactions()) {
			if (faction.getAllPlayers().contains(player)) {
				return faction;
			}
		}
		return null;
	}

	public void changeTurns() {
		getTurnOrder().nextTurn();
		this.currentClue = null;
		this.totalGuessesMadeThisTurn = 0;
	}

	public void removeFaction(Faction guesserFaction) {
		getTurnOrder().removeFaction(guesserFaction);
	}

	public boolean wasClueGivenThisTurn() {
		return this.currentClue != null;
	}

	public void giveClue(WatchWordClue clue) {
		this.currentClue = clue;
	}

	public WatchWordClue getClue() {
		return this.currentClue;
	}

	public void guess() {
		totalGuessesMadeThisTurn++;
	}

	public int getRemainingGuesses() {
		if (!wasClueGivenThisTurn())
			return 0;
		return getClue().getAmount() - totalGuessesMadeThisTurn;
	}
}

class WordTile {
	private String word;
	private Faction faction;
	private boolean revealed;

	public WordTile(String word, Faction name, boolean revealed) {
		this.word = word;
		this.faction = name;
		this.revealed = revealed;
	}

	public String getWord() {
		return this.word;
	}

	public boolean isRevealed() {
		return this.revealed;
	}

	public Faction getFaction() {
		return this.faction;
	}

	public void setRevealed(boolean revealed) {
		this.revealed = revealed;
	}

	@Override
	public String toString() {
		return getWord() + " " + getFaction().getName().substring(0, 1) + " "
				+ (isRevealed() ? "R" : "H");
	}
}

class Faction {
	private String factionName;
	// unique set
	private List<Player> players = new ArrayList<Player>();

	public Faction(String factionName) {
		this.factionName = factionName;
	}

	public void shiftMembership() {
		if (this.isEmpty()) {
			return;
		}
		Player lastPlayer = players.remove(players.size() - 1);
		players.add(0, lastPlayer);
	}

	public boolean isEmpty() {
		return players.isEmpty();
	}

	public Integer getPlayerIndex(Player firstPlayer) {
		for (int x = 0; x < players.size(); x++) {
			if (players.get(x) == firstPlayer) {
				return x;
			}
		}
		return null;
	}

	public Faction(String factionName, List<Player> players) {
		this(factionName);
		this.players = players;
	}

	public String getName() {
		return this.factionName;
	}

	public boolean hasLeader() {
		return getLeader() != null;
	}

	public Player getLeader() {
		if (players.isEmpty()) {
			return null;
		}

		return players.get(0);
	}

	public List<Player> getAllPlayers() {
		return this.players;
	}

	public boolean isLeader(Player player) {
		return hasLeader() && (player == getLeader());
	}

	public List<Player> getFollowers() {
		List<Player> followers = new ArrayList<Player>();
		followers.addAll(getAllPlayers());
		followers.remove(getLeader());
		return followers;
	}

	public boolean addPlayer(Player player) {
		if (players.contains(player)) {
			return false;
		}
		players.add(player);
		return true;
	}

	public boolean removePlayer(Player player) {
		return players.remove(player);
	}
}

class Player {

}
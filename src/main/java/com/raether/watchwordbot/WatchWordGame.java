package com.raether.watchwordbot;

public class WatchWordGame {
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
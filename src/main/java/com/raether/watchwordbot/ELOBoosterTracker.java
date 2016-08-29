package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class ELOBoosterTracker {
	private Map<Pair<Player, Faction>, Integer> weightMap = new HashMap<Pair<Player, Faction>, Integer>();

	public void addWeightedAction(ELOEvent event, Player player, Faction faction) {
		Pair<Player, Faction> pair = Pair.of(player, faction);

		if (!weightMap.containsKey(pair)) {
			weightMap.put(pair, 0);
		}
		weightMap.put(pair, weightMap.get(pair) + event.getWeight());
	}

	public int getWeight(Player player, Faction faction) {
		Pair<Player, Faction> pair = Pair.of(player, faction);
		if (!weightMap.containsKey(pair)) {
			return 0;
		}
		return weightMap.get(pair);
	}

	public Faction getPrimaryFactionForPlayer(Player player) {
		List<Faction> factions = getAllFactionsPlayerHasParticipatedIn(player);

		if (factions.isEmpty()) {
			return null;
		}

		int maxWeight = 0;
		Faction primaryFaction = null;

		for (Faction faction : factions) {
			int weight = this.getWeight(player, faction);
			if(faction.hasPlayer(player)){
				weight = weight *= 2;
			}
			if (weight > maxWeight) {
				maxWeight = weight;
				primaryFaction = faction;
			}
		}

		if (satisfiesParticipationCutoff(maxWeight)) {
			return primaryFaction;
		}
		return null;
	}

	public boolean satisfiesParticipationCutoff(int weight) {
		return weight >= ELOEvent.CLUE_GUESSED.getWeight();
	}

	public List<Player> getAllParticipatingPlayersFor(Faction faction) {
		List<Player> players = new ArrayList<Player>();
		for (Pair<Player, Faction> playerFactionPairs : weightMap.keySet()) {
			if (faction.equals(playerFactionPairs.getValue())) {
				players.add(playerFactionPairs.getKey());
			}
		}
		return players;
	}

	public List<Faction> getAllFactionsPlayerHasParticipatedIn(Player player) {
		List<Faction> factions = new ArrayList<Faction>();
		for (Pair<Player, Faction> playerFactionPairs : weightMap.keySet()) {
			if (player.equals(playerFactionPairs.getKey())) {
				factions.add(playerFactionPairs.getValue());
			}
		}
		return factions;
	}

	public List<Player> getCoreParticipantsFor(Faction faction) {
		List<Player> allParticipantsForFaction = getAllParticipatingPlayersFor(faction);
		List<Player> primaryParticipants = new ArrayList<Player>();
		for (Player player : allParticipantsForFaction) {
			if (faction.equals(getPrimaryFactionForPlayer(player))) {
				primaryParticipants.add(player);
			}
		}
		return primaryParticipants;
	}
}

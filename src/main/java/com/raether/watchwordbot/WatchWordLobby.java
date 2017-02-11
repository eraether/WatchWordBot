package com.raether.watchwordbot;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackUser;

public class WatchWordLobby {
	private SlackChannel channel;
	private TurnOrder turnOrder;
	private BidiMap<SlackUser, Player> playerMapping = new DualHashBidiMap<SlackUser, Player>();

	private double timerMultiplier = 1;

	public WatchWordLobby(SlackChannel channel, TurnOrder order) {
		this.channel = channel;
		this.turnOrder = order;
	}

	public SlackChannel getChannel() {
		return this.channel;
	}

	private Entry<SlackUser, Player> findEntryFor(SlackUser user) {
		for (Entry<SlackUser, Player> entry : playerMapping.entrySet()) {
			if (entry.getKey().getId().equals(user.getId())
					&& entry.getKey().getUserName().equals(user.getUserName())) {
				return entry;
			}
		}
		return null;
	}

	public boolean hasUser(SlackUser user) {
		return findEntryFor(user) != null;
	}

	public boolean addUser(SlackUser user) {
		return addUser(user, new Player());
	}

	public boolean addUser(SlackUser user, Player player) {
		if (hasUser(user)) {
			return false;
		}
		playerMapping.put(user, player);
		turnOrder.getLeastFullFaction().addPlayer(player);
		return true;
	}

	public void removeUser(SlackUser user) {
		Entry<SlackUser, Player> entry = findEntryFor(user);
		if (entry == null) {
			return;
		}
		playerMapping.remove(entry.getKey(), entry.getValue());
		turnOrder.removePlayer(entry.getValue());
	}

	public Set<SlackUser> getUsers() {
		return playerMapping.keySet();
	}

	public Player getPlayer(SlackUser user) {
		Entry<SlackUser, Player> entry = findEntryFor(user);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}

	public SlackUser getUser(Player player) {
		return playerMapping.getKey(player);
	}

	public TurnOrder getTurnOrder() {
		return this.turnOrder;
	}

	public void setTimerMultiplier(double multiplier) {
		this.timerMultiplier = multiplier;
	}

	public double getTimerMultiplier() {
		return this.timerMultiplier;
	}

	public int getAIPlayerCount() {
		int aiPlayerCount = 0;
		for (Faction faction : turnOrder.getAllFactions()) {
			for (Player player : faction.getAllPlayers()) {
				if (player.isAIControlled()) {
					aiPlayerCount += 1;
				}
			}
		}
		return aiPlayerCount;
	}

	public SlackUser findUserByUsernameInChannel(String username) {
		return findUserByUsername(username, getChannel().getMembers());
	}

	// first exact match, then lowercase match, then partial match, then
	// lowercase partial match
	public SlackUser findUserByUsername(String username) {
		return findUserByUsername(username, getUsers());
	}

	public static SlackUser findUserByUsername(String username,
			Collection<SlackUser> users) {
		List<Boolean> possibilities = Arrays.asList(false, true);
		for (boolean caseInsensitivity : possibilities) {
			for (boolean partialMatch : possibilities) {
				SlackUser user = findUserByUsername(username,
						caseInsensitivity, partialMatch, users);
				if (user != null) {
					return user;
				}

			}
		}
		return null;
	}

	public static SlackUser findUserByUsername(String targetUsername,
			boolean caseInsensitivity, boolean partialMatch,
			Collection<SlackUser> users) {
		for (SlackUser user : users) {
			String currentUsername = user.getUserName();
			if (caseInsensitivity) {
				targetUsername = targetUsername.toLowerCase();
				currentUsername = currentUsername.toLowerCase();
			}
			if (partialMatch) {
				if (currentUsername.startsWith(targetUsername)) {
					return user;
				}
			} else {
				if (currentUsername.equals(targetUsername)) {
					return user;
				}
			}
		}
		return null;
	}

	public SlackUser findUserById(String id) {
		for (SlackUser user : getUsers()) {
			if (user.getId().equals(id)) {
				return user;
			}
		}
		return null;
	}
}

package com.raether.watchwordbot.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.raether.watchwordbot.Faction;
import com.raether.watchwordbot.GameState;
import com.raether.watchwordbot.TurnOrder;
import com.raether.watchwordbot.WatchWordBot;
import com.raether.watchwordbot.WatchWordLobby;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.SlackPersona.SlackPresence;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class LobbyCommand extends Command {

	public LobbyCommand() {
		super("lobby", "create a new lobby for players to join", GameState.IDLE);
	}

	@Override
	public void run(WatchWordBot bot, SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {

		if (event.getChannel().isDirect()) {
			session.sendMessage(event.getChannel(),
					"Cannot start a game in a private message!");
			return;
		}
		session.sendMessage(event.getChannel(),
				"Reconnecting to slack service to refresh users, one sec...");
		bot.reconnect(bot.getSession());

		Faction redFaction = new Faction("Red", null);
		Faction blueFaction = new Faction("Blue", null);
		List<Faction> playerFactions = new ArrayList<Faction>();
		playerFactions.add(redFaction);
		playerFactions.add(blueFaction);
		TurnOrder turnOrder = new TurnOrder(playerFactions);

		WatchWordLobby watchWordLobby = new WatchWordLobby(event.getChannel(),
				turnOrder);

		boolean validStart = false;
		Set<SlackUser> users = new HashSet<SlackUser>();
		SlackUser lobbyStarter = bot.getLobby().findUserByUsernameInChannel(
				event.getSender().getUserName());
		if (lobbyStarter != null) {
			users.add(lobbyStarter);
		}
		if (args.isEmpty() || args.peek().equals("opt-in")) {
			validStart = true;
		} else if (args.peek().equals("opt-out")) {
			validStart = true;
			for (SlackUser user : watchWordLobby.getChannel().getMembers()) {
				SlackPresence presence = session.getPresence(user);
				if (user.isBot() == false && (presence == SlackPresence.ACTIVE)) {
					users.add(user);
				}
			}
		} else {
			bot.printUsage(event.getChannel(), "lobby [(opt-in), opt-out]");
			return;
		}
		if (validStart) {
			for (SlackUser user : users) {
				watchWordLobby.addUser(user);
			}
			bot.setLobby(watchWordLobby);
			bot.updateGameState(GameState.LOBBY);
			session.sendMessage(bot.getCurrentChannel(),
					"Created new WatchWord Lobby!");
			session.sendMessage(bot.getCurrentChannel(), bot.printFactions());
		}

	}
}

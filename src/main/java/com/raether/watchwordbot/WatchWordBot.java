package com.raether.watchwordbot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

public class WatchWordBot implements SlackMessagePostedListener {
	private String apiKey = "";
	private SlackSession slackSession;

	private WatchWordLobby watchWordLobby;
	private GameState currentGameState = GameState.IDLE;
	private List<String> wordList = null;
	private WatchWordGame game;

	public WatchWordBot(String apiKey) {
		setAPIKey(apiKey);
	}

	private void setAPIKey(String apiKey) {
		this.apiKey = apiKey;
	}

	private String getAPIKey() {
		return this.apiKey;
	}

	public void loadWordList() throws IOException {
		File f = new File("src/main/resources/wordlist.txt");
		System.out.println(f.getCanonicalPath());
		List<String> words = FileUtils.readLines(f, "UTF-8");
		System.out.println(words.size());
		Set<String> uniqueWords = new TreeSet<String>();
		uniqueWords.addAll(words);
		wordList = new ArrayList<String>();
		wordList.addAll(uniqueWords);
	}

	public void connect() throws IOException {
		SlackSession session = SlackSessionFactory
				.createWebSocketSlackSession(getAPIKey());
		session.connect();
		session.addMessagePostedListener(this);
		updateSession(session);
		beginHeartbeat();
	}

	public void beginHeartbeat() {
		ScheduledExecutorService scheduler = Executors
				.newScheduledThreadPool(1);
		final Runnable heartBeater = new Runnable() {
			@Override
			public void run() {
				onHeartBeat();
			}
		};
		final ScheduledFuture<?> heartBeatHandle = scheduler
				.scheduleAtFixedRate(heartBeater, 500, 500,
						TimeUnit.MILLISECONDS);
	}

	private void updateSession(SlackSession session) {
		this.slackSession = session;
	}

	private SlackSession getSession() {
		return this.slackSession;
	}

	@Override
	public void onEvent(SlackMessagePosted event, SlackSession session) {
		updateSession(session);
		if (event.getSender().getId().equals(session.sessionPersona().getId()))
			return;
		try {
			handleCommand(event, session);
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			e.printStackTrace(ps);
			String content;
			content = baos.toString();
			session.sendMessage(event.getChannel(), "Exception Encountered:\n"
					+ content);
		}
	}

	private synchronized void handleCommand(SlackMessagePosted event,
			SlackSession session) {
		LinkedList<String> args = new LinkedList<String>();
		args.addAll(Arrays.asList(event.getMessageContent().split("\\s+")));// event.getMessageContent().split(" ");

		String command = args.pop().toLowerCase();

		if (command.equals("lobby")) {
			if (currentGameState != GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), GameState.IDLE);
				return;
			}

			if (event.getChannel().isDirect()) {
				session.sendMessage(event.getChannel(),
						"Cannot start a game in a private message!");
				return;
			}

			Faction redFaction = new Faction("Red");
			Faction blueFaction = new Faction("Blue");
			List<Faction> playerFactions = new ArrayList<Faction>();
			playerFactions.add(redFaction);
			playerFactions.add(blueFaction);
			TurnOrder turnOrder = new TurnOrder(playerFactions);

			WatchWordLobby watchWordLobby = new WatchWordLobby(
					event.getChannel(), turnOrder);

			boolean validStart = false;
			Set<SlackUser> users = new HashSet<SlackUser>();
			users.add(event.getSender());
			if (args.isEmpty() || args.peek().equals("opt-out")) {
				validStart = true;
				for (SlackUser user : watchWordLobby.getChannel().getMembers()) {
					if (user.isBot() == false) {
						users.add(user);
					}
				}
			} else if (args.peek().equals("opt-in")) {
				validStart = true;
			} else {
				printUsage(event.getChannel(), "lobby [(opt-out), opt-in]");
				return;
			}
			if (validStart) {
				for (SlackUser user : users) {
					watchWordLobby.addUser(user);
				}
				this.watchWordLobby = watchWordLobby;
				this.currentGameState = GameState.LOBBY;
				session.sendMessage(watchWordLobby.getChannel(),
						"Created new WatchWord Lobby!");
				session.sendMessage(event.getChannel(),
						printFactions(getWatchWordLobby()));
			}
		} else if (command.equals("cancel")) {
			if (currentGameState == GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), new GameState[] {
						GameState.LOBBY, GameState.GAME });
				return;
			}
			session.sendMessage(
					getCurrentChannel(),
					"Game has been canceled by "
							+ getUsernameString(event.getSender()));
			resetGame();
		} else if (command.equals("sync")) {
			session.sendMessage(event.getChannel(), "Synchronization test...");
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			session.sendMessage(event.getChannel(), "Done!");
		} else if (command.equals("list")) {
			if (currentGameState == GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), new GameState[] {
						GameState.LOBBY, GameState.GAME });
				return;
			}
			session.sendMessage(event.getChannel(),
					printFactions(getWatchWordLobby()));
		} else if (command.equals("join")) {
			if (currentGameState == GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), new GameState[] {
						GameState.LOBBY, GameState.GAME });
				return;
			}
			if (!getWatchWordLobby().hasUser(event.getSender())) {
				getWatchWordLobby().addUser(event.getSender());
				session.sendMessage(getCurrentChannel(),
						getUsernameString(event.getSender())
								+ " has joined the game!");
				session.sendMessage(event.getChannel(),
						printFactions(getWatchWordLobby()));

			} else {
				session.sendMessage(event.getChannel(),
						getUsernameString(event.getSender())
								+ ", you're already in the game!");
			}
		} else if (command.equals("swap")) {
			if (currentGameState == GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), new GameState[] {
						GameState.LOBBY, GameState.GAME });
				return;
			}
			if (getWatchWordLobby().getPlayer(event.getSender()) == null) {
				session.sendMessage(event.getChannel(),
						getUsernameString(event.getSender())
								+ ", you are currently not in this game!");
				return;
			}

			Player player = getWatchWordLobby().getPlayer(event.getSender());

			if (args.isEmpty()) {
				Faction currentFaction = getWatchWordLobby().getTurnOrder()
						.getFactionFor(player);
				Faction newFaction = getWatchWordLobby().getTurnOrder()
						.swapFactions(player);

				session.sendMessage(getCurrentChannel(),
						getUsernameString(event.getSender()) + " swapped from "
								+ currentFaction.getName() + " to "
								+ newFaction.getName() + ".");
			} else {
				SlackUser firstUser = null;
				SlackUser secondUser = null;
				if (args.size() == 2) {
					firstUser = findUserByUsername(args.pop(),
							session.getUsers());
					secondUser = findUserByUsername(args.pop(),
							session.getUsers());
				} else {
					printUsage(
							event.getChannel(),
							"swap - swap yourself to the other team\nswap <player1> <player2> swap two players");
					return;
				}

				if (firstUser == null || secondUser == null) {
					session.sendMessage(event.getChannel(),
							"Could not find user(s) with those names.");
					return;
				}

				Player firstPlayer = getWatchWordLobby().getPlayer(firstUser);
				Player secondPlayer = getWatchWordLobby().getPlayer(secondUser);

				if (firstPlayer == null || secondPlayer == null) {
					session.sendMessage(event.getChannel(),
							"One or more of those users are not currently in the game.");
					return;
				}

				getWatchWordLobby().getTurnOrder().swapPlayers(firstPlayer,
						secondPlayer);
				session.sendMessage(getCurrentChannel(),
						getUsernameString(event.getSender()) + " has swapped "
								+ getUsernameString(firstUser) + " with "
								+ getUsernameString(secondUser));
			}
			session.sendMessage(event.getChannel(),
					printFactions(getWatchWordLobby()));

		}

		else if (command.equals("add")) {
			if (currentGameState == GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), new GameState[] {
						GameState.LOBBY, GameState.GAME });
				return;
			}
			if (args.isEmpty()) {
				printUsage(event.getChannel(), "add <player1, player2, ...>");
				return;
			}
			while (!args.isEmpty()) {
				String username = args.pop();
				SlackUser user = findUserByUsername(username,
						session.getUsers());
				if (user != null) {
					if (!getWatchWordLobby().hasUser(user)) {
						getWatchWordLobby().addUser(user);
						session.sendMessage(getCurrentChannel(), event
								.getSender().getUserName()
								+ " added "
								+ getUsernameString(user));
					} else {
						session.sendMessage(event.getChannel(),
								getUsernameString(user)
										+ " is already in the game!");
					}
				} else {
					session.sendMessage(event.getChannel(),
							"Could not find user with username '" + username
									+ "'.");
				}
			}
			session.sendMessage(getCurrentChannel(),
					printFactions(getWatchWordLobby()));
		} else if (command.equals("kick") || command.equals("remove")) {
			if (currentGameState == GameState.IDLE) {
				printIncorrectGameState(event.getChannel(), new GameState[] {
						GameState.LOBBY, GameState.GAME });
				return;
			}
			if (args.isEmpty()) {
				printUsage(event.getChannel(),
						"[kick|remove] <player1, player2, ...>");
				return;
			}
			while (!args.isEmpty()) {
				String username = args.pop();
				SlackUser user = findUserByUsername(username,
						getWatchWordLobby().getUsers());
				if (user != null) {
					getWatchWordLobby().removeUser(user);
					session.sendMessage(getCurrentChannel(), event.getSender()
							.getUserName()
							+ " removed "
							+ getUsernameString(user));
				} else {
					session.sendMessage(getCurrentChannel(),
							"Could not find user already in game with username '"
									+ username + "'.");
				}
			}
			session.sendMessage(watchWordLobby.getChannel(),
					printFactions(getWatchWordLobby()));
		}

		else if (command.equals("start")) {
			if (currentGameState != GameState.LOBBY) {
				printIncorrectGameState(event.getChannel(), GameState.LOBBY);
				return;
			}
			currentGameState = GameState.GAME;
			session.sendMessage(getCurrentChannel(), "Starting the game...");
			long seed1 = System.nanoTime();
			Random random1 = new Random(seed1);
			int totalRows = 2;
			int totalCols = 2;
			List<String> words = generateWords(wordList, totalRows * totalCols,
					random1);

			BuildableWatchWordGrid buildableGrid = new BuildableWatchWordGrid(
					words, totalRows, totalCols);

			TurnOrder turnOrder = this.watchWordLobby.getTurnOrder();
			turnOrder.shuffle(random1);
			Faction neutralFaction = new Faction("Neutral");
			Faction assassinFaction = new Faction("Assassin");

			int firstFactionCards = 1;
			int secondFactionCards = 1;
			int assassinCards = 1;

			buildableGrid.randomlyAssign(turnOrder.getCurrentTurn(),
					firstFactionCards, random1);
			buildableGrid.randomlyAssign(turnOrder.getNextTurn(),
					secondFactionCards, random1);
			buildableGrid.randomlyAssign(assassinFaction, assassinCards,
					random1);
			buildableGrid.fillRemainder(neutralFaction);
			WatchWordGrid grid = buildableGrid.build();
			WatchWordGame game = new WatchWordGame(grid, turnOrder,
					neutralFaction, assassinFaction);

			this.game = game;

			session.sendMessage(getCurrentChannel(), printCardGrid());
			session.sendMessage(getCurrentChannel(),
					printFactions(watchWordLobby));
			session.sendMessage(getCurrentChannel(), printCurrentTurn());
		} else if (command.equals("guess")) {
			if (currentGameState != GameState.GAME) {
				printIncorrectGameState(event.getChannel(), GameState.GAME);
				return;
			}
			if (getWatchWordLobby().getPlayer(event.getSender()) == null) {
				session.sendMessage(event.getChannel(),
						getUsernameString(event.getSender())
								+ ", you are currently not in this game!");
				return;
			}
			if (args.isEmpty()) {
				printUsage(event.getChannel(), "guess <word>");
				return;
			}
			String wordBeingGuessed = args.pop();
			List<WordTile> results = this.game.getGrid().getTilesForWord(
					wordBeingGuessed);
			if (results.isEmpty()) {
				session.sendMessage(event.getChannel(),
						"Could not find word that matches '" + wordBeingGuessed
								+ "'");
				return;
			} else if (results.size() > 2) {
				List<String> words = new ArrayList<String>();
				for (WordTile tile : results) {
					words.add(tile.getWord());
				}
				session.sendMessage(event.getChannel(),
						"Found multiple words matching '" + wordBeingGuessed
								+ "': " + StringUtils.join(words, ", ") + "'");
				return;
			} else {
				WordTile guessedTile = results.get(0);
				Faction guesserFaction = game
						.getFactionForPlayer(watchWordLobby.getPlayer(event
								.getSender()));

				if (this.game.getTurnOrder().getCurrentTurn() != guesserFaction) {
					session.sendMessage(
							event.getChannel(),
							getUsernameString(event.getSender())
									+ ", it is not currently your turn to guess! (You are "
									+ guesserFaction.getName()
									+ " team, it is "
									+ game.getTurnOrder().getCurrentTurn()
											.getName() + "team's turn.)");
					session.sendMessage(event.getChannel(),
							"However, I'll allow it for now...");// return;
				} else if (!game.wasClueGivenThisTurn()) {
					session.sendMessage(
							event.getChannel(),
							getUsernameString(event.getSender())
									+ ", hold your horses!  A clue has not been given yet!");
					session.sendMessage(event.getChannel(),
							"However, I'll allow it for now...");// return;
				}

				session.sendMessage(getCurrentChannel(),
						getUsernameString(event.getSender()) + " has guessed "
								+ results.get(0).getWord() + "!");

				guessedTile.setRevealed(true);

				Faction victor = null;
				boolean pickedAssassin = false;
				boolean pickedOwnCard = false;
				boolean changeTurns = false;

				if (guessedTile.getFaction().equals(game.getAssassinFaction())) {
					game.removeFaction(guesserFaction);
					pickedAssassin = true;
				}

				if (guesserFaction == guessedTile.getFaction()) {
					pickedOwnCard = true;
				} else {
					changeTurns = true;
				}

				if (game.getRemainingGuesses() == 0) {
					changeTurns = true;
				}

				// decide if there's a winner due to no more cards
				for (Faction faction : game.getTurnOrder().getAllFactions()) {
					if (game.getGrid().getUnrevealedTilesForFaction(faction)
							.isEmpty()) {
						victor = faction;
					}
				}

				// remaining team is the winner
				if (game.getTurnOrder().getAllFactions().size() == 1) {
					victor = game.getTurnOrder().getAllFactions().get(0);
				}

				if (pickedAssassin) {
					session.sendMessage(getCurrentChannel(),
							"Ouch! " + getUsernameString(event.getSender())
									+ " has picked the assassin!  "
									+ guesserFaction.getName() + " loses!");
				} else if (pickedOwnCard) {
					session.sendMessage(getCurrentChannel(), "Nice! "
							+ getUsernameString(event.getSender())
							+ " has picked correctly.");
				} else {
					session.sendMessage(getCurrentChannel(), "Dang! "
							+ getUsernameString(event.getSender())
							+ " has picked a "
							+ guessedTile.getFaction().getName() + " card.");
				}

				if (victor != null) {
					String victorString = "";
					for (Player player : victor.getAllPlayers()) {
						victorString += getUsernameString(getWatchWordLobby()
								.getUser(player)) + "\n";
					}
					session.sendMessage(getCurrentChannel(), "Game over!  "
							+ victor.getName()
							+ " has won!  Congradulations to:\n" + victorString);
				}

				if (victor != null) {
					finishGame();
					return;
				}

				if (changeTurns) {
					session.sendMessage(getCurrentChannel(),
							"Out of guesses!  Changing turns.");
					game.changeTurns();
				}

				session.sendMessage(getCurrentChannel(), printCardGrid());
				session.sendMessage(getCurrentChannel(), printClueGiven());
				session.sendMessage(getCurrentChannel(), printCurrentTurn());
			}
		}
	}

	public WatchWordLobby getWatchWordLobby() {
		return this.watchWordLobby;
	}

	private String printCurrentTurn() {
		String out = "It is currently "
				+ game.getTurnOrder().getCurrentTurn().getName() + "'s turn.";
		return out;
	}

	private String printClueGiven() {
		if (game.wasClueGivenThisTurn()) {
			return "A clue has been given: " + game.getClue();
		} else {
			return "A clue has not yet been given.  "
					+ getUsernameString(getWatchWordLobby().getUser(
							game.getTurnOrder().getCurrentTurn().getLeader()))
					+ ", please provide a clue.";
		}
	}

	private static String printFactions(WatchWordLobby lobby) {
		String out = "Here are the teams:\n";
		for (Faction faction : lobby.getTurnOrder().getAllFactions()) {
			String factionString = "[*" + faction.getName() + " team*]\n";
			for (Player player : faction.getAllPlayers()) {
				factionString += getUsernameString(lobby.getUser(player));
				if (faction.isLeader(player)) {
					factionString += " (Leader)";
				}
				factionString += "\n";
			}
			out += factionString;
		}
		return out;
	}

	// need to send pms to leaders with full grid...
	private String printCardGrid() {
		int longestWordLength = 0;
		for (String word : this.wordList) {
			longestWordLength = Math.max(longestWordLength, word.length());
		}
		String out = "The WatchWords grid so far:\n```";
		for (int row = 0; row < game.getGrid().getHeight(); row++) {
			String line = "";
			for (int col = 0; col < game.getGrid().getWidth(); col++) {
				WordTile tile = game.getGrid().getTileAt(row, col);
				String tileString = StringUtils.capitalize(tile.getWord());

				if (tile.isRevealed()) {
					tileString = "<" + tile.getFaction().getName() + ">";
				}
				tileString = "[ "
						+ StringUtils.rightPad(tileString, longestWordLength)
						+ " ]";
				line += tileString;
			}
			line += "\n";
			out += line;
		}
		out += "```";
		return out;
	}

	private void finishGame() {
		this.currentGameState = GameState.LOBBY;
		this.game = null;
		getWatchWordLobby().getTurnOrder().shuffleFactionLeaders();
		getSession().sendMessage(getCurrentChannel(),
				printFactions(getWatchWordLobby()));
	}

	private void resetGame() {
		this.currentGameState = GameState.IDLE;
		this.watchWordLobby = null;
		this.game = null;
	}

	private static List<String> generateWords(List<String> wordList,
			int amount, Random rand) {
		Set<String> words = new HashSet<String>();
		while (words.size() < amount) {
			words.add(getWatchWord(wordList, rand));
		}
		List<String> outputWords = new ArrayList<String>();
		outputWords.addAll(words);
		return outputWords;
	}

	// alters players list
	// private static TurnOrder distributePlayers(Collection<Player> players,
	// List<String> factionNames, Random rand) {
	//
	// List<List<Player>> factionDistribution = new ArrayList<List<Player>>();
	// for (int x = 0; x < factionNames.size(); x++) {
	// factionDistribution.add(new ArrayList<Player>());
	// }
	//
	// List<Player> playersList = new ArrayList<Player>();
	// playersList.addAll(players);
	// Collections.shuffle(playersList, rand);
	//
	// for (int x = 0; x < playersList.size(); x++) {
	// List<Player> faction = factionDistribution.get(x
	// % factionDistribution.size());
	// faction.add(playersList.get(x));
	// }
	//
	// List<Faction> factions = new ArrayList<Faction>();
	// for (int x = 0; x < factionNames.size(); x++) {
	// List<Player> factionPlayers = factionDistribution.get(x);
	// Player leader = null;
	// if (factionPlayers.isEmpty() == false) {
	// leader = factionPlayers.get(0);
	// }
	// factions.add(new Faction(factionNames.get(x), leader,
	// factionPlayers));
	// }
	// TurnOrder turnOrder = new TurnOrder(factions);
	// return turnOrder;
	// }

	private static String getWatchWord(List<String> wordList, Random random) {
		return wordList.get((int) (wordList.size() * random.nextDouble()));
	}

	private static String getUsernameString(SlackUser user, boolean verbose) {
		if (user == null) {
			return "<No User>";
		}
		if (verbose) {
			return user.getUserName() + " (" + user.getRealName() + ")";
		} else {
			return user.getUserName();
		}
	}

	private static String getUsernameString(SlackUser user) {
		return getUsernameString(user, false);
	}

	// first exact match, then lowercase match, then partial match, then
	// lowercase partial match
	private static SlackUser findUserByUsername(String username,
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

	private static SlackUser findUserByUsername(String targetUsername,
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

	private void printUsage(SlackChannel channel, String str) {
		this.getSession().sendMessage(channel, "Usage: " + str);
	}

	// private void printPlayers(SlackChannel channel) {
	// String output = watchWordLobby.getUsers().size()
	// + " Player(s) currently in lobby:\n";
	// for (SlackUser user : watchWordLobby.getUsers()) {
	// output += user.getUserName() + " (" + user.getRealName() + " | "
	// + getSession().getPresence(user) + "\n";
	// }
	// this.getSession().sendMessage(channel, output);
	//
	// }

	private void printIncorrectGameState(SlackChannel channel,
			GameState correctState) {
		printIncorrectGameState(channel, new GameState[] { correctState });
	}

	private void printIncorrectGameState(SlackChannel channel,
			GameState[] gameStates) {

		this.getSession().sendMessage(
				channel,
				"This command can only be used during the "
						+ Arrays.toString(gameStates)
						+ " state.  It is currently the "
						+ this.currentGameState + " state.");

	}

	private SlackChannel getCurrentChannel() {
		if (this.watchWordLobby == null) {
			return null;
		}
		return this.watchWordLobby.getChannel();
	}

	private void onHeartBeat() {
	}
}

enum GameState {
	IDLE, LOBBY, GAME
}

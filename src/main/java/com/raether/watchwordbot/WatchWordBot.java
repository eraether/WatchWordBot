package com.raether.watchwordbot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.raether.watchwordbot.lex.LexicalDatabaseHelper;
import com.raether.watchwordbot.meatsim.AIPlayer;
import com.raether.watchwordbot.meatsim.AISlackPlayer;
import com.raether.watchwordbot.meatsim.BotThoughtProcess;
import com.raether.watchwordbot.meatsim.DesiredBotAction;
import com.raether.watchwordbot.meatsim.PotentialGuess;
import com.raether.watchwordbot.meatsim.PotentialGuessRow;
import com.raether.watchwordbot.ranking.RatingHelper;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPersona.SlackPresence;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

import edu.cmu.lti.jawjaw.db.SynsetDefDAO;
import edu.cmu.lti.jawjaw.pobj.Lang;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.jawjaw.pobj.Synset;
import edu.cmu.lti.jawjaw.pobj.SynsetDef;
import edu.cmu.lti.jawjaw.util.WordNetUtil;
import edu.cmu.lti.lexical_db.ILexicalDatabase;

public class WatchWordBot implements SlackMessagePostedListener {
	private String apiKey = "";
	private SlackSession slackSession;

	private WatchWordLobby watchWordLobby;
	private GameState currentGameState = GameState.IDLE;
	private List<String> wordList = null;
	private WatchWordGame game;
	private List<Thread> aiThreads = new ArrayList<Thread>();
	private Optional<SessionFactory> sessionFactory;

	private static Boolean DEBUG = Boolean.FALSE;

	public WatchWordBot(String apiKey, Optional<SessionFactory> sessionFactory) {
		setAPIKey(apiKey);
		setSessionFactory(sessionFactory);
	}

	private void setSessionFactory(Optional<SessionFactory> sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public Optional<SessionFactory> getSessionFactory() {
		return sessionFactory;
	}

	private void setAPIKey(String apiKey) {
		this.apiKey = apiKey;
	}

	private String getAPIKey() {
		return this.apiKey;
	}

	public void loadWordList() throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("wordlist.txt");
		System.out.println(in);
		List<String> words = IOUtils.readLines(in, "UTF-8");
		System.out.println(words.size());
		Set<String> uniqueWords = new TreeSet<String>();
		uniqueWords.addAll(words);
		wordList = new ArrayList<String>();
		for (String uniqueWord : uniqueWords) {
			String formattedUniqueWord = uniqueWord.replaceAll("[\\s]+", "_");
			if (!formattedUniqueWord.equals(uniqueWord)) {
				System.out.println("Transformed " + uniqueWord + "=>"
						+ formattedUniqueWord);
			}
			wordList.add(formattedUniqueWord);
		}
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
				onHeartBeat(getSession());
			}
		};
		// final ScheduledFuture<?> heartBeatHandle =
		scheduler.scheduleAtFixedRate(heartBeater, 1000, 1000,
				TimeUnit.MILLISECONDS);
	}

	private void reconnect(SlackSession session) {
		try {
			session.disconnect();
		} catch (Exception e) {

		} finally {
			try {
				session.connect();
			} catch (Exception e) {

			}
		}
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

		String commandText = args.pop().toLowerCase();

		List<Command> commands = generateCommands(event, args, session);

		for (Command command : commands) {
			if (command.matches(commandText)) {
				if (!isGameCurrentlyInValidGameState(event.getChannel(),
						command.getValidGameStates())) {
					return;
				}
				command.run();
				break;
			}
		}
	}

	private List<Command> generateCommands(final SlackMessagePosted event,
			final LinkedList<String> args, final SlackSession session) {
		List<Command> commands = new ArrayList<Command>();

		commands.add(new Command("lobby",
				"create a new lobby for players to join", GameState.IDLE) {
			@Override
			public void run() {
				lobbyCommand(event, args, session);
			}
		});

		commands.add(new Command("define", "define a word", GameState.LOBBY,
				GameState.GAME) {
			@Override
			public void run() {
				defineCommand(event, args, session);
			}

		});

		commands.add(new Command("bot", Arrays.asList("bots"),
				"add a bot to the game", false, GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				botCommand(event, args, session);
			}
		});

		commands.add(new Command("cancel", "cancel the game and lobby",
				GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				cancelCommand(event, args, session);
			}
		});

		commands.add(new Command("list", "show all the players in the game",
				GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				listCommand(event, args, session);
			}
		});

		commands.add(new Command("grid", Arrays.asList("board"),
				"show the WatchWords grid", false, GameState.GAME) {
			@Override
			public boolean matches(String command) {
				return super.matches(command) || command.matches("g+r+i+d+");
			}

			@Override
			public void run() {
				gridCommand(event, args, session);
			}
		});

		commands.add(new Command("join", "join the game", GameState.LOBBY,
				GameState.GAME) {
			@Override
			public void run() {
				joinCommand(event, args, session);
			}
		});

		commands.add(new Command("swap", "switch teams or swap two players",
				GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				swapCommand(event, args, session);
			}
		});

		commands.add(new Command("add",
				"add the specified players to the game.", GameState.LOBBY,
				GameState.GAME) {
			@Override
			public void run() {
				addCommand(event, args, session);
			}
		});

		commands.add(new Command("time", "shows the remaining time",
				GameState.GAME) {
			@Override
			public void run() {
				timeCommand(event, args, session);
			}
		});

		commands.add(new Command("kick", Arrays.asList("remove"),
				"removes the specified player from the game.", false,
				GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				kickCommand(event, args, session);
			}
		});

		commands.add(new Command("start", "starts the game", GameState.LOBBY) {
			@Override
			public void run() {
				startCommand(event, args, session);
			}
		});

		commands.add(new Command("clue", "give a clue to your team",
				GameState.GAME) {
			@Override
			public void run() {
				clueCommand(event, args, session);
			}
		});

		commands.add(new Command("guess", "guess a word", GameState.GAME) {
			@Override
			public void run() {
				guessCommand(event, args, session);
			}
		});

		if (DEBUG) {
			commands.add(new Command("win", "win", true, GameState.GAME) {
				@Override
				public void run() {
					winCommand(event, args, session);
				}
			});
		}

		commands.add(new Command("end", Arrays.asList("pass"),
				"end the guessing phase", false, GameState.GAME) {
			@Override
			public void run() {
				endCommand(event, args, session);
			}
		});

		commands.add(new Command("sync", "synchronization test", true,
				GameState.IDLE, GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				syncCommand(event, args, session);
			}
		});

		commands.add(new Command("help", "yes, this is help", true,
				GameState.IDLE, GameState.LOBBY, GameState.GAME) {
			@Override
			public void run() {
				helpCommand(event, args, session);
			}
		});

		return commands;
	}

	private void defineCommand(SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (!LexicalDatabaseHelper.canBeCreated()) {
			session.sendMessage(event.getChannel(),
					"Lexical database link could not be established.");
			return;
		}

		if (args.size() != 1) {
			printUsage(event.getChannel(), "define [word]");
			return;
		}
		String word = args.pop();

		String messageOutput = "Defining *" + word + "*:\n";
		for (POS pos : POS.values()) {
			List<edu.cmu.lti.jawjaw.pobj.Synset> synsets = WordNetUtil
					.wordToSynsets(word, pos);
			for (Synset synset : synsets) {
				SynsetDef def = SynsetDefDAO.findSynsetDefBySynsetAndLang(
						synset.getSynset(), Lang.eng);
				messageOutput += def.getSynset() + ":" + def.getDef() + "\n";
			}
		}
		session.sendMessage(event.getChannel(), messageOutput);
	}

	private void winCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		finishGame(Arrays.asList(game.getTurnOrder().getCurrentTurn()), session);
	}

	private void botCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {

		int numberOfBots = 1;
		if (!args.isEmpty()) {
			try {
				numberOfBots = Integer.parseInt(args.pop());
				if (numberOfBots <= 0) {
					throw new IllegalArgumentException(
							"Number of bots must be > 0");
				}
			} catch (Exception e) {
				printUsage(event.getChannel(), "bot [# of desired bots]");
				return;
			}
		}
		boolean addedBots = false;
		for (int addedBotCount = 0; addedBotCount < numberOfBots; addedBotCount++) {
			int maxAIPlayers = 4;
			int totalAIPlayers = this.getWatchWordLobby().getAIPlayerCount();
			if (totalAIPlayers >= maxAIPlayers) {
				session.sendMessage(event.getChannel(),
						"Can't add any more bots!  You have reached the bot limit ("
								+ maxAIPlayers + " bots)");
				break;
			}

			if (!AIPlayer.canBeCreated()) {
				session.sendMessage(event.getChannel(),
						"Sorry, bots cannot be added due to an environmental error.");
				break;
			}

			AISlackPlayer slackPlayer = new AISlackPlayer("Sim"
					+ (totalAIPlayers + 1));
			Player aiPlayer = new Player(true);
			getWatchWordLobby().addUser(slackPlayer, aiPlayer);
			addedBots = true;
			session.sendMessage(getCurrentChannel(),
					getUsernameString(slackPlayer) + " has joined the game!");

		}
		if (addedBots) {
			session.sendMessage(getCurrentChannel(),
					printFactions(getWatchWordLobby()));
			if (!isAIThreadRunning()) {
				handleAIGuesses();
			}
		}
	}

	private void helpCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		List<Command> commands = generateCommands(event, args, session);

		List<Command> filteredCommands = new ArrayList<Command>();
		for (Command command : commands) {
			if (command.getValidGameStates().contains(this.currentGameState)
					&& !command.isHidden()) {
				filteredCommands.add(command);
			}
		}

		String totalHelpText = "Contextual Help\n";
		for (Command command : filteredCommands) {
			String helpText = "*" + command.getPrimaryAlias() + "*";
			if (command.hasAdditionalAliases()) {
				helpText += " (" + command.getAdditionalAliases() + ")";
			}
			helpText += " = " + command.getHelpText() + "\n";
			totalHelpText += helpText;
		}

		session.sendMessage(event.getChannel(), totalHelpText);
	}

	private void lobbyCommand(SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		if (event.getChannel().isDirect()) {
			session.sendMessage(event.getChannel(),
					"Cannot start a game in a private message!");
			return;
		}
		session.sendMessage(event.getChannel(),
				"Reconnecting to slack service to refresh users, one sec...");
		reconnect(getSession());

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
		SlackUser lobbyStarter = findUserByUsername(event.getSender().getId(),
				watchWordLobby.getChannel().getMembers());
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
			printUsage(event.getChannel(), "lobby [(opt-in), opt-out]");
			return;
		}
		if (validStart) {
			for (SlackUser user : users) {
				watchWordLobby.addUser(user);
			}
			this.watchWordLobby = watchWordLobby;
			this.currentGameState = GameState.LOBBY;
			session.sendMessage(getCurrentChannel(),
					"Created new WatchWord Lobby!");
			session.sendMessage(getCurrentChannel(),
					printFactions(getWatchWordLobby()));
		}
	}

	private void cancelCommand(SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
		session.sendMessage(getCurrentChannel(), "Game has been canceled by "
				+ getUsernameString(event.getSender()));
		resetGame();
	}

	private void syncCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {

		session.sendMessage(getCurrentChannel(), "Synchronization test...");
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		session.sendMessage(getCurrentChannel(), "Done!");
	}

	private void listCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		session.sendMessage(event.getChannel(),
				printFactions(getWatchWordLobby()));
	}

	private void gridCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		session.sendMessage(event.getChannel(), printCardGrid());
	}

	private void joinCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		if (!getWatchWordLobby().hasUser(event.getSender())) {
			getWatchWordLobby().addUser(event.getSender());
			session.sendMessage(getCurrentChannel(),
					getUsernameString(event.getSender())
							+ " has joined the game!");
			session.sendMessage(getCurrentChannel(),
					printFactions(getWatchWordLobby()));

		} else {
			session.sendMessage(event.getChannel(),
					getUsernameString(event.getSender())
							+ ", you're already in the game!");
		}
	}

	private void swapCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
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

			session.sendMessage(
					getCurrentChannel(),
					getUsernameString(event.getSender()) + " swapped from "
							+ currentFaction.getName() + " to "
							+ newFaction.getName() + ".");
		} else {
			SlackUser firstUser = null;
			SlackUser secondUser = null;
			if (args.size() == 2) {
				firstUser = findUserByUsername(args.pop(), getWatchWordLobby()
						.getUsers());
				secondUser = findUserByUsername(args.pop(), getWatchWordLobby()
						.getUsers());
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

	private void addCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		if (args.isEmpty()) {
			printUsage(event.getChannel(), "add <player1, player2, ...>");
			return;
		}
		while (!args.isEmpty()) {
			String username = args.pop();
			SlackUser user = findUserByUsername(username, session.getUsers());
			if (user != null) {
				if (!getWatchWordLobby().hasUser(user)) {
					getWatchWordLobby().addUser(user);
					session.sendMessage(getCurrentChannel(), event.getSender()
							.getUserName()
							+ " added "
							+ getUsernameString(user));
				} else {
					session.sendMessage(event.getChannel(),
							getUsernameString(user)
									+ " is already in the game!");
				}
			} else {
				session.sendMessage(event.getChannel(),
						"Could not find user with username '" + username + "'.");
			}
		}
		session.sendMessage(getCurrentChannel(),
				printFactions(getWatchWordLobby()));
	}

	private void timeCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		if (game.getActingFaction() == null) {
			session.sendMessage(event.getChannel(),
					"Game is currently in an invalid state, there is currently no acting faction!");
			return;
		}
		CompetitiveTime time = game.getRemainingTime();
		if (time == null) {
			session.sendMessage(event.getChannel(),
					"There is currently no timer enabled for the "
							+ game.getActingFaction().getName() + " team.");
			return;
		}
		session.sendMessage(event.getChannel(),
				"Remaining time for the " + game.getActingFaction().getName()
						+ " team: " + time.getTime(TimeUnit.SECONDS)
						+ " secs. (" + time.getOvertime(TimeUnit.SECONDS)
						+ " secs. of overtime)");
	}

	private void kickCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		if (args.isEmpty()) {
			printUsage(event.getChannel(),
					"[kick|remove] <player1, player2, ...>");
			return;
		}
		while (!args.isEmpty()) {
			String username = args.pop();
			SlackUser user = findUserByUsername(username, getWatchWordLobby()
					.getUsers());
			if (user != null) {
				getWatchWordLobby().removeUser(user);
				session.sendMessage(getCurrentChannel(), event.getSender()
						.getUserName() + " removed " + getUsernameString(user));
			} else {
				session.sendMessage(getCurrentChannel(),
						"Could not find user already in game with username '"
								+ username + "'.");
			}
		}
		session.sendMessage(watchWordLobby.getChannel(),
				printFactions(getWatchWordLobby()));
	}

	private void guessCommand(SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {
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
		makeGuess(wordBeingGuessed, session, event.getChannel(),
				event.getSender());
	}

	private synchronized void makeGuess(String wordBeingGuessed,
			SlackSession session, SlackChannel eventChannel, SlackUser eventUser) {
		List<WordTile> results = this.game.getGrid().getTilesForWord(
				wordBeingGuessed);
		if (results.isEmpty()) {
			session.sendMessage(eventChannel,
					"Could not find word that matches '" + wordBeingGuessed
							+ "'");
			return;
		} else if (results.size() > 2) {
			List<String> words = new ArrayList<String>();
			for (WordTile tile : results) {
				words.add(tile.getWord());
			}
			session.sendMessage(eventChannel, "Found multiple words matching '"
					+ wordBeingGuessed + "': " + StringUtils.join(words, ", ")
					+ "'");
			return;
		} else {
			WordTile guessedTile = results.get(0);
			Player guesser = getWatchWordLobby().getPlayer(eventUser);
			Faction guesserFaction = game.getFactionForPlayer(guesser);

			if (this.game.getTurnOrder().getCurrentTurn() != guesserFaction) {
				session.sendMessage(
						eventChannel,
						getUsernameString(eventUser)
								+ ", it is not currently your turn to guess! (You are on the "
								+ guesserFaction.getName()
								+ " team, it is currently the "
								+ game.getTurnOrder().getCurrentTurn()
										.getName() + " team's turn.)");
				if (DEBUG) {
					session.sendMessage(eventChannel,
							"However, I'll allow it for now...");
				} else {
					return;
				}
			} else if (guesser == guesserFaction.getLeader()) {
				session.sendMessage(eventChannel, getUsernameString(eventUser)
						+ ", you're the word smith.  You can't guess!");
				if (DEBUG) {
					session.sendMessage(eventChannel,
							"However, I'll allow it for now...");
				} else {
					return;
				}
			} else if (!game.wasClueGivenThisTurn()) {
				session.sendMessage(eventChannel, getUsernameString(eventUser)
						+ ", hold your horses!  A clue has not been given yet!");
				return;
			}

			session.sendMessage(getCurrentChannel(),
					getUsernameString(eventUser) + " has guessed *"
							+ results.get(0).getWord() + "*!");

			game.guess();
			guessedTile.setRevealed(true);

			Faction victor = null;
			boolean pickedAssassin = false;
			boolean pickedOwnCard = false;
			boolean changeTurns = false;

			if (guessedTile.getFaction().equals(game.getAssassinFaction())) {
				victor = game.getTurnOrder().getFactionAfter(guesserFaction);
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
						"Ouch! " + getUsernameString(eventUser)
								+ " has picked the assassin!  "
								+ guesserFaction.getName() + " loses!");
			} else if (pickedOwnCard) {
				session.sendMessage(getCurrentChannel(), "Nice! "
						+ getUsernameString(eventUser)
						+ " has picked correctly.");
			} else {
				session.sendMessage(getCurrentChannel(), "Dang! "
						+ getUsernameString(eventUser) + " has picked a *"
						+ guessedTile.getFaction().getName() + "* card.");
			}

			if (victor != null) {
				finishGame(Arrays.asList(victor), session);
				return;
			}

			if (changeTurns) {
				session.sendMessage(getCurrentChannel(),
						"Out of guesses!  Changing turns.");
				game.changeTurns();
				waitForClue();
			} else {
				waitForGuess();
			}

			session.sendMessage(getCurrentChannel(), printCardGrid());
			session.sendMessage(getCurrentChannel(), printCurrentTurn());
			session.sendMessage(getCurrentChannel(), printGivenClue());
			for (Faction faction : game.getTurnOrder().getAllFactions()) {
				if (faction.hasLeader()) {
					if (!faction.getLeader().isAIControlled()) {
						session.sendMessageToUser(this.watchWordLobby
								.getUser(faction.getLeader()),
								printCardGrid(true), null);
					}
				}
			}
		}
	}

	private void endCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		endTurn(session, event.getChannel(), event.getSender());
	}

	private void endTurn(SlackSession session, SlackChannel eventChannel,
			SlackUser eventUser) {
		if (getWatchWordLobby().getPlayer(eventUser) == null) {
			session.sendMessage(eventChannel, getUsernameString(eventUser)
					+ ", you are currently not in this game!");
			return;
		}
		Player guesser = getWatchWordLobby().getPlayer(eventUser);
		Faction guesserFaction = game.getFactionForPlayer(guesser);

		if (this.game.getTurnOrder().getCurrentTurn() != guesserFaction) {
			session.sendMessage(eventChannel, "It is not your turn to end!");
			if (DEBUG) {
				session.sendMessage(eventChannel,
						"However, I'll allow it for now...");
			} else {
				return;
			}
		} else if (guesser == guesserFaction.getLeader()) {
			session.sendMessage(
					eventChannel,
					getUsernameString(eventUser)
							+ ", you're the wordsmith.  You can't decide when your team ends their turn!");
			if (DEBUG) {
				session.sendMessage(eventChannel,
						"However, I'll allow it for now...");
			} else {
				return;
			}
		} else if (!game.wasClueGivenThisTurn()) {
			session.sendMessage(eventChannel, getUsernameString(eventUser)
					+ ", hold your horses!  A clue has not been given yet!");
			return;
		} else if (game.getRemainingGuesses() == game.getClue().getAmount()) {
			session.sendMessage(eventChannel,
					"You must make at least one guess before you can end your turn!");
			return;
		}
		game.changeTurns();
		session.sendMessage(getCurrentChannel(), getUsernameString(eventUser)
				+ " has ended the turn.");
		session.sendMessage(getCurrentChannel(), printCardGrid());
		session.sendMessage(getCurrentChannel(), printCurrentTurn());
		session.sendMessage(getCurrentChannel(), printGivenClue());
		waitForClue();
	}

	private void clueCommand(SlackMessagePosted event, LinkedList<String> args,
			SlackSession session) {
		Player player = getWatchWordLobby().getPlayer(event.getSender());
		if (player == null) {
			session.sendMessage(event.getChannel(), "You are not in the game!");
			return;
		}

		if (getWatchWordLobby().getTurnOrder().getFactionFor(player) != game
				.getTurnOrder().getCurrentTurn()) {
			session.sendMessage(event.getChannel(),
					"It is not your turn to give a clue!");
			if (DEBUG) {
				session.sendMessage(event.getChannel(),
						"However, I'll allow it for now...");
			} else {
				return;
			}
		}

		if (player != game.getTurnOrder().getCurrentTurn().getLeader()) {
			session.sendMessage(event.getChannel(),
					"Only the current turn's leader can give a clue.");
			if (DEBUG) {
				session.sendMessage(event.getChannel(),
						"However, I'll allow it for now...");
			} else {
				return;
			}
		}
		if (game.getClue() != null) {
			session.sendMessage(event.getChannel(), "A clue was already given.");
			session.sendMessage(event.getChannel(), printGivenClue());
			return;
		}

		if (args.size() != 2) {
			printUsage(event.getChannel(), "clue <word> [amount|unlimited]");
			return;
		}

		String word = args.pop();
		String unparsedNumber = args.pop();
		int totalGuesses = 0;
		if (unparsedNumber.toLowerCase().startsWith("unlimited")) {
			totalGuesses = 1000000;
		} else {
			try {
				totalGuesses = Integer.parseInt(unparsedNumber);
			} catch (Exception e) {
				session.sendMessage(event.getChannel(), "Could not parse '"
						+ unparsedNumber + "' as a number.");
				return;
			}
			;
		}

		if (totalGuesses < 1) {
			session.sendMessage(event.getChannel(),
					"You must give a clue with at least one guess!");
			return;
		}

		List<WordTile> tiles = game.getGrid().getTilesForWord(word);
		if (!tiles.isEmpty()) {
			session.sendMessage(event.getChannel(),
					"Your clue matches tiles on the board!  Please give a different clue.");
			return;
		}

		totalGuesses = totalGuesses + 1;// include bonus
		game.giveClue(new WatchWordClue(word, totalGuesses));
		session.sendMessage(getCurrentChannel(),
				getUsernameString(event.getSender()) + " has given a clue.");
		session.sendMessage(getCurrentChannel(), printGivenClue());
		session.sendMessage(getCurrentChannel(), printCardGrid());
		waitForGuess();
	}

	private void startCommand(SlackMessagePosted event,
			LinkedList<String> args, SlackSession session) {

		if (args.size() == 1) {
			if (args.pop().equalsIgnoreCase("debug")) {
				DEBUG = Boolean.TRUE;
			} else {
				DEBUG = Boolean.FALSE;
			}
		}

		currentGameState = GameState.GAME;
		session.sendMessage(getCurrentChannel(), "Starting the game...");
		long seed1 = System.nanoTime();
		Random random1 = new Random(seed1);
		int totalRows = 5;
		int totalCols = 5;
		List<String> words = generateWords(wordList, totalRows * totalCols,
				random1);

		BuildableWatchWordGrid buildableGrid = new BuildableWatchWordGrid(
				words, totalRows, totalCols);

		TurnOrder turnOrder = this.watchWordLobby.getTurnOrder();
		turnOrder.shuffle(random1);
		for (Faction faction : turnOrder.getAllFactions()) {

			long overtimeDuration = 10;
			TimeUnit overtimeTimeUnit = TimeUnit.MINUTES;
			faction.setCompetitiveTimer(new CompetitiveTimer(overtimeDuration,
					overtimeTimeUnit));
		}

		Faction neutralFaction = new Faction("Neutral", null);
		Faction assassinFaction = new Faction("Assassin", null);

		int firstFactionCards = 9;
		int secondFactionCards = 8;
		int assassinCards = 1;

		buildableGrid.randomlyAssign(turnOrder.getCurrentTurn(),
				firstFactionCards, random1);
		buildableGrid.randomlyAssign(turnOrder.getNextTurn(),
				secondFactionCards, random1);
		buildableGrid.randomlyAssign(assassinFaction, assassinCards, random1);
		buildableGrid.fillRemainder(neutralFaction);
		WatchWordGrid grid = buildableGrid.build();
		WatchWordGame game = new WatchWordGame(grid, turnOrder, neutralFaction,
				assassinFaction);

		this.game = game;

		session.sendMessage(getCurrentChannel(), printCardGrid());
		session.sendMessage(getCurrentChannel(), printFactions(watchWordLobby));
		session.sendMessage(getCurrentChannel(), printCurrentTurn());
		session.sendMessage(getCurrentChannel(), printGivenClue());

		for (Faction faction : game.getTurnOrder().getAllFactions()) {
			if (faction.hasLeader() && !faction.getLeader().isAIControlled()) {
				SlackUser user = getWatchWordLobby().getUser(
						faction.getLeader());

				SlackUser opponent = getWatchWordLobby().getUser(
						game.getTurnOrder().getFactionAfter(faction)
								.getLeader());
				try {
					session.sendMessageToUser(
							user,
							printWordSmithInstructions(user, opponent, faction,
									game.getAssassinFaction()), null);
					session.sendMessageToUser(user, printCardGrid(true), null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		printMatchQuality();
		waitForClue();
	}

	private boolean isGameCurrentlyInValidGameState(SlackChannel channel,
			List<GameState> validStates) {

		GameState currentState = this.currentGameState;
		if (validStates.contains(currentState)) {
			return true;
		}
		printIncorrectGameState(channel, validStates);
		return false;
	}

	private void waitForClue() {
		stopAllAIThreads();
		this.game.startCountingDown(5, TimeUnit.MINUTES);
	}

	private void waitForGuess() {
		stopAllAIThreads();
		this.game.startCountingDown(3, TimeUnit.MINUTES);
		handleAIGuesses();
	}

	private boolean isAIThreadRunning() {
		return !this.aiThreads.isEmpty();
	}

	private void stopAllAIThreads() {
		for (Thread t : this.aiThreads) {
			try {
				t.interrupt();
			} catch (Exception e) {
				e.printStackTrace();
			}
			;
		}
		this.aiThreads.clear();
	}

	private void queueAIThread(Thread t) {
		this.aiThreads.add(t);
		t.start();
	}

	private void handleAIGuesses() {
		if (this.game == null)
			return;

		Player aiGuesseeOnCurrentTeam = null;
		for (Player follower : this.game.getActingFaction().getFollowers()) {
			if (follower.isAIControlled()) {
				aiGuesseeOnCurrentTeam = follower;
				break;
			}
		}

		if (aiGuesseeOnCurrentTeam == null) {
			return;
		}
		final SlackUser aiSlackUser = getWatchWordLobby().getUser(
				aiGuesseeOnCurrentTeam);
		sendMessageToCurrentChannel("The AI (" + getUsernameString(aiSlackUser)
				+ ") is attempting to guess using your provided clue!");

		final List<WatchWordClue> currentFactionClues = this.game
				.getAllCluesForFaction(game.getActingFaction());

		final List<WatchWordClue> otherFactionClues = new ArrayList<WatchWordClue>();
		for (Faction faction : getGame().getTurnOrder()
				.getAllTurnsExceptCurrent()) {
			otherFactionClues.addAll(getGame().getAllCluesForFaction(faction));
		}

		queueAIThread(new Thread() {
			@Override
			public void run() {
				sendMessageToCurrentChannel("Sleepin' a bit to give humans a tiny chance to act...");
				try {
					Thread.sleep(5000);
				} catch (Exception e) {

				}
				AIPlayer player = new AIPlayer();
				BotThoughtProcess thoughtProcess = player.makeGuess(
						currentFactionClues, otherFactionClues, getGame()
								.getGrid(), getGame().getTurnOrder());
				if (thoughtProcess.getDesiredAction() == DesiredBotAction.END_TURN) {
					endTurn(getSession(), getCurrentChannel(), aiSlackUser);
				} else if (thoughtProcess.getDesiredAction() == DesiredBotAction.GUESS) {

					List<WordTile> tiles = game.getGrid().getTilesForWord(
							thoughtProcess.getBestGuess().getWord());

					// makes using bots a bit more fun, as they no longer guess
					// the assassin with as high of a probability. Eventually
					// this can be removed once bots are a bit better
					for (WordTile tile : tiles) {
						if (tile.getFaction() == game.getAssassinFaction()) {
							PotentialGuess potentialGuess = thoughtProcess
									.getBestGuess();
							potentialGuess.rethinkGuess(2.0, 0.5);
						}
					}

					List<PotentialGuess> allPotentialGuesses = thoughtProcess
							.getSortedGuessList();
					int startingIndex = Math.max(0,
							allPotentialGuesses.size() - 3);
					int endingIndex = allPotentialGuesses.size();
					List<PotentialGuess> mostProbablePotentialGuesses = allPotentialGuesses
							.subList(startingIndex, endingIndex);
					Collections.reverse(mostProbablePotentialGuesses);
					sendMessageToCurrentChannel("--- Printing Confidence Output ---");
					String out = "";
					for (PotentialGuess potentialGuess : mostProbablePotentialGuesses) {
						out += printPotentialGuess(potentialGuess) + "\n";
					}
					sendMessageToCurrentChannel(out);
					sendMessageToCurrentChannel("-----------------------------------");
					makeGuess(thoughtProcess.getBestGuess().getWord(),
							getSession(), getCurrentChannel(), aiSlackUser);
				}
			}
		});
	}

	private String printPotentialGuess(PotentialGuess potentialGuess) {
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(2);
		format.setMinimumIntegerDigits(0);

		String output = "Confidence in *" + potentialGuess.getWord() + "*: "
				+ format.format(potentialGuess.getWeightedCertainty());

		List<PotentialGuessRow> sortedRows = potentialGuess.getAllGuessRows();
		Collections.sort(sortedRows, new Comparator<PotentialGuessRow>() {

			@Override
			public int compare(PotentialGuessRow o1, PotentialGuessRow o2) {
				Double a = Math.abs(o1.getWeightedCertainty());
				Double b = Math.abs(o2.getWeightedCertainty());
				return b.compareTo(a);
			}
		});

		List<String> contributors = new ArrayList<String>();
		output += " (";
		for (PotentialGuessRow row : sortedRows) {
			boolean inPositive = potentialGuess.getPositiveGuessResults()
					.contains(row);
			String prefix = inPositive ? "+" : "-";
			contributors.add(format.format(row.getWeightedCertainty()) + "/"
					+ format.format(row.getCertainty()) + " [" + prefix + ""
					+ row.getClue() + "]");
		}
		output += StringUtils.join(contributors, ", ");
		output += ")";
		return output;
	}

	private void sendMessageToCurrentChannel(String output) {
		if (getSession() == null || getCurrentChannel() == null) {
			System.out.println("Nowhere to send the current message to.");
			return;
		}
		getSession().sendMessage(getCurrentChannel(), output);
	}

	private String printAbbreviatedFaction(Faction faction) {
		return "(" + faction.getName().substring(0, 1) + ")";
	}

	private String printWordSmithInstructions(SlackUser user,
			SlackUser opponent, Faction yourFaction, Faction assassinFaction) {
		String out = "";
		out += "Hi "
				+ getUsernameString(user)
				+ ", as the Word Smith, try to get your team to guess all of the words marked with "
				+ yourFaction.getName() + " *"
				+ printAbbreviatedFaction(yourFaction) + "*.\n";
		out += "The only information you can give your team each turn is a single word, and the number of guesses your team can make.  When you're ready, type ```clue``` to give your team a word.\n";
		out += "If your team guesses the Assassin card "
				+ printAbbreviatedFaction(assassinFaction)
				+ ", the game is immediately over.  Try your best to avoid hinting at the assassin.\n";
		out += "If you aren't sure if your clue is legal, feel free to pm "
				+ getUsernameString(opponent, true)
				+ "and ask for clarification.\n";
		out += "Good luck!";
		return out;
	}

	public WatchWordGame getGame() {
		return this.game;
	}

	public WatchWordLobby getWatchWordLobby() {
		return this.watchWordLobby;
	}

	private String printCurrentTurn() {
		String out = "It is currently *"
				+ game.getTurnOrder().getCurrentTurn().getName() + "*'s turn.";
		return out;
	}

	private String printGivenClue() {
		if (game.wasClueGivenThisTurn()) {
			String out = "Current clue: *" + game.getClue().getWord()
					+ "*, guesses remaining: ";
			out += "*" + (game.getRemainingGuesses() - 1) + "*";
			if (game.getRemainingGuesses() == 1) {
				out += " (Bonus Guess)";
			}
			return out;
		} else {
			return "A clue has not yet been given.  *"
					+ getUsernameString(getWatchWordLobby().getUser(
							game.getTurnOrder().getCurrentTurn().getLeader()))
					+ "*, please provide a clue.";
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

	private String printCardGrid(boolean forLeader) {
		int longestWordLength = 10;
		for (String word : game.getGrid().getAllWords()) {
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

				tileString = StringUtils
						.rightPad(tileString, longestWordLength);
				if (forLeader) {
					tileString += " ("
							+ tile.getFaction().getName().substring(0, 1) + ")";
				}
				tileString = "[ " + tileString + " ]";
				line += tileString;
			}
			line += "\n";
			out += line;
		}
		out += "```\n";

		if (forLeader) {
			for (Faction faction : game.getEveryKnownFaction()) {
				out += "*" + faction.getName() + "*: ";
				List<WordTile> wordTiles = game.getGrid()
						.getUnrevealedTilesForFaction(faction);
				List<String> words = new ArrayList<String>();
				for (WordTile tile : wordTiles) {
					words.add(tile.getWord());
				}
				out += StringUtils.join(words, ", ") + "\n";
			}
		}

		// Print unrevealed tiles for each player faction
		List<Faction> playerFactions = game.getTurnOrder().getAllFactions();
		for (Faction playerFaction : playerFactions) {
			int unrevealedTiles = game.getGrid()
					.getUnrevealedTilesForFaction(playerFaction).size();
			String conditionallyPluralizedCard = unrevealedTiles == 1 ? "card"
					: "cards";
			out += "\n*" + playerFaction.getName() + "* has *"
					+ unrevealedTiles + "* " + conditionallyPluralizedCard
					+ " left to guess.";
		}

		return out;
	}

	private String printCardGrid() {
		return printCardGrid(false);
	}

	private void finishGame(List<Faction> victors, SlackSession session) {
		String victorString = "";
		for (Faction victor : victors) {
			String singleVictorString = "Game over!  " + victor.getName()
					+ " has won!  Congratulations to:\n";
			for (Player player : victor.getAllPlayers()) {
				singleVictorString += getUsernameString(getWatchWordLobby()
						.getUser(player)) + "\n";
			}
			victorString += singleVictorString + "\n";

		}
		session.sendMessage(getCurrentChannel(), victorString);

		ArrayList<Faction> losers = new ArrayList<Faction>(game.getTurnOrder()
				.getAllFactions());
		losers.removeAll(victors);
		updateRankings(victors, losers);

		partialGameReset();
		getWatchWordLobby().getTurnOrder().shuffleFactionLeaders();
		getSession().sendMessage(getCurrentChannel(),
				"\nReturning back to the game lobby...");
		getSession().sendMessage(getCurrentChannel(),
				printFactions(getWatchWordLobby()));
	}

	private void printMatchQuality() {
		if (!this.getSessionFactory().isPresent()) {
			return;
		}
		if (this.game == null) {
			return;
		}
		Session session = null;
		try {
			session = getSessionFactory().get().openSession();
			session.beginTransaction();
			double gameBalance = RatingHelper.getMatchQuality(getGame()
					.getTurnOrder(), getWatchWordLobby(), session);
			NumberFormat format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(2);
			format.setMinimumFractionDigits(2);
			gameBalance *= 100 * 2;// Max game quality seems to be 0.5
			getSession().sendMessage(
					getCurrentChannel(),
					"Current game is *" + format.format(gameBalance)
							+ "*% balanced.");
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void updateRankings(List<Faction> victors, List<Faction> losers) {
		if (!this.getSessionFactory().isPresent()) {
			System.out
					.println("No link to any storage available...  Not writing results.");
			return;
		}
		Session session = null;
		try {
			session = getSessionFactory().get().openSession();
			session.beginTransaction();
			RatingHelper.updatePlayerRatings(victors, losers,
					getWatchWordLobby(), session);
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void partialGameReset() {
		this.currentGameState = GameState.LOBBY;
		stopAllAIThreads();
		this.game = null;
	}

	private void resetGame() {
		partialGameReset();
		this.currentGameState = GameState.IDLE;
		this.watchWordLobby = null;
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

	@SuppressWarnings("unused")
	private SlackUser findUserById(String id, Collection<SlackUser> members) {
		for (SlackUser user : members) {
			if (user.getId().equals(id)) {
				return user;
			}
		}
		return null;
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

	private void printIncorrectGameState(SlackChannel channel,
			List<GameState> gameStates) {

		this.getSession().sendMessage(
				channel,
				"This command can only be used during the " + gameStates
						+ " state.  It is currently the "
						+ this.currentGameState + " state.");

	}

	private SlackChannel getCurrentChannel() {
		if (this.watchWordLobby == null) {
			return null;
		}
		return this.watchWordLobby.getChannel();
	}

	private void onHeartBeat(SlackSession session) {
		if (this.game != null && game.getActingFaction() != null) {
			CompetitiveTime time = game.getRemainingTime();
			if (time == null) {
				return;
			}
			long totalTime = time.getTotalTime(TimeUnit.SECONDS);
			if (totalTime == 0) {
				session.sendMessage(getCurrentChannel(),
						"Out of time AND out of overtime!  Game over!");
				session.sendMessage(getCurrentChannel(),
						"(psssh...nothin personnel...kid...)");
				finishGame(Arrays.asList(game.getTurnOrder().getNextTurn()),
						session);
			} else if (totalTime % 60 == 0) {
				session.sendMessage(getCurrentChannel(), game
						.getActingFaction().getName()
						+ " team, you have "
						+ time.getTime(TimeUnit.MINUTES)
						+ " min remaining ("
						+ time.getOvertime(TimeUnit.MINUTES) + " min overtime)");
			} else if (totalTime < 60 && totalTime % 5 == 0) {
				session.sendMessage(getCurrentChannel(), game
						.getActingFaction().getName()
						+ " team, time's running out!  "
						+ totalTime
						+ " secs. remaining!");
			}
		}
	}
}

enum GameState {
	IDLE, LOBBY, GAME
}

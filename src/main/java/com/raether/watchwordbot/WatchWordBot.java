package com.raether.watchwordbot;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jskills.Rating;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.kohsuke.github.GHRepository;

import com.raether.watchwordbot.commands.BanishCommand;
import com.raether.watchwordbot.commands.BotCommand;
import com.raether.watchwordbot.commands.CancelCommand;
import com.raether.watchwordbot.commands.ClueCommand;
import com.raether.watchwordbot.commands.Command;
import com.raether.watchwordbot.commands.DefineCommand;
import com.raether.watchwordbot.commands.EndCommand;
import com.raether.watchwordbot.commands.FeatCommand;
import com.raether.watchwordbot.commands.GridCommand;
import com.raether.watchwordbot.commands.GuessCommand;
import com.raether.watchwordbot.commands.HelpCommand;
import com.raether.watchwordbot.commands.JoinCommand;
import com.raether.watchwordbot.commands.KickCommand;
import com.raether.watchwordbot.commands.ListCommand;
import com.raether.watchwordbot.commands.LobbyCommand;
import com.raether.watchwordbot.commands.PenaltyCommand;
import com.raether.watchwordbot.commands.StartCommand;
import com.raether.watchwordbot.commands.SwapCommand;
import com.raether.watchwordbot.commands.SyncCommand;
import com.raether.watchwordbot.commands.TimeCommand;
import com.raether.watchwordbot.commands.WinCommand;
import com.raether.watchwordbot.feat.UserFeatureRequest;
import com.raether.watchwordbot.meatsim.AIPlayer;
import com.raether.watchwordbot.meatsim.BotThoughtProcess;
import com.raether.watchwordbot.meatsim.DesiredBotAction;
import com.raether.watchwordbot.meatsim.PotentialGuess;
import com.raether.watchwordbot.meatsim.PotentialGuessRow;
import com.raether.watchwordbot.message.DefaultMessageGenerator;
import com.raether.watchwordbot.message.MessageGenerator;
import com.raether.watchwordbot.ranking.RatingHelper;
import com.raether.watchwordbot.ranking.RatingPrinter;
import com.raether.watchwordbot.user.UserEntity;
import com.raether.watchwordbot.user.UserHelper;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

public class WatchWordBot implements SlackMessagePostedListener {
	// I/O
	private String apiKey = "";
	private SlackSession slackSession;

	// Gameplay
	private GameState currentGameState = GameState.IDLE;
	private WatchWordLobby watchWordLobby;
	private WatchWordGame game;
	private WordGenerator wordGenerator = new WordGenerator();
	private List<Thread> aiThreads = new ArrayList<Thread>();
	private MessageGenerator messageGenerator = new DefaultMessageGenerator();
	public static Boolean DEBUG = Boolean.FALSE;
	private Long lastIssuedCommandTime = null;
	private TextRenderer textRenderer;

	// add-ons
	private Optional<SessionFactory> sessionFactory;
	private Optional<GHRepository> repo;

	public WatchWordBot(String apiKey, Optional<SessionFactory> sessionFactory,
			Optional<GHRepository> repo) {
		setAPIKey(apiKey);
		setSessionFactory(sessionFactory);
		setGHRepo(repo);
	}

	public void loadResources() throws IOException {
		wordGenerator.loadWordList();
		textRenderer = new TextRenderer();
	}

	private void setGHRepo(Optional<GHRepository> repo) {
		this.repo = repo;
	}

	public Optional<GHRepository> getGHRepo() {
		return this.repo;
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

	public void reconnect(SlackSession session) {
		try {
			session.disconnect();
		} catch (Exception e) {

		} finally {
			try {
				session.connect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void updateSession(SlackSession session) {
		this.slackSession = session;
	}

	public SlackSession getSession() {
		return this.slackSession;
	}

	@Override
	public void onEvent(SlackMessagePosted event, SlackSession session) {
		updateSession(session);
		if (event.getSender().getId().equals(session.sessionPersona().getId()))
			return;
		try {
			updateRendererScope(event, session);
			handleCommand(event, session);
			updateRendererScope(event, session);
		} catch (Exception e) {
			textRenderer.handleException(e);
		}
	}

	public void updateRendererScope(SlackMessagePosted event,
			SlackSession session) {
		textRenderer.updateScope(event, session);
	}

	public void updateGameState(GameState state) {
		this.currentGameState = state;
	}

	public MessageGenerator getMessageGenerator() {
		return this.messageGenerator;
	}

	private synchronized void handleCommand(SlackMessagePosted event,
			SlackSession session) {
		updateLastIssuedCommandTime();
		LinkedList<String> args = new LinkedList<String>();
		args.addAll(Arrays.asList(event.getMessageContent().split("\\s+")));// event.getMessageContent().split(" ");

		boolean matchingPrefix = true;
		if (DEBUG) {
			String debugPrefix = args.pop().toLowerCase();
			matchingPrefix = debugPrefix.equals("d");
		}

		if (matchingPrefix) {
			String commandText = args.pop().toLowerCase();
			List<Command> commands = generateCommands(event, args, session);
			Command matchingCommand = findMatchingCommand(commandText, args,
					commands, event.getChannel());
			if (matchingCommand != null) {
				matchingCommand.run(this, event, args, session);
			}
		} else {
			handlePlainTextMessage(event, session);
		}
	}

	private void handlePlainTextMessage(SlackMessagePosted event,
			SlackSession session) {
		addGuesseePointActivityForMessage(event.getSender());
	}

	private void addGuesseePointActivityForMessage(SlackUser user) {
		if (getLobby() == null) {
			return;
		}

		Player player = getLobby().getPlayer(user);
		if (player == null) {
			return;
		}

		Faction faction = getLobby().getTurnOrder().getFactionFor(player);
		if (faction == null) {
			return;
		}
		if (faction.getLeader() != player) {
			recordELOEvent(ELOEvent.GUESSER_SENT_PLAINTEXT_MESSAGE, player);
		}
	}

	public void recordELOEvent(ELOEvent event, Player player) {
		if (getGame() == null) {
			return;
		}

		if (player == null || event == null) {
			return;
		}

		Faction faction = getLobby().getTurnOrder().getFactionFor(player);
		if (faction == null) {
			return;
		}

		getGame().getELOBoosterTracker().addWeightedAction(event, player,
				faction);
	}

	public Command findMatchingCommand(String commandText, List<String> args,
			Collection<Command> commands, SlackChannel channel) {
		for (Command command : commands) {
			if (command.matches(commandText, args)) {
				if (!isGameCurrentlyInValidGameState(channel,
						command.getValidGameStates())) {
					return null;
				}
				return command;
			}
		}
		return null;
	}

	public List<Command> generateCommands(final SlackMessagePosted event,
			final LinkedList<String> args, final SlackSession session) {
		List<Command> commands = new ArrayList<Command>();
		commands.add(new BanishCommand());
		commands.add(new BotCommand());
		commands.add(new CancelCommand());
		commands.add(new ClueCommand());
		commands.add(new DefineCommand());
		commands.add(new EndCommand());
		commands.add(new FeatCommand());
		commands.add(new GridCommand());
		commands.add(new GuessCommand());
		commands.add(new HelpCommand());
		commands.add(new JoinCommand());
		commands.add(new KickCommand());
		commands.add(new ListCommand());
		commands.add(new LobbyCommand());
		commands.add(new PenaltyCommand());
		commands.add(new StartCommand());
		commands.add(new SwapCommand());
		commands.add(new SyncCommand());
		commands.add(new TimeCommand());
		commands.add(new WinCommand());
		return commands;
	}

	protected void fireIncorrectCommandUsage() {
		// printUsage();
	}

	protected Session createHibernateSession() {
		if (!this.getSessionFactory().isPresent()) {
			return null;
		}
		Session hibernateSession = null;
		try {
			hibernateSession = getSessionFactory().get().openSession();

		} catch (Exception e) {
			e.printStackTrace();

			if (hibernateSession != null) {
				hibernateSession.close();
			}
		}
		return hibernateSession;
	}

	protected void closeHibernateSession(Session session) {
		try {
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static String printUserFeedbackRequest(UserFeatureRequest request) {
		return request.getId() + ": " + request.getDescription();
	}

	public String printCommands(String title, List<Command> commands) {
		List<Command> filteredCommands = new ArrayList<Command>();
		for (Command command : commands) {
			if (command.getValidGameStates().contains(this.currentGameState)
					&& !command.isHidden()) {
				filteredCommands.add(command);
			}
		}

		String totalHelpText = title + "\n";
		for (Command command : filteredCommands) {
			String helpText = "*" + command.getPrimaryAlias() + "*";
			if (command.hasAdditionalAliases()) {
				helpText += " (" + command.getAdditionalAliases() + ")";
			}
			helpText += " = " + command.getHelpText() + "\n";
			totalHelpText += helpText;
		}
		return totalHelpText;
	}

	public synchronized void makeGuess(String wordBeingGuessed,
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
			Player guesser = getLobby().getPlayer(eventUser);
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

			recordELOEvent(ELOEvent.CLUE_GUESSED, guesser);

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
						messageGenerator.getAssassinPickMessage() + " "
								+ getUsernameString(eventUser)
								+ " has picked the assassin!  "
								+ guesserFaction.getName() + " loses!");
			} else if (pickedOwnCard) {
				session.sendMessage(getCurrentChannel(),
						messageGenerator.getCorrectPickMessage() + " "
								+ getUsernameString(eventUser)
								+ " has picked correctly.");
			} else {
				session.sendMessage(getCurrentChannel(),
						messageGenerator.getIncorrectPickMessage() + " "
								+ getUsernameString(eventUser)
								+ " has picked a *"
								+ guessedTile.getFaction().getName()
								+ "* card.");
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

	public void endTurn(SlackSession session, SlackChannel eventChannel,
			SlackUser eventUser) {
		if (getLobby().getPlayer(eventUser) == null) {
			session.sendMessage(eventChannel, getUsernameString(eventUser)
					+ ", you are currently not in this game!");
			return;
		}
		Player guesser = getLobby().getPlayer(eventUser);
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
		} else if (!game.getClue().hasGuessed()) {
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

	public void refreshGameResources() {
		refreshBanishedWordList();
	}

	private void refreshBanishedWordList() {
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

	public void waitForClue() {
		stopAllAIThreads();
		this.game.startCountingDown(4, TimeUnit.MINUTES);
	}

	public void waitForGuess() {
		stopAllAIThreads();
		this.game.startCountingDown(2, TimeUnit.MINUTES);
		handleAIGuesses();
	}

	public boolean isAIThreadRunning() {
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

	public void handleAIGuesses() {
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
		final SlackUser aiSlackUser = getLobby()
				.getUser(aiGuesseeOnCurrentTeam);
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

	public String printWordSmithInstructions(SlackUser user,
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

	public WatchWordLobby getLobby() {
		return this.watchWordLobby;
	}

	public String printCurrentTurn() {
		String out = "It is currently *"
				+ game.getTurnOrder().getCurrentTurn().getName() + "*'s turn.";
		return out;
	}

	public String printGivenClue() {
		if (game.wasClueGivenThisTurn()) {
			String out = "Current clue: *" + game.getClue().getWord()
					+ "*, guesses remaining: ";
			if (game.getClue().isUnlimited()) {
				out += "*Unlimited*";
			} else if (game.getClue().isZero()) {
				out += "*Zero*";
			} else {
				out += "*" + (game.getRemainingGuesses() - 1) + "*";
				if (game.getRemainingGuesses() == 1) {
					out += " (Bonus Guess)";
				}
			}
			return out;
		} else {
			return "A clue has not yet been given.  *"
					+ getUsernameString(getLobby().getUser(
							game.getTurnOrder().getCurrentTurn().getLeader()))
					+ "*, please provide a clue.";
		}
	}

	private Rating getUserRating(SlackUser user, boolean isLeader) {
		if (!this.getSessionFactory().isPresent()) {
			return null;
		}
		Rating rating = null;
		Session session = null;
		try {
			session = getSessionFactory().get().openSession();
			session.beginTransaction();
			rating = RatingHelper.getRatingForUser(user, isLeader, session);
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return rating;
	}

	public String printFactions() {
		String out = "Here are the teams:\n";
		for (Faction faction : getLobby().getTurnOrder().getAllFactions()) {
			String factionString = "[*" + faction.getName() + " team*]\n";
			for (Player player : faction.getAllPlayers()) {
				factionString += getUsernameString(getLobby().getUser(player));

				String printedRating = RatingPrinter.printRating(getUserRating(
						getLobby().getUser(player), faction.isLeader(player)));
				System.out.println(printedRating);
				if (printedRating != null) {
					factionString += " " + printedRating;
				}

				if (faction.isLeader(player)) {
					factionString += " (Leader)";
				}
				factionString += "\n";
			}
			out += factionString;
		}
		return out;
	}

	public String printCardGrid(boolean forLeader) {
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

	public String printCardGrid() {
		return printCardGrid(false);
	}

	public void finishGame(List<Faction> victors, SlackSession session) {
		String finalCardGrid = printCardGrid(true);
		session.sendMessage(getCurrentChannel(), "Final Card Grid:\n"
				+ finalCardGrid);

		String victorString = "";
		for (Faction victor : victors) {
			String singleVictorString = messageGenerator.getWinMessage() + "  "
					+ victor.getName() + " has won!  Congratulations to:\n";
			List<Player> participatingPlayers = getGame()
					.getELOBoosterTracker().getCoreParticipantsFor(victor);
			for (Player player : participatingPlayers) {
				singleVictorString += getUsernameString(getLobby().getUser(
						player))
						+ "\n";
				assignGBPs(10, "*Congratulations*!  Here are some gbps for winnin'!", player);
			}
			victorString += singleVictorString + "\n";
		}

		session.sendMessage(getCurrentChannel(), victorString);

		ArrayList<Faction> losers = new ArrayList<Faction>(game.getTurnOrder()
				.getAllFactions());
		losers.removeAll(victors);
		for (Faction loser : losers) {
			List<Player> participatingPlayers = getGame()
					.getELOBoosterTracker().getCoreParticipantsFor(loser);
			for (Player player : participatingPlayers) {
				assignGBPs(5, "Here are some gbps for participating!", player);
			}
		}

		updateRankings(victors, losers, game.getELOBoosterTracker());

		partialGameReset();
		getLobby().getTurnOrder().shuffleFactionLeaders();
		getSession().sendMessage(getCurrentChannel(),
				"\nReturning back to the game lobby...");
		getSession().sendMessage(getCurrentChannel(), printFactions());
	}

	private void assignGBPs(int gbps, String messageText, Player winner) {
		if (!this.getSessionFactory().isPresent()) {
			return;
		}
		SlackUser user = this.getLobby().getUser(winner);
		Session session = null;
		try {
			session = getSessionFactory().get().openSession();
			session.beginTransaction();
			UserEntity entity = UserHelper.readOrCreateUserEntity(user.getId(),
					user.getUserName(), session);
			int oldGBPs = entity.getGBPs();
			entity.setGBPs(oldGBPs + gbps);
			session.saveOrUpdate(entity);
			session.getTransaction().commit();
			String text = messageText + "  (+*" + gbps + "*).  You now have *"
					+ entity.getGBPs() + "* gbps!";
			getSession().sendMessageToUser(user, text, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	public void printMatchQuality() {
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
					.getTurnOrder(), getLobby(), session);
			NumberFormat format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(2);
			format.setMinimumFractionDigits(2);
			gameBalance *= 100;
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

	private void updateRankings(List<Faction> victors, List<Faction> losers,
			ELOBoosterTracker eloBoosterTracker) {
		if (!this.getSessionFactory().isPresent()) {
			System.out
					.println("No link to any storage available...  Not writing results.");
			return;
		}
		Session session = null;
		try {
			session = getSessionFactory().get().openSession();
			session.beginTransaction();
			RatingHelper.updatePlayerRatings(victors, losers, getLobby(),
					eloBoosterTracker, session);
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	public void partialGameReset() {
		this.currentGameState = GameState.LOBBY;
		stopAllAIThreads();
		this.game = null;
	}

	public void fullGameReset() {
		partialGameReset();
		this.currentGameState = GameState.IDLE;
		this.watchWordLobby = null;
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

	public static String getUsernameString(SlackUser user) {
		return getUsernameString(user, false);
	}

	public void printUsage(SlackChannel channel, String str) {
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

	public SlackChannel getCurrentChannel() {
		if (this.watchWordLobby == null) {
			return null;
		}
		return this.watchWordLobby.getChannel();
	}

	private void onHeartBeat(SlackSession session) {
		handleCompetitiveTimer(session);
		checkForAbandonedGame(session);
	}

	protected void checkForAbandonedGame(SlackSession session) {
		if (this.currentGameState == GameState.IDLE) {
			return;
		}

		// more than an hour has elapsed since anyone has issued any commands...
		if (getCurrentTime(TimeUnit.MINUTES)
				- getLastIssuedCommandTime(TimeUnit.MINUTES) >= 60) {
			fullGameReset();
		}
	}

	protected long getCurrentTime(TimeUnit unit) {
		return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	protected Long getLastIssuedCommandTime(TimeUnit unit) {
		if (lastIssuedCommandTime == null) {
			return null;
		}

		return unit.convert(lastIssuedCommandTime, TimeUnit.MILLISECONDS);
	}

	protected void updateLastIssuedCommandTime() {
		this.lastIssuedCommandTime = getCurrentTime(TimeUnit.MILLISECONDS);
	}

	protected void handleCompetitiveTimer(SlackSession session) {

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

				session.sendMessage(
						getCurrentChannel(),
						game.getActingFaction().getName()
								+ " team, you have "
								+ printTime(time.getTime(TimeUnit.SECONDS),
										TimeUnit.SECONDS)
								+ " remaining ("
								+ printTime(time.getOvertime(TimeUnit.SECONDS),
										TimeUnit.SECONDS) + " overtime)");
			} else if (totalTime < 60 && totalTime % 5 == 0) {
				session.sendMessage(getCurrentChannel(), game
						.getActingFaction().getName()
						+ " team, time's running out!  "
						+ totalTime
						+ " secs. remaining!");
			}
		}
	}

	private String printTime(long time, TimeUnit sourceUnit) {
		long minutes = TimeUnit.MINUTES.convert(time, sourceUnit);
		long seconds = TimeUnit.SECONDS.convert(time, sourceUnit) - minutes
				* 60;

		if (seconds == 0 && minutes != 0) {
			return "" + minutes + " min";
		}

		return "" + minutes + "m " + seconds + "s";
	}

	public WordGenerator getWordGenerator() {
		return this.wordGenerator;
	}

	public void setGame(WatchWordGame game) {
		this.game = game;
	}

	public void setLobby(WatchWordLobby watchWordLobby) {
		this.watchWordLobby = watchWordLobby;
	}

	public GameState getGameState() {
		return this.currentGameState;
	}

	public void penalizeCurrentFaction(int penaltyAmount) {
		List<WordTile> remainingUnrevealedWordTiles = game.getGrid()
				.getUnrevealedTilesForFaction(game.getNextFaction());

		int wordsBeingRevealed = Math.min(penaltyAmount,
				remainingUnrevealedWordTiles.size() - 1);

		for (int x = 0; x < wordsBeingRevealed; x++) {
			WordTile randomTile = remainingUnrevealedWordTiles
					.remove((int) (Math.random() * remainingUnrevealedWordTiles
							.size()));
			randomTile.setRevealed(true);
		}

		getSession().sendMessage(
				getCurrentChannel(),
				"*" + game.getActingFaction().getName()
						+ "* has been penalized for " + wordsBeingRevealed
						+ " tiles.");

		game.changeTurns();
		getSession().sendMessage(getCurrentChannel(), printCardGrid());
		getSession().sendMessage(getCurrentChannel(), printCurrentTurn());
		getSession().sendMessage(getCurrentChannel(), printGivenClue());
		waitForClue();
	}

}

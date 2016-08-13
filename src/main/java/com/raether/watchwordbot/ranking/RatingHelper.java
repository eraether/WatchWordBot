package com.raether.watchwordbot.ranking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jskills.GameInfo;
import jskills.IPlayer;
import jskills.ITeam;
import jskills.Rating;
import jskills.Team;
import jskills.trueskill.FactorGraphTrueSkillCalculator;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Session;

import com.raether.watchwordbot.Faction;
import com.raether.watchwordbot.Player;
import com.raether.watchwordbot.TurnOrder;
import com.raether.watchwordbot.WatchWordLobby;
import com.raether.watchwordbot.user.UserEntity;
import com.raether.watchwordbot.user.UserHelper;
import com.ullink.slack.simpleslackapi.SlackUser;

public class RatingHelper {

	public static GameInfo getGameInfo() {
		return GameInfo.getDefaultGameInfo();
	}

	public static void addDefaultRatingToUser(UserEntity entity, Session session) {
		if (entity.getClueGiverRating() == null) {
			entity.setClueGiverRating(new RatingValue(getGameInfo()
					.getDefaultRating()));
			session.saveOrUpdate(entity.getClueGiverRating());
		}
		if (entity.getGuesserRating() == null) {
			entity.setGuesserRating(new RatingValue(getGameInfo()
					.getDefaultRating()));
			session.saveOrUpdate(entity.getGuesserRating());
		}
	}

	public static void updatePlayerRatings(List<Faction> victors,
			List<Faction> losers, WatchWordLobby lobby, Session session) {
		List<Faction> allFactions = new ArrayList<Faction>();
		allFactions.addAll(victors);
		allFactions.addAll(losers);

		System.out.println("Updating player ratings...");
		GameInfo gameInfo = getGameInfo();
		Collection<ITeam> teams = new ArrayList<ITeam>();
		List<Integer> rankings = new ArrayList<Integer>();
		final int WINNER = 1;// lower = better
		final int LOSER = 2;

		for (Faction victor : victors) {
			teams.add(buildITeam(victor, gameInfo, lobby, session));
			rankings.add(WINNER);
		}
		for (Faction loser : losers) {
			teams.add(buildITeam(loser, gameInfo, lobby, session));
			rankings.add(LOSER);
		}

		FactorGraphTrueSkillCalculator calculator = new FactorGraphTrueSkillCalculator();
		Map<IPlayer, Rating> newRankings = calculator.calculateNewRatings(
				gameInfo, teams,
				ArrayUtils.toPrimitive(rankings.toArray(new Integer[] {})));
		for (IPlayer player : newRankings.keySet()) {
			@SuppressWarnings("unchecked")
			jskills.Player<Player> castPlayer = (jskills.Player<Player>) player;
			SlackUser user = lobby.getUser(castPlayer.getId());
			Player wwPlayer = castPlayer.getId();
			boolean isLeader = false;
			for (Faction faction : allFactions) {
				if (faction.isLeader(wwPlayer)) {
					isLeader = true;
					break;
				}
			}

			UserEntity entity = UserHelper.readOrCreateUserEntity(user.getId(),
					user.getUserName(), session);
			updateRatingForPlayer(entity, isLeader, newRankings.get(player),
					session);
		}
	}

	private static void updateRatingForPlayer(UserEntity entity,
			boolean wasLeader, Rating newRating, Session session) {
		if (wasLeader) {
			entity.getClueGiverRating().update(newRating);
			session.saveOrUpdate(entity.getClueGiverRating());
		} else {
			entity.getGuesserRating().update(newRating);
			session.saveOrUpdate(entity.getGuesserRating());
		}
		session.saveOrUpdate(entity);
	}

	private static ITeam buildITeam(Faction faction, GameInfo info,
			WatchWordLobby lobby, Session session) {

		Team team = new Team();

		for (Player player : faction.getAllPlayers()) {
			SlackUser user = lobby.getUser(player);

			String slackId = user.getId();
			String slackName = user.getUserName();
			jskills.Player<Player> jskillsPlayer = new jskills.Player<Player>(
					player);
			UserEntity entity = UserHelper.readOrCreateUserEntity(slackId,
					slackName, session);
			if (faction.isLeader(player)) {
				team.addPlayer(jskillsPlayer, entity.getClueGiverRating()
						.createRating());
			} else {
				team.addPlayer(jskillsPlayer, entity.getGuesserRating()
						.createRating());
			}
		}
		return team;
	}

	public static double getMatchQuality(TurnOrder turnOrder,
			WatchWordLobby lobby, Session session) {
		List<ITeam> teams = new ArrayList<ITeam>();
		GameInfo info = getGameInfo();
		for (Faction faction : turnOrder.getAllFactions()) {
			teams.add(buildITeam(faction, info, lobby, session));
		}
		FactorGraphTrueSkillCalculator calculator = new FactorGraphTrueSkillCalculator();
		return calculator.calculateMatchQuality(info, teams);

	}
}

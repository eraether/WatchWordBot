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
import com.raether.watchwordbot.WatchWordLobby;

public class RatingHelper {
	public static void updatePlayerRatings(List<Faction> victors,
			List<Faction> losers, WatchWordLobby lobby, Session session) {
		System.out.println("Updating player ratings...");
		GameInfo gameInfo = GameInfo.getDefaultGameInfo();
		Collection<ITeam> teams = new ArrayList<ITeam>();
		List<Integer> rankings = new ArrayList<Integer>();
		final int WINNER = 1;
		final int LOSER = 0;

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
			UserEntity entity = readOrCreateUserEntity(
					lobby.getUser(castPlayer.getId()).getId(),
					gameInfo.getDefaultRating(), session);
			updateRatingForPlayer(entity, newRankings.get(player), session);
		}
	}

	private static void updateRatingForPlayer(UserEntity entity,
			Rating newRating, Session session) {

		System.out.println("Setting rating for " + entity.getUserId()
				+ " from " + entity.getRating().getMean() + "("
				+ entity.getRating().getStandardDeviation() + ")" + " to "
				+ newRating.getMean() + "(" + newRating.getStandardDeviation()
				+ ")");
		entity.setRating(new RatingValue(newRating));
		session.saveOrUpdate(entity);
	}

	private static ITeam buildITeam(Faction faction, GameInfo info,
			WatchWordLobby lobby, Session session) {

		Team team = new Team();
		for (Player player : faction.getAllPlayers()) {
			String slackId = lobby.getUser(player).getId();
			jskills.Player<Player> jskillsPlayer = new jskills.Player<Player>(
					player);
			UserEntity entity = readOrCreateUserEntity(slackId,
					info.getDefaultRating(), session);
			team.addPlayer(jskillsPlayer, entity.getRating().createRating());
		}
		return team;
	}

	private static UserEntity readOrCreateUserEntity(String slackId,
			Rating defaultRating, Session session) {
		UserEntity entity = readUserEntity(slackId, session);
		if (entity == null) {
			entity = new UserEntity();
			entity.setUserId(slackId);
		}
		if (entity.getRating() == null) {
			entity.setRating(new RatingValue(defaultRating));
		}
		return entity;
	}

	private static UserEntity readUserEntity(String slackId, Session session) {
		UserEntity entity = session.get(UserEntity.class, slackId);
		return entity;
	}
}

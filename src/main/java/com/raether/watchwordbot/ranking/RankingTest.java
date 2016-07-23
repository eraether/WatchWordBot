package com.raether.watchwordbot.ranking;

import java.util.Collection;
import java.util.Map;

import jskills.GameInfo;
import jskills.IPlayer;
import jskills.ITeam;
import jskills.Player;
import jskills.Rating;
import jskills.Team;
import jskills.trueskill.TwoTeamTrueSkillCalculator;
import static org.junit.Assert.assertEquals;

public class RankingTest {
	private final static double ErrorTolerance = 0.085;

	public static void main(String[] args) {
		TwoTeamTrueSkillCalculator calculator = new TwoTeamTrueSkillCalculator();
		Player<Integer> player1 = new Player<Integer>(1);
		Player<Integer> player2 = new Player<Integer>(2);
		Player<Integer> player3 = new Player<Integer>(3);

		Team team1 = new Team().addPlayer(player1, new Rating(28, 7))
				.addPlayer(player2, new Rating(27, 6))
				.addPlayer(player3, new Rating(26, 5));

		Player<Integer> player4 = new Player<Integer>(4);
		Player<Integer> player5 = new Player<Integer>(5);

		Team team2 = new Team().addPlayer(player4, new Rating(30, 4))
				.addPlayer(player5, new Rating(31, 3));

		GameInfo gameInfo = GameInfo.getDefaultGameInfo();

		Collection<ITeam> teams = Team.concat(team1, team2);
		Map<IPlayer, Rating> newRatingsWinLoseExpected = calculator
				.calculateNewRatings(gameInfo, teams, 1, 2);

		// Winners
		assertRating(28.658, 6.770, newRatingsWinLoseExpected.get(player1));
		assertRating(27.484, 5.856, newRatingsWinLoseExpected.get(player2));
		assertRating(26.336, 4.917, newRatingsWinLoseExpected.get(player3));

		// Losers
		assertRating(29.785, 3.958, newRatingsWinLoseExpected.get(player4));
		assertRating(30.879, 2.983, newRatingsWinLoseExpected.get(player5));

		Map<IPlayer, Rating> newRatingsWinLoseUpset = calculator
				.calculateNewRatings(gameInfo, Team.concat(team1, team2), 2, 1);

		// Winners
		assertRating(32.012, 3.877, newRatingsWinLoseUpset.get(player4));
		assertRating(32.132, 2.949, newRatingsWinLoseUpset.get(player5));

		// Losers
		assertRating(21.840, 6.314, newRatingsWinLoseUpset.get(player1));
		assertRating(22.474, 5.575, newRatingsWinLoseUpset.get(player2));
		assertRating(22.857, 4.757, newRatingsWinLoseUpset.get(player3));

		assertMatchQuality(0.254,
				calculator.calculateMatchQuality(gameInfo, teams));
	}

	private static void assertRating(double expectedMean,
			double expectedStandardDeviation, Rating actual) {
		assertEquals(actual.getMean(), expectedMean, ErrorTolerance);
		assertEquals(actual.getStandardDeviation(), expectedStandardDeviation,
				ErrorTolerance);
	}

	private static void assertMatchQuality(double expectedMatchQuality,
			double actualMatchQuality) {
		assertEquals(actualMatchQuality, expectedMatchQuality, 0.0005);
	}
}

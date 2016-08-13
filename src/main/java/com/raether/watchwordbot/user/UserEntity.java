package com.raether.watchwordbot.user;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.raether.watchwordbot.ranking.RatingValue;

@Entity
public class UserEntity {
	@Id
	private String userId;
	// @Transient
	private String username;
	@OneToOne
	private RatingValue guesserRating;
	@OneToOne
	private RatingValue clueGiverRating;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public RatingValue getGuesserRating() {
		return this.guesserRating;
	}

	public void setGuesserRating(RatingValue value) {
		this.guesserRating = value;
	}

	public RatingValue getClueGiverRating() {
		return this.clueGiverRating;
	}

	public void setClueGiverRating(RatingValue value) {
		this.clueGiverRating = value;
	}

	public boolean hasUndefinedRatings() {
		return getClueGiverRating() == null || getGuesserRating() == null;
	}
}

package org.recsyschallenge.algorithms.helpers;

import org.recsyschallenge.algorithms.enums.SessionEventType;

public class AlgorithmMesurer {
	private long guessedBuys;
	private long guessedClicks;
	private long testCount;
	private long clickSessionsCount;
	private long buySessionsCount;

	public AlgorithmMesurer() {
		this.guessedBuys = 0;
		this.guessedClicks = 0;
		this.setTestCount(0);
		this.buySessionsCount = 0;
		this.clickSessionsCount = 0;
	}

	public float getGuessedPercent() {
		return (((float) (guessedBuys + guessedClicks)) / this.getTestCount()) * 100;
	}

	public void incSessoinCount(SessionEventType type) {
		switch (type) {
		case BuyEvent:
			this.buySessionsCount++;
			break;
		case ClickEvent:
			this.clickSessionsCount++;
			break;
		}
	}

	public void incGuessedCount(SessionEventType type) {
		switch (type) {
		case BuyEvent:
			this.guessedBuys++;
			break;
		case ClickEvent:
			this.guessedClicks++;
			break;
		}
	}

	public long getClickSessionsCount() {
		return clickSessionsCount;
	}

	public long getBuySessionsCount() {
		return buySessionsCount;
	}
	
	public long getGuessedClicks() {
		return guessedClicks;
	}

	public long getGuessedBuys() {
		return guessedBuys;
	}
	
	public long getGuessed() {
		return guessedBuys + guessedClicks;
	}

	public long getTestCount() {
		return testCount;
	}

	public void setTestCount(long testCount) {
		this.testCount = testCount;
		this.buySessionsCount = 0;
		this.clickSessionsCount = 0;
	}


}

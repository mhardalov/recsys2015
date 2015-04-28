package org.recsyschallenge.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.recsyschallenge.algorithms.enums.SessionEventType;

public class SessionInfo {

	private final int sessionId;
	private List<SessionBuys> buys;
	private List<SessionClicks> clicks;

	public SessionInfo(int sessionId) {
		this.buys = new ArrayList<SessionBuys>();
		this.clicks = new ArrayList<SessionClicks>();
		this.sessionId = sessionId;
	}

	public SessionEventType hasBuys() {
		if (this.getBuys().size() == 0)
			return SessionEventType.ClickEvent;
		else {
			return SessionEventType.BuyEvent;
		}
	}

	public List<SessionBuys> getBuys() {
		return new ArrayList<SessionBuys>(this.buys);
	}

	public void addBuy(SessionBuys buy) {
		this.buys.add(buy);
	}

	public List<SessionClicks> getClicks() {
		return new ArrayList<SessionClicks>(this.clicks);
	}

	/**
	 * Returns a map of clicked items, counted and grouped by "Category".
	 * 
	 * @return Map with key the "Category" name and value number of items
	 *         clicked by this category.
	 */
	public Map<String, Integer> getClickedItems() {
		Map<String, Integer> clickedItems = new HashMap<String, Integer>();
		for (SessionClicks click : this.clicks) {
			String category = click.getCategory();

//			if (category != "0") {
//				continue;
//			}

			// TODO: validate
			// If it is product name then we transfer it to "product" feature
			if (category.length() > 3) {
				category = "product";
			}

			Integer count = clickedItems.get(category);

			if (count == null) {
				count = 0;
			}

			clickedItems.put(category, count + 1);
		}

		return clickedItems;
	}

	public double getClickSessionLength() {
		int clickSize = this.clicks.size();
		if (clickSize == 0)
			return 0;

		Date firstClick = this.clicks.get(0).getTimestamp();
		Date lastClick = this.clicks.get(this.clicks.size() - 1).getTimestamp();

		return Math.abs(firstClick.getTime() - lastClick.getTime() / clickSize);
	}

	public double getAvgSessionLength() {
		int clickSize = this.clicks.size();
		if (clickSize == 0)
			return 0;

		double avg = 0;
		for (SessionClicks click : this.clicks) {
			avg += click.getTimestamp().getTime();
		}

		return avg / clickSize;
	}

	public boolean isItemBought(int itemId) {
		for (SessionBuys buy : this.getBuys()) {
			if (buy.getItemId() == itemId) {
				return true;
			}
		}
		return false;
	}

	public void addClick(SessionClicks click) {
		this.clicks.add(click);
	}

	public int getSessionId() {
		return sessionId;
	}

	public List<String> getDataString() {
		int sessionEnd = this.hasBuys().ordinal();

		List<String> result = new ArrayList<>();
		for (SessionClicks click : this.clicks) {
			result.add(click.toString() + "," + sessionEnd);
		}

		return result;
	}
}

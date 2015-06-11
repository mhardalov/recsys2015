package org.recsyschallenge.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.recsyschallenge.algorithms.enums.SessionEventType;

public class SessionInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4097902057395609332L;
	private static final float threshold = 6.0f;

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

			Integer count = clickedItems.get(category);

			if (count == null) {
				count = 0;
			}

			clickedItems.put(category, count + 1);
		}

		return clickedItems;
	}

	public double getClickSessionLength() {
		// int clickSize = this.clicks.size();

		Date firstClick = this.clicks.get(0).getTimestamp();
		Date lastClick = this.clicks.get(this.clicks.size() - 1).getTimestamp();
		float sessionLength = (lastClick.getTime() - firstClick.getTime())
				/ (60 * 1000F);

		return sessionLength;
	}

	public double getAvgSessionLength() {
		int clickSize = this.clicks.size();

		double avg = 0;
		for (SessionClicks click : this.clicks) {
			avg += click.getTimestamp().getTime();
		}

		// time in minutes
		avg /= (60 * 1000F);

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

	public GenericUserPreferenceArray toPreferenceArray() {
		int userId = this.getSessionId();

		List<GenericPreference> items = new ArrayList<>();
		for (SessionClicks click : this.getClicks()) {
			int itemId = click.getItemId();
			items.add(new GenericPreference(userId, itemId, (this
					.isItemBought(itemId) ? 5.0f : 1.0f)));
		}
		return new GenericUserPreferenceArray(items);
	}

	public List<Integer> getRecIntersect(List<RecommendedItem> recommendations) {

		List<Integer> boughtItems = null;
		for (SessionClicks click : this.getClicks()) {
			int clickItemId = click.getItemId();

			for (RecommendedItem item : recommendations) {
				if (item.getValue() > threshold
						&& item.getItemID() == clickItemId) {
					if (boughtItems == null) {
						boughtItems = new ArrayList<Integer>();
					}

					boughtItems.add(clickItemId);

					break;
				}
			}
		}

		return boughtItems;
	}
}

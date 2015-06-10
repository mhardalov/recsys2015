package org.recsyschallenge.models;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

import org.recsyschallenge.helpers.TimeStampHelper;

public class SessionClicks implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1122601273559521421L;
	private final int sessionID;
	private Date timestamp;
	private final int itemId;
	private final String category; 

	public SessionClicks(String[] row) throws ParseException {
		this.sessionID = Integer.parseInt(row[0]);
		String date = row[1];
		try {
			this.timestamp = TimeStampHelper.parseDate(date);
		} catch (Exception e) {
			e.toString();
			this.timestamp = new Date();
		}
		
		this.itemId = Integer.parseInt(row[2]);
		this.category = row[3];
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + getSessionID();
		hash = hash * 31 + getTimestamp().hashCode();
		hash = hash * 31 + getItemId();
		hash = hash * 31 + getCategory().hashCode();

		return hash;
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof SessionClicks))
			return false;

		SessionClicks other = (SessionClicks) o;
		if (this.getSessionID() != other.getSessionID())
			return false;
		if (this.getTimestamp().equals(other.getSessionID()))
			return false;
		if (this.getItemId() != other.getItemId())
			return false;
		if (!this.getCategory().equals(other.getCategory()))
			return false;

		return true;
	}

	public int getSessionID() {
		return sessionID;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public int getItemId() {
		return itemId;
	}

	public String getCategory() {
		return category;
	}

	public String toString() {
		return this.sessionID + "," + this.itemId + "," + this.category + ",";
	}
}

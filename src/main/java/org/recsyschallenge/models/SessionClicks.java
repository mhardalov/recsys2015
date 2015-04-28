package org.recsyschallenge.models;

import java.text.ParseException;
import java.util.Date;

import org.recsyschallenge.helpers.TimeStampHelpers;

public class SessionClicks {	
	private final int sessionID;
	private final Date timestamp;
	private final int itemId;
	private final String category;	

	public SessionClicks(String[] row) throws ParseException {
		this.sessionID = Integer.parseInt(row[0]);
		this.timestamp = TimeStampHelpers.parseDate(row[1]);
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

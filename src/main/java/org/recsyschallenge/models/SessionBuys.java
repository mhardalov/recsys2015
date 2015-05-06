package org.recsyschallenge.models;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

import org.recsyschallenge.helpers.TimeStampHelper;

public class SessionBuys implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3015150074325523361L;
	private final int sessionID;
	private final Date timestamp;
	private final int itemId;
	private final double price;
	private final int quantity;

	public SessionBuys(String[] row) throws ParseException {
		this.sessionID = Integer.parseInt(row[0]);
		this.timestamp = TimeStampHelper.parseDate(row[1]);
		this.itemId = Integer.parseInt(row[2]);
		this.price = Double.parseDouble(row[3]);
		this.quantity = Integer.parseInt(row[4]);
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + getSessionID();
		hash = hash * 31 + getTimestamp().hashCode();
		hash = hash * 31 + getItemId();
		hash = hash * 31 + Double.valueOf(getPrice()).hashCode();
		hash = hash * 31 + getQuantity();
		
		return hash;
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof SessionBuys))
			return false;

		SessionBuys other = (SessionBuys) o;
		if (this.getSessionID() != other.getSessionID())
			return false;
		if (this.getTimestamp().equals(other.getSessionID()))
			return false;
		if (this.getItemId() != other.getItemId())
			return false;
		if (Double.compare(this.getPrice(), other.getPrice()) != 0)
			return false;
		if (this.getQuantity() != other.getQuantity())
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

	public int getQuantity() {
		return quantity;
	}

	public double getPrice() {
		return price;
	}
}

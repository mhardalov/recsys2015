package org.recsyschallenge.algorithms.enums;

public enum SessionEventType {
	BuyEvent,
	ClickEvent;
	
	public static SessionEventType valueOf(int x) {
        switch(x) {
        case 0:
            return BuyEvent;
        case 1:
            return ClickEvent;
        }
        return null;
    }
}

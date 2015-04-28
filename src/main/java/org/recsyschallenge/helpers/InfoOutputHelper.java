package org.recsyschallenge.helpers;

import java.util.Date;

public class InfoOutputHelper {
	private InfoOutputHelper() {

	}

	public static void printInfo(String input) {
		System.out.println(new Date().toString() + "\t" + input);
	}
}

package org.recsyschallenge;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.recsyschallenge.algorithms.SGDClassification;
import org.recsyschallenge.helpers.FilesParserHelper;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.models.SessionInfo;

public class RecSysMain {

	private static final float ratio = (float) 3.5;
	private static final String CLICKS_PATH = "/media/ramdisk/yoochoose-clicks.dat";
	private static final String BUYS_PATH = "/media/ramdisk/yoochoose-buys.dat";

	public static void randomForest() {
		// String[] data = new String[sessionsSize];
		// int i = 0;
		// for (SessionInfo session : sessions.values()) {
		// int sessionResult = session.hasBuys();
		//
		// for (SessionClicks click : session.getClicks()) {
		// data[i] = click.toString() + sessionResult;
		// i++;
		// }
		//
		// if (i >= sessionsSize) {
		// break;
		// }
		// }
		//
		// BreimanExample.main(data);
		// BreimanExample ex = new BreimanExample();
		// ex.run(data);
	}

	public static void main(String[] args) throws Exception {

		// Max buys 509696
		FilesParserHelper parser = FilesParserHelper.newInstance(CLICKS_PATH,
				BUYS_PATH, 509696, ratio);
		Map<Integer, SessionInfo> sessions = parser.parseSessions();

		int sessionsSize = sessions.size();
		InfoOutputHelper.printInfo("Parsed sessions: "
				+ String.valueOf(sessionsSize));

		List<SessionInfo> sessionsList = new ArrayList<SessionInfo>(
				sessions.values());

		Collections.shuffle(sessionsList, new SecureRandom());

		// 10% of all data
		int testRecords = (int) (sessionsSize * 0.1);
		List<SessionInfo> test = sessionsList.subList(0, testRecords);
		List<SessionInfo> train = sessionsList.subList(testRecords,
				sessionsSize);

		SGDClassification classification = new SGDClassification(8000, 5000);

		classification.train(train);
		classification.test(test, false);
		classification.dissect(sessionsList);

		// RecommenderSVD recommender = new RecommenderSVD(sessionsList);
		// List<RecommendedItem> users = recommender.recommendUser(87, 100);
		//
		// for (RecommendedItem user : users) {
		// InfoOutputHelper.printInfo((user.toString());
		// }
	}

}

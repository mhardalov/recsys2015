package org.recsyschallenge;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.recsyschallenge.algorithms.classification.SGDClassification;
import org.recsyschallenge.algorithms.recommender.UserBasedRecomender;
import org.recsyschallenge.helpers.FilesParserHelper;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.models.SessionInfo;

public class RecSysMain {

	private static final float ratio = (float) 5;
	private static final String TEST_PATH = "/media/ramdisk/yoochoose-test.dat";
	private static final String CLICKS_PATH = "/media/ramdisk/yoochoose-clicks.dat";
	private static final String BUYS_PATH = "/media/ramdisk/yoochoose-buys.dat";
	private static final String OUTPUT_PATH = "/home/momchil/Desktop/RecSys/output/yoochoose.output";
	private static final String MODEL_PATH = "/home/momchil/Desktop/RecSys/models/";

	// private static void train(SGDClassification classification,
	// List<SessionInfo> train)

	public static void main(String[] args) throws Exception {

		// Max buys 509696
		// FilesParserHelper parser = FilesParserHelper.newInstance(CLICKS_PATH,
		// BUYS_PATH, 509696, ratio);
		FilesParserHelper testFileParser = FilesParserHelper.newInstance(TEST_PATH, "");
		Map<Integer, SessionInfo> sessions;
		List<SessionInfo> sessionsList;
		
		try {			
			sessions = testFileParser.parseSessions();			
	
			sessionsList = new ArrayList<SessionInfo>(
					sessions.values());
		} finally {
			testFileParser.dispose();
			testFileParser = null;
		}
		
		int sessionsSize = sessionsList.size();
		InfoOutputHelper.printInfo("Parsed sessions: "
				+ String.valueOf(sessionsSize));
		

		// Collections.shuffle(sessionsList, new SecureRandom());

		// 10% of all data
		// int testRecords = (int) (sessionsSize * 0.1);
		// List<SessionInfo> test = sessionsList.subList(0, testRecords);
		// List<SessionInfo> train = sessionsList.subList(testRecords,
		// sessionsSize);

		SGDClassification classification = new SGDClassification(8000, 5000);

		classification.loadModel(MODEL_PATH);
		// classification.train(train);
		// classification.test(sessionsList, false);
		List<SessionInfo> buySessions = classification.classify(sessionsList);
		// classification.dissect(sessionsList);
		// classification.saveModel(MODEL_PATH);
		
		sessions.clear();
		sessions = null;
		
		sessionsList.clear();
		sessionsList = null;
		
		FilesParserHelper trainParser = FilesParserHelper.newInstance(CLICKS_PATH, BUYS_PATH);

		Map<Integer, SessionInfo> trainSessions = trainParser.parseSessions();
		List<SessionInfo> trainSessionsList = new ArrayList<SessionInfo>(trainSessions.values());

		trainSessions.clear();
		sessionsSize = trainSessionsList.size();
		InfoOutputHelper.printInfo("Parsed sessions: "
				+ String.valueOf(sessionsSize));

		UserBasedRecomender recommender = new UserBasedRecomender(trainSessionsList,
				buySessions);

		trainSessionsList.clear();
		trainSessionsList = null;

		Map<Integer, Set<Integer>> buySessionInfo = recommender
				.getUserIntersect(buySessions);
		recommender.exportToFile(OUTPUT_PATH, buySessionInfo);
	}
}

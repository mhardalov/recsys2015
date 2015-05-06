package org.recsyschallenge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.spark.api.java.JavaRDD;
import org.recsyschallenge.algorithms.classification.SGDClassification;
import org.recsyschallenge.algorithms.recommender.UserBasedRecomender;
import org.recsyschallenge.helpers.FilesParserHelper;
import org.recsyschallenge.helpers.InfoOutputHelper;
import org.recsyschallenge.helpers.SparkHelper;
import org.recsyschallenge.models.SessionInfo;

public class RecSysMain {

	private static final float ratio = (float) 5;
	private static final String TEST_PATH = "/media/ramdisk/yoochoose-test.dat";
	private static final String CLICKS_PATH = "/media/ramdisk/yoochoose-clicks.dat";
	private static final String BUYS_PATH = "/media/ramdisk/yoochoose-buys.dat";
	private static final String OUTPUT_PATH = "/home/momchil/Desktop/RecSys/output/yoochoose.output";
	private static final String MODEL_PATH = "/home/momchil/Desktop/RecSys/models/";

	private static SGDClassification getClassifier() {
		return new SGDClassification(8000, 5000);
	}

	private static SGDClassification loadModelFromFile(String path)
			throws FileNotFoundException, IOException {
		SGDClassification classification = getClassifier();
		classification.loadModel(path);

		return classification;
	}

	private SGDClassification trainClassifier() throws ParseException,
			IOException {
		// Max buys 509696
		FilesParserHelper parser = FilesParserHelper.newInstance(CLICKS_PATH,
				BUYS_PATH, 509696, ratio);

		Map<Integer, SessionInfo> sessions;
		List<SessionInfo> sessionsList;

		try {
			sessions = parser.parseSessions();

			sessionsList = new ArrayList<SessionInfo>(sessions.values());
		} finally {
			parser.dispose();
			parser = null;
		}

		int sessionsSize = sessionsList.size();

		Collections.shuffle(sessionsList, new SecureRandom());

		// 10% of all data
		int testRecords = (int) (sessionsSize * 0.1);
		List<SessionInfo> test = sessionsList.subList(0, testRecords);
		List<SessionInfo> train = sessionsList.subList(testRecords,
				sessionsSize);

		SGDClassification classification = getClassifier();
		classification.train(train);
		testClassifier(classification, test);
		dissectClassifier(classification, sessionsList);
		saveModel(classification, MODEL_PATH);

		return classification;
	}

	private static List<SessionInfo> classifySessions(String testPath,
			SGDClassification classification) throws ParseException,
			IOException {

		FilesParserHelper testFileParser = FilesParserHelper.newInstance(
				testPath, "");
		Map<Integer, SessionInfo> sessions = testFileParser.parseSessions();
		List<SessionInfo> sessionsList = new ArrayList<SessionInfo>(
				sessions.values());

		try {

			int sessionsSize = sessionsList.size();
			InfoOutputHelper.printInfo("Parsed sessions: "
					+ String.valueOf(sessionsSize));

			List<SessionInfo> buySessions = classification
					.classify(sessionsList);

			return buySessions;
		} finally {
			testFileParser.dispose();
			testFileParser = null;

			sessions.clear();
			sessions = null;

			sessionsList.clear();
			sessionsList = null;
		}
	}

	private static void testClassifier(SGDClassification classification,
			List<SessionInfo> testSessions) throws IOException {
		classification.test(testSessions, false);
	}

	private static void dissectClassifier(SGDClassification classification,
			List<SessionInfo> sessions) throws IOException {
		classification.dissect(sessions);
	}

	private static UserBasedRecomender getRecommender(
			List<SessionInfo> buySessions) throws IOException, TasteException,
			ParseException {

		Map<Integer, SessionInfo> trainSessions;
		List<SessionInfo> trainSessionsList;

		FilesParserHelper trainParser = FilesParserHelper.newInstance(
				CLICKS_PATH, BUYS_PATH, 509696, 6);

		try {

			trainSessions = trainParser.parseSessions();
			trainSessionsList = new ArrayList<SessionInfo>(
					trainSessions.values());

//			trainSessions.clear();			

			int sessionsSize = trainSessionsList.size();
			InfoOutputHelper.printInfo("Parsed sessions: "
					+ String.valueOf(sessionsSize));

			UserBasedRecomender recommender = new UserBasedRecomender(
					trainSessionsList, buySessions);

			return recommender;

		} finally {
			trainParser.dispose();
			trainSessionsList = null;
		}
	}

	private static void saveModel(SGDClassification classification,
			String modelPath) throws IOException {
		classification.saveModel(modelPath);
	}

	public static void main(String[] args) throws Exception {

		SGDClassification classification = loadModelFromFile(MODEL_PATH);

		List<SessionInfo> buySessions = classifySessions(TEST_PATH,
				classification);

		UserBasedRecomender recommender = getRecommender(buySessions);

		Map<Integer, List<Integer>> buySessionInfo = recommender
				.getUserIntersect();
		recommender.exportToFile(OUTPUT_PATH, buySessionInfo);

		buySessions.clear();
		buySessions = null;
	}
}

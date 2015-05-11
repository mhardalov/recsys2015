package org.recsyschallenge.helpers;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.recsyschallenge.models.SessionBuys;
import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

import scala.Tuple2;

public class HDFSParserHelper implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3478741054506932870L;
	private final String clicksPath;
	private final String buysPath;

	private static Map<Integer, SessionInfo> sessions;
	private final long sessionsLimit;
	private final boolean loadAllData;
	private final float clicksRatio;

	public static HDFSParserHelper newInstance(String clicksPath,
			String buysPath) {
		return new HDFSParserHelper(clicksPath, buysPath, 0, 1);
	}

	public static HDFSParserHelper newInstance(String clicksPath,
			String buysPath, long sessionsLimit, float clicksRatio) {
		return new HDFSParserHelper(clicksPath, buysPath, sessionsLimit,
				clicksRatio);
	}

	private HDFSParserHelper(String clicksPath, String buysPath,
			long sessionsLimit, float clicksRatio) {
		this.clicksPath = clicksPath;
		this.buysPath = buysPath;
		this.sessionsLimit = sessionsLimit;
		this.loadAllData = (sessionsLimit <= 0);
		HDFSParserHelper.sessions = new HashMap<Integer, SessionInfo>();
		this.clicksRatio = clicksRatio;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void parseClicks() throws IOException, ParseException {
		JavaRDD<String> lines = SparkHelper.sc
				.textFile("hdfs://localhost:9000/recsys/RecSys/yoochoose-clicks.dat");

		JavaPairRDD<Integer, SessionClicks> clicks = lines.mapToPair(s -> {
			String[] row = s.split(",");
			int sessionId = Integer.valueOf(row[0]);
			SessionClicks click = new SessionClicks(row);

			return new Tuple2(sessionId, click);
		});

		JavaPairRDD<Integer, Iterable<SessionClicks>> groupSessions = clicks
				.distinct().groupByKey();

		groupSessions.foreach(sessionTuple -> {
			int sessionId = sessionTuple._1();

			SessionInfo session = sessions.get(sessionId);
			if (session == null) {
				session = new SessionInfo(sessionId);
			}
			for (SessionClicks click : sessionTuple._2) {
				session.addClick(click);
			}

			sessions.put(sessionId, session);

		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void parseBuys() {
		JavaRDD<String> lines = SparkHelper.sc
				.textFile("hdfs://localhost:9000/recsys/RecSys/yoochoose-buys.dat");

		JavaPairRDD<Integer, SessionBuys> buys = lines.mapToPair(s -> {
			SessionBuys buy = new SessionBuys(s.split(","));
			return new Tuple2(buy.getSessionID(), buy);
		});

		JavaPairRDD<Integer, Iterable<SessionBuys>> groupSessions = buys
				.distinct().groupByKey();

		groupSessions.foreach(sessionTuple -> {
			int sessionId = sessionTuple._1();

			SessionInfo session = sessions.get(sessionId);
			if (session == null) {
				session = new SessionInfo(sessionId);
			}
			for (SessionBuys click : sessionTuple._2) {
				session.addBuy(click);
			}

			sessions.put(sessionId, session);

		});
	}

	public Map<Integer, SessionInfo> parseSessions() throws ParseException,
			IOException {
		InfoOutputHelper.printInfo("Starting parse phase");

		this.parseBuys();
		if (!this.loadAllData) {
			assert HDFSParserHelper.sessions.size() <= this.sessionsLimit;
		}

		this.parseClicks();
		if (!this.loadAllData) {
			assert HDFSParserHelper.sessions.size() <= this.sessionsLimit
					* (1 + this.clicksRatio);
		}

		// 9619219
		System.out.println();

		return HDFSParserHelper.sessions;
	}

	public void dispose() {
		HDFSParserHelper.sessions.clear();
		HDFSParserHelper.sessions = null;
	}
	// hdfs://localhost:9000/recsys/RecSys/
}

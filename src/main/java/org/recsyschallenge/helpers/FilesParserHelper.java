package org.recsyschallenge.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.recsyschallenge.models.SessionBuys;
import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

import au.com.bytecode.opencsv.CSVReader;

public class FilesParserHelper {
	private final String clicksPath;
	private final String buysPath;

	private Map<Integer, SessionInfo> sessions;
	private final long sessionsLimit;
	private final boolean loadAllData;
	private final float clicksRatio;

	public static FilesParserHelper newInstance(String clicksPath,
			String buysPath) {
		return new FilesParserHelper(clicksPath, buysPath, 0, 1);
	}

	public static FilesParserHelper newInstance(String clicksPath,
			String buysPath, long sessionsLimit, float clicksRatio) {
		return new FilesParserHelper(clicksPath, buysPath, sessionsLimit,
				clicksRatio);
	}

	private FilesParserHelper(String clicksPath, String buysPath,
			long sessionsLimit, float clicksRatio) {
		this.clicksPath = clicksPath;
		this.buysPath = buysPath;
		this.sessionsLimit = sessionsLimit;
		this.loadAllData = (sessionsLimit <= 0);
		this.sessions = new HashMap<Integer, SessionInfo>();
		this.clicksRatio = clicksRatio;
	}

	private void parseClicks() throws IOException, ParseException {
		InputStream csvFilename = new FileInputStream(clicksPath);
		String[] row = null;

		CSVReader csvReader = new CSVReader(new InputStreamReader(csvFilename));

		row = csvReader.readNext();
		long i = 0;
		long max = (long) (sessions.size() * clicksRatio);
		while (row != null) {

			int sessionId = Integer.parseInt(row[0]);

			SessionClicks click = new SessionClicks(row);
			SessionInfo session = this.sessions.get(sessionId);

			if (session == null) {
				if (!this.loadAllData && i >= max) {
					row = csvReader.readNext();
					continue;
				}

				session = new SessionInfo(sessionId);
				i++;
			}

			session.addClick(click);
			this.sessions.put(sessionId, session);

			row = csvReader.readNext();
		}

		csvReader.close();
	}

	private void parseBuys() throws ParseException, IOException {
		if (this.buysPath == "") {
			return;
		}
		InputStream csvFilename = new FileInputStream(buysPath);

		String[] row = null;

		CSVReader csvReader = new CSVReader(new InputStreamReader(csvFilename));

		row = csvReader.readNext();
		while (row != null
				&& (this.loadAllData || sessions.size() < this.sessionsLimit)) {

			int sessionId = Integer.parseInt(row[0]);

			SessionBuys buy = new SessionBuys(row);
			SessionInfo session = this.sessions.get(sessionId);

			if (session == null) {
				session = new SessionInfo(sessionId);
			}

			session.addBuy(buy);
			this.sessions.put(sessionId, session);

			row = csvReader.readNext();
		}

		csvReader.close();
	}

	public Map<Integer, SessionInfo> parseSessions() throws ParseException,
			IOException {
		InfoOutputHelper.printInfo("Starting parse phase");

		this.parseBuys();
		if (!this.loadAllData) {
			assert this.sessions.size() <= this.sessionsLimit;
		}

		this.parseClicks();
		if (!this.loadAllData) {
			assert this.sessions.size() <= this.sessionsLimit
					* (1 + this.clicksRatio);
		}

		System.out.println();

		return this.sessions;
	}

	public void dispose() {
		this.sessions.clear();
		this.sessions = null;
	}
}

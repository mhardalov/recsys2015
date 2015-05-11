package org.recsyschallenge.helpers;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;

public class HDFSHelperTests {

	@Test
	public void testParseFiles() throws ParseException, IOException {
		SparkHelper.initSparkContext(4);
		HDFSParserHelper hdfs = HDFSParserHelper.newInstance("hdfs://localhost:9000/recsys/RecSys/yoochoose-clicks.dat", "hdfs://localhost:9000/recsys/RecSys/yoochoose-buys.dat");
		hdfs.parseSessions();
		
	}

}

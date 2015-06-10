package org.recsyschallenge.algorithms.classification.builders;

public class SGDClassificationBuilder {

	private final int features;
	private final int interval;
	private final int avgWindow;
	private final int upSamplingRatio;
	private final int buysCount;
	private final float clicksRatio;

	public SGDClassificationBuilder(int features, int interval, int avgWindow,
			int upSamplingRatio, int buysCount, float clicksRatio) {
		this.features = features;
		this.interval = interval;
		this.avgWindow = avgWindow;
		this.upSamplingRatio = upSamplingRatio;
		this.buysCount = buysCount;
		this.clicksRatio = clicksRatio;
	}

	public int getFeatures() {
		return features;
	}

	public int getInterval() {
		return interval;
	}

	public int getAvgWindow() {
		return avgWindow;
	}

	public int getUpSamplingRatio() {
		return upSamplingRatio;
	}

	public int getBuysCount() {
		return buysCount;
	}

	public float getClicksRatio() {
		return clicksRatio;
	}

}

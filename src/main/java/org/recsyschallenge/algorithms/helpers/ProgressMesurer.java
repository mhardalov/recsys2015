package org.recsyschallenge.algorithms.helpers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.recsyschallenge.helpers.InfoOutputHelper;

public class ProgressMesurer implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2407238836686175385L;

	private final long stepOffset;
	private final long maxSteps;
	private AtomicLong currentProgress;
	private AtomicLong indicatorPercent;

	public ProgressMesurer(long stepOffset, long maxSteps) {
		this.stepOffset = stepOffset;
		this.maxSteps = maxSteps;
		currentProgress = new AtomicLong(0);
		indicatorPercent = new AtomicLong(0);
	}

	private long getPercentDone(boolean increase) {
		long currProg = (increase ? currentProgress.incrementAndGet()
				: currentProgress.get());

		return (long) (((float) currProg / maxSteps) * 100);
	}

	public synchronized void stepIt() {
		if (this.getPercentDone(true) == indicatorPercent.get()) {
			InfoOutputHelper.printInfo(indicatorPercent + "% done");
			indicatorPercent.addAndGet(stepOffset);
		}
	}

}

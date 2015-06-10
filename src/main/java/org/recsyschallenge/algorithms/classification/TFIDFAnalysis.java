package org.recsyschallenge.algorithms.classification;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.recsyschallenge.models.SessionClicks;
import org.recsyschallenge.models.SessionInfo;

public class TFIDFAnalysis {
	private Map<Integer, Integer> dictionary;
	private Map<Integer, Long> documentFrequency;
	private Map<String, Integer> dictionaryCategories;
	private Map<String, Long> categoriesFrequency;
	private int documentCount;

	public TFIDFAnalysis() {
		dictionary = new HashMap<Integer, Integer>();
		documentFrequency = new HashMap<Integer, Long>();
		dictionaryCategories = new HashMap<String, Integer>();
		categoriesFrequency = new HashMap<String, Long>();
		documentCount = 0;
	}

	public TFIDFAnalysis(TFIDFAnalysis tfidf) {
		dictionary = new HashMap<Integer, Integer>(tfidf.getDictionary());
		documentFrequency = new HashMap<Integer, Long>(
				tfidf.getDocumentFrequency());
		dictionaryCategories = new HashMap<String, Integer>(
				tfidf.getDictionaryCategories());
		categoriesFrequency = new HashMap<String, Long>(
				tfidf.getCategoriesFrequency());
		documentCount = tfidf.getDocumentCount();
	}

	public Map<Integer, Integer> getDictionary() {
		return dictionary;
	}

	public Map<Integer, Long> getDocumentFrequency() {
		return documentFrequency;
	}

	public Map<String, Long> getCategoriesFrequency() {
		return categoriesFrequency;
	}

	public Map<String, Integer> getDictionaryCategories() {
		return dictionaryCategories;
	}

	public int getDocumentCount() {
		return documentCount;
	}

	private void processItem(int itemId, boolean isItemBought) {
		Long count;
		Integer itemCount = this.dictionary.get(itemId);
		if (itemCount == null) {
			itemCount = 1;

			this.dictionary.put(itemId, itemCount);
			count = 0l;
			this.documentFrequency.put(itemId, count);
		} else {
			this.dictionary.put(itemId, itemCount + 1);
			count = this.documentFrequency.get(itemId);
		}

		if (isItemBought) {
			this.documentFrequency.put(itemId, (count + 1));
		}
	}

	private void processCategory(String category, boolean isItemBought) {
		Long count;
		Integer itemCount = this.dictionaryCategories.get(category);
		if (itemCount == null) {
			itemCount = 1;

			this.dictionaryCategories.put(category, itemCount);
			count = 0l;
			this.categoriesFrequency.put(category, count);
		} else {
			this.dictionaryCategories.put(category, itemCount + 1);
			count = this.categoriesFrequency.get(category);
		}

		if (isItemBought) {
			this.categoriesFrequency.put(category, (count + 1));
		}
	}

	public void addTermsFromDoc(SessionInfo session) throws IOException {

		for (SessionClicks item : session.getClicks()) {
			int itemId = item.getItemId();
			boolean isItemBought = session.isItemBought(itemId);
			String category = item.getCategory();

			this.processItem(itemId, isItemBought);
			this.processCategory(category, isItemBought);
		}

		this.documentCount++;
	}

	public double getItemWeight(int itemId) {
		double itemWeight = 0;
		Integer totalCount = this.getDictionary().get(itemId);

		if (totalCount != null) {
			Long freq = this.getDocumentFrequency().get(itemId);
			itemWeight = (double) freq.intValue() / totalCount;
		}

		return itemWeight;
	}

	public double getCategoryWeight(String category) {
		double itemWeight = 0;
		Integer totalCount = this.getDictionaryCategories().get(category);

		if (totalCount != null) {
			Long freq = this.getCategoriesFrequency().get(category);
			itemWeight = (double) freq.intValue() / totalCount;
		}

		return itemWeight;
	}
}
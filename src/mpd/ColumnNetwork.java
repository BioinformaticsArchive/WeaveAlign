package mpd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import mpd.Column.ColumnKey;
import mpd.utils.MuInt;

class ColumnNetwork {
	HashMap<ColumnKey, Column> contMap = new HashMap<ColumnKey, Column>();			// container map
	HashMap<ColumnKey, ColClass> succMap = new HashMap<ColumnKey, ColClass>();		// successor map
	
	HashMap<ColumnKey, Integer> giCount;		// gap insensitive count

	int numberOfNodes = 0;
	int[] firstDescriptor;
	Column firstCol;
	Column lastCol;

	double gValue;		// g parameter of the MPG algorithm
	boolean optGi;		// true if viterbi is based on gap insensitive score 
	int n;				// total number of alignments in network
	
	long buildTime;		// time spent in building the network (creating columns, hashing etc.)
	long viterbiTime;	// time spent in Viterbi algorithm

	public ColumnNetwork(double gValue, boolean optGi, boolean outGi) {
		this.gValue = gValue;
		this.optGi = optGi;
		
		if(outGi || optGi)
			giCount = new HashMap<Column.ColumnKey, Integer>();
	}

	/**
	 * Adds an alignment to the column network.
	 * 
	 * @param align alignment as a string array
	 * @return first (dummy) column of the network
	 */
	Column addAlignment(String[] align) {
		int len = align[0].length(), size = align.length, i, j, d;
		ColClass succClass;
		
		buildTime -= System.currentTimeMillis();
		
		if(n == 0) {
			// add first dummy column
			firstDescriptor = new int[size];
			Arrays.fill(firstDescriptor, -1);
			succClass = add(firstDescriptor, null, -1);
		} else {
			succClass = firstCol.succ;
			firstCol.count++;
		}

		int[] descriptor = firstDescriptor;
		boolean allGap;
		for(j = 0; j < len; j++) {
			int[] nextDescriptor =  new int[size];
			allGap = true;
			for(i = 0; i < size; i++){
				d = descriptor[i];
				d += d & 1;
				if(align[i].charAt(j) != '-') {
					d++;
					allGap = false;
				}
				nextDescriptor[i] = d;
			}
			descriptor = nextDescriptor;
			if(!allGap)
				succClass = add(descriptor, succClass, 0);
		}

		if(n == 0) {
			// add last dummy column
			descriptor = descriptor.clone();
			for(i = 0; i < size; i++) {
				descriptor[i] += (descriptor[i] & 1) + 1;
			}
			add(descriptor, succClass, 1);
		} else {
			lastCol.count++;
		}
		
		n++;
		buildTime += System.currentTimeMillis();
		
		return firstCol;
	}
	
	double scoreAlignment(String[] align, MuInt rlen) {
		int len = align[0].length(), size = align.length, i, j, d;

		rlen.value = 0;
		double score = 0;
		int[] descriptor = new int[size];
		Arrays.fill(descriptor, -1);
		boolean allGap;
		for(j = 0; j < len; j++) {
			int[] nextDescriptor =  new int[size];
			allGap = true;
			for(i = 0; i < size; i++){
				d = descriptor[i];
				d += d & 1;
				if(align[i].charAt(j) != '-') {
					d++;
					allGap = false;
				}
				nextDescriptor[i] = d;
			}
			descriptor = nextDescriptor;
			if(!allGap) {
				rlen.value++;
				ColumnKey key = new ColumnKey(descriptor);
				Column col = contMap.get(key);
				if(col == null)
					throw new Error("could not find column");
				score += getColMarginal(col, optGi);	//(double)col.count/n;
			}
		}

		return score;
	}
	
	/**
	 * Adds a new alignment column into the network. If already in the network, column count is incremented.
	 * @param descriptor Alignment column represented by an array of signed integers
	 * @param type type identifier for column: -1 for first dummy, 0 for regular col, 1 for last dummy
	 */
	ColClass add(int[] descriptor, ColClass predClass, int type) {
		Column column;

		ColumnKey key = new ColumnKey(descriptor);
		
		if(giCount != null) {
			ColumnKey spKey = key.giKey();
			Integer c;
			if((c=giCount.get(spKey)) == null)
				c = 1;
			else
				c++;
			giCount.put(spKey, c);
		}
		
		if((column=contMap.get(key)) != null) {
			++column.count;
			return column.succ;
		}

		column = new Column(key);
		contMap.put(key, column);
		numberOfNodes++;
		
		if(type == -1)
			firstCol = column;
		else
			predClass.succList.add(column);
		
		ColClass succClass;
		if(type == 1) {
			lastCol = column;
			succClass = null;
		} else {
			ColumnKey succKey = key.succ();
			if((succClass = succMap.get(succKey)) == null)
				succMap.put(succKey, succClass = new ColClass());
		}
		column.succ = succClass;

		return succClass;
	}
	
	double getColMarginal(Column col, boolean gi) {
		return !gi? (double)col.count/n :
						(double)giCount.get(col.key.giKey())/n;
	}

	Column updateViterbi() {
		viterbiTime -= System.currentTimeMillis();
		
		for(ColClass succClass : succMap.values()) {
			succClass.score = Double.NEGATIVE_INFINITY;
			succClass.viterbi = null;
		}
		double score = updateViterbi(firstCol.succ);
		
		viterbiTime += System.currentTimeMillis();
		System.out.format(Locale.ENGLISH, "Viterbi score: %.3f\n", score);
		
		return firstCol;
	}

	double updateViterbi(ColClass colClass) {
		if(colClass == null)
			return 0;
		if(colClass.score != Double.NEGATIVE_INFINITY)
			return colClass.score;
		List<Column> list = colClass.succList;
		for(Column col : list) {
			double sc = getColMarginal(col, optGi) - gValue + updateViterbi(col.succ);
			if(sc > colClass.score) {
				colClass.score = sc;
				colClass.viterbi = col;
			}
		}
		return colClass.score;
	}

}
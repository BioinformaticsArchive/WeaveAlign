package mpd;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import mpd.utils.MuInt;

public class MpdGlobalFast {

	CurrentAlignment curAlig;
	
	ColumnNetwork network;
	
	String t[][];
	String[] sequences;
	String[] seqNames;
	StringBuilder[] alignBuilder;

	int totalSamples;
	
	List<Double> decoding;
	List<Double> decodingGi;
	String[] alignment;
	
	
	MpdAnnotator annotator;
	
	String outputFile;
	String scoreFile;
	public boolean outGi = false;
	
	static Comparator<String[]> compStringArr = new Comparator<String[]>() {
		@Override
		public int compare(String[] a1, String[] a2) {
			return a1[0].compareTo(a2[0]);
		}
	};
	
	public MpdGlobalFast(double gValue, boolean optGi, boolean outGi) {
		network = new ColumnNetwork(gValue, optGi, outGi);
		this.outGi = outGi;
	}
		
	public void setGValue(double gValue) {
		network.gValue = gValue;
	}
	
	public double getGValue() {
		return network.gValue;
	}
	
	public void setAnnotator(MpdAnnotator annotator) {
		this.annotator = annotator;
	}

	public void addAlignment(String[] align) {
		if(t == null) {
			int sizeOfAlignments = align.length;
			t = new String[sizeOfAlignments][];
			sequences = null;
		}
		totalSamples++;
		
		String[] sortedAlign = getAlign(align);
		network.addAlignment(sortedAlign);
	}

	private String[] getAlign(String[] align) {
		for(int i = 0; i < t.length; i++) {			
				t[i] = align[i].split("\t");	
		}
		Arrays.sort(t, compStringArr);

		String[] sortedAlign = new String[t.length];
		for(int i = 0; i < t.length; i++)
			sortedAlign[i] = t[i][1];
		return sortedAlign;
	}

	public void scoreSample(int no, String[] align, FileWriter writer, boolean computePosterior) {
		String[] sortedAlign = getAlign(align);
		MuInt len = new MuInt(0);		
		double score = network.scoreAlignment(sortedAlign, len, computePosterior);
		
		try {
			writer.write(no+"\t"+String.format("%.6f", score)+"\t"+len+"\n");
		} catch (IOException e) {
		}
	}
	
	public void computeEquivalenceClassFreqs() {
		network.computeEquivalenceClassFreqs();				
	}
	private void updateAll() {
		int sizeOfAlignments = t.length;
		if(sequences == null) {

			alignBuilder = new StringBuilder[sizeOfAlignments];
			for(int i = 0; i < sizeOfAlignments; i++)
				alignBuilder[i] = new StringBuilder();
			alignment = new String[sizeOfAlignments];

			sequences = new String[sizeOfAlignments];			
			StringBuilder b = new StringBuilder();
			int len = t[0][1].length();
			for(int i = 0; i < sizeOfAlignments; i++){
				b.setLength(0);
				for(int j = 0; j < len; j++){
					if(t[i][1].charAt(j) != '-'){
						b.append(t[i][1].charAt(j));
					}
				}
				sequences[i] = b.toString();
			}
		}


		Column firstCol;
		if(annotator != null) {
			if(seqNames == null) {
				seqNames = new String[sizeOfAlignments];
				for(int i = 0; i < t.length; i++)
					seqNames[i] = t[i][0];
			}
			firstCol = annotator.annotate(network, sequences, seqNames);
		} else {
			firstCol = network.updateViterbi();
		}

		Column col = firstCol.succ.viterbi;
		decoding = new ArrayList<Double>();
		if(outGi)
			decodingGi = new ArrayList<Double>();
		while(col.succ != null) {
			int[] desc = col.key.desc;
			
			decoding.add(network.getColMarginal(col, false));
			if(outGi)
				decodingGi.add(network.getColMarginal(col, true));
			for(int i = 0; i < desc.length; i++) {
				alignBuilder[i].append((desc[i] & 1) == 0 ? '-' : sequences[i].charAt(desc[i] >> 1));
			}
			col = col.succ.viterbi;
		}

		for(int i = 0; i < sizeOfAlignments; i++) {
			alignment[i] = t[i][0]+"\t"+alignBuilder[i].toString();
			alignBuilder[i].setLength(0);
		}

	}
	
	public long getBuildTime() {
		return network.buildTime;
	}
	
	public long getViterbiTime() {
		return network.viterbiTime;
	}
	
	public long getAnnotTime() {
		return annotator.annotTime;
	}
	
	public void finalise() throws IOException {
		updateAll();
		FileWriter writer = new FileWriter(outputFile);
		try{
			for(int i = 0; i < alignment.length; i++) {
				String[] row = alignment[i].split("\t");
				writer.write(">");
				writer.write(row[0]);
				writer.write("\n");
				writer.write(row[1]);
				writer.write("\n");
			}
			if(!outputFile.equals(scoreFile)) {
				writer.close();
				if(scoreFile != null)
					writer = new FileWriter(scoreFile);
			} else {
				writer.write("\n#scores\n\n");
			}
			if(scoreFile != null) {
				if(decoding != null) {
					if(decodingGi != null) {
						for(int i = 0; i < decoding.size(); i++)
							writer.write(decoding.get(i)+"\t"+decodingGi.get(i)+"\n");
					} else {
						for(int i = 0; i < decoding.size(); i++)
							writer.write(decoding.get(i)+"\n");
					}
				}
			}
			writer.close();
		}
		catch(IOException e){
		}
		
	}

}
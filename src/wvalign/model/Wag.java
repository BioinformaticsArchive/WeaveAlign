package wvalign.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import wvalign.model.Blosum62;

/**
 * Implements the WAG (Whelan &amp; Goldman model) substitution model, see
 * also <a href="http://www.ncbi.nlm.nih.gov/pubmed/11319253">http://www.ncbi.nlm.nih.gov/pubmed/11319253
 * </a>
 * 
 * @author miklos, novak
 *
 */
public class Wag extends AminoAcidModel {

	public static final String menuName = "WAG";

	/**
	 * Constructor used when instantiation is necessary. 
	 */
	public Wag() throws IOException{
		String rate = "data/wagMatrix.dat";
		String equilibrium = "data/wagEq.dat";
		String alphabetFile = "data/alphabet.dat";
		try{
			attachedScoringScheme = new Blosum62();
		}
		catch(FileNotFoundException e){
		}
		catch(IOException e){
		}
		BufferedReader bf;
		ClassLoader cl = getClass().getClassLoader();
		//System.out.println(cl+" "+cl.getResource(alphabetFile)+" "+alphabetFile);
		bf = new BufferedReader(new InputStreamReader(cl.getResource(alphabetFile).openStream()));
//		bf = new BufferedReader(new FileReader(alphabetFile));
		String a = bf.readLine();
		alphabet = new char[a.length()];
		for(int i = 0; i < alphabet.length; i++){
			alphabet[i] = a.charAt(i);
		}
		int size = alphabet.length;
		v = new double[size][size];
		w = new double[size][size];
		d = new double[size];
		e = new double[size];
		params = new double[0];
		bf = new BufferedReader(new InputStreamReader(cl.getResource(rate).openStream()));
//		bf = new BufferedReader(new FileReader(rate));
		String[] temp;
		for(int i = 0; i < size; i++){
			temp = (bf.readLine()).split(" ");
			for(int j = 0; j < size; j++){
				v[i][j] = Double.parseDouble(temp[j]);
			}
		}
		String t = bf.readLine(); //reading an empty line
		for(int i = 0; i < size; i++){
			temp = (bf.readLine()).split(" ");
			for(int j = 0; j < size; j++){
				//	System.out.println(i+" "+j+" "+temp[j]);
				w[i][j] = Double.parseDouble(temp[j]);
			}
		}
		t = bf.readLine(); //reading an empty line
		for(int i = 0; i < size; i++){
			t = bf.readLine();
			d[i] = Double.parseDouble(t);
		}

		bf = new BufferedReader(new InputStreamReader(cl.getResource(equilibrium).openStream()));
//		bf = new BufferedReader(new FileReader(equilibrium));
		for(int i = 0; i < size; i++){
			t = bf.readLine();
			e[i] = Double.parseDouble(t);
		}
	}
	
}

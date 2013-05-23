package statalign;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JOptionPane;

import statalign.base.MainManager;
import statalign.ui.ErrorMessage;
import statalign.ui.MainFrame;

/**
 * <p>The entry point of the application. If the program is called from command line and
 * arguments are given, it runs in terminal mode (experimental).
 * 
 * <p>If no arguments are given or launched from a jar file, it opens a main frame.
 *
 * @author miklos, novak
 * 
 */
public class StatAlign{

	/**
	 * StatAlign version data.
	 */
	public static final int majorVersion = 1;
	public static final int minorVersion = 2;
	public static final String version = "v1.2";
	
	public static final boolean allowVersionCheck = false;

	/**
	 * Only method of the class.
	 * If a Fasta input file is given as argument, it runs in terminal mode
	 * (without graphical interface), otherwise it launches the main GUI
	 * of the program.
	 * 
	 * @param args [0]: the input file name containing the sequences in Fasta format
	 * @throws IOException
	 */
	public static void main(String args[]) {
		if(args.length != 0) {
			// console mode

			System.out.println("StatAlign "+version+"\n");
			MainManager manager = new MainManager(null);
			if(CommandLine.fillParams(args, manager) > 0)
				System.exit(1);
			
			manager.start();

		} else {
			// GUI mode
			
			MainFrame mf = null;
			try {
				System.out.println("StatAlign "+version+"\n");
				mf = new MainFrame();
				
				if(allowVersionCheck) {
					URL urlVersion = new URL("http://phylogeny-cafe.elte.hu/StatAlign/version.txt");
					try {
						URLConnection connection = urlVersion.openConnection();
						//System.out.println("Connection letrehozva");
						connection.setConnectTimeout(2000);
						String s = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
						if(!s.equals(version)) {
							JOptionPane.showMessageDialog(mf, "You are using StatAlign "+version+
															". StatAlign "+s+" is now available!\n"+
															"To download the new version, please visit" +
															" http://phylogeny-cafe.elte.hu/StatAlign/",
															"New version available!",
															JOptionPane.INFORMATION_MESSAGE);
						}
					} catch(Exception e) {}
				}
			} catch(Exception e) {
				new ErrorMessage(mf,e,true);
			}
		}
	}

}
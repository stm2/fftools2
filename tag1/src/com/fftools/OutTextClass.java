package com.fftools;

import javax.swing.JTextArea;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Die Klasse wird statisch in alle Classen eingebunden, die Ausgaben
 * in Richtung Datei (FFTools_log.txt) oder in Richtung
 * JFrame via JTextArea machen wollen
 * Alle Ausgaben laufen defaultmässig in die TextDatei
 * Die Anzeige im JTextArea kann unterbunden werden mittels
 * setScreenOut
 * 
 * @author Fiete
 *
 */

public class OutTextClass {
	private JTextArea txtOutput = null;

	private boolean screenOut = true;

	private boolean showedVersionsTXTOutput = false;
	
	private String defaultFileName = "FFTools_log";
	private String actFileName = null;
	
	private long lastPointSetAt = 0;
	private long setPointEveryMilliseks = 300;
	
	private int cntPoints = 0;
	private int maxRowPoints = 20;
	
	private HashMap<String,FileWriter> fileWriters = null; 
	
	private static OutTextClass DEFAULT = new OutTextClass();
	
	/**
	 * setzt den JTextArea, sollte recht früh, beim allerersten
	 * Aufruf gesetzt werden, in der classe, die als erstes
	 * OutTextClass statisch referenziert
	 */
	public void setTxtOut(JTextArea _txtOutput){
		this.txtOutput = _txtOutput;
	}
	
	public JTextArea getTxtOut(){
		if (this.txtOutput==null) {
			return null;
		} else {
			return this.txtOutput;
		}
	}
	
	
	public void addPoint(){
		Date date = new Date();
		
		long actMillis = date.getTime();
		if (actMillis-lastPointSetAt>setPointEveryMilliseks){
			lastPointSetAt = actMillis;
			if (txtOutput!=null){
				txtOutput.append(".");
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print(".");
				System.out.flush();
			}
			cntPoints++;
			if (cntPoints>maxRowPoints){
				cntPoints=0;
				if (txtOutput!=null){
					txtOutput.append("*\n");
					txtOutput.setCaretPosition((txtOutput.getText().length()));
				} else {
					System.out.print("*\n");
					System.out.flush();
				}
				
			}
		}
	}
	
	/**
	 * Ausgabe des Strings s, Zeilenendzeichen wird (davor) mitgesetzt
	 * @param s
	 */
	public void addOutLine(String s) {
		String sS = "\n" + s;
		this.addOutChars(sS);
	}
	
	public void addNewLine(){
		this.addOutChars("\n");
	}
	
	
	public void addOutChars(String s){
		String sS = s;
		String originalString = sS;
		if (screenOut) {
			if (txtOutput!=null){
				if (!showedVersionsTXTOutput){
					sS = getVersionsString() + sS;
					showedVersionsTXTOutput = true;
				}
				txtOutput.append(sS);
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print(sS);
				System.out.flush();
			}
			cntPoints=0;
		}
		this.writeToLog(originalString);
	}
	
	
	public void addOutChars(String s,int charAmount){
		String sS = formatString(s, charAmount, " ");
		String originalString = sS;
		if (screenOut) {
			if (txtOutput!=null){
				if (!showedVersionsTXTOutput){
					sS = getVersionsString() + sS;
					showedVersionsTXTOutput = true;
				}
				txtOutput.append(sS);
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print(sS);
				System.out.flush();
			}
			cntPoints=0;
		}
		this.writeToLog(originalString);
	}
	
	
	private String formatString(String s, int countChars, String aChar){
		String erg=null;
		
		if (s==null){
			s="";
		}
		
		if (s.length()>countChars){
			return s.substring(0,countChars-1);
		}
		
		if (aChar==null || aChar.length()<1){
			return "ERR";
		}
		
		erg = s;
		while (erg.length()<countChars){
			erg = aChar + erg;
		}

		return erg;
	}
	
	
	public void addOutLine(String s,boolean explizitScreenout){
		boolean change_value = false;
		
		if (explizitScreenout!=this.screenOut){
			change_value=true;
			this.screenOut = explizitScreenout;
		}
		
		this.addOutLine(s);
		
		if (change_value){
			this.screenOut = !this.screenOut;
		}
	}
	
	
	private String getVersionsString(){
		String versionS = "\nVersions:\n";
		versionS+="FFTools2: " + VersionInfo.getVersionInfo() + "\n";
		versionS+="Magellan: " + magellan.library.utils.VersionInfo.getVersion(null) + "\n";
		return versionS;
	}
	
	/**
	 * 
	 */
	public void closeOut() {
		String s = getDateS() + " closing log.\n";
		this.addOutLine(s);
	}
	
	/**
	 * Ausgabe aller Strings in die TextDatei
	 */
	
	public void writeToLog(String s){
		if (actFileName==null){
			actFileName=defaultFileName;
		}
		FileWriter fileWriter = null;
		if (fileWriters!=null){
			fileWriter = fileWriters.get(actFileName);
		}
		if (fileWriter==null){
			try {
				fileWriter = new FileWriter(actFileName + ".txt",true);
				
				if (fileWriters==null){
					fileWriters=new HashMap<String, FileWriter>();
				}
				fileWriters.put(actFileName, fileWriter);
				
				fileWriter.write("\n\n\n");
				fileWriter.write("***** " + getDateS() + " new logentries follow....******\n");
				fileWriter.write(getVersionsString());
				fileWriter.write("\n");
			} catch (IOException e) {
				// pech
			}
		}
		try {
			fileWriter.write(s);
			fileWriter.flush();
		} catch (IOException e) {
			// dumm
		}
	}
	
	/**
	 * bestimmt, ob addOutLine auch versucht, auf JTextArea zu schreiben
	 * oder ob lediglich in textFile geschrieben wird
	 * @param _screenOut
	 */
	
	public void setScreenOut(boolean _screenOut){
		// this.addOutLine("new setting for screenOut: " + this.screenOut);
		this.screenOut = _screenOut;
	}
	
	
	
	
	
	/**
	 * liefert für alle Aufrufe die beim Initialiserung erzeugte
	 * Default-OutTextClass
	 * @return
	 */
	
	public static OutTextClass getInstance() {
		return DEFAULT;
	}
	
	public static String getDateS() {
		SimpleDateFormat fr = new SimpleDateFormat("_yyyyMMdd_HHmmss");
  	  	String dateS =  fr.format(new Date());
  	  	return dateS;
	}
	
	
	public void setFile(String s){
		if (s!=null && s.length()>3){
			actFileName = s;
		}
	}
	
	public void setFileStandard(){
		actFileName = defaultFileName;
	}

	/**
	 * @return the screenOut
	 */
	public boolean isScreenOut() {
		return screenOut;
	}
	
}

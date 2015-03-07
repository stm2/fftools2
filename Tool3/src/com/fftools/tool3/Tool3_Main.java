package com.fftools.tool3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;

import com.fftools.OutTextClass;
import com.fftools.tool1.Settings;
import com.fftools.tool1.User;
import com.fftools.utils.FileCopy;

/*
 * Wendet Selections an und erstellt aus *.ct dateien die *.xt1.cr
 */
public class Tool3_Main {
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	private static final int mode_writing=1;
	private static final int mode_skipping=2;
	
	private static Settings s = null;
	private static User u = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		outText.setFile("FFTools_log_Tool3_" + FileCopy.getDateSDay());
		outText.addOutLine(FileCopy.getDateS() + " start Tool3");
		try {
			s = new Settings("toolsettings.config");
	    	s.sayStatus();
	    	
	    	for (Iterator<User> iter = s.users.iterator();iter.hasNext();){
        		u = (User)iter.next(); 
        		String verz = s.getDirectory("zuege") + File.separator + u.getName() + File.separator;
        		workVerz(verz);
        	}
		} catch (Throwable exc) { // any fatal error
            outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt            
            System.exit(1);
        }
        outText.addOutLine(FileCopy.getDateS() + " OK\n");
		
	}
	
	private static void workVerz(String verz){
    	// bekommt ein Userverzeichnis übergeben, die Settings sind global
    	File file = new File(verz);
    	File[] files = file.listFiles();
    	if (files!=null) {
    		if (files.length>0) {
		    	for (int i = 0; i < files.length; i++) {
		    		File actF = files[i];
		    		// if (actF.getName().endsWith(".cr") && actF.isFile() && !actF.getName().endsWith(".xt1.cr")) {
		    		if (actF.getName().endsWith(".cr") && actF.isFile() && !actF.getName().endsWith(".xt1.cr")) {
		    			// ein CR ist im Verzeichnis gefunden worden...
		    			outText.addOutLine("work on file: " + actF);
		    			workFile(actF);
		    		} else if (!actF.getName().endsWith(".cr") && actF.isFile()){
		    			outText.addOutLine("skipping: " + actF);
		    		}
		 		}
    		} else {
    			outText.addOutLine("dir empty: " + verz);
    		}
    	} else {
    		outText.addOutLine("no dir found: " + verz);
    	}
    }
	
    private static void workFile(File f){
    	// hier wir haben nun ein *.cr file was wir verkleinern wollen
    	// liste aller regionsCooridnaten holen
    	ArrayList<Tool3Coord> coordList = getSelections();
    	if (coordList==null || coordList.size()==0){
    		outText.addOutLine("nothing in selctions found for user " + u.getName() + " (file:" + f.toString() + ")");
    		return;
    	}
    	outText.addOutLine("Tool3 processing file:" + f.toString() + ", with " + coordList.size() + " coords to check.");
    	String newFileName = f.toString();
    	// cr weg
    	int l = newFileName.length();
    	newFileName = newFileName.substring(0,l-3) + ".xt1.cr";
    	outText.addOutLine("generating new file:" + newFileName);
    	BufferedReader in = null;
    	BufferedWriter out = null;
    	long cIN = 0;
		long cOUT = 0;
    	try {
    		String charset = getCharset(f);
    		in = new BufferedReader(new InputStreamReader(new FileInputStream(f),charset));
    		File newFile = new File(newFileName);
    		if (newFile.exists()){
    			if (newFile.delete()){
    				outText.addOutLine("existing " + newFile.toString() + " deleted");
    			} else {
    				outText.addOutLine("existing " + newFile.toString() + " NOT deleted");
    			}
    		} else {
    			outText.addOutLine("new file detected");
    		}
    		out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile),charset));
    		String line = in.readLine();
    		int mode = mode_writing;
    		while (line!=null){
    			cIN++;
    			
    			// check
    			if (line.startsWith("REGION")){
    				boolean isInList = isRegionLineOK(line, coordList);
    				if (mode==mode_writing){
    					// wir schreiben gerade
    					if (!isInList){
    						// wechseln zu skipping
    						mode = mode_skipping;
    					}
    				} else {
    					// wir skippen gerade
    					if (isInList){
    						// wechseln zum schreiben
    						mode = mode_writing;
    					}
    				}
    			}
    			
    			if (line.startsWith("MESSAGETYPE") || line.startsWith("TRANSLATION")){
    				mode=mode_writing;
    			}
    			
    			if (mode==mode_writing){
	    			out.write(line + "\n");
	    			cOUT++;
    			}
    			line = in.readLine();
    		}
    		in.close();
    		out.close();
    		if (f.renameTo(new File(f.toString()+"_procT3_" + FileCopy.getDateS()))){
    			outText.addOutLine("File renamed OK");
    		} else {
    			outText.addOutLine("File NOT renamed");
    		}
    	} catch (FileNotFoundException e){
    		outText.addOutLine("workSelFile not found:" + f.toString());
    		outText.addOutLine(e.toString());
    		return;
    	} catch (IOException e2){
    		outText.addOutLine("IO Exception found:" + f.toString());
    		outText.addOutLine(e2.toString());
    		return;
    	}
    	outText.addOutLine("Files closed. Read: " + cIN + " lines, wrote: " + cOUT + "lines");
    }
	
    private static boolean isRegionLineOK(String line, ArrayList<Tool3Coord> list){
    	boolean erg = false;
    	if (!line.startsWith("REGION")){
    		return erg;
    	}
    	line = line.substring(7);
    	Tool3Coord c = new Tool3Coord(line);
    	if (c.isSet()){
    		for (Tool3Coord e : list){
    			if (e.equals(c)){
    				return true;
    			}
    		}
    	}
    	return erg;
    }
    
    
    private static String getCharset(File f){
    	String erg = "ISO-8859-1";
    	BufferedReader in = null;
    	int counter = 0;
    	try {
    		in = new BufferedReader(new FileReader(f));
    		
    		String line = in.readLine();
    		while (line!=null && counter<10){
    			int i = line.indexOf(";charset");
    			if (i>1){
    				erg = line.substring(0, i-1).trim().replace("\"", "");
    				outText.addOutLine("charset info found in " + f.toString() + ", returning: " + erg);
    				in.close();
    				return erg;
    			}
    			line = in.readLine();
    		}
    		in.close();
    		outText.addOutLine("no charset info found in " + f.toString() + ", returning: " + erg);
    	} catch (FileNotFoundException e){
    		outText.addOutLine("workSelFile not found:" + f.toString());
    		outText.addOutLine(e.toString());
    		return erg;
    	} catch (IOException e2){
    		outText.addOutLine("IO Exception found:" + f.toString());
    		outText.addOutLine(e2.toString());
    		return erg;
    	}
    	
    	
    	
    	return erg;
    }
    
    private static ArrayList<Tool3Coord> getSelections(){
    	String VerzName = (s.getDirectory("selections") + File.separator + u.getName() + File.separator);
    	File verz = new File(VerzName);
    	File[] files = verz.listFiles();
    	int counter = 0;
    	ArrayList<Tool3Coord> erg = new ArrayList<Tool3Coord>();
    	if (files!=null) {
    		if (files.length>0) {
		    	for (int i = 0; i < files.length; i++) {
		    		File actF = files[i];
		    		if (actF.getName().endsWith(".sel") && actF.isFile()) {
		    			outText.addOutLine("work on selectionfile: " + actF);
		    			counter=0;
		    			ArrayList<Tool3Coord> toAdd = workSelFile(actF);
		    			if (toAdd!=null){
		    				for (Tool3Coord actCoord : toAdd){
		    					if (!erg.contains(actCoord)){
		    						erg.add(actCoord);
		    						counter++;
		    					}
		    				}
		    			}
		    			outText.addOutLine("added " + counter + " Coords in " + actF);
		    			
		    		}
		 		}
    		} else {
    			outText.addOutLine("dir empty: " + verz);
    		}
    	} else {
    		outText.addOutLine("nothing found: " + verz);
    	}
    	return erg;
    }
    
    private static ArrayList<Tool3Coord> workSelFile(File f){
    	BufferedReader in = null;
    	ArrayList<Tool3Coord> erg = null;
    	try {
    		in = new BufferedReader(new FileReader(f));
    		String line = in.readLine();
    		while (line!=null){
    			Tool3Coord C = new Tool3Coord(line);
    			if (C.isSet()){
    				if (erg==null){
    					erg = new ArrayList<Tool3Coord>();
    				}
    				erg.add(C);
    			}
    			line = in.readLine();
    		}
    		in.close();
    	} catch (FileNotFoundException e){
    		outText.addOutLine("workSelFile not found:" + f.toString());
    		outText.addOutLine(e.toString());
    		return null;
    	} catch (IOException e2){
    		outText.addOutLine("IO Exception found:" + f.toString());
    		outText.addOutLine(e2.toString());
    		return null;
    	}
    	return erg;
    }
	
}

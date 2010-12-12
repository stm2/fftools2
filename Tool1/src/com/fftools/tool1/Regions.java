package com.fftools.tool1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import magellan.client.actions.map.SaveSelectionAction;
import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.Region;

import com.fftools.OutTextClass;


public class Regions {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private Hashtable<CoordinateID,Region> selectedRegions = null;
	// der megagrosse report
	private GameData data = null;
	
	public Regions(GameData _data){
		this.data = _data;
	}
	
	public Hashtable<CoordinateID,Region> getSelectedRegions(){
		return this.selectedRegions;
	}
	
	public void clear() {
		if (selectedRegions!=null) {
			this.selectedRegions.clear();
		}
	}
	
	public int readVerz(String VerzName){
		int regCounter = 0;
		File verz = new File(VerzName);
    	File[] files = verz.listFiles();
    	this.clear();
    	if (files!=null) {
    		if (files.length>0) {
		    	for (int i = 0; i < files.length; i++) {
		    		File actF = files[i];
		    		if (actF.getName().endsWith(".sel") && actF.isFile()) {
		    			outText.addOutLine("work on selectionfile: " + actF);
		    			int cnt = this.workFile(actF);
		    			regCounter += cnt;
		    			outText.addOutLine("identified " + cnt + " Regions");
		    		}
		 		}
    		} else {
    			outText.addOutLine("dir empty: " + verz);
    		}
    	} else {
    		outText.addOutLine("nothing found: " + verz);
    	}
		return regCounter;
	}
	
	private int workFile(File actF){
		List<CoordinateID> coordinates = new LinkedList<CoordinateID>();
		int result = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(actF));

			while(true) {
				String line = br.readLine();

				if(line == null) {
					break;
				}

				if(line.indexOf(SaveSelectionAction.COMMENT) != -1) {
					// remove trailing comment
					line = line.substring(0, line.indexOf(SaveSelectionAction.COMMENT));
				}

				coordinates.add(CoordinateID.parse(line, SaveSelectionAction.DELIMITER));
			}

			br.close();
		} catch(Exception exc) {
			outText.addOutLine("could not read: " + actF.getName());
			outText.addOutLine(exc.toString());
		}
		for(Iterator<CoordinateID> iter = coordinates.iterator(); iter.hasNext();) {
			CoordinateID c = (CoordinateID) iter.next();

			if(data.regions().get(c) != null) {
				if (this.selectedRegions==null){
					this.selectedRegions = new Hashtable<CoordinateID,Region>();
				}
				selectedRegions.put(c, (Region)data.regions().get(c));
				result +=1;
			} else {
				outText.addOutLine("unknown Region in sel file " + actF.getName() + ":" + c.toString());
			}
		}
		return result;
	}
	
	
	public int saveSelection(File f){
		if (this.selectedRegions==null || this.selectedRegions.size()==0){
			outText.addOutLine("nothing to write in Regions.saveSelection. (" + f.getName()+")");
			return 0;
		}
		int cnt = 0;
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
			
			for(Iterator<CoordinateID> iter = selectedRegions.keySet().iterator(); iter.hasNext();) {
				pw.println(((CoordinateID) iter.next()).toString(SaveSelectionAction.DELIMITER));
				cnt++;
			}
			
			pw.close();
		} catch (IOException e) {
			outText.addOutLine("Fehler bei Schreiben der Selektion: " + e.toString());
			return 0;
		}
		return cnt;
	}
	
	public int getOverallRegionCount(){
		if (this.data==null){
			return 0;
		}
		if (this.data.regions()==null || this.data.regions().size()==0){
			return 0;
		}
		return this.data.regions().size();
	}
	
}

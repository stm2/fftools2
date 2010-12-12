package com.fftools.tool1;


import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.io.file.FileType;
import magellan.library.io.file.FileTypeFactory;
import magellan.library.utils.Encoding;
import magellan.library.utils.OrderWriter;

import com.fftools.OutTextClass;
import com.fftools.utils.FileCopy;

public class myFaction {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private String name = null;
	private String email = null;
	private String smtpHost = null;
	private String pass = null;
	private GameData data = null;
	
	private boolean sendOrders = true;
	private boolean nachfordern = true;
	
	public void setName(String _name){
		this.name = _name;
	}
	
	public String getName() {
		return this.name;
	}
	public void setEmail(String _name){
		this.email = _name;
	}
	
	public String getEmail() {
		return this.email;
	}
	public void setSMTPHost(String _name){
		this.smtpHost = _name;
	}
	
	public String getSMTPHost() {
		return this.smtpHost;
	}
	
	public String getPass() {
		return this.pass;
	}
	
	public void setPass(String _pass){
		this.pass = _pass;
	}
	
	public void setGameData(GameData _data) {
		this.data = _data;
	}
	
	public boolean makeToSend(GameData _data, Settings s, User u,LogMail log){
		this.data = _data;
		// checks
		if (this.name==null){
			outText.addOutLine("error myFaction makeToSend: for the faction no name (number) set.");
			log.addLog("error myFaction makeToSend: for the faction no name (number) set.");
			return false;
		}
		if (this.pass==null){
			outText.addOutLine("error myFaction makeToSend: for the faction no pass set.");
			log.addLog("error myFaction makeToSend: for the faction no pass set.");
			return false;
		}
		if (this.email==null){
			outText.addOutLine("error myFaction makeToSend: for the faction no email set.");
			log.addLog("error myFaction makeToSend: for the faction no email set.");
			return false;
		}
		if (this.smtpHost==null){
			outText.addOutLine("error myFaction makeToSend: for the faction no smtpHost set.");
			log.addLog("error myFaction makeToSend: for the faction no smtpHost set.");
			return false;
		}
		
		// die richtige faction finden..eigene Suche zur
		// Vermeidung der EntityID...
		Faction actF = null;
		for (Iterator<Faction> iter = this.data.factions().values().iterator();iter.hasNext();){
    		Faction F = (Faction)iter.next();
    		// outText.addOutLine("Test Faction ID:" + F.getID().toString() + " : " + F.getName());
    		if (F.getID().toString().equals(this.name)) {
    			// Treffer
    			actF = F;
    			break;
    		}
    	}	
		
		if (actF==null){
			outText.addOutLine("error myFaction makeToSend: faction not found:" + this.name);
			log.addLog("error myFaction makeToSend: faction not found:" + this.name);
			return false;
		}
		
		
		// OK..checken, ob überhaupt units dieser faction bestätigt sind und geschrieben werden sollen
		boolean somethingToWrite = false;
		for (Iterator<Unit> iter = actF.units().iterator();iter.hasNext();) {
			Unit actU = (Unit)iter.next();
			if (actU.isOrdersConfirmed()) {
				somethingToWrite = true;
				break;
			}
		}
		
		if (!somethingToWrite) {
			outText.addOutLine("for faction no confirmed units found:" + this.name);
			log.addLog("for faction no confirmed units found:" + this.name);
			return false;
		}
		
		
		// Test
		// outText.addOutLine("Test: myFaction makeToSend: faction to send:" + this.name);
		// hier weiter...write füttern..
		
		// passwort setzen
		actF.setPassword(this.getPass());
		// String myFileName = s.getDirectory("data") + File.separator + u.getName() + File.separator + "orders_" + this.name + FileCopy.getDateS() + ".sendMe.txt";
		String myFileName = s.getDirectory("data") + File.separator + u.getName() +  "_" + this.name + FileCopy.getDateS() + ".sendME";
		// File orderFile = new File(myFileName);
		
		try {
			// FileType fT = new FileType(new File(myFileName),false);
			FileType fT = FileTypeFactory.singleton().createFileType(new File(myFileName), false);
			// FileWriter FW = new FileWriter(myFileName,true);
			// BufferedWriter bFW = new BufferedWriter(FW);
			fT.setCreateBackup(false);
			// Default leads since UTF-8 to errors from server
			// Writer wR = fT.createWriter(FileType.DEFAULT_ENCODING);
			Writer wR = fT.createWriter(Encoding.ISO.toString());
			// Writer wR = fT.createWriter(FileType.UTF_8);
			if (write(wR, false,true, actF,log)) {
				outText.addOutLine("wrote orders: " + myFileName);
				log.addLog("wrote orders: " + myFileName);
			} else {
				outText.addOutLine("returned false: wrote orders: " + myFileName);
				log.addLog("returned false: wrote orders: " + myFileName);
				return false;
			}
			
			wR.flush();
			wR.close();
		
			
			return true;
			
		} catch(IOException ioe) {
			outText.addOutLine("Error writing orders: " + myFileName);
			outText.addOutLine(ioe.toString());
			log.addLog("Error writing orders: " + myFileName);
			return false;
		}
		
		
	}
	
	
	private boolean write(Writer stream, boolean forceUnixLineBreaks, boolean closeStream, Faction faction,LogMail log) {

		// Faction faction = (Faction) cmbFaction.getSelectedItem();
		
		try {
			OrderWriter cw = new OrderWriter(data, faction);
			cw.setAddECheckComments(true);
			cw.setRemoveComments(true, false);
			cw.setConfirmedOnly(true);
			cw.setWriteUnitTagsAsVorlageComment(false);

			cw.setForceUnixLineBreaks(forceUnixLineBreaks);
			cw.setWriteTimeStamp(false);

			int writtenUnits = cw.write(stream);
			/**
			if(closeStream) {
				stream.flush();
				stream.close();
			}
			*/
			int allUnits = 0;
			for(Iterator<Unit> iter = data.units().values().iterator(); iter.hasNext();) {
				Unit u = (Unit) iter.next();

				if(!(u instanceof TempUnit) && u.getFaction().equals(faction)) {
					allUnits++;
				}
			}
			outText.addOutLine("order Writer: wrote " + writtenUnits + " of " + allUnits + " units");
			log.addLog("order Writer: wrote " + writtenUnits + " of " + allUnits + " units");

		} catch(IOException ioe) {
			outText.addOutLine("Error order Writer");
			log.addLog("Error order Writer");
			return false;
		}

		return true;
	}

	/**
	 * @return the sendOrders
	 */
	public boolean isSendOrders() {
		return sendOrders;
	}

	/**
	 * @param sendOrders the sendOrders to set
	 */
	public void setSendOrders(boolean sendOrders) {
		this.sendOrders = sendOrders;
	}

	/**
	 * @return the nachfordern
	 */
	public boolean isNachfordern() {
		return nachfordern;
	}

	/**
	 * @param nachfordern the nachfordern to set
	 */
	public void setNachfordern(boolean nachfordern) {
		this.nachfordern = nachfordern;
	}
	
	
}

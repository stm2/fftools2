package com.fftools.nachfordern;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.fftools.OutTextClass;
import com.fftools.tool1.Server;
import com.fftools.tool1.Settings;
import com.fftools.tool1.myFaction;
import com.fftools.utils.FileCopy;

/*
 * Nachfordern soll nicht erhaltene Reports vom server nachfordern
 * dies soll deutlich *nach* den ersten erhaltenen reports passieren 
 * und schön kommuniziert werden
 * Vorraussetzungen:
 * 1. infoSent.txt ist vorhanden und älter als 1 h
 * 2. es müssen weniger als ReportsMax Reporte nachgefordert werden
 * verzeichnisse: (aus settings = toolsettings.config)
 * reports -> verzeichnis, in welchem die aktuellen reporte liegen sollen (als *.cr)
 * ReportsMax = MaxNachfordern (= 5)
 * Vorgehen
 * 1. Anzahl erwarteter Reports bestimmen (alle Factions in settings)
 * 2. Liste fehlender reports bestimmen
 * 3. Liste nachzufordernder reports bestimmen (Faction mit Password)
 * 4. Entscheidung, ob nachgefordert wird
 * 5. Falls noch nicht geschehen, info an monopol-server
 */



public class Nachfordern_Main {

	private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	private static Settings s = null;

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// sets thre output file
    	outText.setFile("FFTools_log_Nachfordern_" + FileCopy.getDateSDay());
		
		
		outText.addOutLine(FileCopy.getDateS() + " start Nachfordern");
		try {
			s = new Settings("toolsettings.config");
	    	s.sayStatus();
	    	// Voraussetzungen checken
	    	if (shouldIRun()){
	    		// welche fehlen insgesamt ?
	    		ArrayList<myFaction> missingFactions = getMissingFactionsList();
	    		if (missingFactions!=null && missingFactions.size()>0){
	    			// von denen ermitteln, wieviele nachgefordert werden könnten
	    			ArrayList<myFaction> nachfordernFactions = getNachfordernFactionsList(missingFactions);
	    			int maxNachfordern = Integer.parseInt(s.getDirectory("MaxNachfordern"));
	    			if (maxNachfordern<nachfordernFactions.size()){
	    				// kein Nachfordern...zu viele
	    				declineRequest(missingFactions.size(), nachfordernFactions, maxNachfordern);
	    			} else {
	    				// Nachfordern möglich
	    				doNachfordern(nachfordernFactions);
	    			}
	    		} else {
	    			outText.addOutLine("keine fehlenden reporte gefunden");
	    		}
	    	}
	    	
		} catch (Throwable exc) { // any fatal error
            outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt            
            System.exit(1);
        }
        outText.addOutLine(FileCopy.getDateS() + " OK\n");
	}
	
	/**
	 * Checked, ob Nachfordern an sich starten soll
	 * @return
	 */
	private static boolean shouldIRun(){
		// info sent muss da sein
		File testFile = new File(s.getDirectory("reports")+ File.separator + "infoSent.txt");
		if (!testFile.exists()){
			outText.addOutLine("infoSent.txt nicht vorhanden: " + s.getDirectory("reports")+ File.pathSeparator + "infoSent.txt");
			return false;
		}
		Integer i = Integer.parseInt(s.getDirectory("NachfordernWartezeit"));
		// in Millisekunden ausrechnen:
		int maxTime = i.intValue() * 60 * 1000;
		long lastMod = testFile.lastModified();
		Date d = new Date(lastMod);
		long actual = getActualServerFileDate().getTime();
		outText.addOutLine("lastMod date: " + d);
		outText.addOutLine("actual date: " + getActualServerFileDate());
		if (!(actual-lastMod>maxTime)){
			outText.addOutLine("infoSent nicht alt genug. Datei: " + lastMod + ", jetzt: " + actual + ", diff: " + (actual-lastMod) + ", maxDiff: " + maxTime);
			return false;
		} else {
			outText.addOutLine("infoSent OK. Datei: " + lastMod + ", jetzt: " + actual + ", diff: " + (actual-lastMod) + ", maxDiff: " + maxTime);			
		}
		return true;
	}
	
	/**
	 * Liefert eine Liste mit den Factions, deren Reporte *nicht* da sind
	 * @return
	 */
	private static ArrayList<myFaction> getMissingFactionsList(){
		if (s.factions==null || s.factions.size()==0){
			return null;
		}
		ArrayList<myFaction> erg = new ArrayList<myFaction>();
		for (myFaction f : s.factions){
			if (existReport(f)){
				outText.addOutLine("report gefunden: " + f.getName());
			} else {
				outText.addOutLine("report fehlt: " + f.getName());
				erg.add(f);
			}
		}
		return erg;
	}
	
	
	/**
	 * liefert Liste aller Factions, die nachfordern=OK haben (und passwort)
	 * aus der missingList
	 * @param missingList
	 * @return
	 */
	private static ArrayList<myFaction> getNachfordernFactionsList(ArrayList<myFaction> missingList){
		if (missingList==null || missingList.size()==0){
			return null;
		}
		ArrayList<myFaction> erg = new ArrayList<myFaction>();
		for (myFaction f : missingList){
			if (f.getPass()!=null && f.getPass().length()>0 && f.isNachfordern()){
				erg.add(f);
			} else {
				outText.addOutLine("nicht zum Nachfordern: " + f.getName());
			}
		}
		return erg;
	}
	
	
	/**
	 * liefert true, wenn zu der Faction ein CR in "reports" existiert
	 * sonst false
	 * @param f
	 * @return
	 */
	private static boolean existReport(myFaction f){
		File reportDir = new File(s.getDirectory("reports"));
		if (!reportDir.exists() || !reportDir.isDirectory()){
			outText.addOutLine("ReportDir nicht vorhanden oder kein Dir: " + reportDir);
			return false;
		}
		String[] fileNames = reportDir.list();
		if (fileNames==null || fileNames.length==0){
			outText.addOutLine("ReportDir ohne Dateien, Listing leer");
			return false;
		}
		
		for (String s : fileNames){
			String search = f.getName().toLowerCase() + ".cr";
			if (s.toLowerCase().endsWith(search)){
				return true;
			}
		}
		
		return false;
	}
	
	
	
	private static void declineRequest(int allMissing, ArrayList<myFaction> nachfordern, int maxNachfordern){
		String serverOUT = s.getDirectory("serverOUT");
		Server server = s.getServer(serverOUT);
		outText.addOutLine(FileCopy.getDateS() +  " sending MailMessage Nachfordern nicht moeglich");
		try {
		
			MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );
	    	
	        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
	        
	        String actFrom = server.getSender();
	        if (actFrom!=null){
	        	addressFrom=new InternetAddress(actFrom);
	        }
	        
	        msg.setFrom( addressFrom );
	        String emailAdress = s.getDirectory("NachfordernMailInfo");
	        InternetAddress addressTo = new InternetAddress(emailAdress);
	        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        
	        msg.setSubject("Statusreport Nachfordern (!)");
	        
	        String bodyText = "";
	        
	        bodyText+="Aktuell fehlen dem Monopol Server " + allMissing + " Reporte\n";
	        bodyText+="Für diese " + nachfordern.size() + " Parteien könnte nachgefordert werden:\n";
			for (myFaction f : nachfordern){
				bodyText+=f.getName() + " ";
			}
	        
	        bodyText+="\n\nLeider ist die maximale Anzahl nachzufordernder Reports: " + maxNachfordern + "\n";
	        bodyText+="Daher wird die automatische Nachforderung nicht gestartet.\n\n";
	        bodyText+="Wichtig: soll die automatische Nachforderung durch den Server\n";
	        bodyText+="komplett gestoppt werden, bitte eine Mail mit dem Betreff\n";
	        bodyText+="STOPP NACHFORDERN\n";
	        bodyText+="an den server (monopol@gdr-group.com) schicken. Danke.\n";
	        
	        
	        msg.setContent(bodyText, "text/plain" );
	        Transport.send( msg );
	        outText.addOutLine(FileCopy.getDateS() +  " sending OK ");
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
			outText.addOutLine(FileCopy.getDateS() +  " sending NOT OK:\n" + exc.toString());
            System.exit(1);
        }
	}
	
	
	
	
	/**
	 * fordert nach
	 * @param nachfordern
	 */
	private static void doNachfordern(ArrayList<myFaction> nachfordern){
		// nur was melden, wenn nicht gestoppt
		File testFile = new File(s.getDirectory("reports")+ File.separator + "stoppNachfordern.txt");
		if (testFile.exists()){
			outText.addOutLine("stoppNachfordern vorhanden");
			return;
		}
		// ok,  nicht gestoppt, also was melden
		// nur was melden, wenn nicht bereits nachgefordert
		testFile = new File(s.getDirectory("reports")+ File.separator + "nachgefordert.txt");
		if (testFile.exists()){
			outText.addOutLine("nachgefordert vorhanden");
			return;
		}
		// ok,  nicht gestoppt, also was melden
		String MailEresseaServer = s.getDirectory("eressea_server_email");
		String MailMonopolServer = s.getDirectory("NachfordernMailInfo");
		String serverOUT = s.getDirectory("serverOUT");
		
		// String MailEresseaServer = "sf@fietefietz.de";
		// String MailMonopolServer = "sf@fietefietz.de";
		
		// 1. NachforderungsMails
		for (myFaction f : nachfordern){
			doNachfordernFaction(f, MailEresseaServer,s.getServer(serverOUT));
		}
		
		// 2. info an Monopol
		// informNachfordern(nachfordern, MailMonopolServer);
		informNachfordern(nachfordern, MailMonopolServer,s.getServer(serverOUT));
		
		// 3. nachgefordert.txt schreiben
		File myF = new File(s.getDirectory("reports") + File.separator + "nachgefordert.txt");
		try {
			FileWriter fW = new FileWriter(myF,true);
			BufferedWriter ausgabe = new BufferedWriter(fW);
			ausgabe.write("Nachforderungen durchgeführt für:");
			ausgabe.newLine();
			for (myFaction f : nachfordern){
				ausgabe.write(f.getName() + " ");
			}
			ausgabe.newLine();
			ausgabe.write(FileCopy.getDateS());
			ausgabe.newLine();
			ausgabe.close();
			outText.addOutLine("nachgefordert.txt geschrieben");
		} catch (IOException el){
			outText.addOutLine("fehler beim schreiben der nachgefordert.txt");
			outText.addOutLine(el.toString());
		}
	}
	
	
	
	private static void doNachfordernFaction(myFaction f, String adress, Server server){
		outText.addOutLine(FileCopy.getDateS() +  " sending MailMessage Nachfordern fuer " + f.getName());
		try {
		
			MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );
	    	/* Derzeit mit Absender vom Monopol senden, damit nachgeforderter Report
	    	 * dort eingeht
	    	 * später komplett wechseln ?
	    	 */
	        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
	        /*
	        String actFrom = server.getSender();
	        if (actFrom!=null){
	        	addressFrom=new InternetAddress(actFrom);
	        }
	        */
	        msg.setFrom( addressFrom );
	        
	        InternetAddress addressTo = new InternetAddress(adress);
	        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        
	        msg.setSubject("eressea report " + f.getName() + " \"" + f.getPass() + "\"");
	        
	        String bodyText = "";
	        bodyText+="Automatisch generierte Mail des Servers des Monopols\n";
	        bodyText+="bei Fragen, Anregungen oder Kritik zu dieser Mail bitte Kontakt aufnehmen:\n";
	        bodyText+="sf@fietefietz.de\n";
	        bodyText+="Danke\n";
	        bodyText+="Fiete\n";
	        
	        
	        msg.setContent(bodyText, "text/plain" );
	        Transport.send( msg );
	        outText.addOutLine(FileCopy.getDateS() +  " sending OK: " + f.getName());
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
			outText.addOutLine(FileCopy.getDateS() +  " sending NOT OK:\n" + exc.toString());
            System.exit(1);
        }
		
	}
	
	
	
	
	
	
	private static void informNachfordern(ArrayList<myFaction> nachfordern, String adress, Server server){
		
		outText.addOutLine(FileCopy.getDateS() +  " sending MailMessage informNachfordern");
		try {
		
			MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );
	
	        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
	        String actFrom = server.getSender();
	        if (actFrom!=null){
	        	addressFrom=new InternetAddress(actFrom);
	        }
	        msg.setFrom( addressFrom );
	        
	        InternetAddress addressTo = new InternetAddress(adress);
	        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        
	        msg.setSubject("Statusreport Nachfordern");
	        
	        String bodyText = "";
	        
	        bodyText+="Der Monopol server hat soeben " + nachfordern.size() + " Reporte beim Eressea Server nachgefordert.\n";
	        bodyText+="Dies geschah für folgende Parteien:\n";
			for (myFaction f : nachfordern){
				bodyText+= f.getName() + " ";
			}
			bodyText+="\n\n";
			bodyText+="Weitere (automatische) Nachforderungsversuche wird es bis auf Weiteres nicht geben.\n";
	        msg.setContent(bodyText, "text/plain" );
	        Transport.send( msg );
	        outText.addOutLine(FileCopy.getDateS() +  " sending OK");
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
			outText.addOutLine(FileCopy.getDateS() +  " sending NOT OK:\n" + exc.toString());
            System.exit(1);
        }
        
	}
	
	
	
	private static Date getActualServerFileDate(){
		File myF = new File(s.getDirectory("reports") + File.separator + "test.txt");
		try {
			FileWriter fW = new FileWriter(myF,true);
			BufferedWriter ausgabe = new BufferedWriter(fW);
			ausgabe.write("test:");
			ausgabe.newLine();
			
			ausgabe.close();
			return new Date(myF.lastModified());
		} catch (IOException el){
			outText.addOutLine("test.txt");
			outText.addOutLine(el.toString());
			
		}
		return new Date();
	}
}

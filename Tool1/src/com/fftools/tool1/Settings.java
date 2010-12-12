package com.fftools.tool1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import com.fftools.OutTextClass;


public class Settings {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	public ArrayList<User> users = null;
	public ArrayList<myFaction> factions = null;
	public ArrayList<Server> servers = null;
	private Hashtable<String,String> directories = null;
	
	private BufferedReader in = null;
	
	public Settings(String FileName){
		try {
			in = new BufferedReader(new FileReader(FileName));
			String line = in.readLine();
			while (line!=null){
				if (line.equalsIgnoreCase("<user>")) {parseUser();}
				if (line.equalsIgnoreCase("<faction>")) {parseFaction();}
				if (line.equalsIgnoreCase("<directories>")) {parseDirectories();}
				if (line.equalsIgnoreCase("<server>")) {parseServer();}
				line = in.readLine();
			}
			in.close();
		} catch (FileNotFoundException e) {
			outText.addOutLine(FileName + " Problem: " +  e.toString());
			System.exit(1);
		} catch (IOException e2) {
			outText.addOutLine(FileName + " Problem: " +  e2.toString());
			System.exit(1);
		}
	}
	
	private void parseUser(){
		User u = null;
		try {
			String line = in.readLine();
			while (line!=null && !line.equalsIgnoreCase("</user>")) {
				if (line.startsWith("<name=")) {
					if (u==null){u = new User();}
					String name = line.substring(6,line.length()-1);
					u.setName(name);
				}
				if (line.startsWith("<pass=")) {
					if (u==null){u = new User();}
					String name = line.substring(6,line.length()-1);
					u.setPass(name);
				}
				line = in.readLine();
			}
			if (u!=null){this.addUser(u);}
		} catch (IOException e) {
			outText.addOutLine(" parseUser Problem: " +  e.toString());
			System.exit(1);
		}
	}
	
	private void parseFaction(){
		myFaction f = null;
		try {
			String line = in.readLine();
			while (line!=null && !line.equalsIgnoreCase("</faction>")) {
				if (line.startsWith("<name=")) {
					if (f==null){f = new myFaction();}
					String name = line.substring(6,line.length()-1);
					f.setName(name);
				}
				if (line.startsWith("<email=")) {
					if (f==null){f = new myFaction();}
					String name = line.substring(7,line.length()-1);
					f.setEmail(name);
				}
				if (line.startsWith("<smtp=")) {
					if (f==null){f = new myFaction();}
					String name = line.substring(6,line.length()-1);
					f.setSMTPHost(name);
				}
				if (line.startsWith("<pass=")) {
					if (f==null){f = new myFaction();}
					String name = line.substring(6,line.length()-1);
					f.setPass(name);
				}
				if (line.equalsIgnoreCase("<orders=no>")){
					if (f==null){f = new myFaction();}
					f.setSendOrders(false);
				}
				if (line.equalsIgnoreCase("<nachfordern=no>")){
					if (f==null){f = new myFaction();}
					f.setNachfordern(false);
				}
				line = in.readLine();
			}
			if (f!=null){this.addFaction(f);}
		} catch (IOException e) {
			outText.addOutLine(" parseFaction Problem: " +  e.toString());
			System.exit(1);
		}
	}
	
	
	private void parseServer(){
		Server server = null;
		try {
			String line = in.readLine();
			while (line!=null && !line.equalsIgnoreCase("</server>")) {
				if (line.startsWith("<name=")) {
					if (server==null){server = new Server();}
					String name = line.substring(6,line.length()-1);
					server.setName(name);
				}
				if (line.startsWith("<host=")) {
					if (server==null){server = new Server();}
					String name = line.substring(6,line.length()-1);
					server.setHost(name);
				}
				if (line.startsWith("<user=")) {
					if (server==null){server = new Server();}
					String name = line.substring(6,line.length()-1);
					server.setUser(name);
				}
				if (line.startsWith("<plainPass=")) {
					if (server==null){server = new Server();}
					String name = line.substring(11,line.length()-1);
					server.setPass(name);
				}
				if (line.startsWith("<procDir=")) {
					if (server==null){server = new Server();}
					String name = line.substring(9,line.length()-1);
					server.setProcDir(name);
				}
				if (line.startsWith("<sender=")) {
					if (server==null){server = new Server();}
					String name = line.substring(8,line.length()-1);
					server.setSender(name);
				}
				line = in.readLine();
			}
			if (server!=null){this.addServer(server);}
		} catch (IOException e) {
			outText.addOutLine(" parseServer Problem: " +  e.toString());
			System.exit(1);
		}
	}
	
	private void addUser(User u){
		if (this.users == null) {
			this.users = new ArrayList<User>();
		}
		this.users.add(u);
	}
	
	private void addFaction(myFaction f){
		if (this.factions == null) {
			this.factions = new ArrayList<myFaction>();
		}
		this.factions.add(f);
	}
	
	private void addServer(Server server){
		if (this.servers == null) {
			this.servers = new ArrayList<Server>();
		}
		this.servers.add(server);
	}
	
	
	private void parseDirectories() {
		try {
			String line = in.readLine();
			while (line!=null && !line.equalsIgnoreCase("</directories>")) {
				if (!line.startsWith("#")){
					line = line.substring(1, line.length()-1);
					int i = line.indexOf("=");
					String s1 = line.substring(0,i);
					String s2 = line.substring(i+1);
					if (this.directories==null) {
						this.directories = new Hashtable<String, String>();
					}
					this.directories.put(s1, s2);
				}
				line = in.readLine();
			}
			
		} catch (IOException e) {
			outText.addOutLine(" parseUser Problem: " +  e.toString());
			System.exit(1);
		}
	}
	
	public String getDirectory(String key){
		String erg = "";
		if (this.directories!=null){
			if (this.directories.containsKey(key)) {
				erg = this.directories.get(key);
			} else {
				outText.addOutLine("no DIR found for: " + key);
			}
			
		} else {
			outText.addOutLine("no DIRs known: " + key);
		}
		return erg;
	}
	
	public Server getServer(String name){
		if (this.servers==null || this.servers.size()==0){
			return null;
		}
		
		for (Server s:this.servers){
			if (s.getName().equalsIgnoreCase(name)){
				return s;
			}
		}
		
		return null;
	}
	
	
	public void sayStatus(){
		outText.addOutLine("Settings Status: ");
		if (this.users==null) {
			outText.addOutLine("*keine user");
		} else {
			outText.addOutLine("*user:" + this.users.size());
		}
		if (this.factions==null) {
			outText.addOutLine("*keine factions");
		} else {
			outText.addOutLine("*factions:" + this.factions.size());
		}
		if (this.servers==null) {
			outText.addOutLine("*keine server");
		} else {
			outText.addOutLine("*server:" + this.servers.size());
		}
		
	}
	/**
	 * 
	 * Findet Faction anhand Factionname (Base36 nummer)
	 * 
	 * 
	 * @param factionName
	 * @return myFaction, or null of not found or factions = null
	 */
	public myFaction findFaction(String factionName) {
		myFaction erg = null;
		
		if (this.factions==null) {
			return erg;
		}
		
		for (Iterator<myFaction> iter = this.factions.iterator();iter.hasNext();){
			myFaction actF = (myFaction)iter.next();
			if (actF.getName().equalsIgnoreCase(factionName)) {
				return actF;
			}
		}
		
		return erg;
	}
	
	/**
	 * Liefert User anhand des Paswortes
	 * @param pass
	 * @return
	 */
	public User getUser(String pass){
		if (this.users==null || this.users.size()==0){
			return null;
		}
		
		for (User u:this.users){
			if (u.getPass().equals(pass)){
				return u;
			}
		}
		
		return null;
	}
	
	
	
	/**
	 * 
	 * Findet Faction anhand Factionname (Base36 nummer)
	 * 
	 * 
	 * @param factionName
	 * @return myFaction, or null of not found or factions = null
	 */
	public boolean isKnownFaction(String factionName) {
		boolean erg = false;
		
		if (this.factions==null) {
			return erg;
		}
		
		for (Iterator<myFaction> iter = this.factions.iterator();iter.hasNext();){
			myFaction actF = (myFaction)iter.next();
			if (actF.getName().equalsIgnoreCase(factionName)) {
				return true;
			}
		}
		
		return erg;
	}
	
	/**
	 * Liefert true, wenn passwort für diese Partei bekannt ist
	 * @param factionName (Base 36 number)
	 * @return
	 */
	public boolean isFactionWithPassword(String factionName){
		boolean erg = false;
		
		if (this.factions==null) {
			return erg;
		}
		
		for (Iterator<myFaction> iter = this.factions.iterator();iter.hasNext();){
			myFaction actF = (myFaction)iter.next();
			if (actF.getName().equalsIgnoreCase(factionName) && actF.getPass()!=null && actF.getPass().length()>1) {
				return true;
			}
		}
		return erg;
	}
	
	
}

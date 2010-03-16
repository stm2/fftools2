package com.fftools.overlord;


import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.io.cr.CRParser;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.pools.akademie.AkademieManager;
import com.fftools.pools.alchemist.AlchemistManager;
import com.fftools.pools.ausbildung.AusbildungsManager;
import com.fftools.pools.ausbildung.LernplanHandler;
import com.fftools.pools.bau.BauManager;
import com.fftools.pools.circus.CircusPoolManager;
import com.fftools.pools.heldenregionen.HeldenRegionsManager;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.pools.pferde.PferdeManager;
import com.fftools.pools.treiber.TreiberPoolManager;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.transport.TransportManager;


/**
 * 
 * Klasse handelt Manager und Scripts
 * F�r Scripts zu INfozwecken
 * 
 * @author Fiete
 *
 */
public class Overlord {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private ArrayList<OverlordInfo> infoObjects = null;
	private ArrayList<OverlordRun> runnings = null;
	
	private int maxDurchLauf = -1;
	
	private ScriptMain scriptMain = null;
	
	/**
	 * die manager
	 */
	private MatPoolManager matPoolManager = null;
	private CircusPoolManager circusPoolManager = null;
	private TreiberPoolManager treiberPoolManager = null;
	private TransportManager transportManager = null;
	private AusbildungsManager ausbildungsManager=null; 
	private AlchemistManager alchemistManager = null;
	private AkademieManager akademieManager  = null;
	private PferdeManager pferdeManager = null;
	private HeldenRegionsManager heldenRegionsManager = null;
	private BauManager bauManager = null;
	
	/**
	 * die handler
	 */
	private TradeAreaHandler tradeAreaHandler = null;
	private LernplanHandler lernplanHandler = null;
	
	/**
	 * Hilfslisten
	 */
	private ArrayList<ScriptUnit> deletedUnits = null;
	
	
	/**
	 * Konstruktor
	 * @param scM
	 */
	public Overlord(ScriptMain scM){
		this.scriptMain = scM;
	}
	
	/**
	 * start des durchlaufes
	 * pro durchlauf: erst scripte, dann manager
	 *
	 */
	public void run(){
		if (maxDurchLauf<0){
			outText.addOutLine("Overlord: run nicht m�glich: kein Max Durchlauf");
			return;
		}
		if (this.scriptMain.getScriptUnits()==null){
			outText.addOutLine("Overlord: run nicht m�glich: keine Scriptunits");
			return;
		}
		
		// Scriptgeteuerte Tags 3, 4 und 5 zur�cksetzen, weil diese bei jedem scriptlauf neu vergeben werden.
		for (Iterator<ScriptUnit> iter = this.scriptMain.getScriptUnits().values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			if (!isDeleted(scrU)){ 
				 // Tag3 l�schen, wenn er vergeben ist
				 if (scrU.getUnit().containsTag(CRParser.TAGGABLE_STRING3)){
					 scrU.getUnit().removeTag(CRParser.TAGGABLE_STRING3);			 
				 }
				 
				// Tag4 l�schen, wenn er vergeben ist
				 if (scrU.getUnit().containsTag(CRParser.TAGGABLE_STRING4)){
					 scrU.getUnit().removeTag(CRParser.TAGGABLE_STRING4);			 
				 }
				 
				// Tag5 l�schen, wenn er vergeben ist
				 if (scrU.getUnit().containsTag(CRParser.TAGGABLE_STRING5)){
					 scrU.getUnit().removeTag(CRParser.TAGGABLE_STRING5);			 
				 }
			}
		}
		
		
		for (int mainDurchlauf = 0;mainDurchlauf<Integer.MAX_VALUE;mainDurchlauf++){
			// scriptunits anstossen
			for (Iterator<ScriptUnit> iter = this.scriptMain.getScriptUnits().values().iterator();iter.hasNext();){
				ScriptUnit scrU = (ScriptUnit)iter.next();
				if (!isDeleted(scrU)){
					scrU.runScripts(mainDurchlauf);
					outText.addPoint();
				}
			}
			
			// manager laufen lassen ?!
			if (this.runnings!=null){
				for (Iterator<OverlordRun> iter = this.runnings.iterator();iter.hasNext();){
					Object o = iter.next();
					OverlordInfo oI = (OverlordInfo)o;
					if (isInRun(oI, mainDurchlauf)){
						OverlordRun oR = (OverlordRun)o;
						oR.run(mainDurchlauf);
					}
				}
			}
			
			// maximalen Durchlauf erreicht?
			if (mainDurchlauf>maxDurchLauf){
				// Abbruch
				break;
			}
			outText.addOutChars("," + mainDurchlauf);
		}
		
		
		
	}
	
	
	/**
	 * f�gt ein script hinzu, wenn es nicht bereits da ist
	 * @param s
	 */
	public void addOverlordInfo(OverlordInfo s){
		if (this.infoObjects==null){
			this.infoObjects = new ArrayList<OverlordInfo>(2);
		}
		// jedes script nur einmal...am namen erkennen
		boolean schonda = false;
		for (Iterator<OverlordInfo> iter = this.infoObjects.iterator();iter.hasNext();){
			OverlordInfo myS = (OverlordInfo)iter.next();
			if (myS.getClass().getName().equals(s.getClass().getName())){
				schonda = true;
				break;
			}
		}
		if (!schonda){
			this.infoObjects.add(s);
			this.checkMaxDurchlauf(s);
		}
	}
	
	/**
	 * �berpr�ft und setzt gegebenenfalls die max Anzahl Durchl�ufe
	 * @param s
	 */
	private void checkMaxDurchlauf(OverlordInfo s){
		if (s.runAt()==null){return;}
		for (int i = 0;i<=s.runAt().length-1;i++){
			int actX = (int)s.runAt()[i];
			if (actX>this.maxDurchLauf){
				this.maxDurchLauf = actX;
			}
		}
	}
	
	/**
	 * Ist eine OverlordInfo im aktuellen Durchlauf enthalten?
	 * @param s
	 * @param check
	 * @return true or false
	 */
	private boolean isInRun(OverlordInfo s,int check){
		if (s.runAt()==null){return false;}
		for (int i = 0;i<s.runAt().length;i++){
			int actX = (int)s.runAt()[i];
			if (actX==check){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Kurze Info, was wann lief
	 *
	 */
	public void informUs(){
		if (this.infoObjects==null){
			outText.addOutLine("Scriptinfo: keine scripte bekannt");
			return;
		}
		outText.addOutLine("Overlord Skriptinfos");
		String names = "";
		for (int d = 0;d<=this.maxDurchLauf;d++){
			// scripts
			names = "";
			for (Iterator<OverlordInfo> iter = this.infoObjects.iterator();iter.hasNext();){
				OverlordInfo myS = (OverlordInfo)iter.next();
				if (this.isInRun(myS, d)){
					// treffer
					if (names.length()>1){
						names+=",";
					}
					names+=this.getSimpleClassName(myS.getClass());
				}
			}
			if (names.length()>1){
				outText.addOutLine(d + ":" + names);
			}
			// Managers
			names = "";
			if (this.runnings!=null){
				for (Iterator<OverlordRun> iter = this.runnings.iterator();iter.hasNext();){
					OverlordInfo myS = (OverlordInfo)iter.next();
					if (this.isInRun(myS, d)){
						// treffer
						if (names.length()>1){
							names+=",";
						}
						names+=this.getSimpleClassName(myS.getClass());
					}
				}
			}
			if (names.length()>1){
				outText.addOutLine(d + "->Manager:" + names);
			}
			
		}
		names = "";
		for (Iterator<OverlordInfo> iter = this.infoObjects.iterator();iter.hasNext();){
			OverlordInfo myS = (OverlordInfo)iter.next();
			if (myS.runAt()==null){
				// treffer
				if (names.length()>1){
					names+=",";
				}
				names+=this.getSimpleClassName(myS.getClass());
			}
		}
		if (names.length()>1){
			outText.addOutLine("no info:" + names);
		}
	}
	
	
	private String getSimpleClassName(Class<?> s){
		String work = s.getName();
		int i = work.lastIndexOf(".");
		if (i>0){
			work = work.substring(i+1);
		}
		return work;
	}

	/**
	 * f�gt einen auszuf�hrenden (Manager) hinzu
	 * @param o der Manager
	 */
	private void addRunner(OverlordRun o){
		if (this.runnings==null){
			this.runnings = new ArrayList<OverlordRun>(1);
		}
		if (!this.runnings.contains(o)){
			this.runnings.add(o);
			// wenn es Instance vonm OverlordInfo ist, dann maxRun anpassen
			if (o instanceof OverlordInfo) {
				OverlordInfo oI = (OverlordInfo)o;
				this.checkMaxDurchlauf(oI);
			}
		}
	}
	
	/**
	 * @return the circusPoolManager
	 */
	public CircusPoolManager getCircusPoolManager() {
		if (circusPoolManager==null){
			  circusPoolManager = new CircusPoolManager(this.scriptMain);
			  this.addRunner(circusPoolManager);
		}
		return circusPoolManager;
	}

	/**
	 * @return the circusPoolManager
	 */
	public TreiberPoolManager getTreiberPoolManager() {
		if (treiberPoolManager==null){
			  treiberPoolManager = new TreiberPoolManager(this.scriptMain);
			  this.addRunner(treiberPoolManager);
		}
		return treiberPoolManager;
	}
	

	/**
	 * @return the matPoolManager
	 */
	public MatPoolManager getMatPoolManager() {
		if (this.matPoolManager==null){
			this.matPoolManager = new MatPoolManager(this.scriptMain);
			this.addRunner(matPoolManager);
		}
		return matPoolManager;
	}


	/**
	 * @return the scriptMain
	 */
	public ScriptMain getScriptMain() {
		return scriptMain;
	}


	/**
	 * @return the tradeAreaHandler
	 */
	public TradeAreaHandler getTradeAreaHandler() {
		if (tradeAreaHandler==null){
			tradeAreaHandler = new TradeAreaHandler(scriptMain);
			// diesen nur in die scripts mit aufnehmen
			this.addOverlordInfo(tradeAreaHandler);
		}
		return tradeAreaHandler;
	}

	/**
	 * @return the lernplanHandler
	 */
	public LernplanHandler getLernplanHandler() {
		if (lernplanHandler==null){
			lernplanHandler = new LernplanHandler();
			// diesen nur in die scripts mit aufnehmen
			this.addOverlordInfo(lernplanHandler);
		}
		return lernplanHandler;
	}
	
	
	/**
	 * @return the transportManager
	 */
	public TransportManager getTransportManager() {
		if (this.transportManager==null){
			this.transportManager = new TransportManager(this.scriptMain);
			this.addRunner(this.transportManager);
		}
		return transportManager;
	}
	
	/**
	 * @return the ausbildungsManager
	 */
	public AusbildungsManager getAusbildungsManager() {
		if (this.ausbildungsManager==null){
			this.ausbildungsManager = new AusbildungsManager(this.scriptMain);
			this.addRunner(this.ausbildungsManager);
		}
		return ausbildungsManager;
	}
	
		
	/**
	 * 
	 * @return the alchemistManager
	 */
	public AlchemistManager getAlchemistManager(){
		if (this.alchemistManager==null){
			this.alchemistManager = new AlchemistManager(this);
			this.addRunner(this.alchemistManager);
		}
		return this.alchemistManager;
	}
	
	
	/**
	 * 
	 * @return the akademieManager
	 */
	public AkademieManager getAkademieManager(){
		if (this.akademieManager==null){
			this.akademieManager = new AkademieManager(this.scriptMain);
			this.addRunner(this.akademieManager);
		}
		return this.akademieManager;
	}
	
	/**
	 * 
	 * @return the PferdeManager
	 */
	public PferdeManager getPferdeManager(){
		if (this.pferdeManager==null){
			this.pferdeManager = new PferdeManager(this);
			this.addRunner(this.pferdeManager);
		}
		return this.pferdeManager;
	}

	/**
	 * @return the heldenRegionsManager
	 */
	public HeldenRegionsManager getHeldenRegionsManager() {
		if (this.heldenRegionsManager==null){
			this.heldenRegionsManager = new HeldenRegionsManager(this.scriptMain);
			this.addRunner(this.heldenRegionsManager);
		}
		return heldenRegionsManager;
	}

	/**
	 * @return the bauManager
	 */
	public BauManager getBauManager() {
		if (this.bauManager==null){
			this.bauManager = new BauManager(this.scriptMain);
			this.addRunner(this.bauManager);
		}
		return bauManager;
	}

	
	/**
	 * setzt die ScriptUnit auf die Liste gel�schter Units
	 * sollte ab jetzt ignoriert werden vom gesammten Script
	 * @param u
	 */
	public void deleteScriptUnit(ScriptUnit u){
		if (this.deletedUnits==null){
				this.deletedUnits = new ArrayList<ScriptUnit>();
		}
		if (!this.deletedUnits.contains(u)){
			this.deletedUnits.add(u);
		}
		
	}
	
	public boolean isDeleted(ScriptUnit u){
		if (this.deletedUnits==null || this.deletedUnits.size()==0){
			return false;
		}
		if (this.deletedUnits.contains(u)){
			return true;
		}
		return false;
	}
	
	
}

package com.fftools.pools.treiber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.LuxuryPrice;
import magellan.library.Region;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.circus.CircusPool;
import com.fftools.trade.TradeArea;
import com.fftools.trade.Trader;


/**
 * Klasse die Unterhalter einer Region koordiniert und selbst 
 * von CircusPoolManager verwaltet wird
 * @author Marc
 *
 */

public class TreiberPool {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	
	// der PoolManager sammelt die Pools 
	public TreiberPoolManager treiberPoolManager;
	
	// Region f�r die der Pool arbeiten soll
	public Region region = null;
	
	// Liste mit den TreiberPoolRelations
	private ArrayList <TreiberPoolRelation> listOfRelations = null;
	
	private int regionMaxTreiben;
	
	// Wird vom Pool runtergez�hlt bis alles abgesch�pft ist.
	private int remainingTreiben;	
	
	private int regionsVerdienst=0;
	
	// falls Region nicht komplett von Monopol bewirtschaftet wird
	private int limit = 100000;
	
	// das wievielfache des w�chentlichen einkommens soll in der Region bevorratet werden?
	private int default_faktor_silberbestand_region = 3;
	private int faktor_silberbestand_region = default_faktor_silberbestand_region;
	
	private boolean silberknapp=false;
	
	// Keplers berechnung des TreibenMax
	private int LUX=0;
	private int UNT=0;
	private boolean keplerMode = false;
	private boolean keplerCalcRun = false;
	private ArrayList<String> keplerInfo = new ArrayList<String>();
	
	
	/**
	 * Konstruktor
	 * @param _cpm
	 * @param _region
	 */
	public TreiberPool(TreiberPoolManager _cpm, Region _region ){
		treiberPoolManager = _cpm;
		
		region = _region;
		regionMaxTreiben = (region.getPeasantWage()-10)*region.getModifiedPeasants();
		if (region.getTrees()>0){
			regionMaxTreiben-=region.getTrees() * 8 * 10;
		}
		if (region.getSprouts()>0){
			regionMaxTreiben-=region.getSprouts() * 4 * 10;
		}
		
		remainingTreiben = region.getSilver();
		// zun�chst gleich Max, damit mehrfache Aenderungen auffallen!
		limit = regionMaxTreiben;
	}
	
	
	/**
	 * F�gt dem TreiberPool eine TreiberPoolRelation hinzu
	 * @param _cpr TreiberPoolRelation 
	 */
	public void addTreiberPoolRelation(TreiberPoolRelation _cpr){
		// gibt es schon eine liste mit Relations? Falls nicht anlegen!
		if (listOfRelations == null){
	    	listOfRelations = new ArrayList <TreiberPoolRelation>();
		}
	    
		listOfRelations.add(_cpr);	
	}
	
	/**
	 * Zentrale Methode zum TreiberPool anstossen
	 *
	 */
	public void runPool(){
		
		
		
		// Check ob es Relations gibt
		if (this.listOfRelations != null){
			 
			// Sortiert Relations nach Talentwert
			// sortieren hinter != null gelegt, damit nix leeres sortiert wird
			Collections.sort(this.listOfRelations);
			
			// wenn der faktor = default, in scripteroptions schauen
			if (getFaktor_silberbestand_region()==default_faktor_silberbestand_region){
				int reportFaktor = reportSettings.getOptionInt("Faktor_Silberbestand_Region", region); 
				if (reportFaktor>0){
					this.faktor_silberbestand_region =reportFaktor;
				}
			}
			
			if (!keplerMode){
			
				// Neuberchnung des maximal zu verdienenden Silbers
				// wir lassen einen gewissen Vorrat in Abh�ngig des Steuereinkommens zu
				if (regionMaxTreiben>0){
					remainingTreiben = region.getSilver() - (regionMaxTreiben * getFaktor_silberbestand_region());
					if (remainingTreiben<=0){
						// zu wenig Silber!
						this.silberknapp=true;
					} 
				} else {
					remainingTreiben=0;
				}
			} else {
				remainingTreiben=regionMaxTreiben;
			}
			
			int regionMaxTreibenAll=remainingTreiben;
			//Durchiterieren der cpr   
			for (Iterator<TreiberPoolRelation> iter1 = this.listOfRelations.iterator();iter1.hasNext();){
			     TreiberPoolRelation cpr = (TreiberPoolRelation) iter1.next();
	             
			     // Ist Silber der Bauern verschleudert?
			     if (remainingTreiben <= 0){
			    	 // Sende Vorzeichen-Signal an Relation und damit an Script
			    	 cpr.setDoTreiben(-1);
			     }
			     if (!keplerMode){
			       cpr.getSkriptUnit().addComment("freigegeben zum Treiben in Reg Region: " + regionMaxTreibenAll);
			       cpr.getSkriptUnit().addComment("Silberstatus: regionMaxTreiben=" + regionMaxTreiben + ", Faktor: " + getFaktor_silberbestand_region() + ", Bestand: " + region.getSilver());
			     }
                  //   Es ist Silber da aber zu wenig f�r die Leistung der Einheit.
			     if ((remainingTreiben > 0) && (cpr.getVerdienst() > remainingTreiben)){
			    	 
			    	 cpr.setDoTreiben(remainingTreiben);
			    	 regionsVerdienst = regionsVerdienst + remainingTreiben;
			    	 // Silber der Bauern ist aufgebraucht...
			    	 remainingTreiben = 0;
			     }
			     
			     
			     // Check: Noch Silber zu verdienen AND Verdienst geringer als Restsilber?
			     if ((remainingTreiben > 0) && (cpr.getVerdienst()<= remainingTreiben)){
			    	 // Unterhalte soviel wie geht!
			    	 cpr.setDoTreiben(cpr.getVerdienst());
			    	 regionsVerdienst = regionsVerdienst + cpr.getVerdienst();
			    	 // Freies Silber reduzieren um Unterhalungsverm�gen der Einheit 
			    	 remainingTreiben = remainingTreiben - cpr.getVerdienst();
			       
			     }
			    
			
		     }
		
		}	
	
	}

/**
 * 
 * Gibt den Verdienst aller Treiber in der Region zur�ck
 */	
public int getRegionsVerdienst(){
	return regionsVerdienst;
}

public int getRemainingTreiben(){
	return remainingTreiben;
}
	
public int getUnterhaltungslimit(){
	return limit;
}

/**
 * 
 * Setzt limit und korrigiert remainigTreiben
 * 
 */
public void setTreibenLimit(int _lim){
	limit = _lim;
	if (limit < remainingTreiben){
		remainingTreiben = limit;
	}
}

/**
 * Gibt maximale zu treibende Silbermenge in PoolRegion zur�ck
 */
public int getRegionMaxTreiben(){
	return regionMaxTreiben;
}

/**
 * 
 * Gibt null wenn nichts gefunden wird.
 * 
 */
public TreiberPoolRelation getTreiberPoolRelation(ScriptUnit _su){
	boolean go_on= true;
	TreiberPoolRelation cpr = null;
	for (Iterator<TreiberPoolRelation> iter2 = this.listOfRelations.iterator();(iter2.hasNext()&&(go_on));){
	    // casten, vergesse das zu gern!
		cpr = (TreiberPoolRelation) iter2.next();
	    if (cpr.getSkriptUnit() == _su){
	    	go_on=false;
	    	return cpr;
	    }
	}
	return cpr;  
}


public int getFaktor_silberbestand_region() {
	return faktor_silberbestand_region;
}


public void setFaktor_silberbestand_region(int faktor_silberbestand_region) {
	this.faktor_silberbestand_region = faktor_silberbestand_region;
}


public boolean isSilberknapp() {
	return silberknapp;
}

/**
 * Berechnet das maximal zu treibende Silber nach Kepler
 * @return
 */
public void keplerRegionMaxTreiben(){
	
	if (keplerCalcRun){
		return;
	}
	keplerCalcRun = true;
	/*
	In einer Region:
	Es wird festgestellt, wieviel die Bauern f�r den Ankauf von Luxusg�tern ausgeben (LUX).
	Es wird festgestellt, wieviel die Bauern f�r Unterhaltung ausgeben (UNT).
	Dann wird gerechnet: Silberbestand + Bauernverdienst - Bauernunterhalt - Bauernzuwachsunterhalt - LUX - UNT. Das Ergebnis ist die Obergrenze f�r Silber eintreiben (OGT).
	Jetzt muss ein Rucksackproblem gel�st werden: Optimiere Anzahl aktiver Treiber, so dass ein Rucksack mit Kapzit�t OGT maximal gef�llt wird.
	Alle nicht aktiven Treiber machen irgendwas anderes. Ggfs. spaltet sich ein grosse Treibereinheit auf, um noch n�her ans Optimum zu kommen.
	Bauern brauchen keine Silberreserven.
	*/
	// Ankauf von Luxusg�tern...und Unterhaltung
	this.LUX = this.getLUX();
	this.UNT = this.getUNT();
	// Bauernverdienst
	int bauernverdienst = (region.getPeasantWage()-10)*region.getModifiedPeasants();
	if (region.getTrees()>0){
		bauernverdienst-=region.getTrees() * 8 * 10;
	}
	if (region.getSprouts()>0){
		bauernverdienst-=region.getSprouts() * 4 * 10;
	}
	this.keplerInfo.add("Bauernverdienst: " + bauernverdienst + " Silber");
	
	// Zuwachsbere�cksichtigung, nehmen wir mal 1% als maximum an
	double popGrowth = 0.001;
	int popPlus = (int)Math.round((double)region.getModifiedPeasants() * popGrowth);
	if (popPlus<0){popPlus=1;}
	int unterhaltPopPlus = popPlus * 10;
	this.keplerInfo.add("M�glicher Bauernzuwachs: " + popPlus + " Bauern. Reserviere daf�r " + unterhaltPopPlus + " Silber.");
	
	int erg = this.region.getSilver() + bauernverdienst - unterhaltPopPlus - LUX - UNT;
	this.keplerInfo.add("OberGrenzeTreiben damit: " + erg + " Silber");
	this.regionMaxTreiben=erg;
	this.limit = regionMaxTreiben;
}

/**
 * Berechnet das maximal zu treibende Silber nach Kepler, Version 2
 */
public void maxTreibsilberFreigabe(){
	/**
	 * // script setScripterOption Treibsilberfreigabe=max
	 *		mit folgender Semantik:
	 * Einkommen = Bauern * Lohn
	 * Bev�lkerung = Bauern * 1,001 // abgerundet
	 * Versorgung = Bev�lkerung * 10
	 * Treibsilber = Regionssilber + Einkommen - Versorgung
	 * Alle sonstigen Aktivit�ten wie Unterhaltung, Handel, Rekrutierungen, Ver�nderungen durch GIB 0, Krieg usw. sollen nicht ber�cksichtigt werden. Diese Option ist f�r Spieler gedacht, die in stabilen Regionen nur treiben und optimal absch�pfen wollen, ohne dass jemand hungert.
	 */
	
	
	this.keplerInfo.add("maxSilberfreigabe nach Kepler (v2)");
	// Einkommen
	long einkommen = (region.getPeasantWage()-1) * region.getModifiedPeasants();
	this.keplerInfo.add("Einkommen: " + einkommen + " (Bauern(" + region.getModifiedPeasants() + ") * Lohn(" + (region.getPeasantWage()-1) + "))");
	long bev = (long)Math.floor(region.getModifiedPeasants() * 1.001);
	this.keplerInfo.add("Bev�lkerung: " + bev + " (Bauern * 1.001 abgerundet)");
	long vers=bev * 10;
	this.keplerInfo.add("Versorgung: " + vers + " (Bev�lkerung * 10)");
	int erg = (int)(this.region.getSilver() + einkommen - vers);
	this.keplerInfo.add("MaxTreiben damit: " + erg + " (Regionssilber + Einkommen - Versorgung)");
	this.regionMaxTreiben=erg;
	this.limit = regionMaxTreiben;
}


/**
 * Ermittelt die Azusgaben der Bauern in der Region f�r Luxusg�ter
 * @return
 */
private int getLUX(){
	int erg=0;
	// h�ndler finden, der verkauft
	TradeArea TA = treiberPoolManager.scriptMain.getOverlord().getTradeAreaHandler().getTAinRange(this.region);
	if (TA==null){
		this.keplerInfo.add("kein TradeArea gefunden, keine LUX angabe");
		return 0;
	}
	Trader t = TA.getVerkaufsTrader(this.region);
	if (t==null){
		this.keplerInfo.add("kein Trader in Region gefunden, keine LUX angabe");
		return 0;
	}
	ScriptUnit verkaeufer = t.getScriptUnit();
	// checken, was das depot an verkaufsf�higen sachen so bekommt
	for(Iterator<LuxuryPrice> iter = this.region.getPrices().values().iterator(); iter.hasNext();) {
		LuxuryPrice p = (LuxuryPrice) iter.next();
		if(p.getPrice() > 0) {
			// Verkaufsgut
			// wieviel gibts davon?
			Item modItem = verkaeufer.getModfiedItemMatPool2(p.getItemType());
			if (modItem!=null && modItem.getAmount()>0){
				int modAmount = modItem.getAmount();
				modAmount = Math.min(modAmount, this.region.maxLuxuries());
			    if (modAmount>0) {
			    	int modPrice = modAmount * p.getPrice();
			    	erg += modPrice;
			    	this.keplerInfo.add("Bauern k�nnen " + modAmount + " " + p.getItemType().getName() + " kaufen, f�r " + modPrice + " Silber.");
			    }	
			}
		}
	}	
	
	this.keplerInfo.add("Gesamtausgaben der Bauern hier f�r LUX: " + erg);
	return erg;
}

/**
 * Ermittelt die Azusgaben der Bauern in der Region f�r Unterhaltung
 * @return
 */
private int getUNT(){
	
	CircusPool CP = treiberPoolManager.scriptMain.getOverlord().getCircusPoolManager().getCircusPool(this.region);
	if (CP==null){
		this.keplerInfo.add("kein CircusPool gefunden, keine UNT angabe");
		return 0;
	}
	int erg = CP.getRegionsVerdienst();
	this.keplerInfo.add("Gesamtausgaben der Bauern hier f�r UNT: " + erg);
	return erg;
}


/**
 * @return the keplerMode
 */
public boolean isKeplerMode() {
	return keplerMode;
}


/**
 * @param keplerMode the keplerMode to set
 */
public void setKeplerMode(boolean keplerMode) {
	this.keplerMode = keplerMode;
}


/**
 * @return the keplerInfo
 */
public void addKeplerInfo(ScriptUnit sc) {
	for (String s:this.keplerInfo){
		sc.addComment(s);
	}
}

}

package com.fftools.pools.treiber;

import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;

/**
 * 
 * Fasst alle Treiberpools zusammen und verbindet sie mit Scriptmain
 * Sp�ter kann hier eine Wanderung der Treiber zu freien Regionen oder anderen Berufen 
 * koordiniert werden.
 * 
 * Ein TreiberPoolArea existiert (noch) nicht.
 *  
 * @author Fiete
 *
 */

public class TreiberPoolManager implements OverlordRun,OverlordInfo {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf = 30;
	
	private int[] runners = {Durchlauf};
	//  Verbindung zu FFtools halten �ber Scriptmain
	public ScriptMain scriptMain;
	
	// Schwupps da stecken die Pools!
	private Hashtable<Region,TreiberPool> treiberPoolMap = null;
		
	
	
	
	/**
	 * Konstruktor noch gaanz einfach.
	 */
	public TreiberPoolManager(ScriptMain _scriptmain){
	     scriptMain = _scriptmain;	
	}
	
/**
 * 
 * Gibt den TreiberPool zur�ck UND meldet ScriptUnit �ber TreiberPoolRelation dort an!
 * MatPool abgekupfert...
 * 
 */
	public TreiberPool getTreiberPool(ScriptUnit _u){
		// gibt es schon eine Poolmap, falls nicht wird angelegt!
				if (treiberPoolMap == null){treiberPoolMap = new Hashtable <Region,TreiberPool>();}
		Region region = _u.getUnit().getRegion();
		TreiberPool cp = treiberPoolMap.get(region);
		// falls noch kein MatPool fuer region da ist...anlegen
		if (cp==null){
			cp = new TreiberPool(this,region);

            // und nu natuerlich den TreiberPool der treiberPoolMap hinzufuegen
			// hat ich natuerlich vergessen beim ersten versuch
			treiberPoolMap.put(region,cp);
		} 
		// Sucht ein Script �ber diese Methode nach dem TreiberPool wird automatisch 
		// eine Relation im Pool angemeldet unter verwendund der ScriptUnit.
		// Skript mu� daher nicht addTreiberPoolRelation aufrufen (darf auch nicht)!
		cp.addTreiberPoolRelation(new TreiberPoolRelation(_u, cp));	
		return cp;
	
	}	


/**
 * 
 * Hier stoesst man die TreiberPools an.
 *
 */	
	
    public void run(int Durchlauf){
    	if (treiberPoolMap != null){
	        for (Iterator<TreiberPool> iter = treiberPoolMap.values().iterator();iter.hasNext();){
		     TreiberPool cp = (TreiberPool)iter.next();
		     cp.runPool();}
    	 }    
    }
    /**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
}
	
	
	
	
	
	
	
	
	

	
	
	
	
	


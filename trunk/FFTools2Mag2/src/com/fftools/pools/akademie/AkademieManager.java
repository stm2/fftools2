package com.fftools.pools.akademie;

	import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;

	public class AkademieManager implements OverlordRun,OverlordInfo {
		
		private static final OutTextClass outText = OutTextClass.getInstance();
		
		private static final int Durchlauf1 = 580;
		private static final int Durchlauf2 = 710;
		
		private int[] runners = {Durchlauf1, Durchlauf2};
		
		//  Verbindung zu FFtools halten über Scriptmain
		public ScriptMain scriptMain;
		
		// Da stecken die Pools!
		private Hashtable<Region,AkademiePool> AkademiePoolMap = null;
			
		
		
		
		/**
		 * Konstruktor noch gaanz einfach.
		 */
		public AkademieManager(ScriptMain _scriptmain){
		     scriptMain = _scriptmain;	
		}
		
	/**
	 * 
	 * Gibt den AkademiePool zurück UND meldet ScriptUnit über CircusPoolRelation dort an!
	 * MatPool abgekupfert...
	 * 
	 */
		public AkademiePool getAkademiePool(ScriptUnit _u){
			// gibt es schon eine Poolmap, falls nicht wird angelegt!
			if (AkademiePoolMap == null){AkademiePoolMap = new Hashtable <Region,AkademiePool>();}
			Region region = _u.getUnit().getRegion();
			AkademiePool ap = AkademiePoolMap.get(region);
			// falls noch kein AkademiePool fuer region da ist...anlegen
			if (ap==null){
				ap = new AkademiePool(this,region);

	            // und nun natuerlich den AkademiePool der AkademiePoolMap hinzufuegen
				AkademiePoolMap.put(region,ap);
			} 
			
			return ap;
		
		}	


	/**
	 * 
	 * Hier stoesst man die AkademiePools an.
	 *
	 */	
		
	    public void run(int Durchlauf){
	    	outText.addOutLine("AkademieManager",true);
	    	long start = System.currentTimeMillis();
	    	if (AkademiePoolMap != null){
		        for (Iterator<AkademiePool> iter = AkademiePoolMap.values().iterator();iter.hasNext();){
		        	AkademiePool ap = (AkademiePool)iter.next();
			        ap.runPool();
			        outText.addPoint();
		        }
	    	 }
	    	long ende = System.currentTimeMillis();
	    	outText.addOutLine("AkademieManager benötigte:" + (ende-start) + "ms",true);
	    }
	    /**
	     * Meldung an den Overlord
	     * @return
	     */
	    public int[] runAt(){
	    	return runners;
	    }
	  	
	}


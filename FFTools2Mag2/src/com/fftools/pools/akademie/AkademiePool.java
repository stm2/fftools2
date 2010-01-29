package com.fftools.pools.akademie;

import java.util.ArrayList;

import magellan.library.Region;

import com.fftools.ReportSettings;
import com.fftools.pools.akademie.relations.AkademieSchueler;
import com.fftools.pools.akademie.relations.ScriptAkademie;


public class AkademiePool {

	private boolean vorlauf= true;  
	private boolean mittellauf= true;  
	// private MatPoolRequest vorlaufRequest = null;
	// private static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	// private AkademieManager akademieManager = null;
	// private Region region=null;
	private ArrayList<AkademieSchueler> schuelerList=null;
    private ArrayList<ScriptAkademie> akademieList = null;
	// MatPool holen...
	// private MatPool matPool = null;

	
	
	/**
		 * Konstruktor 
		 *
		 */
		
		public AkademiePool(AkademieManager _am, Region _region){
			// akademieManager=_am;
			// region =_region;	   
	        //  MatPool holen...
	  		// this.matPool = this.akademieManager.scriptMain.getOverlord().getMatPoolManager().getRegionsMatPool(this.region);
	   	    
	    }
	
	
	/**
     * Hier rennt der Pool. 
     * und zwar zunächst der anforderungslauf, dann der nachlauf
     */
	
	public void runPool(){
		
		if (!vorlauf&&!mittellauf){
			this.runPoolNachlauf();
			vorlauf=false;
		}
				
		if (!vorlauf&&mittellauf){
			this.runPoolMittellauf();
			mittellauf = false;
		}
	 
		if (vorlauf&&mittellauf){
			this.runPoolVorlauf();
			vorlauf=false;
		}
		
	}
	
	/*
	 * Fügt möglichen schüler in den Pool ein
	 */
	public void addAkademieRelation(AkademieSchueler _ar){
     //	falls keine Liste da, zaubere eine Neue...
		if (this.schuelerList == null){
			this.schuelerList = new ArrayList<AkademieSchueler>();
		}
		
		this.schuelerList.add(_ar);
	}
	
	
	/*
	 * Bietet akademie im pool an
	 */
	public void addScriptAkademie(ScriptAkademie _akademie){
		 //	falls keine Liste da, zaubere eine neue...
		if (this.akademieList == null){
			this.akademieList = new ArrayList<ScriptAkademie>();
		}
		
		this.akademieList.add(_akademie);
	}
	
	
	
	
	
	private void runPoolVorlauf(){
		
	}
		
    
	private void runPoolMittellauf(){
		
	}
	
	
	private void runPoolNachlauf(){
		
	}
	
	
	
}

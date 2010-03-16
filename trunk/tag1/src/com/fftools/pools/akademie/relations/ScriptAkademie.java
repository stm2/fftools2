package com.fftools.pools.akademie.relations;

import java.util.ArrayList;

import magellan.library.Building;
import magellan.library.Region;

import com.fftools.ScriptUnit;

/**
 * Repräsentiert eine Akademie in FFTools, beinhaltet ihre Lernplätze und den Verwalter
 * sowie alle anderen Akademiebezogenen Werte.
 * 
 *  Der Verwalter einer Akademie meldet diese Klasse für den Akademiepool an.
 *  
 *  @author Marc
 *
 */
public class ScriptAkademie {

	// private int gesamtSchuelerplatz = 25;
	private ArrayList<AkademieSchueler> schuelerListe = null;
	// private AkademiePool akademiePool=null;
	private Building akademie= null;
	private ScriptUnit verwalter = null;
	
	public ScriptAkademie(Building _akademie, ScriptUnit _verwalter, int _plaetze ){
		
		this.akademie = _akademie;
		
		this.verwalter = _verwalter;
		
		// mehr als 25 passen nicht rein.. default 25
		if (_plaetze<=25){
		    // this.gesamtSchuelerplatz = _plaetze;
		}
		
				
	}
	
	
	public ScriptUnit getVerwalter(){
		return this.verwalter;
	}
	
	public Region getRegion(){
		return this.akademie.getRegion();
	}
	
    public Building getAkademie(){
    	return akademie;
    }
	
    public ArrayList<AkademieSchueler> getSchuelerList(){
    	return this.schuelerListe;
    }
	
    public void setSchuelerList(ArrayList<AkademieSchueler> _liste){
    	if (_liste!=null){
    		this.schuelerListe = _liste;
    	}
    	
    }
    
    public void addSchueler(AkademieSchueler _schueler){
       
    	if (this.schuelerListe==null){
    		this.schuelerListe = new ArrayList<AkademieSchueler>();
    	}
    	
    	if (_schueler != null){
    	   this.schuelerListe.add(_schueler);       }
        }

}

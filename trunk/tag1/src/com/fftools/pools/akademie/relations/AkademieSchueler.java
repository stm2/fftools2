package com.fftools.pools.akademie.relations;

import com.fftools.*;

/**
 * Schüler nutzen diese Relation um im AkademiePool einen Lernplatz zu beantragen.
 * 
 *  * @author Marc
 *
 */
public class AkademieSchueler {
    
	private ScriptUnit scriptunit=null;
	private ScriptAkademie scriptAkademie = null;
	
	public AkademieSchueler(ScriptUnit _u){
       this.scriptunit =_u;  
       	
    }
    	
	public void setScriptAkademie(ScriptAkademie _akademie){
    	this.scriptAkademie = _akademie;
    }
	
	
	public ScriptAkademie getScriptAkademie(){
		return this.scriptAkademie;
	}
	
	
	public ScriptUnit getScriptUnit(){
        return this.scriptunit;		
	}
	

	
}

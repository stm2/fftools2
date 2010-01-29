package com.fftools.pools.ausbildung.relations;



/*
 * Wenn eine Lehrergruppe die Schranken für die Auslastung erfüllt aber noch Lehrplätze ungenutzt sind, 
 * dann ist muß es einen Weg geben die freien Plätze nochmal im Pool anzubieten ohne die alte Lernkette aufzulösen, 
 * denn diese war ja gültig, nur eben nicht 100% ausgelastet.
 * 
 *   Diese Class wird dem Pool untergeschoben als "AusbildungsRelation" aber
 *   fast in Wahrheit eine ganze Lehrergruppe zusammen.
 *   Die freien Lernplätze werden nochmal verpoolt ohne sich auf vbestimmte Einheiten festzulegen und falls Schüler hinzukommen 
 *   werden diese an jede in der Class angemeldeten Lehrer weitergegeben.
 *   Bei Pool.performPooling und bei Pool.Relation2Orders sind die angemeldeten Relationsa ja uptodate... 
 *   es muß lediglich verhindert werden, dass sie Class selbst in diesen Methoden angesprochen wird.
 *   
 *   => Lehrerketten werden mit der Class gebaut ABER die Wirkung wird in die angemeldeten AusbildungsRelations 
 *   weitergeschoben und über diese dann umgesetzt.
 *   
 *   Damit sollte es möglich sein auch Magie sinnvoll zu poolen auch wenn die Schranken sehr locker sind für die Auslastung! 
 *   
 *   Nochmal: Diese Classe nicht poolen und 2orders!!!!!! Es ist nur eine sammelklasse zum kettenbau!!!
 *    
 */


public class LehrerAusbildungsRelation extends AusbildungsRelation{

 public LehrerAusbildungsRelation(){
	 // Gefährlicher Konstruktor!... nur damit keiner motzt
	 super(null,null,null);
 }

 
 
 
 
 
 
 
 
	
}

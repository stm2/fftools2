package com.fftools.pools.ausbildung.relations;



/*
 * Wenn eine Lehrergruppe die Schranken f�r die Auslastung erf�llt aber noch Lehrpl�tze ungenutzt sind, 
 * dann ist mu� es einen Weg geben die freien Pl�tze nochmal im Pool anzubieten ohne die alte Lernkette aufzul�sen, 
 * denn diese war ja g�ltig, nur eben nicht 100% ausgelastet.
 * 
 *   Diese Class wird dem Pool untergeschoben als "AusbildungsRelation" aber
 *   fast in Wahrheit eine ganze Lehrergruppe zusammen.
 *   Die freien Lernpl�tze werden nochmal verpoolt ohne sich auf vbestimmte Einheiten festzulegen und falls Sch�ler hinzukommen 
 *   werden diese an jede in der Class angemeldeten Lehrer weitergegeben.
 *   Bei Pool.performPooling und bei Pool.Relation2Orders sind die angemeldeten Relationsa ja uptodate... 
 *   es mu� lediglich verhindert werden, dass sie Class selbst in diesen Methoden angesprochen wird.
 *   
 *   => Lehrerketten werden mit der Class gebaut ABER die Wirkung wird in die angemeldeten AusbildungsRelations 
 *   weitergeschoben und �ber diese dann umgesetzt.
 *   
 *   Damit sollte es m�glich sein auch Magie sinnvoll zu poolen auch wenn die Schranken sehr locker sind f�r die Auslastung! 
 *   
 *   Nochmal: Diese Classe nicht poolen und 2orders!!!!!! Es ist nur eine sammelklasse zum kettenbau!!!
 *    
 */


public class LehrerAusbildungsRelation extends AusbildungsRelation{

 public LehrerAusbildungsRelation(){
	 // Gef�hrlicher Konstruktor!... nur damit keiner motzt
	 super(null,null,null);
 }

 
 
 
 
 
 
 
 
	
}

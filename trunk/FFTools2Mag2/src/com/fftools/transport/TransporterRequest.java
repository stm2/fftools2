package com.fftools.transport;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.TransportScript;
import com.fftools.utils.FFToolsRegions;

/**
 * dient als Datenstruktur zur Kommunikation der Requests, die der 
 * TM dem Transporter �bergibt und durch das script Transport dann
 * tats�chlich dem MatPoolMg �bergeben werden
 * 20080311: erg�nzt um die passende request um beide units in einem rutsch zu
 * informieren
 * @author Fiete
 *
 */
public class TransporterRequest {
	private int Anzahl = 0;
	private TransportOffer transportOffer = null;
	private TransportRequest transportRequest = null;
	private Transporter transporter = null;
	private String intro = "";
	
	private int ETA_cache=-1;
	
	private static int defaultTransporterPrio = 2;
	
	/**
	 * Konstruktor
	 * @param anzahl
	 * @param transportRequest
	 * @param transportOffer
	 */
	public TransporterRequest(int anzahl, TransportOffer transportOffer, TransportRequest transportRequest, Transporter transporter, String intro) {
		this.Anzahl = anzahl;
		this.transportOffer = transportOffer;
		this.transportRequest = transportRequest;
		this.transporter = transporter;
		this.intro = intro;
	}
	
	/**
	 * liefert den passenden request an den MatPool zu diesem Request
	 * ben�tigt ein Transportscript zum einfachen Erstellen des MatPoolRequests
	 * @param s
	 * @return
	 */
	public MatPoolRequest getMatPoolRequest(TransportScript s){	
		MatPoolRequest mpr = new MatPoolRequest(s,this.Anzahl,this.transportOffer.getItemName(),defaultTransporterPrio,"TM");
		// grunds�tzlich nur in der Region
		mpr.setOnlyRegion(true);
		return mpr;
	}
	
	/**
	 * informiert die betroffenen requester und offerer �ber die Zuteilung des transportes
	 * und die vorraussichtliche Ankunftszeit
	 *
	 */
    public void informUnits(){
		// ETA berechnung
		int ETAAnzahlRunden = this.getETA();
		transportOffer.getScriptUnit().addComment("TM(-):" + Anzahl + " " + transportOffer.getItemName() + " (P:" + transportRequest.getPrio()  + ",ETA:" + ETAAnzahlRunden + ") nach " + transportRequest.getRegion().toString() + " mit " + this.transporter.getScriptUnit().getUnit().toString(true) + " in " + this.transporter.getScriptUnit().getUnit().getRegion().toString());
		transportRequest.getScriptUnit().addComment("TM(+):" + Anzahl + " " + transportOffer.getItemName() + " (P:" + transportRequest.getPrio() + ",ETA:" + ETAAnzahlRunden +  ") von " + transportOffer.getRegion().toString() + ") mit " + this.transporter.getScriptUnit().getUnit().toString(true) + " in " + this.transporter.getScriptUnit().getUnit().getRegion().toString());
		this.transporter.getScriptUnit().addComment(intro + ":" + Anzahl + " " + transportOffer.getItemName() + "(P:" + transportRequest.getPrio() + ",ETA:" + ETAAnzahlRunden + ") von " + transportOffer.getRegion().toString() + " nach " + transportRequest.getRegion().toString());
	}
	
    private int getETA(){
		// ETA berechnung
		// immer: Entfernung Offer->request
    	if (this.ETA_cache<0){
			int ETAAnzahlRunden = FFToolsRegions.getPathDistLandGotoInfo(this.transporter.getScriptUnit().getScriptMain().gd_ScriptMain, transportOffer.getRegion().getCoordinate(),transportRequest.getRegion().getCoordinate(), true).getAnzRunden();
			// wenn Transporter nicht in OfferRegion kommt enfernung dorthin noch dazu
			if (!this.transporter.getScriptUnit().getUnit().getRegion().equals(transportOffer.getRegion())){
				ETAAnzahlRunden+=FFToolsRegions.getPathDistLandGotoInfo(this.transporter.getScriptUnit().getScriptMain().gd_ScriptMain, this.transporter.getScriptUnit().getUnit().getRegion().getCoordinate(), transportOffer.getRegion().getCoordinate(), true).getAnzRunden();
			}
			this.ETA_cache = ETAAnzahlRunden;
    	} 
		return this.ETA_cache;
	}
    
    /**
     * das Gewicht dieser Transaktion
     * @return
     */
    public int getWeight(){
    	int verbrauch = (int)Math.ceil((transportOffer.getItem().getItemType().getWeight()*Anzahl));
    	return verbrauch;
    }
    
}

package com.fftools.pools.matpool.relations;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.MatPool;
import com.fftools.scripts.Script;
import com.fftools.transport.TransportRequest;

/**
 * Anfrage an den Matpool
 * 
 * @author Fiete
 *
 */
public class MatPoolRequest extends MatPoolRelation  implements Comparable<MatPoolRequest>{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	/**
	 * Konstanten zur Ber�cksichtigung der Kapazit�t der Unit
	 */
	public static final int KAPA_unbenutzt = 0;
	public static final int KAPA_max_zuFuss = 1;
	public static final int KAPA_max_zuPferd = 2;
	public static final int KAPA_benutzer = 3;
	public static final int KAPA_weight = 4;
	// zu erweitern: KAPA_Schiff = 5;
	
	
	
	public static final int TM_sortMode_dist = 0;
	public static final int TM_sortMode_amount = 1;
	
	/**
	 * die angeforderten (substitutionsf�higen) Items
	 */
	private ArrayList<ItemType> itemTypes = null;
	
	/**
	 * die urspr�nglich geforderte Menge
	 */
	private int gefordert = 0;
	
	/**
	 * die Priorit�t der Anforderung
	 */
	private int prio = 0;
	
	/**
	 * die Priorit�t der Anforderung gg TM, falls abweichend
	 */
	private int prioTM = 0;
	
	/**
	 * Rereferenz auf generierendes script
	 * dar�ber kann scriptunit erkannt werden
	 */
	private Script script = null;
	
	/**
	 * Ber�cksichtigung der Kapazit�t der Unit
	 */
	private int kapaPolicy = KAPA_unbenutzt;
	
	/**
	 * SortierModus des TransportManagers
	 */
	private int TMsortMode = TM_sortMode_dist;
	
	
	/**
	 * eventuell vom benutzer �bergebene maximale Kapazit�tsnutzung
	 * oder gewicht (FF 20080306)
	 */
	private int userKapa = 0;
	
	/**
	 * Kommentar zum Request, kann beim GIB ausgegeben werden
	 */
	private String kommentar = null;
	
	/**
	 * zur Fehlerverfolgung 
	 */
	private String originalGegenstand = "";
	
	/**
	 * ID zur Identifizierung eines bestimmten Requests
	 */
	private int ID=-1;
	
	/**
	 * sichert die Reihenfolge der Bearbeitung der Requests
	 * stellt sicher, dass bei mehreren L�ufen die gleiche 
	 * Reihenfolge beachtet wird und so bei gleicher Prio
	 * trotzdem gleiche Ergebnisse m�glich sind.  
	 */
	private int ReihenfolgeNummer = Integer.MAX_VALUE;
	
	/**
	 * legt fest, ob dieser MPR lediglich auf die Region einwirken soll, nicht aufs Area
	 *  
	 */
	private boolean onlyRegion = false;
	
	/**
	 * schr�nkt die zu nutzenden Transporter ein
	 * Strings k�nnen beliebige Strinhs, ItemTypes oder ItemGroups sein.
	 */
	private ArrayList<String> transporterSpecs = null;
	
	/**
	 * falls dieser MatPool einen TM-Request ausl�st...hier ist er.
	 */
	private TransportRequest transportRequest = null;
	
	/**
	 * Matpool, der die scriptunit angeh�rt
	 * wird lediglich f�r kapa-benutzung gebraucht (Feststellung depot...)
	 */
	private MatPool myMP = null;
	
	/**
	 * ist es erlaubt, f�r spezielle Reihenfolgen die angegeben Prios
	 * zu ver�ndern?
	 * (besp: pferdePrio immer erh�hen?)
	 * sollen Pferde mit genau prio X angefordert werden, muss prioChange=false!
	 */
	private boolean prioChange = true;
	
	/**
	 * Legt neuen Request an
	 * @param _script
	 * @param anzahl
	 * @param gegenstand
	 * @param _prio
	 * @param _kapaPolicy
	 */
	public MatPoolRequest(int _ID, Script _script,int anzahl,String gegenstand,int _prio,String _kommentar,int _kapaPolicy,int _userKapa){
		this.ID = _ID;
		this.script = _script;
		this.setScriptUnit(this.script.scriptUnit);
		
		this.prio = _prio;
		this.prioTM =_prio;
		this.kapaPolicy = _kapaPolicy;
		this.gefordert = anzahl;
		this.userKapa = _userKapa;
		this.kommentar = _kommentar;
		this.originalGegenstand = gegenstand;
		
		// gegenstand ist entweder der Name eines ItemTypes oder der einer (script)Kategorie
		// ist es ein ItemType
		ItemType itemType = reportSettings.getRules().getItemType(gegenstand); 
		if (itemType!=null){
			// es ist ein ItemType
			this.itemTypes = new ArrayList<ItemType>();
			this.itemTypes.add(itemType);
		} else {
			// ist es eine Kategorie?
			ArrayList<ItemType> _itemTypes = reportSettings.getItemTypes(gegenstand);
			if (_itemTypes!=null){
				// reportSettings liefert bereits sortierte Liste
				this.itemTypes = new ArrayList<ItemType>();
				this.itemTypes.addAll(_itemTypes);
			}
		}
		// wenn itemTypes immer noch null ist - war es ein Tippfehler o.�.
		// das angelegte ibject kann nicht derefrenziert werden von hier aus
		// MatPool darf einen Request nur annehmen, wenn itemTypes != null
		
		// Regionsetting
		// Festlegung: "Request Alles" wird grunds�tzlich nicht auuserhalb der Region wirksam
		if (this.getOriginalGefordert()==Integer.MAX_VALUE){
			this.setOnlyRegion(true);
		}
		
	}
	
	/**
	 * erzeugt einen neuen MPR analog zu dem �bergebenen
	 * @param MPR
	 */
	public MatPoolRequest(MatPoolRequest MPR){
		this(MPR.getId(),MPR.getScript(),MPR.getOriginalGefordert(),MPR.getOriginalGegenstand(),MPR.getPrio(),MPR.getKommentar(),MPR.kapaPolicy,MPR.userKapa);
		this.setPrioTM(MPR.getPrioTM());
	}
	
	
	/**
	 * Konstruktor des Requests ohne ID mit Kapa
	 * @param _script
	 * @param anzahl
	 * @param gegenstand
	 * @param _prio
	 */
	public MatPoolRequest(Script _script,int anzahl,String gegenstand,int _prio,String _kommentar,int _kapaPolicy,int _userKapa){
		this(-1, _script,anzahl,gegenstand,_prio,_kommentar,_kapaPolicy,_userKapa);
	}
	
	/**
	 * Konstruktor des Requests ohne Kapa-Ber�cksichtigung, mit ID
	 * @param _script
	 * @param anzahl
	 * @param gegenstand
	 * @param _prio
	 */
	public MatPoolRequest(int _ID, Script _script,int anzahl,String gegenstand,int _prio,String _kommentar){
		this(_ID, _script,anzahl,gegenstand,_prio,_kommentar,KAPA_unbenutzt,0);
	}
	
	public MatPoolRequest(int _ID, Script _script,int anzahl,String gegenstand,int _prio,String _kommentar,int _kapaPolicy){
		this(_ID, _script,anzahl,gegenstand,_prio,_kommentar,_kapaPolicy,0);
	}
	
	
	/**
	 * Konstruktor des Requests ohne Kapa-Ber�cksichtigung, ohne ID
	 * @param _script
	 * @param anzahl
	 * @param gegenstand
	 * @param _prio
	 */
	public MatPoolRequest(Script _script,int anzahl,String gegenstand,int _prio,String _kommentar){
		this(-1, _script,anzahl,gegenstand,_prio,_kommentar,KAPA_unbenutzt,0);
	}
	
	public MatPoolRequest(Script _script,int anzahl,String gegenstand,int _prio,String _kommentar,int _kapaPolicy){
		this(-1, _script,anzahl,gegenstand,_prio,_kommentar,_kapaPolicy,0);
	}
	
	/**
	 * liefert f�r ein itemType die durch dieses script noch anzufordernde menge
	 * unter ber�cksichtigung eventuelle kapa-beschr�nkungen
	 * @param itemType
	 * @return auf jeden Fall int, niemals null
	 */
	public int getForderung(ItemType itemType){
		// debug
		if (this.getPrio()==21){
			int iiii=1;
			iiii++;
		}
		if (this.getScriptUnit().getUnitNumber().equalsIgnoreCase("qv44")){
			int iiii=1;
			iiii++;
		}
		
		// debug
		// this.getScriptUnit().addComment("Debug: noch gefordert f�r: " + itemType.getName()+ ", bearbeitet aktuell: " + this.getBearbeitet());
		
		
		// die einfachen sachen vorneweg
		if (this.itemTypes==null){
			// darf nicht sein
			// irgendeine Ausgabe - Fehlerlog ?!
			return 0;
		}
		if (!this.itemTypes.contains(itemType)){
			// falsches itemType
			return 0;
		}
		if (this.getBearbeitet()>=this.gefordert){
			// schon vollst�ndig bedient
			return 0;
		}
		if (this.getScriptUnit().getSetKapaPolicy()!=KAPA_unbenutzt){
			this.kapaPolicy = this.getScriptUnit().getSetKapaPolicy();
		}
		if (this.kapaPolicy==KAPA_unbenutzt){
			// ohne Ber�cksichtigung der Kapazit�t
			return (this.gefordert - this.getBearbeitet());
		}
		
		
		// int freeKapa = this.getScriptUnit().getFreeKapa(kapaPolicy, userKapa);
		
		// ab hier soll also die Kapazit�t eine rolle spielen
		
		// Auftrennen und Sonderbehandlung von Pferden und Wagen!
		if (itemType.equals(reportSettings.getRules().getItemType("Pferd")) && kapaPolicy!=KAPA_weight){
			return this.getForderungPferd();
		}
		if (itemType.equals(reportSettings.getRules().getItemType("Wagen")) && kapaPolicy!=KAPA_weight ){
			return this.getForderungWagen();
		}
		
		// welche ist denn noch frei
		// kapa in GE!
		int freeKapa = this.getScriptUnit().getFreeKapaMatPool2(kapaPolicy, userKapa);
		
		
		// debug
		// this.getScriptUnit().addComment("Debug: freeKapa=" + freeKapa + " (userKapa:" + userKapa +")");
		
		
		// Wenn Depot in Region, dann wird alles noch offerierte
		// auch falls es nicht angefragt wird ja ans Depot gehen
		// daher kann das Gewicht dieser Offers zu FreeKapa hinzu
		// Frage: haben wir ein Depot in der Region
		if (this.myMP==null){
			// erstmal MP holen
			this.myMP = this.getScript().getOverlord().getMatPoolManager().getRegionsMatPool(this.getScriptUnit());
		}
		int offeredWeight = 0;
		if (this.myMP!=null && this.myMP.getDepotUnit()!=null){
			// MP vorhanden und Depotunit drinne
			ArrayList<MatPoolOffer> offers = this.myMP.getOffers(this.getScriptUnit());
			if (offers!=null && offers.size()>0){
				for (MatPoolOffer actOffer : offers){
					if (actOffer.getAngebot()>0){
						if ((!actOffer.getItemType().equals(reportSettings.getRules().getItemType("Pferd"))) && (!actOffer.getItemType().equals(reportSettings.getRules().getItemType("Wagen"))) || this.kapaPolicy==KAPA_weight){
							offeredWeight+=actOffer.getAngebotsGewicht();
						}
					}
				}
			}
		}
		freeKapa+=offeredWeight;
		
		// debug
		// this.getScriptUnit().addComment("Debug: nach offered weight: freeKapa=" + freeKapa + " (userKapa:" + userKapa +")");
		
		if (freeKapa<=0){
			//nichts mehr frei
			return 0;
		}
		
		// das Gewicht des ItemTypes...
		// in GE
		float gewicht = (itemType.getWeight());
		if (gewicht==0){
			// das Teil hat gar kein Gewicht! -> keine Einschr�nkungen
			// durch freeKapa
			return (this.gefordert - this.getBearbeitet());
		}
		
		// nach kapa-beschr�nkung maximal zur Verf�gung stehende Menge
		int maxNachGewicht = (int)Math.floor((float)freeKapa/gewicht);

		// nach Forderung und bereits bearbeitet noch zu erhaltene Menge
		int maxNachRequest = (this.gefordert - this.getBearbeitet());
		
		// debug
		// this.getScriptUnit().addComment("Debug: maxNachGewicht=" + maxNachGewicht + ", maxNachRequest=" + maxNachRequest+ " (minimum returned)");
		
		// endg�ltiges Ergebnis als Minumum aus den beiden oberen
		return Math.min(maxNachGewicht, maxNachRequest);
	}

	
	public void incBearbeitet(int amount,ItemType itemType){
		// super.incBearbeitet(amount);
		super.setBearbeitet(super.getBearbeitet()+ amount);

		this.getScriptUnit().changeModifiedItemsMatPools(itemType,amount);
		
		// Spezialbehandlung Pferden Info �ber Ver�nderung der PferdeKapa
		if (itemType.getID().equals(EresseaConstants.I_HORSE)){
			if (this.getScriptUnit().getSetKapaPolicy()==MatPoolRequest.KAPA_max_zuPferd){
				int free = this.getScriptUnit().getFreeKapaMatPool2(this.getScriptUnit().getSetKapaPolicy());
				this.getScriptUnit().addComment("Ver�nderte Reitkapa: " + free + " GE",true);
			}
			if (this.getScriptUnit().getSetKapaPolicy()==MatPoolRequest.KAPA_max_zuFuss){
				int free = this.getScriptUnit().getFreeKapaMatPool2(this.getScriptUnit().getSetKapaPolicy());
				this.getScriptUnit().addComment("Ver�nderte GehenKapa: " + free + " GE",true);
			}
		}

	}
	
	
	/**
	 * liefert die geforderte PferdeMenge
	 * @return
	 */
	private int getForderungPferd(){
		int skillLevel = 0;
		Unit u = this.script.getUnit();
		Skill s = u.getModifiedSkill(new SkillType(StringID.create("Reiten")));

		if(s != null) {
			skillLevel = s.getLevel();
		}
		int maxHorsesWalking = ((skillLevel * u.getModifiedPersons() * 4) + u.getModifiedPersons());
		int maxHorsesRiding = (skillLevel * u.getModifiedPersons() * 2);
		
		int mengeNachKapa = 0;
		
		switch (this.kapaPolicy){
		case KAPA_unbenutzt:
			// sollte hier nicht mehr auftauchen, oben angefangen
			mengeNachKapa = (this.gefordert);
			break;
		case KAPA_benutzer:
			// dat macht ja nun gar keinen Sinn
			// oder sp�ter implementuieren: gew�nschte Kapa der Unit?
			mengeNachKapa = 0;
			break;
		case KAPA_max_zuPferd:
			mengeNachKapa = maxHorsesRiding;
			break;
		case KAPA_max_zuFuss:
			mengeNachKapa = maxHorsesWalking;
			break;
		case KAPA_weight:
			// neu: NUR gewicht der Pferde entscheidend
			// daher keine Spezialanforderung
			// sollte also hier niemals auftauchen!
			mengeNachKapa = 0;
			this.getScriptUnit().addComment("!!scriptFehler in Berechnung der ben�tigten Pferde!!", true);
			this.getScriptUnit().doNotConfirmOrders();
			break;
		}
		
		// Ber�cksichtigung einer eventuell durch den User festegesetzten Menge:
		mengeNachKapa = Math.min(this.gefordert, mengeNachKapa);
		
		// noch zu fordernde menge
		return (Math.max(0,(mengeNachKapa - this.getBearbeitet())));
	}
	
	/**
	 * liefert die aktuel zu fordernde Menge an Wagen
	 * Anzahl der vorhandenen Pferde bereits in "Modified"
	 * @return
	 */
	private int getForderungWagen(){
		// wieviel Pferde habe ich eigentlich?
		int anzPferd = 0;
		Item pferd = this.script.scriptUnit.getModifiedItem(reportSettings.getRules().getItemType("Pferd"));
		
		if (pferd!=null){
			anzPferd = pferd.getAmount();
		}
		if (anzPferd<=0){
			// keine Pferde da...hm.
			return 0;
		}
		
		int anzMaxWagen = (int) anzPferd / 2;
		
		// falls weniger gefordert, soll das gelten
		anzMaxWagen = Math.min(anzMaxWagen, this.gefordert);
		
		// prinzipiel derzeit keine Unterscheidung der kapaPolicy notwendig
		
		// R�ckgabe
		return Math.max(0, (anzMaxWagen - this.getBearbeitet()));
	}
	
	/**
	 * @return the prio
	 */
	public int getPrio() {
		return prio;
	}	
	
	/**
	 * sortieren anhand der Prio
	 * danach nach Amount
	 */
	public int compareTo(MatPoolRequest o) {
		int Diff = (o.getPrio() - this.getPrio());
		// bei gleicher Prio Anzahl gefordert (grosse requests zuerst?!)
		// kleine Requests zuerst macht auch Sinn -> sind die eher raus aus dem Pool
		
		// FF neu 20070421: 
		// um viele Requests abzuschliessen: kleine zuerst
		// aber auf jeden Fall gleiche Reihenfolge
		
		// hier Reihenfolge -> aufsteigend
		if (Diff==0) {
			Diff = this.getReihenfolgeNummer() - o.getReihenfolgeNummer();
		}
		
		// wenn immer noch gleich, jetzt die kleinen zuerst
		if (Diff==0){
			// wie kann der jetzt auf privates feld gefordert zugreifen bei mpr2 ?
			Diff = this.gefordert - o.gefordert;
		}
		
		return Diff;
	}

	/**
	 * liefert niemals null, entweder den kommentar oder ""
	 * @return the kommentar
	 */
	public String getKommentar() {
		if (this.kommentar==null){
			return "";
		}
		return kommentar;
	}

	/**
	 * @return the itemTypes
	 */
	public ArrayList<ItemType> getItemTypes() {
		return itemTypes;
	}
	
	/**
	 * @return the script
	 */
	public Script getScript() {
		return script;
	}

	/**
	 * @return the gefordert
	 */
	public int getOriginalGefordert() {
		return gefordert;
	}
	
	/**
	 * setzt gefordert - nur VOR dem MP zu setzen
	 * @param _gefordert
	 */
	public void setOriginalGefordert(int _gefordert){
		this.gefordert = _gefordert;
	}
	

	/**
	 * @return the originalGegenstand
	 */
	public String getOriginalGegenstand() {
		return originalGegenstand;
	}
    
	/**
	 * Erm�gliht es einem Script eine request f�r sp�tere Pr�fungen zu markieren
	 * @param ID
	 */
    public void setId(int _ID){
    this.ID = _ID;	
    	
    }
    
    /**
     * Gibt die ID des Request zur�ck, -1 ist default
     * @return ID
     */
    
    public int getId(){
      return this.ID;
    }

    public String toString(){
    	return this.gefordert + " " + this.originalGegenstand + " (Prio " + this.prio + "), bearb.:" + this.getBearbeitet();
    }
    
    
    /**
     * liefert war, wenn itemType teil dieses requests ist
     * @param itemType
     * @return
     */
    public boolean containsItemtype(ItemType itemType){
    	if (this.itemTypes==null || this.itemTypes.size()==0){
    		return false;
    	}
    	return this.itemTypes.contains(itemType);
    }


	/**
	 * @return the reihenfolgeNummer
	 */
	public int getReihenfolgeNummer() {
		return ReihenfolgeNummer;
	}


	/**
	 * @param reihenfolgeNummer the reihenfolgeNummer to set
	 */
	public void setReihenfolgeNummer(int reihenfolgeNummer) {
		ReihenfolgeNummer = reihenfolgeNummer;
	}


	/**
	 * @return the onlyRegion
	 */
	public boolean isOnlyRegion() {
		return onlyRegion;
	}


	/**
	 * @param onlyRegion the onlyRegion to set
	 */
	public void setOnlyRegion(boolean onlyRegion) {
		this.onlyRegion = onlyRegion;
	}


	/**
	 * @return the transporterSpecs
	 */
	public ArrayList<String> getTransporterSpecs() {
		return transporterSpecs;
	}
    
	/**
	 * f�gt eine Spezialisierungsrestriktion zu dem MatPoolRequest hinzu.
	 * @param spec
	 */
	public void addSpec(String spec){
		if (spec==null || spec.length()==0){
			return;
		}
		if (this.transporterSpecs==null){
			this.transporterSpecs = new ArrayList<String>();
		}
		this.transporterSpecs.add(spec);
	}
    
	/**
	 * f�gt eine Liste von Specs den Specs des MPR hinzu
	 * @param specs
	 */
	public void addSpecs(ArrayList<String> specs){
		if (specs!=null){
			for (Iterator<String> iter = specs.iterator();iter.hasNext();){
				String s = (String)iter.next();
				this.addSpec(s);
			}
		}
	}


	/**
	 * @return the transportRequest
	 */
	public TransportRequest getTransportRequest() {
		return transportRequest;
	}


	/**
	 * @param transportRequest the transportRequest to set
	 */
	public void setTransportRequest(TransportRequest transportRequest) {
		this.transportRequest = transportRequest;
	}


	/**
	 * @return the prioTM
	 */
	public int getPrioTM() {
		return prioTM;
	}


	/**
	 * @param prioTM the prioTM to set
	 */
	public void setPrioTM(int prioTM) {
		this.prioTM = prioTM;
	}
	
	/**
	 * liefert eine Aussage �ber Anzahl der ItemTypes...
	 * als erkennung einer ItemGroup
	 * genutzt im MP
	 * @return
	 */
	public boolean hasMultipleItemTypes(){
		boolean erg = false;
		if (this.itemTypes!=null && this.itemTypes.size()>1){
			erg = true;
		}
		return erg;
	}
	
	/**
	 * liefert wahr, wenn dieser Request die kapa der unit ber�cksichtigt
	 * @return
	 */
	public boolean usesKapa(){
		if (this.kapaPolicy==KAPA_unbenutzt){
			return false;
		} else {
			return true;
		}
	}


	/**
	 * @return the tMsortMode
	 */
	public int getTMsortMode() {
		return TMsortMode;
	}


	/**
	 * @param msortMode the tMsortMode to set
	 */
	public void setTMsortMode(int msortMode) {
		TMsortMode = msortMode;
	}

	/**
	 * @param kommentar the kommentar to set
	 */
	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	/**
	 * @param prio the prio to set
	 */
	public void setPrio(int prio) {
		this.prio = prio;
	}

	/**
	 * @return the prioChange
	 */
	public boolean isPrioChange() {
		return prioChange;
	}

	/**
	 * @param prioChange the prioChange to set
	 */
	public void setPrioChange(boolean prioChange) {
		this.prioChange = prioChange;
	}
	
}

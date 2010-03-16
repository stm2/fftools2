package com.fftools.pools.alchemist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import magellan.library.ID;
import magellan.library.Item;
import magellan.library.Potion;
import magellan.library.Region;
import magellan.library.rules.ItemType;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Alchemist;

/**
 * 
 * steuert die Aufgaben der Alchimisten einer Region
 * @author Fiete
 *
 */
public class AlchemistPool {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	/**
	 * eine Einheit ist Ausgangspunkt aller Kr�uter
	 */
	public Alchemist krautDepot = null;
	
	/**
	 * alle in dieser Region verscripteten Alchemisten
	 */
	private ArrayList<Alchemist> alchemisten = null;
	
	/**
	 * Referenz auf die Region zu diesem Pool
	 */
	private Region region = null;
	
	/**
	 * Referenz auf den Manager, um eventuell dort
	 * gelagerte Trank-Liste zu nutzen
	 * 
	 */
	private AlchemistManager alchemistManager = null;
	
	/**
	 * HashMap der Tr�nke dieser Region
	 */
	private HashMap<ID, AlchemistTrank> potions = null;
	
	/**
	 * Gegenst�nde, die durch vorbehalte restriktiv gehandhabt werden
	 * (nur wenn mehr als vorbehaltMax vorhanden, dann diesen surplus 
	 * verwenden)
	 * Amount der Items: vorbehaltMax
	 */
	private HashMap<ItemType, Item> vorbehalte = null;
	
	/**
	 * ein kleiner Helfer, unsch�n, aber ging nicht anders
	 */
	private int trankMenge = 0;
	
	/**
	 * verf�gbare Trankzutaten
	 */
	private HashMap<ItemType, Integer>availableIngredients = null;
	
	/**
	 * maximal machbare Tr�nke nach Kr�utersituation in Region
	 */
	private HashMap<AlchemistTrank, Integer> maxPossible = new HashMap<AlchemistTrank, Integer>();
	
	/**
	 * // Ergebnisliste anlegen
	 * 
	 */
	HashMap<AlchemistTrank,Integer> prodTable = new HashMap<AlchemistTrank, Integer>();
	
	
	ArrayList<AlchemistTrank> trankListeAusreichendDepot = new ArrayList<AlchemistTrank>();
	
	/**
	 * Konstruktor
	 *
	 */
	public AlchemistPool(AlchemistManager manager,Region r){
		this.region = r;
		this.alchemistManager = manager;
	}
	
	
	/**
	 * f�gt einen definierten Trank den reportweiten tr�nken hinzu
	 * @param trank
	 */
	public void addAlchemistTrank(AlchemistTrank trank){
		if (trank.getPotion()==null){
			return;
		}
		if (this.potions==null){
			this.potions= new HashMap<ID, AlchemistTrank>();
		}
		this.potions.put(trank.getPotion().getID(),trank);
	}
	
	/**
	 * f�gt den Alchi zur Liste der zu bearbeitenden Alchis hinzu
	 * @param alchi
	 */
	public void addAlchemist(Alchemist alchi){
		if (this.alchemisten==null){
			this.alchemisten=new ArrayList<Alchemist>();
		}
		if (!this.alchemisten.contains(alchi)){
			this.alchemisten.add(alchi);
		}
	}
	
	/**
	 * nu endlich..Pool soll laufen
	 *
	 */
	public void run(){
		//  CHECKS
		if (this.region==null){
			outText.addOutLine("!!!Unerwarteter AlchemistenPool ohne Region!!!", true);
			return;
		}		
		if (this.alchemisten==null || this.alchemisten.size()==0){
			// nix zu tun
			outText.addOutLine("Unerwartet keine Alchemisten in " + this.region.toString(), true);
			return;
		}
		if (this.krautDepot==null){
			//	nix zu tun
			outText.addOutLine("Unerwartet kein KrautDepot in " + this.region.toString(), true);
			return;
		}
		
		// haben wir TrankOrder?
		if (this.potions==null || this.potions.size()==0){
			// keine potions f�r diese Region definiert
			if (this.alchemistManager.getPotions()==null || this.alchemistManager.getPotions().size()==0){
				// keine Tr�nke
				outText.addOutLine("Keine Tr�nke definiert f�r Region " + this.region.toString(), true);
				return;
			} else {
				// f�r diese Region reportweite tr�nke �bernehmen
				this.potions = this.alchemistManager.getPotions();
			}
		}
		
		// Liste der zu brauenden Tr�nke
		ArrayList<AlchemistTrank> tranks = new ArrayList<AlchemistTrank>();
		tranks.addAll(this.potions.values());
		
		// Comparator erzeugen
		AlchemistTrankComparator ATC = new AlchemistTrankComparator();
		// Liste sortieren nach Rang
		Collections.sort(tranks, ATC);
				
		// Liste durchlaufen
		for (Iterator<AlchemistTrank> iter = tranks.iterator();iter.hasNext();){
			AlchemistTrank actTrank = (AlchemistTrank)iter.next();
			// diesen Trank bearbeiten
			int actProd = this.workOnTrank(actTrank);
			// Ergebnis ablegen
			this.prodTable.put(actTrank, new Integer(actProd));
		}
		
		// alle Alchis �ber RegionsProd informieren
		this.informAllAlchis(tranks);
		
		// nicht ben�tigte Alchemisten Lernen Lassen 
		// debug..einfach nur Kommentar ausgeben
		this.processedUnusedAlchis();

	}
	
	/**
	 * Informiert alle Alchies �ber die Produktion der Region
	 * @param tranks
	 */
	private void informAllAlchis(ArrayList<AlchemistTrank> tranks){
		for (Iterator<Alchemist> iter = this.alchemisten.iterator();iter.hasNext();){
			Alchemist alchi = (Alchemist)iter.next();
			alchi.addComment("Regionsproduktionsinfo folgt: (" + tranks.size() + " Tr�nke definiert, " + this.alchemisten.size()+ " Alchis automatisch am K�cheln)",true);
			alchi.addComment(this.countUsedAlchis() + " sind fleissig, " + this.countUnusedAlchis() + " lernen.",true);
			// Liste durchlaufen
			for (Iterator<AlchemistTrank> iter2 = tranks.iterator();iter2.hasNext();){
				AlchemistTrank actTrank = (AlchemistTrank)iter2.next();
				String s = "Rang " + actTrank.getRang() + " - " + actTrank.getPotion().getName() + ": ";
				Integer istProd = this.prodTable.get(actTrank);
				Integer maxProd = this.maxPossible.get(actTrank);
				s += istProd.toString() + "/" + maxProd.toString();
				if (this.trankListeAusreichendDepot.contains(actTrank)){
					s+=", Depotvorrat OK (max:" + actTrank.getVorratMax() + ")";
				}
				alchi.addComment(s, true);
			}
		}
	}
	
	
	/**
	 * bearbeitet die nicht ben�tigten Alchis
	 *
	 */
	private void processedUnusedAlchis(){
		for (Iterator<Alchemist> iter = this.alchemisten.iterator();iter.hasNext();){
			Alchemist alchi = (Alchemist)iter.next();
			if (!alchi.isBrauend()){
				alchi.addComment("unbesch�ftigter Alchi");
				// Lernen
				alchi.addOrder("LERNEN Alchemie", true);
				// Silber anfordern
				alchi.getZubehoerDetail("Silber", 210);
			}
		}
	}
	
	/**
	 * z�hlt die nicht ben�tigten Alchis
	 *
	 */
	private int countUnusedAlchis(){
		int erg = 0;
		for (Iterator<Alchemist> iter = this.alchemisten.iterator();iter.hasNext();){
			Alchemist alchi = (Alchemist)iter.next();
			if (!alchi.isBrauend()){
				erg++;
			}
		}
		return erg;
	}
	
	/**
	 * z�hlt die arbeitenden Alchis
	 *
	 */
	private int countUsedAlchis(){
		int erg = 0;
		for (Iterator<Alchemist> iter = this.alchemisten.iterator();iter.hasNext();){
			Alchemist alchi = (Alchemist)iter.next();
			if (alchi.isBrauend()){
				erg++;
			}
		}
		return erg;
	}
	
	
	/**
	 * versucht, den aktuellen Trank zu brauen...
	 * @param trank
	 */
	private int workOnTrank(AlchemistTrank trank){
		
		// wieviele k�nnen nach der Kr�uterlage hergestellt werden?
		int maxDepot = this.maxTrankNachKr�utern(trank.getPotion());
		
		// checked, ob eventuell genug vom Trank vorhanden
		if (!this.isUnderVorratMax(trank)){
			// Hinweis beim Krautdepot..;_))
			if (this.krautDepot!=null){
				this.krautDepot.addComment(trank.getPotion().getName() + " ausreichend beim Depot.");
			}
			this.maxPossible.put(trank, new Integer(maxDepot));
			this.trankListeAusreichendDepot.add(trank);
			return 0;
		}

		// in maxPossible ablegen
		this.maxPossible.put(trank, new Integer(maxDepot));
		this.trankMenge = maxDepot;
		boolean alchisAvailable = true;
		int trankProd=0;
		while (this.trankMenge>0 && alchisAvailable){
			Alchemist actAlchi = this.getNextBestAlchi(trank);
			if (actAlchi==null){
				alchisAvailable=false;
				if (trank.isVorbehalt()){
					this.addVorbehalt(trank);
				}
			} else {
				trankProd += this.processMachen(trank, actAlchi);
			}
		}
		return trankProd;
	}

	
	/**
	 * liefert den am n�chstbesten geeigneten Alchie
	 * wichtig: trankMenge ist PoolIntern
	 * @param trank
	 * @return
	 */
	private Alchemist getNextBestAlchi(AlchemistTrank trank){
		
		
        // wieviele Talentstufen braucht man daf�r?
		int neededTalentstufen = this.trankMenge * trank.getPotion().getLevel() * 2;
		
		/*
		 * Den Alchemisten finden, welcher
		 * 1. noch frei ist
		 * 2. von seiner Talentstufe her den Trank �berhaupt machen kann
		 * 3. wenn er ihn machen w�rde die Mindestauslastung erreicht
		 * 4. unter allen dadurch am besten ausgelastet sein w�rde.
		 */ 
		// ArrayList alle verf�gbaren Alchemisten
		ArrayList<Alchemist> goodAlchis = new ArrayList<Alchemist>();
		for (Iterator<Alchemist> iter=this.alchemisten.iterator();iter.hasNext();){
			Alchemist actAlchi = (Alchemist)iter.next();
			// 1. noch frei
			if (!actAlchi.isBrauend()){
				// 2. ausreichend gut?
				if (actAlchi.getTalentLevel()>=trank.getPotion().getLevel() * 2){
					// 3. Mindestauslastung erf�llt?
					// Prozentuale Auslastung berechnung
					double actAusl = ((double)neededTalentstufen / (double)actAlchi.getTalentPunkte())*100;
					actAlchi.helpAuslastung = actAusl;
					if (actAusl>actAlchi.getMinAuslastung()){
						// zur Liste hinzu
						goodAlchis.add(actAlchi);
					}
				}
			}
		}
		
		// es gint welche...jetzt den raussuchen, der am besten ausgelastet wird...
		double bestDiff = Double.MAX_VALUE;
		Alchemist bestAlchemist = null;
		for (Iterator<Alchemist> iter = goodAlchis.iterator();iter.hasNext();){
			Alchemist actAlchi = (Alchemist)iter.next();
			double diff = Math.abs(actAlchi.helpAuslastung - 100);
			if (diff<bestDiff){
				bestDiff = diff;
				bestAlchemist = actAlchi;
			}
		}
		
		
		return bestAlchemist;
	}
	
	/**
	 * setzt das Brauen um
	 * noch umzusetzende Menge nach Kr�utern in this.trankMenge
	 * @param trank
	 * @param alchi
	 */
	private int processMachen(AlchemistTrank trank, Alchemist alchi){
		// wieviel zu brauen? soll: this.trankMenge
		// wieviel kann er denn brauen?
		int alchiTalentPunkte = alchi.getTalentPunkte();
		// damit kann er wieviel machen?
		int alchiAnzahlTrank = (int)Math.floor((double)alchiTalentPunkte / (double)(trank.getPotion().getLevel()*2));
		// Minimum feststellen
		int production = Math.min(this.trankMenge,alchiAnzahlTrank);
		// umsetzen ... alle Zutaten anfordern
		for (Iterator<Item> iter = trank.getPotion().ingredients().iterator();iter.hasNext();){
			Item actPotionItem = (Item)iter.next();
			MatPoolRequest MPR = new MatPoolRequest(alchi,actPotionItem.getAmount() * production,actPotionItem.getName(),alchi.getKrautPrio() + 10,"Alchemist");
			alchi.addMatPoolRequest(MPR);
		}
		// mache befehl setzen
		String potionName = trank.getPotion().getName();
		if (potionName.indexOf(" ")>0) {
			potionName = "\"" + potionName + "\"";
		}
		alchi.addOrder("MACHEN " + production + " " + potionName, true);
		// alchi als brauend markieren
		alchi.setBrauend(true);
		// trankMenge reduzieren
		this.trankMenge -= production;
		
		// Auslastungshinweis an Alchemist
		int benutzteTalentPunkte = production * trank.getPotion().getLevel()*2;
		int Auslastung = (int)Math.floor(((double)benutzteTalentPunkte / (double)alchi.getTalentPunkte()) * 100);
		alchi.addComment("Auslastung (dieses Alchis): " + Auslastung + "% (" + benutzteTalentPunkte + "/" + alchi.getTalentPunkte() + ")");

		// verf�gbare Ingedients reduzieren
		this.decAvailableIngredientsOfPotion(trank.getPotion(), production);
		return production;
	}
	
	
	
	/**
	 * legt die Ingredients als unter Vorbehalt stehend fest
	 * @param trank
	 */
	private void addVorbehalt(AlchemistTrank trank){
		// checks
		if (trank.getVorbehaltMax()<1){
			return;
		}
		if (!trank.isVorbehalt()){
			return;
		}
		
		if (this.vorbehalte==null){
			this.vorbehalte = new HashMap<ItemType, Item>();
		}
		
		// durch die Zutaten laufen
		for (Iterator<Item> iter = trank.getPotion().ingredients().iterator();iter.hasNext();){
			Item actPotionItem = (Item)iter.next();
			// in den Vorbehalten enthalten?
			Item vorbehaltItem = this.vorbehalte.get(actPotionItem.getItemType());
			if (vorbehaltItem==null){
				// nein...Neuanlage
				vorbehaltItem = new Item(actPotionItem.getItemType(),trank.getVorbehaltMax());
				this.vorbehalte.put(actPotionItem.getItemType(), vorbehaltItem);
			} else {
				// schon dort...Latte h�her legen
				vorbehaltItem.setAmount(vorbehaltItem.getAmount() + trank.getVorbehaltMax());
			}
		}
	}
	
	
	/**
	 * berechnet, wieviele Tr�nke maximal mit den Items bei der 
	 * Krautdepot unit hergestellt werden k�nnen
	 * @param potion
	 * @return
	 */
	private int maxTrankNachKr�utern(Potion potion){
		int erg = 0;
		if (this.krautDepot==null){return 0;}
		if (potion==null){return 0;}

		
		
		erg = Integer.MAX_VALUE;
		// Zutaten durchlaufen
		for (Iterator<Item> iter = potion.ingredients().iterator();iter.hasNext();){
			Item actPotionItem = (Item)iter.next();
			int actAnzahlDepot=0;
			ItemType actItemType = actPotionItem.getItemType();
			
			actAnzahlDepot = this.getAvailableIngredients(actItemType);
			// check, ob bereits durch vorbehalt belastet	
			if (this.vorbehalte!=null && this.vorbehalte.size()>0){
				Item vorbehaltItem = this.vorbehalte.get(actItemType);
				if (vorbehaltItem!=null){
					actAnzahlDepot -= vorbehaltItem.getAmount();
					actAnzahlDepot = Math.max(0,actAnzahlDepot);
				}
			}

			if (actPotionItem.getAmount()>0){
				int anzahlNachActItem = (int)Math.floor((double)actAnzahlDepot/(double)actPotionItem.getAmount());
				erg = Math.min(erg,anzahlNachActItem);
			}
		}
		return erg;
	}
	
	/**
	 * liefert noch verf�gbare Trankzutaten
	 * @param itemType
	 * @return
	 */
	private int getAvailableIngredients(ItemType itemType){
		int erg = 0;
		
		// existiert bereits HashMap
		if (this.availableIngredients==null){
			this.availableIngredients = new HashMap<ItemType, Integer>();
		}
		
		Integer availInt = this.availableIngredients.get(itemType);
		if (availInt==null){
			// noch nicht vorhanden
			Item actDepotItem = this.krautDepot.scriptUnit.getModifiedItem(itemType);
			int actAnzahlDepot = 0;
			if (actDepotItem!=null){
				actAnzahlDepot = actDepotItem.getAmount();
			}
			erg = actAnzahlDepot;
			
			// Einschub Bauer
			if (itemType.getName().equalsIgnoreCase("Bauer")){
				erg = this.region.getModifiedPeasants();
			}
			
			
			// hinzuf�gen
			availInt = new Integer(erg);
			this.availableIngredients.put(itemType,availInt);
		} else {
			erg = availInt.intValue();
		}
		return erg;
	}
	
	/**
	 * reduziert verf�gbare menge an Zutaten
	 * @param itemType
	 * @param Anzahl
	 */
	private void decAvailableIngredients(ItemType itemType, int Anzahl){
		Integer availInt = this.availableIngredients.get(itemType);
		if (availInt==null){
			// eigentlich m�chtig was falsch gelaufen
			return;
		}
		int actValue = availInt.intValue();
		actValue -= Anzahl;
		availInt = new Integer(actValue);
		this.availableIngredients.put(itemType, availInt);
	}
	
	/**
	 * reduziert verf�gbare menge an Zutaten eines Trankes
	 * @param potion
	 * @param Anzahl
	 */
	private void decAvailableIngredientsOfPotion(Potion potion, int Anzahl){
		for (Iterator<Item> iter = potion.ingredients().iterator();iter.hasNext();){
			Item actPotionItem = (Item)iter.next();
			this.decAvailableIngredients(actPotionItem.getItemType(), Anzahl);
		}
	}
	
	/**
	 * liefert nur dann false, wenn wegen vorratMax nicht
	 * gebraut werden soll
	 * @param trank
	 * @return
	 */
	private boolean isUnderVorratMax(AlchemistTrank trank){
		boolean erg = true;
		// Depotunit feststellen
		ScriptUnit depotUnit = this.alchemistManager.getOverlord().getMatPoolManager().getRegionsMatPool(this.region).getDepotUnit();
		if (depotUnit!=null){
			Item actTrankItem = depotUnit.getModifiedItem(trank.getTrankItemType());
			if (actTrankItem!=null){
				int actAnzahl = actTrankItem.getAmount();
				if (actAnzahl>trank.getVorratMax()){
					erg=false;
				}
			}
		}
		return erg;
	}
	
	
}

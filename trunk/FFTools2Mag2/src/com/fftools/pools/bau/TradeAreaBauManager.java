package com.fftools.pools.bau;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;
import magellan.library.Unit;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Bauauftrag;
import com.fftools.scripts.Bauen;
import com.fftools.scripts.Burgenbau;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;


/**
 *Verwaltet Bauen-scripte eines TradeAreas
 *
 */
public class TradeAreaBauManager {
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	
	private TradeArea tradeArea = null;
	
	// merken wir uns zu jeder ScriptUnit doch einfach die bauScripte
	private Hashtable<ScriptUnit, ArrayList<Bauen>> bauScripte = null;
	
	// merken wir uns zu jeder ScriptUnit doch einfach die bauAufträge
	private Hashtable<ScriptUnit, ArrayList<Bauauftrag>> bauAufträge = null;
	

	// Die Infozeilen
	private ArrayList<String> infoLines = new ArrayList<String>();
	
	// Die scriptUnit, die den automatischen Burgenbau regestriert hat
	private Burgenbau registerBurgenbau = null;
	
	
	public TradeAreaBauManager (TradeArea _tradeArea){
		this.tradeArea=_tradeArea;
	}
	
	

	
	/**
	 * Zentrale Zuordnung der Bauaufträge zu automatischen Bauarbeitern
	 */
	public void run0(){
		
		this.processBurgenbau();
		
		if (this.bauAufträge==null || this.bauAufträge.size()==0){
			this.infoLines.add("keine Bauaufträge im TA bekannt");
			return;
		}
		
		if (this.bauScripte==null || this.bauScripte.size()==0){
			this.infoLines.add("keine Bauarbeiter im TA bekannt");
			return;
		}
		// Liste der automatischen Bauarbeiter bauen
		ArrayList<Bauen> autoBauer = new ArrayList<Bauen>();
		for (ScriptUnit su:this.bauScripte.keySet()){
			ArrayList<Bauen> actList = this.bauScripte.get(su);
			if (actList!=null && actList.size()>0){
				for (Bauen b:actList){
					if (b.isAutomode()){
						autoBauer.add(b);
					}
				}
			}
		}
		
		if (autoBauer.size()==0){
			this.infoLines.add("keine automatischen Bauarbeiter im TA bekannt");
			return;
		}
		
		

		
		// erste Infozeilen
		this.infoLines.add("TA-Bau: " + this.bauAufträge.size() + " Bauauftragshalter und " + autoBauer.size()+ " autom. Bauarbeiter");
		// Liste der Aufträge
		this.infoLines.add("bekannte Aufträge:");
		ArrayList<Bauen> auftragsBauscripte = new ArrayList<Bauen>();
		for (ScriptUnit su:this.bauAufträge.keySet()){
			ArrayList<Bauauftrag> actList = this.bauAufträge.get(su);
			if (actList!=null && actList.size()>0){
				for (Bauauftrag b:actList){
					if (b.getBauScript()!=null){
						// this.infoLines.add(b.getBauScript().toString());
						auftragsBauscripte.add(b.getBauScript());
					} else {
						this.infoLines.add("Auftrag ohne Bauscript bei " + su.unitDesc());
					}
				}
			}
		}
		// Sortieren
		Collections.sort(auftragsBauscripte, new BauScriptComparator());
		for (Bauen b:auftragsBauscripte){
			this.infoLines.add(b.toString() + " bei " + b.unitDesc());
		}
		
		
		
		
		// Liste der auto-Bauarbeiter
		this.infoLines.add("bekannte automatische Bauarbeiter:");
		for (Bauen b:autoBauer){
			this.infoLines.add(b.getUnitBauInfo());
		}

		// Abarbeiten
		ArrayList<Bauen> availableBauarbeiter = new ArrayList<Bauen>();
		for (Bauen b:auftragsBauscripte){
			if (!b.isFertig()){
				// welches Talent brauchen wir
				String actTalentName = "Burgenbau";
				if (b.getActTyp()==Bauen.STRASSE){
					actTalentName="Strassenbau";
				}
				// debug
				if (b.scriptUnit.getUnitNumber().equalsIgnoreCase("bnfw")){
					int i=0;
					i++;
				}
				
				
				// mit welchem level
				int level_needed = 1;
				if (b.getActTyp()==Bauen.BURG){
					level_needed = FFToolsGameData.getCastleSizeBuildSkillLevel(b.getActSize());
				}
				if (b.getActTyp()==Bauen.BUILDING && b.getBuildingType()!=null){
					level_needed = b.getBuildingType().getBuildSkillLevel();
				}
				
				
				// noch verfügbare Bauarbeiter zusammensuchen
				availableBauarbeiter.clear();
				for (Bauen arbeiter:autoBauer){
					if (!arbeiter.hasPlan() && arbeiter.scriptUnit.getSkillLevel(actTalentName)>=level_needed){
						availableBauarbeiter.add(arbeiter);
					}
				}
				if (availableBauarbeiter.size()>0) {
					// Sortieren mit Relevanz zu:
					// ZielRegion und benötigtem TP und Level und Skill
					BauauftragScriptComparator bc = new BauauftragScriptComparator(b.region(),level_needed,actTalentName,(b.getTargetSize()-b.getActSize()));
					Collections.sort(availableBauarbeiter, bc);
					// Zuordnen an den ersten besten
					for (Bauen arbeiter:availableBauarbeiter){
						boolean ok =BauZuordnung(b, arbeiter, actTalentName);
						if (ok){
							break;
						}
					}
					
				}
			}
		}
		
		
		// was ist mit nicht versorgten Bauarbeitern?
		
		for (Bauen arbeiter:autoBauer){
			if (!arbeiter.hasPlan() ){
				processWaitingArbeiter(arbeiter);
			}
		}
		
		
		
		Collections.reverse(this.infoLines);
		
	}
	
	/**
	 * Baumanager hat diesem Arbeiter keinen Auftrag erteilt
	 * @param arbeiter
	 */
	private void processWaitingArbeiter(Bauen arbeiter){
		arbeiter.autoLearn();
	}
	
	/**
	 * vollzieht die Zuordnung
	 * Ergebniss:
	 * 	- entweder Bauauftrag
	 *  - oder GoTo bzw Reiten lernen
	 * @param Auftrag
	 * @param Arbeiter
	 */
	private boolean BauZuordnung(Bauen Auftrag,Bauen Arbeiter, String actTalentName){
		
		// sind wir in der gleichen Region?
		if (Auftrag.region().equals(Arbeiter.region())){
			// wir sind in der gleichen Region!
			// Dem Arbeiter das Bauscript überhelfen
			// ohne mode=auto...
			// mit enstprechendem Lerntalent
			Bauen newBauscript = Auftrag.clone();
			newBauscript.setScriptUnit(Arbeiter.scriptUnit);
			newBauscript.setPlaningMode(false);
			
			Arbeiter.addComment("übernommener Auftrag: " + newBauscript.toString());
			Arbeiter.addComment("übernommener Auftrag von: " + Auftrag.unitDesc());
			this.addBauScript(newBauscript);
			Arbeiter.scriptUnit.addAScriptNow(newBauscript);
			Arbeiter.setAutomode_hasPlan(true);
			Auftrag.addComment("Bauen: " + Arbeiter.unitDesc() + " übernimmt: " + Auftrag.toString());
			return true;
			
		} else {
			// wir müssen erst hin.
			// können wir reiten?
			// debug
			int minReitLevel=Bauen.minReitLevel;
			if (Arbeiter.scriptUnit.getSkillLevel("Reiten")>minReitLevel){
				// ja, hinreiten und pferde requesten
				GotoInfo gotoInfo = FFToolsRegions.makeOrderNACH(Arbeiter.scriptUnit, Arbeiter.region().getCoordinate(), Auftrag.region().getCoordinate(), true);
				Arbeiter.addComment("dieser Region NEU als Bauarbeiter zugeordnet: " +  Auftrag.region().toString());
				Arbeiter.addComment("Auftrag: " + Auftrag.toString());
				Arbeiter.addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
				Arbeiter.setAutomode_hasPlan(true);
				Arbeiter.setHasGotoOrder(true);
				Auftrag.addComment("Bauen: " + Arbeiter.unitDesc() + " übernimmt: " + Auftrag.toString());
				// Pferde requesten...
				MatPoolRequest MPR = new MatPoolRequest(Arbeiter,Arbeiter.scriptUnit.getUnit().getModifiedPersons(), "Pferd", 21, "Bauarbeiter unterwegs" );
				Arbeiter.addMatPoolRequest(MPR);
				
				return true;
				
			} else {
				// nein, auf T1 Reiten lernen
				// mit Ausreichend info versehen
				Arbeiter.addComment("Als Bauarbeiter fast zugeordnet: " + Auftrag.toString() + " bei " + Auftrag.unitDesc());
				Arbeiter.addComment("aber da noch nicht reiten könnend, erstmal lernen");
				Arbeiter.scriptUnit.addOrder("Lernen Reiten", true, true);
				
				Arbeiter.setAutomode_hasPlan(true);
				return false;
			}
		}
		
	}
	
	
	
	/**
	 * Checkt den Status der bauscripte
	 *
	 */
	
	public void run1(){
		
		this.informUnits();
		
		if (this.bauScripte==null){return;}
		
		BauScriptComparator bauC = new BauScriptComparator();
		
		for (Iterator<ScriptUnit> iter = this.bauScripte.keySet().iterator();iter.hasNext();){
			ScriptUnit actUnit = (ScriptUnit)iter.next();
			
			// debug
			if (actUnit.getUnitNumber().equalsIgnoreCase("12o0")){
				int i=0;
				i++;
			}
			
			boolean allFertig = true;
			String lernTalent = "";
			boolean hasCommand = false;
			boolean isOnAutomode = false;
			Bauen actBauen = null;
			ArrayList<Bauen> actList = this.bauScripte.get(actUnit);
			if (actList!=null && actList.size()>0){
				// sortieren
				Collections.sort(actList,bauC);
				for (Iterator<Bauen> iter2 = actList.iterator();iter2.hasNext();){
					actBauen = (Bauen)iter2.next();
					if (actBauen.isAutomode()){isOnAutomode=true;}
					if (!actBauen.isFertig()){
						allFertig = false;
						if (actBauen.getLernTalent().length()>0){
							lernTalent=actBauen.getLernTalent();
						}
						if (actBauen.getBauBefehl().length()>0){
							// Baubefehl
							hasCommand = true;
							actBauen.addOrder(actBauen.getBauBefehl(), true);
							break;
						}
						if (actBauen.isHasGotoOrder()){
							hasCommand = true;
							break;
						}
						if (actBauen.isAutomode() && !actBauen.hasPlan()){
							// automode einheiten erhalten vom bei der Zuordnung den Lernbefehl
							hasCommand = true;
							break;
						}
					}
				}
			}
			if (allFertig && !isOnAutomode){
				// alle bauaufträge fertig
				actUnit.addComment("Alle Bauaufträge erledigt");
				actUnit.doNotConfirmOrders();
			}
			
			if (!hasCommand){
				// soll Lernen
				if (lernTalent.length()>0 && actBauen!=null){
					// alles fein
					actBauen.addComment("Keine Bautätigkeit. Einheit soll Lernen.");
					actBauen.lerneTalent(lernTalent,true);
				} else {
					// kann nicht lernen
					actUnit.addComment("!!!Bauen: Unit soll Lernen, kann aber nicht!");
					actUnit.doNotConfirmOrders();
				}
			}
		}
	}
	
	
	public void addBauScript(Bauen bauen){
		if (this.bauScripte==null){
			this.bauScripte = new Hashtable<ScriptUnit, ArrayList<Bauen>>();
		}
		ArrayList<Bauen> actList = this.bauScripte.get(bauen.scriptUnit);
		if (actList==null){
			actList = new ArrayList<Bauen>();
		} 
		if (!actList.contains(bauen)){
			actList.add(bauen);
			this.bauScripte.put(bauen.scriptUnit,actList);
		}
	}
	
	
	public void addBauAuftrag(Bauauftrag bauAuftrag){
		if (this.bauAufträge==null){
			this.bauAufträge = new Hashtable<ScriptUnit, ArrayList<Bauauftrag>>();
		}
		ArrayList<Bauauftrag> actList = this.bauAufträge.get(bauAuftrag.scriptUnit);
		if (actList==null){
			actList = new ArrayList<Bauauftrag>();
		} 
		if (!actList.contains(bauAuftrag)){
			actList.add(bauAuftrag);
			this.bauAufträge.put(bauAuftrag.scriptUnit,actList);
		}
	}


	/**
	 * @return the tradeArea
	 */
	public TradeArea getTradeArea() {
		return tradeArea;
	}
	
	
	private void informUnits(){
		// alle Bauauftragshalter informieren
		if (this.bauAufträge!=null && this.bauAufträge.size()>0){
			for (ScriptUnit su:this.bauAufträge.keySet()){
				report2Unit(su);
			}
		}
		
		// Burgenbau
		if (this.registerBurgenbau!=null){
			report2Unit(this.registerBurgenbau.scriptUnit);
		}
		// ToDo: autobauer benachrichtigen
		
		
	}
	
	// infolines an Unit
	private void report2Unit(ScriptUnit su){
		if (this.infoLines.size()>0){
			
			for (String s:this.infoLines){
				su.addComment(s);
			}
		} else {
			su.addComment("TA-Baumanager hat nix zu berichten");
		}
	}
	
	/**
	 * registriert die scriptUnit als Initiator für den Burgenbau
	 * @param u
	 */
	public void addBurgenBau(Burgenbau u){
		if (this.registerBurgenbau==null){
			this.registerBurgenbau = u;
			u.addComment("Automatischer Burgenbau für dieses TA registriert: " + this.getTradeArea().getName());
		} else {
			u.addComment("!! Automatischer Burgenbau konnte nicht registriert werden. Ist bereits geschehen durch: " +this.registerBurgenbau.unitDesc());
		}
	}
	
	/**
	 * ergänzt Bauaufträge für den automatischen Burgenbau
	 */
	private void processBurgenbau(){
		// haben wir ne entsprechende registrierung?
		if (this.registerBurgenbau==null){
			return;
		}
		if (this.registerBurgenbau.getAnzahl()<=0){
			this.registerBurgenbau.addComment("Burgenbau: keine automatischen Bauaufträge erstellt (Anzahl = " + this.registerBurgenbau.getAnzahl() + ")");
			return;
		}
		
		ArrayList<Region> burgenRegionen = this.tradeArea.getBurgenbauRegionen();
		if (burgenRegionen.size()==0){
			this.registerBurgenbau.addComment("Burgenbau: keine automatischen Bauaufträge erstellt: keine Regionen bekannt");
			return;
		}
		int i=1;
		int actPrio=0;
		ArrayList<String> infos = new ArrayList<String>();
		for (Region r:burgenRegionen){
			actPrio	= this.registerBurgenbau.getPrio()-(i-1);
			TradeRegion TR = this.registerBurgenbau.getOverlord().getTradeAreaHandler().getTradeRegion(r);
			Unit u = TR.getDepot();
			if (u == null){
				// kein Depot
				infos.add("Burgenbau ("+actPrio+"): kein Depot in " + r.toString());
			} else {
				ScriptUnit su = this.registerBurgenbau.scriptUnit.getScriptMain().getScriptUnit(u);
				if (su==null){
					infos.add("Burgenbau ("+actPrio+"): kein Depot-Scriptunit in " + r.toString());
				} else {
					// ergänzen
					Bauauftrag bA = new Bauauftrag();
					ArrayList<String> newArgs = new ArrayList<String>();
					newArgs.add("typ=Burg");
					newArgs.add("Prio=" + actPrio);
					newArgs.add("Ziel=" + FFToolsRegions.getNextCastleSize(r));
					bA.setScriptUnit(su);
					bA.setArguments(newArgs);
					bA.setGameData(this.registerBurgenbau.gd_Script);
					su.addAScriptNow(bA);
					bA.run1();
					su.addComment("Burgenbau: Auftrag hier hinzugefügt");
					infos.add("Burgenbau ("+actPrio+"): auf " + FFToolsRegions.getNextCastleSize(r) + " in " + r.toString() + " bei " + su.unitDesc());
				}
			}
			
			i++;
			if (i>this.registerBurgenbau.getAnzahl()){
				break;
			}
		}
		if (infos.size()>0){
			Collections.reverse(infos);
			for (String s:infos){
				this.registerBurgenbau.addComment(s);
			}
		}
	}
	
}

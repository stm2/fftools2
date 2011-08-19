package com.fftools.scripts;


import java.util.Iterator;
import java.util.LinkedList;

import magellan.library.Item;
import magellan.library.LuxuryPrice;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeUtils;
import com.fftools.trade.Trader;



public class Handeln extends TradeAreaScript{
	
	private Region r = null;
	
	public static final int Durchlauf_VorMatpool = 3;
	public static final int Durchlauf_NachMatpool = 70;
	public static final int Durchlauf_NachMatpool2 = 152;
	
	
	private int[] runsAt = {Durchlauf_VorMatpool,Durchlauf_NachMatpool,Durchlauf_NachMatpool2};
	
	// private final int DEFAULT_REQUEST_PRIO = 10;  moved to trader
	private String kommentar = "Handel";
	private String kommentar_silberRequest = "Handelseinkauf";
	
	// ein ganz simpler merker
	private int requested_silver = -1;
	private LuxuryPrice buyingPrice = null;
	private int adjustedBuyAmount = -1;
	// private int prio = DEFAULT_REQUEST_PRIO;   moved to trader
	
	private int overallSellAmount = 0;
	

	private String default_silbervorrat_kommentar = "H�ndler - SilberDepot";

	private LinkedList<MatPoolRequest> silberDepotMPRs = null;
	
	
	
	private boolean doSomething = false; 
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Handeln() {
		super.setRunAt(this.runsAt);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void runScript(int scriptDurchlauf){
		
		
		
		switch (scriptDurchlauf){
		
		case Durchlauf_VorMatpool:this.vorMatpool();break;

		case Durchlauf_NachMatpool:this.nachMatPool();break;
		
		case Durchlauf_NachMatpool2:this.nachMatPool2();break;
		
		}
	}
	
	/**
	 * requested was man verkaufen kann
	 * (in den n�chsten X runden...)
	 * 
	 *
	 */
	private void vorMatpool(){
		
		super.addVersionInfo();
		
		// falls Trader noch unbekannt (sollte nicht sein)
		// Trader sollten vorher beim Durchlaufen der scriptunits
		// in scriptmain bereits gesetzt sein, inkl TradeAreas usw
		// jetzt einen setzen
		
		this.getTrader().setScript(this);
		this.getTrader().init();
		
		if (this.getTrader().isLernen()){
			return;
		}
		
		if (this.getTrader().getBuy()==null){
			// kann nix kaufen und verkaufen
			if (this.getTrader().isKaufen()||this.getTrader().isVerkaufen()){
				this.addComment("!!! kein Handel m�glich - kein Handelsgut.");
				outText.addOutLine("!!!H�ndler in Region ohne Handelsgut:" + this.unitDesc(), true);
			}
			return;
		}
		
		
		
		// Region...
		r = this.scriptUnit.getUnit().getRegion();
		if(r.getPrices() != null) {
			String infoString = "";
			for(Iterator<LuxuryPrice> iter = r.getPrices().values().iterator(); iter.hasNext();) {
				LuxuryPrice p = (LuxuryPrice) iter.next();

				if(p.getPrice() > 0) {
					// hier was zum verkaufen...
					// FF 20070109 eventuell soll er gar nicht verkaufen...
					if (this.getTrader().isVerkaufen()){
						ItemType lux = p.getItemType();
						int amount = r.maxLuxuries() * this.getTrader().getVerkaufsvorratsrunden();
						if (infoString.length()>1){
							infoString += ",";
						}
						infoString += amount + " " + lux.getName();
						MatPoolRequest HR = new MatPoolRequest(this,amount,lux.getName(),this.getTrader().getSellItemRequestPrio(),kommentar);
						HR.setOnlyRegion(true);
						this.addMatPoolRequest(HR);
					}
				} else {
					// neu: kein Silber anfordern
					this.buyingPrice = p;
				}
			}
	        // super.addComment("Zum Verkauf ben�tigt: " + infoString, true);
		} else {
			// keine Region prices
			// Meldung wenn er verkaufen oder kaufen sollte..sonst nicht
			if (this.getTrader().isKaufen() || this.getTrader().isVerkaufen()){
				super.addComment("keine Preisinfos verf�gbar", true);
				outText.addOutLine("!!!Handeln: keine Preisinfos verf�gbar: " + this.unitDesc());
				super.scriptUnit.doNotConfirmOrders();
			}
		}
	}
	
	private void nachMatPool(){
		// erstmal verkaufen, was wir so haben
		// mitz�hlen, ob Talente reichen....
		// feststellen, ob er �berhaupt etwas machen kann...
		
		// user info hier?
		this.showTradeAreaInfo();
		
		if (this.getTrader().isLernen()){
			return;
		}
		
		if (this.getTrader().getBuy()==null){
			return;
		}
		
		
		
		int neededTalent = 0;
		// Verkaufen..
		if(r.getPrices() != null && this.getTrader().isVerkaufen()) {
			for(Iterator<LuxuryPrice> iter = r.getPrices().values().iterator(); iter.hasNext();) {
				LuxuryPrice p = (LuxuryPrice) iter.next();
				if(p.getPrice() > 0) {
					Item item = this.scriptUnit.getModifiedItem(p.getItemType());
					int amount = 0;
					if (item!=null){
						amount = item.getAmount();
					}
					if (amount>0){
						// OK...hier k�nnen wir was verkaufen
						super.addOrder("VERKAUFEN ALLES " + p.getItemType().getName(),true);
						doSomething = true;
						// Talente mitz�hlen...
						if (amount>r.maxLuxuries()){amount=r.maxLuxuries();}
						overallSellAmount+=amount;
						// neededTalent += (int)Math.ceil((double)amount/10);
					}
				}
			}
		}
		neededTalent = (int)Math.ceil((double)overallSellAmount/10);
		// so....kaufen...
		// wat haben wir denn so an Geld -> sp�ter, was wir so kriegen k�nnen
		// jetzt fragen wir uns, ob er noch gen�gend Talente hat
		if (this.getTrader().isKaufen()){
			// Talentpunkte der unit ermitteln...
			SkillType handelsSkillType =  super.gd_Script.rules.getSkillType("Handeln");
			Skill handelsSkill =  this.scriptUnit.getUnit().getModifiedSkill(handelsSkillType);
			int persons = this.scriptUnit.getUnit().getModifiedPersons();
			int availableTalent = 0;
			if (handelsSkill!=null){
				availableTalent = handelsSkill.getLevel() * persons;
			}
			
			// wieviel soll gekauft werden?
			int kaufMenge = this.getTrader().getBuy().getAmount();
			if (this.getTrader().getBuyPolicy() == Trader.trader_buy_setManager){
				// soll suggested �bernehmen, egal wieviel
				int vorMenge = getTradeArea().suggestedBuyAmount(getTradeRegion(),true);
				if (vorMenge>0){
					kaufMenge = vorMenge;
				} else if (vorMenge==-1){
					addComment("!! Es gibt keinen Grund, hier etwas einzukaufen!!");
					kaufMenge=0;
				} else {
					addComment("!!unerwartete vorgeschlagene Einkaufsmenge!!");
					this.scriptUnit.doNotConfirmOrders();
				}
			}
			if (this.getTrader().getBuyPolicy() == Trader.trader_buy_Max){
				// soll suggested �bernehmen, mindestens aber r.max
				int vorMenge = getTradeArea().suggestedBuyAmount(getTradeRegion(),true);
				if (vorMenge>0){
					// nur wenn �berkauf
					// unterkauf nicht
					if (vorMenge>kaufMenge){
						kaufMenge = vorMenge;
					}
				} else if (vorMenge==-1){
					addComment("!! Es gibt keinen Grund, hier etwas einzukaufen!!");
					kaufMenge=0;
				} else {
					addComment("!!unerwartete vorgeschlagene Einkaufsmenge!!");
					this.scriptUnit.doNotConfirmOrders();
				}
			}
			
			this.getTrader().getBuy().setAmount(kaufMenge);
			
			// hier kleiner Schnitt.....nicht sch�n.
			
			int sollKauf = this.getTrader().getBuy().getAmount();
			// wieviel Talent daf�r ben�tigt?
			int sollTalent = (int)Math.ceil((double)sollKauf/10);
		
			// durch Verk�ufe verbrauchtes Talent abziehen vom avail
			availableTalent -= neededTalent;
			
			if (sollTalent>availableTalent){
				// wenigstens ne Info schreiben...
				this.addComment("zu wenig Talent zum Kauf: " + availableTalent + "/" + sollTalent);
				this.addOutLine("!Handeln: zu wenig Talent zum Kauf: (" + availableTalent + "/" + sollTalent + "): " + this.unitDesc());
				
				if (availableTalent>0){
					// ok, wir k�nnen noch ein wenig einkaufen
					// wieviel ist damit m�glich?
					int availKauf = availableTalent * 10;
					// adjusted entsprechend setzen
					this.adjustedBuyAmount = Math.min(sollKauf, availKauf);
				} else {
					// nix zu machen, wir k�nnen nicht kaufen
					this.adjustedBuyAmount = 0;
				}
				
				
			} else {
				// alles sch�n, genug vorhanden
				this.adjustedBuyAmount = sollKauf;
			}
			
			// Kauf setzen
			this.getTrader().getBuy().setAmount(this.adjustedBuyAmount);
			// adjusted silber anfordern
			int amount = TradeUtils.getPrice(kaufMenge, Math.abs(this.buyingPrice.getPrice()), r.maxLuxuries());
			this.requested_silver = amount;
			MatPoolRequest HR = new MatPoolRequest(this,amount,"Silber",this.getTrader().getSilverRequestPrio(),kommentar_silberRequest);
			super.addComment("Einkaufssilber: " + amount + " Silber (" + kaufMenge + " " + this.buyingPrice.getItemType().getName() + ")",true);
			this.addMatPoolRequest(HR);
			
			// Silvervorrat
			
			
			if (this.getTrader().getAnzahl_silber_runden()>0 && reportSettings.getOptionBoolean("DepotSilber", this.region())){
				super.setPrioParameter(this.getTrader().getSilverRequestPrio()-2,-0.5,0,2);
				
				int kostenProRunde = this.requested_silver;
			
				this.addComment("SilberVorrat(Depot): fordere " + this.getTrader().getAnzahl_silber_runden() + " Runden a " + kostenProRunde + " Silber an.");
				
				
				this.silberDepotMPRs = new LinkedList<MatPoolRequest>();
				// los gehts
				for (int i = 1;i<=this.getTrader().getAnzahl_silber_runden();i++){
					int actPrio = super.getPrio(i-1);
					MatPoolRequest actMPR = new MatPoolRequest(this,kostenProRunde,"Silber",actPrio,this.default_silbervorrat_kommentar);
					this.addMatPoolRequest(actMPR);
					this.silberDepotMPRs.add(actMPR);
				}
			} else {
				super.addComment("Debug: anzahlSilberRunden: " + this.getTrader().getAnzahl_silber_runden());
				if (reportSettings.getOptionBoolean("DepotSilber", this.region())){
					super.addComment("Debug: SilberDepot aktiv");
				} else {
					super.addComment("Debug: SilberDepot inaktiv");
				}
				super.addComment("Keine SilberDepotFunktion aktiviert");
			}
			
		}	
	}
	
	
	private void privat_return(){
		if (!doSomething){
			super.addComment("H�ndler hat keinen Auftrag und lernt.");
			super.lerneTalent("Handeln", true);
		}
	}
	
	/**
	 * Bekommen wir gen�gend Silber?
	 * Sonst warnen
	 */
	private void nachMatPool2(){
		if (!this.getTrader().isKaufen()){
			privat_return();
			return;
		}
		if (this.getTrader().isLernen()){
			privat_return();
			return;
		}
		if (this.getTrader().getBuy()==null){
			privat_return();
			return;
		}
		
		if (this.getTrader().getBuy().getAmount()<=0){
			super.addComment("Keinen Kaufbefehl gesetzt. Kein Bedarf", true);
			privat_return();
			return;
		}
			
		ItemType silver =  super.gd_Script.rules.getItemType("Silber");
		Item silverItem = this.scriptUnit.getModifiedItem(silver);
		int silverAmount = 0;
		if (silverItem != null){
			silverAmount = silverItem.getAmount();
		}
		if (this.requested_silver>-1 && silverAmount<this.requested_silver){
			// nicht gen�gend Silber erhalten...
			// wieviel kann man damit kaufen?
			int changedAmount = TradeUtils.getMenge(silverAmount, Math.abs(this.buyingPrice.getPrice()), r.maxLuxuries());
			if (changedAmount>0){
				// kann nur vermindert kaufen
				super.addComment("Silberknappheit. Sollkauf: " + this.getTrader().getBuy().getAmount() + ", wegen Silberknappheit nur: " + changedAmount, true);
				outText.addOutLine("!!!*Handeln: Silberknappheit->" + changedAmount + "/" + this.getTrader().getBuy().getAmount() + ": " + this.unitDesc());
				super.addOrder("KAUFEN " + changedAmount + " " + this.getTrader().getBuy().getItemType().getName(),true);
				doSomething=true;
			} else {
				// kann gar nix kaufen
				super.addComment("Kein Silber. Sollkauf: " + this.getTrader().getBuy().getAmount(), true);
				outText.addOutLine("!!!** Handeln: Kein Kaufsilber: " + this.unitDesc());
				super.scriptUnit.doNotConfirmOrders();
			}
		} else {
			// gehe davon aus, genug Silber zu haben
			if (this.getTrader().getBuy().getAmount()>0){
				super.addOrder("KAUFEN " + this.getTrader().getBuy().getAmount() + " " + this.getTrader().getBuy().getItemType().getName(),true);
				doSomething=true;
			}
		}
		
		
		// Silbervorrsat info
		this.SiberVorratInfo();
		
		// �berhaupt was getan?
		// dann bisher kein langer Befehl erfolgt..schlagen wir lernen vor
		
		privat_return();
	}
	
	/**
	 * rein informativ f�r die unit
	 * @param scriptDurchlauf
	 */
	public void SiberVorratInfo(){
		if (this.silberDepotMPRs==null || this.silberDepotMPRs.size()==0){
			return;
		}
		for (Iterator<MatPoolRequest> iter = this.silberDepotMPRs.iterator();iter.hasNext();){
			MatPoolRequest MPR = (MatPoolRequest)iter.next();
			String erg = "DepotSilber gefordert:";
			erg += MPR.getOriginalGefordert() + "(Prio " + MPR.getPrio() + ")";
			erg += ",bearbeitet:" + MPR.getBearbeitet();
			this.addComment(erg);
			
			// Problem
			if (MPR.getPrio()==this.getTrader().getSilverRequestPrio() && MPR.getBearbeitet()<MPR.getOriginalGefordert()){
				// max prio und nicht erf�llt ?!
				this.scriptUnit.doNotConfirmOrders();
				
				// TM Info
				String s2Info = "";
				if (MPR.getTransportRequest()!=null){
					s2Info = " (TM: " + MPR.getTransportRequest().getBearbeitet() + " geladen) ";
				}
				
				this.addComment("!!! H�ndler: DepotSilberWarnung !!!" + s2Info);
				outText.addOutLine("!!! H�ndler: DepotSilberWarnung: " + s2Info + this.unitDesc(),true);
			}
		}
	}
	
	
	private void showTradeAreaInfo(){
		if (this.getTrader().isUserWishIslandInfo()){
			LinkedList<String> islandInfo = TradeUtils.getIslandInfo(this.scriptUnit.getUnit().getRegion(), this.scriptUnit.getScriptMain());
			if (islandInfo==null){
				addComment("keine Inselinfo verf�gbar");
				return;
			}
			if (islandInfo.size()==0){
				addComment("keine Inselinfo verf�gbar");
				return;
			}
			for (Iterator<String> iter = islandInfo.iterator();iter.hasNext();){
				String s = (String)iter.next();
				this.addComment(s,false);
			}
		}
		

		LinkedList<String> info = this.getTradeArea().getTradeAreaUnitInfo(this.getTradeRegion());
		if (info==null){return;}
		if (info.size()==0){return;}
		for (Iterator<String> iter = info.iterator();iter.hasNext();){
			String s = (String)iter.next();
			this.addComment(s,false);
		}
	}
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
}

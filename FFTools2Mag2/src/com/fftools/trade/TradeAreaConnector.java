package com.fftools.trade;

import java.util.ArrayList;

import magellan.library.rules.ItemType;

import com.fftools.ScriptUnit;

public class TradeAreaConnector {

	private ScriptUnit SU1 = null;
	private ScriptUnit SU2 = null;
	
	private TradeArea TA1 = null;
	public TradeArea getTA1() {
		return TA1;
	}

	private TradeArea TA2 = null;
	
	public TradeArea getTA2() {
		return TA2;
	}

	private String Name = "not named";
	
	
	public String getName() {
		return Name;
	}

	private TradeAreaHandler TAH=null;
	
	private boolean isValid = false;
	
	public boolean isValid() {
		return isValid;
	}

	
	private ArrayList<TAC_Transfer> transfersTo1 = new ArrayList<TAC_Transfer>();
	private ArrayList<TAC_Transfer> transfersTo2 = new ArrayList<TAC_Transfer>();
	
	
	public TradeAreaConnector(ScriptUnit u1,ScriptUnit u2,String _Name,TradeAreaHandler _TAH){
		this.TAH = _TAH;
		this.SU1 = u1;
		this.SU2 = u2;
		
		this.TA1 = TAH.getTAinRange(this.SU1.getUnit().getRegion());
		this.TA2 = TAH.getTAinRange(this.SU2.getUnit().getRegion());
		
		this.Name = _Name;
		
		if (this.TA1!=null && this.TA2!= null && !this.TA1.equals(this.TA2)){
			isValid = true;
		}
		
		if (isValid && this.Name.length()<2){
			isValid = false;
		}
		if (isValid && this.Name.equalsIgnoreCase("not named")){
			isValid = false;
		}
		
	}

	public ScriptUnit getSU1() {
		return SU1;
	}

	public ScriptUnit getSU2() {
		return SU2;
	}
	
	
	private class TAC_Transfer{
		public ItemType itemType;
		// Menge nach relativer Distru
		public int amount_1 = 0;
		// Menge nach finaler Distru
		public int amount_2 = 0;
		
		public TAC_Transfer(ItemType _itemType,int _amount_1,int _amount_2){
			this.itemType = _itemType;
			this.amount_1 = _amount_1;
			this.amount_2 = _amount_2;
		}	
	}
	
	/**
	 * ergänzt einen Transfer
	 * @param targetTA
	 * @param itemType
	 * @param amount
	 */
	public void addTransfer(TradeArea targetTA,ItemType itemType, int amount){
		TAC_Transfer actT = new TAC_Transfer(itemType,amount,0);
		if (targetTA.equals(this.getTA1())){
			this.transfersTo1.add(actT);
		}
		if (targetTA.equals(this.getTA2())){
			this.transfersTo2.add(actT);
		}
	}
	
	/**
	 * liefert den entsprechenden Betrag des Transfers in diesem TAC zum TA!
	 * @param targetTA
	 * @param itemType
	 * @return
	 */
	public int getTransferAmount(TradeArea targetTA, ItemType itemType,int whichAmount){
		ArrayList<TAC_Transfer> myList = null;
		if (targetTA.equals(this.getTA1())){
			myList = this.transfersTo1;
		}
		if (targetTA.equals(this.getTA2())){
			myList = this.transfersTo2;
		}
		if (myList==null || myList.size()==0){
			return 0;
		}
		for (TAC_Transfer actT:myList){
			if (actT.itemType.equals(itemType)){
				if (whichAmount==1){
					return actT.amount_1;
				}
				if (whichAmount==2){
					return actT.amount_2;
				}
				return 0;
			}
		}
		return 0;
	}
	
	
}

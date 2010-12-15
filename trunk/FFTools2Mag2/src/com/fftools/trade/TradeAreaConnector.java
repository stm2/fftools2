package com.fftools.trade;

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
	
}

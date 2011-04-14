package com.fftools.pools.bau;


import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Bauauftrag;
import com.fftools.scripts.Bauen;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaHandler;


/**
 *Verwaltet Bauen-scripte
 *
 */
public class BauManager implements OverlordRun,OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf0 = 9;
	private static final int Durchlauf1 = 105;
	
	
	private int[] runners = {Durchlauf0,Durchlauf1};
	
	private ScriptMain scriptMain = null;
	private TradeAreaHandler tradeAreaHandler = null;

	public BauManager (ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
		this.tradeAreaHandler = this.scriptMain.getOverlord().getTradeAreaHandler();
	}
	
	
	
	public void informUs(){
		
	}
	
	
	/**
	 * Checkt den Status der bauscripte
	 *
	 */
	
	public void run(int durchlauf){
		
		
		
		if (this.tradeAreaHandler.getTradeAreas()==null || this.tradeAreaHandler.getTradeAreas().size()==0){
			return;
		}
		
		for (TradeArea tA:this.tradeAreaHandler.getTradeAreas()){
			if (tA.hasBauManager()){
				if (durchlauf==Durchlauf0){
					tA.getTradeAreaBauManager().run0();
				}
				if (durchlauf==Durchlauf1){
					tA.getTradeAreaBauManager().run1();
				}
			}
		}
	}
	
	
	public void addBauScript(Bauen bauen){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(bauen.region());
		if (tA==null){
			outText.addOutLine("!!!addBauscript nicht erfolgreich: kein TA :" + bauen.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addBauScript(bauen);
		bauen.addComment("zugeordnet zum TA: " + tA.getName());
	}
	
	
	public void addBauAuftrag(Bauauftrag bauAuftrag){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(bauAuftrag.region());
		if (tA==null){
			outText.addOutLine("!!!addBauAuftrag nicht erfolgreich: kein TA :" + bauAuftrag.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addBauAuftrag(bauAuftrag);
		bauAuftrag.addComment("zugeordnet zum TA: " + tA.getName());
	}
	
	
	public int[] runAt(){
		return runners;
	}
	
}

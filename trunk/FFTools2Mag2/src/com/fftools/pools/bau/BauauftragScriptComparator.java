package com.fftools.pools.bau;

import java.util.Comparator;

import magellan.library.Region;

import com.fftools.scripts.Bauen;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * small class to compare 2 Bauen-scripts (als Aufträge)
 * @author Fiete
 *
 */
public class BauauftragScriptComparator implements Comparator<Bauen> {
	
	private Region targetRegion;
	private int level=1;
	private String talentName;
	
	
	public BauauftragScriptComparator(Region r, int _level,String _talentName){
		this.targetRegion = r;
		this.level=_level;
		this.talentName = _talentName;
	}
	
	
	
	
	public int compare(Bauen b1, Bauen b2){
		int wert1 = 1000;
		int wert2 = 1000;
		
		
		// Berechnung: zurückzulegender Weg + Bauzeit
		if (targetRegion.equals(b1.scriptUnit.getUnit().getRegion())){
			wert1 =0;
		} else {
			GotoInfo gI1 = FFToolsRegions.makeOrderNACH(b1.scriptUnit, targetRegion.getCoordinate() ,b1.scriptUnit.getUnit().getRegion().getCoordinate(), false);
			wert1 = gI1.getAnzRunden();
		}
		if (targetRegion.equals(b2.scriptUnit.getUnit().getRegion())){
			wert2 =0;
		} else {
			GotoInfo gI2 = FFToolsRegions.makeOrderNACH(b2.scriptUnit, targetRegion.getCoordinate() ,b2.scriptUnit.getUnit().getRegion().getCoordinate(), false);
			wert1 = gI2.getAnzRunden();
		}

		
		// Bauzeit
		double tp1 = Math.ceil(b1.scriptUnit.getSkillLevel(this.talentName)/this.level);
		double tp2 = Math.ceil(b2.scriptUnit.getSkillLevel(this.talentName)/this.level);
		
		tp1 += wert1;
		tp2 += wert2;
		
		return (int) Math.round((tp1-tp2));
	}
}


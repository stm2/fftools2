package com.fftools.utils;

import java.util.Iterator;

import magellan.library.GameData;
import magellan.library.Ship;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.SkillType;

import com.fftools.scripts.Script;

/**
 * Unit-Handling
 * @author Fiete
 *
 */

public class FFToolsUnits {
	
	/**
	 * Berechnet Magielernkosten
	 * Ber�cksichtight Rassenbonus und Akademieaufenthalt
	 * 
	 * 
	 * @param u
	 * @param data
	 * @return
	 */
	public static int calcMagieLernKosten(Unit u, GameData data){
		int erg = 0;
		SkillType mageSkillType = data.rules.getSkillType("Magie",false);
		Skill mageLevel = u.getModifiedSkill(mageSkillType);
		int raceBonus = 0;
		
//		 Ber�cksichtigung von Rassenbonus! (War zuerst nicht vorhanden)
    	if(u.getDisguiseRace() != null) {
			raceBonus = u.getDisguiseRace().getSkillBonus(mageSkillType);
		} else {
			if(u.getRace() != null) {
				raceBonus = u.getRace().getSkillBonus(mageSkillType);
			}
		}
    	int relevant_skill = 0;
    	if (mageLevel!=null){
    		relevant_skill = mageLevel.getLevel() - raceBonus;
    		if (relevant_skill<0){
    			relevant_skill=0;
    		}
    	}
    	erg = (50+25*(1+relevant_skill+1)*(relevant_skill+1))*u.getModifiedPersons();
    	if (u.getModifiedBuilding()!=null){
    		if (u.getModifiedBuilding().getBuildingType().getName().equalsIgnoreCase("Akademie")){
    			erg*=2;
    		}
    	}
		return erg;
	}
	
	public static int getModifiedSkillLevel(Skill skill,Unit unit, boolean includeBuilding) {
		if((unit != null) && (unit.getModifiedPersons() != 0)) {
			int raceBonus = 0;
			int terrainBonus = 0;
			int buildingBonus = 0;

			if(unit.getRace() != null) {
				raceBonus = unit.getRace().getSkillBonus(skill.getSkillType());
			}

			if(unit.getRegion() != null) {
				terrainBonus = unit.getRace().getSkillBonus(skill.getSkillType(), unit.getRegion().getRegionType());
			}

			if(includeBuilding && (unit.getBuilding() != null)) {
				buildingBonus = unit.getBuilding().getBuildingType().getSkillBonus(skill.getSkillType());
			}

			return Skill.getLevel(skill.getPoints() / unit.getModifiedPersons(), raceBonus, terrainBonus,
							buildingBonus, unit.isStarving());
		}

		return 0;
	}
	
	
	public static SkillType getBestSkillType(Unit u){
		// bestSkillType feststellen
		SkillType bestSkillType = null;
		int bestSkillLevel = 0;
		if (u.getModifiedSkills()!=null && u.getModifiedSkills().size()>0){
			for (Iterator<Skill> iter = u.getModifiedSkills().iterator();iter.hasNext();){
				Skill actSkill = (Skill)iter.next();
				if (actSkill.getLevel()>=bestSkillLevel){
					bestSkillType = actSkill.getSkillType();
					bestSkillLevel = actSkill.getLevel();
				}
			}
		}
		return bestSkillType;
	}
	
	public static String getBestSkillTypeName(Unit u){
		SkillType bst = FFToolsUnits.getBestSkillType(u);
		if (bst!=null){
			return bst.getName();
		}
		return null;
	}
	
	/**
	 * ?berpr?ft, ob einheit Kapit�n eines schiffes
	 * versucht, das shiff zu setzen (wird f�r pathbuilding en?tigt)
	 * wenn beides OK -> true, sonst false
	 * @return
	 */
	public static boolean checkShip(Script aScript){
		// schiff checken
		Ship myS = aScript.scriptUnit.getUnit().getModifiedShip();
		if (myS==null){
			return false;
		}
		Ship ship = myS;
		
		// Kapt�n checken
		Unit captn = ship.getOwnerUnit();
		// ist es unsere unit?
		if (captn!=null && captn.equals(aScript.scriptUnit.getUnit())){
			return true;
		}
		
		// Sonderfall: wir sind jetzt auf keinem oder einem anderen Shiff
		// und betreten ein neues ... dann doch OK geben
		Ship actShip = aScript.scriptUnit.getUnit().getShip();
		if (actShip==null || !actShip.equals(ship)){
			return true;
		}
		return false;
	}
	
}

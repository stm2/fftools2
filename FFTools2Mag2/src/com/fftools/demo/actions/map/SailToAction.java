/*
 *  Copyright (C) 2000-2004 Roger Butenuth, Andreas Gampe,
 *                          Stefan Goetz, Sebastian Pappert,
 *                          Klaas Prause, Enno Rehling,
 *                          Sebastian Tusk, Ulrich Kuester,
 *                          Ilja Pavkovic
 *
 * This file is part of the Eressea Java Code Base, see the
 * file LICENSING for the licensing information applying to
 * this file.
 *
 */

package com.fftools.demo.actions.map;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import magellan.client.event.OrderConfirmEvent;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Unit;
import magellan.library.UnitContainer;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.utils.Regions;
import magellan.library.utils.Resources;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 *
 * @author Fiete
 * @version
 */
public class SailToAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	private Region targetRegion = null;
	private String command = "";
	
	private GameData data=null;
	
	private boolean isShipableRegion=false;
	
	public Region getTargetRegion() {
		return targetRegion;
	}

	public void setTargetRegion(Region targetRegion) {
		this.targetRegion = targetRegion;
		command = "// script SailTo " + targetRegion.getCoordX()+","+targetRegion.getCoordY()+" ;" + targetRegion.getName(); 
		setName(command);
		this.data = this.selectionObserver.getClient().getData();
		
		this.isShipableRegion=false;
        if (targetRegion.getRegionType().isOcean()){
           this.isShipableRegion=true;  
        } else {
          // run through the neighbors
          for (Region r:targetRegion.getNeighbors().values()) {
            if (r.getRegionType().isOcean()) {
              this.isShipableRegion=true;
              break;
            }
          }
        }
	}

	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public SailToAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("// script SailTo");
        this.selectionObserver = selectionObserver; 
        this.data = this.selectionObserver.getClient().getData();
        
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		ArrayList<Object> units = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (units!=null && units.size()>0){
			for (Iterator<Object> iter = units.iterator();iter.hasNext();){
				Unit actUnit = (Unit)iter.next();
				this.Goto(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein Goton m�glich.","Fehler",true);
		}
	}

	/**
	 * f�hrt den set Goto f�r diese Unit durch
	 * @param u
	 */
	private void Goto(Unit u){
		if (this.targetRegion==null){
			new MsgBox(this.selectionObserver.getClient(),"Kein Goto m�glich. (keine Region)","Fehler",true);
			return;
		}
		
		String path = "";
	   List<Region>regionList=null;
	   
	   boolean PathNotFound = false;
	   String order = "nix";
	   
  	     
    	if (isSeaConnPossible(u)){
  	       regionList = Regions.planShipRoute(u.getModifiedShip(),data, this.targetRegion.getCoordinate());
           path=Regions.getDirections(regionList);
       } else {
    	   order = "; !!! nicht m�glich (Kein Kapit�n, ung�nstige Zielregion)";
    	   PathNotFound=true;
       }
  	    
  	   
  	     if (path!=null && path.length()>0){
  	       // Pfad gefunden
  	    	order = Resources.getOrderTranslation(EresseaConstants.O_MOVE) + " " + path;
  	     } else {
  	    	 order = "; !!! Kein Weg gefunden!";
  	    	 PathNotFound=true;
  	     }
	   
	    u.addOrder(order);   
		u.addOrder(command);
		if (!PathNotFound){
			u.setOrdersConfirmed(true);
		}
		List<Unit> units = new LinkedList<Unit>();
		units.add(u);
		this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units));
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,u));
	}
	
	
	/**
	   * Pr�fen ob f�r diese Unit eine Seeverbindung zur destRegion
	   * prinzipiell m�glich ist
	   */
	  private boolean isSeaConnPossible(Unit u){
	    // oder ben�tigt...bei gleicher Region->false!
	    // Region extrahieren
	    Region originRegion = u.getRegion();
	    // nicht in gleicher Region
	    if (originRegion.equals(this.targetRegion)){
	      return false;
	    }
	    // Unit muss Kapit�n sein
	    boolean capt=false;
	    UnitContainer uc = u.getModifiedUnitContainer();
	    if (uc!=null){
	      if (uc instanceof Ship){
	        Ship s = (Ship)uc;
	        if (s.getOwner()!=null && s.getOwner().equals(u)){
	          capt = true;
	        }
	      }
	    }
	    if (!capt){
	      // Kapit�n
	      return false;
	    }
	    
	    // Zielregion muss am Meer liegen oder Ozean sein
	    if (!this.isShipableRegion){
	      return false;
	    }
	    
	    // Alle Fehler (prinzipiell) ausgeschlossen (?)
	    
	    return true;
	  }
	
}

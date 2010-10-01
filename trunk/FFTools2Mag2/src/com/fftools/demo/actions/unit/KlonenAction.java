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

package com.fftools.demo.actions.unit;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import magellan.client.event.TempUnitEvent;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.UnitID;
import magellan.library.io.cr.CRParser;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 *
 * @author Fiete
 * @version
 */
public class KlonenAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public KlonenAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("Klonen");
        this.selectionObserver = selectionObserver; 
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
				this.klone(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein Klonen m�glich.","Fehler",true);
		}
	}

	/**
	 * f�hrt den Klonvorgang f�r diese Unit durch
	 * @param u
	 */
	private void klone(Unit u){
		Unit parentUnit = u;
		if(u instanceof TempUnit) {
			parentUnit = ((TempUnit) u).getParent();
		}
		// neue Unit ID
		UnitID id = UnitID.createTempID(this.selectionObserver.getClient().getData(), this.selectionObserver.getClient().getProperties(), parentUnit);
		// Die tempUnit anlegen
		TempUnit tempUnit = parentUnit.createTemp(id);
		
		// name setzen
		tempUnit.addOrderAt(0, "BENENNEN EINHEIT \"" + parentUnit.getModifiedName() + "\" ;dnt", true);
		// script orders �bernehmen
		for (Iterator<String> iter=parentUnit.getOrders().iterator();iter.hasNext();){
			String actOrder = (String)iter.next();
			if (actOrder.toLowerCase().startsWith("// script")){
				tempUnit.addOrderAt(0, actOrder, false);
			}
		}
		
		// rekrutieren
		tempUnit.addOrderAt(0, "// script Runde " + this.selectionObserver.getClient().getData().getDate().getDate() + " script Rekrutieren " + parentUnit.getModifiedPersons(), false);
		
		// tag1 und tag2 �bernehmen
		String tag1 = parentUnit.getTag(CRParser.TAGGABLE_STRING);
		if (tag1!=null && tag1.length()>0){
			tempUnit.putTag(CRParser.TAGGABLE_STRING,tag1);
		}
		String tag2 = parentUnit.getTag(CRParser.TAGGABLE_STRING2);
		if (tag2!=null && tag2.length()>0){
			tempUnit.putTag(CRParser.TAGGABLE_STRING2,tag2);
		}
		
		this.selectionObserver.getClient().getDispatcher().fire(new TempUnitEvent(this, tempUnit, TempUnitEvent.CREATED));
	}
	
	
}

package com.fftools.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Order;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;


/**
 * viele scripts erwarten hinter dem scriptnamen optionen
 * Beispiel route=fest oder vorrat=5
 * Diese utility vereinfacht den zugriff auf diese optionen
 * @author Fiete
 *
 */
public class FFToolsOptionParser {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	/**
	 * welche befehle sollen ausgwertet werden..bzw
	 * hinter welchen befehlen stehen die auszuwertenden 
	 * optioinen?
	 * // script wird autoamtisch erg�nzt 
	 */
	private ArrayList<String> filters = null;
	
	/** 
	 * die erkannten optionen
	 */
	private Hashtable<String,String> options = null;
	
	/**
	 * nur zur Sicherheit eine lokale Referenz.
	 */
	private ScriptUnit scriptUnit = null;
	
	
	/**
	 * Konstruktor
	 * @param u
	 */
	public FFToolsOptionParser(ScriptUnit u){
		this(u,null);
	}
	
	/**
	 * initialisiert neuen OptionParser
	 * @param u Scriptunit
	 * @param filterName ein m�glicher filter
	 */
	public FFToolsOptionParser(ScriptUnit u, String filterName){
		this.scriptUnit = u;
		if (filterName!=null){
			// �bergebenen Filter setzen
			this.filters= new ArrayList<String>();
			this.filters.add(filterName);
		}
		this.init();
	}
	
	
	/**
	 * parst die orders der unit
	 * checked die options
	 *
	 */
	private void init(){
		// gleichzeitig reset
		if (this.options!=null){
			this.options.clear();
		}
		
		// wenn gar kein filter gesetzt..wars dass...
		if (this.filters==null || this.filters.size()==0){
			return;
		}
		
		// gleich mal parsen..
		for (Order o:this.scriptUnit.getUnit().getOrders2()){
			String order = o.getText();
			for (Iterator<String> iter2 = this.filters.iterator();iter2.hasNext();){
				String filter = (String)iter2.next();
				if (order.toLowerCase().startsWith("// script " + filter.toLowerCase())){
					// treffer
					String myOrder = order.substring(("// script " + filter.toLowerCase()).length());
					if (myOrder.length()>0 && myOrder.indexOf("=")>0){
						this.parseOptionLine(myOrder);
					}
				}
			}
		}
	}
	
	/**
	 * untersucht den �bergebenen String hinter dem scriptnamen
	 * @param s alles hinter dem script namen
	 */
	private void parseOptionLine(String s){
		// zerlegen in durch space getrennte abschnitte
		String[] pairs = s.split(" ");
		for (int i=0;i<pairs.length;i++){
			String s2 = pairs[i];
			if (s2.indexOf("=")>0){
				this.parseSingleOption(s2);
			}
		}
	}
	
	/**
	 * untersucht ein einzelnes p�rchen key=value
	 * @param s
	 */
	private void parseSingleOption(String s){
		String[] pair = s.split("=");
		if (pair.length!=2){
			outText.addOutLine("!!Optionenparser Fehler:" + s + " (" + this.scriptUnit.getUnit().toString(true) + ")");
			return;
		}
		String key = pair[0];
		String value = pair[1];
		
		if (!(key.length()>0) || !(value.length()>0)){
			outText.addOutLine("!!Optionenparser Fehler_x:" + s + " (" + this.scriptUnit.getUnit().toString(true) + ")");
			return;
		}
		
		if (this.options==null){
			this.options = new Hashtable<String, String>();
		}
		
		// this.options.put(key.toLowerCase(), value.toLowerCase());
		this.options.put(key.toLowerCase(), value);
	}
	
	/**
	 * komplettes filterarray explizit erg�nzen...
	 * @param _filteList
	 */
	public void addFilter(ArrayList<String> _filterList){
		if (this.filters==null){
			this.filters = new ArrayList<String>();
		}
		this.filters.addAll(_filterList);
		this.init();
	}
	
	/**
	 * komplettes filterarray explizit setzen...
	 * @param _filteList
	 */
	public void setFilter(ArrayList<String> _filterList){
		this.filters=null;
		this.addFilter(_filterList);
	}
	
	/**
	 * liefert ein String value zum zugeh�rigen key oder ""
	 * liefert also nie NULL
	 * @param key
	 * @return
	 */
	public String getOptionString(String key){
		if (this.options == null){return "";}
		String value = (String) this.options.get(key.toLowerCase());
		if (value==null){return "";}
		return value;
	}
	
	/**
	 * liefert eine ArrayList<String>zum zugeh�rigen key oder
	 * eine leere Liste liefert also nie NULL
	 * @param key
	 * @return
	 */
	public ArrayList<String> getOptionStringList(String key){
		if (this.options == null){return new ArrayList<String>();}
		String value = (String) this.options.get(key.toLowerCase());
		if (value==null){return new ArrayList<String>();}
		ArrayList<String> erg = new ArrayList<String>();
		String[] ret = value.split(",");
		for (int i = 0; i < ret.length; i++)
		{
		    erg.add(ret[i]);
		}
		return erg;
	}

	/**
	 * liefert ein int value zum zugeh�rigen key oder ifNotFound
	 * @param key
	 * @return
	 */
	public int getOptionInt(String key,int ifNotFound){
		if (this.options==null){return ifNotFound;}
		String value = this.options.get(key.toLowerCase());
		if (value==null){return ifNotFound;}
		int erg = ifNotFound;
		try {
			erg = Integer.parseInt(value);
		} catch (NumberFormatException e){
			// war also keine Zahl
		}
		return erg;
	}
	
	/**
	 * liefert ein boolean value zum zugeh�rigen key oder ifNotFound
	 * @param key
	 * @return
	 */
	public boolean getOptionBoolean(String key,boolean ifNotFound){
		if (this.options==null){return ifNotFound;}
		String value = this.options.get(key.toLowerCase());
		if (value==null){return ifNotFound;}
		boolean erg = ifNotFound;
		if (value.equalsIgnoreCase("an") || 
				value.equalsIgnoreCase("ja") ||
				value.equalsIgnoreCase("true") ||
				value.equalsIgnoreCase("on")
				){
			erg = true;
		}
		if (value.equalsIgnoreCase("aus") || 
				value.equalsIgnoreCase("nein") ||
				value.equalsIgnoreCase("off") ||
				value.equalsIgnoreCase("false")){
			erg = false;
		}
		return erg;
	}
	
	/**
	 * liefert nur wahr, wenn dem key tats�chlich value in den optionen
	 * zugeordnet ist, ansonsten immer false 
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean isOptionString(String key,String value){
		if (this.options==null){return false;}
		String myValue = this.options.get(key.toLowerCase());
		if (myValue==null){return false;}
		// entscheidender check
		if (myValue.equalsIgnoreCase(value)){
			return true;
		}
		return false;
	}
	
	/**
	 * der �bergebene String wird geparst, erkannte Optionen hinzugef�gt
	 * @param s
	 */
	public void addOptionString(String s){
		if (s==null){
			return;
		}
		if (s.length()==0){
			return;
		}
		this.parseOptionLine(s);
	}
	
	/**
	 * die �bergebene ArrayList wird als Liste von Parametern interpretiert 
	 * und einzeln als Option geparst
	 * @param l
	 */
	public void addOptionList(ArrayList<String> l){
		if (l==null){
			return;
		}
		if (l.size()==0){
			return;
		}
		for (Iterator<String> iter = l.iterator();iter.hasNext();){
			String s = (String)iter.next();
			this.addOptionString(s);
		}
	}
	
	public void reset(){
		if (this.options!=null){
			this.options.clear();
		}
		if (this.filters!=null){
			this.filters.clear();
		}
	}
	
}

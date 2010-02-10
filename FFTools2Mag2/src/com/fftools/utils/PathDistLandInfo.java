package com.fftools.utils;

import magellan.library.CoordinateID;
import magellan.library.GameData;

/**
 * for caching the calls to FfTools.getPathDistLand 
 * @author Fiete
 *
 */
public class PathDistLandInfo {
	private CoordinateID von = null;
	private CoordinateID nach = null;
	private boolean reitend = false;
	
	/**
	 * Konstruktor
	 * @param data
	 * @param von
	 * @param nach
	 */
	public PathDistLandInfo(CoordinateID von, CoordinateID nach, boolean reitend) {
		super();
		
		this.von = von;
		this.nach = nach;
		this.reitend = reitend;
	}
	
	public boolean equals(Object o){
		boolean retVal=true;
		if (!(o instanceof PathDistLandInfo)){
			return false;
		}
		PathDistLandInfo other = (PathDistLandInfo)o;
		
		if (!other.getVon().equals(this.von)){
			return false;
		}
		if (!other.getNach().equals(this.nach)){
			return false;
		}
		if (other.isReitend()!=this.reitend){
			return false;
		}
		return retVal;
	}
	
	public boolean is(CoordinateID _von,CoordinateID _nach,boolean _reitend){
		boolean retVal=true;

		if (!_von.equals(this.von)){
			return false;
		}
		if (!_nach.equals(this.nach)){
			return false;
		}
		if (_reitend!=this.reitend){
			return false;
		}
		
		
		return retVal;
	}

	

	/**
	 * @return the nach
	 */
	public CoordinateID getNach() {
		return nach;
	}

	/**
	 * @return the von
	 */
	public CoordinateID getVon() {
		return von;
	}

	/**
	 * @return the reitend
	 */
	public boolean isReitend() {
		return reitend;
	}

	/**
	 * @param reitend the reitend to set
	 */
	public void setReitend(boolean reitend) {
		this.reitend = reitend;
	}
	
}

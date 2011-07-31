package com.fftools.tool3;


public class Tool3Coord {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	private int x = 0;
	private int y = 0;
	private int z = 0;
	
	private boolean isSet = false;
	
	
	
	
	public Tool3Coord(String s){
		isSet=false;
		// outText.addOutLine("Tool3Coord with s=" + s);
		String[] splittArray = s.split(" ");
		if (splittArray.length<2 || splittArray.length>3){
			return;
		}
		x = Integer.parseInt(splittArray[0]);
		y = Integer.parseInt(splittArray[1]);
		if (splittArray.length==3){
			z = Integer.parseInt(splittArray[2]);
		} else {
			z = 0;
		}
		isSet=true;
	}
	
	public String toString(){
		String erg = "unset";
		if (isSet){
			erg = x + "," + y;
			if (z!=0) {
				erg = erg + "," + z;
			}
		}
		return erg;
	}
	
	public boolean equals(Object o){
		if (!(o instanceof Tool3Coord)){
			return false;
		}
		Tool3Coord other = (Tool3Coord)o;
		if (this.isSet && other.isSet && this.x==other.x && this.y==other.y && this.z==other.z){
			return true;
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}
	/**
	 * @param x the x to set
	 */
	public void setX(int x) {
		this.x = x;
	}
	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}
	/**
	 * @param y the y to set
	 */
	public void setY(int y) {
		this.y = y;
	}
	/**
	 * @return the z
	 */
	public int getZ() {
		return z;
	}
	/**
	 * @param z the z to set
	 */
	public void setZ(int z) {
		this.z = z;
	}

	/**
	 * @return the isSet
	 */
	public boolean isSet() {
		return isSet;
	}
	
	
	
	
}

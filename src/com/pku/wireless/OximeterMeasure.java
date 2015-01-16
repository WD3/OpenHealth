package com.pku.wireless;

import java.util.Date;
import java.util.List;

import es.libresoft.mdnf.SFloatType;

public class OximeterMeasure {
	private int oximeter;
	private Date date;
	private String unit;
	private String type;
	
	public void setOximeter(Object obj){
		this.oximeter = (int)(((SFloatType)obj).doubleValueRepresentation());
	}
	public int getOximeter(){
		return this.oximeter;
	}
	public void setType(String type){
		this.type = type;
	}
	public String getType(){
		return this.type;
	}
	public void setUint(String unit){
		this.unit = unit;
	}
	public String getUnit(){
		return this.unit;
	}

}

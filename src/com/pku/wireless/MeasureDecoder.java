package com.pku.wireless;

import ieee_11073.part_10101.Nomenclature;
import ieee_11073.part_20601.asn1.AbsoluteTime;
import ieee_11073.part_20601.asn1.OID_Type;
import ieee_11073.part_20601.asn1.TYPE;
import ieee_11073.part_20601.phd.dim.Attribute;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cmdTester.ShellMeasure;

import Config.BloodPressureAgent;
import es.libresoft.openhealth.events.MeasureReporter;
import es.libresoft.openhealth.logging.Logging;

public class MeasureDecoder{
	private List<Object> measures;
	private List<Object> attributes;
	private Iterator<Object> ims;
	private Iterator<Object> iat;
	private BloodpressureMeasure bloodPressureMeasure;
	private PulseMeasure pulseMeasure;
	private OximeterMeasure oximeterMeasure;
	static MeasureListener mListener;
	private MeasureReporter mr;
	
	private boolean bloodPressureFlag;
	private boolean pulseFlag;
	private boolean oximeterFlag;
	
	private MeasureDecoder(){};
	private static class MeasureDecoderHolder{
		static final MeasureDecoder measureDecoder = new MeasureDecoder();
	}

	public static MeasureDecoder getInstance(){		
		return MeasureDecoderHolder.measureDecoder;
	}
	public void setMeasureReporter(MeasureReporter mr){
		this.mr = mr;
	}
	public void init(){
		if(mr == null)	return;
		measures = mr.getMeasures();
		ims = measures.iterator();
		attributes = mr.getAttributes();
		iat = attributes.iterator();
	}
	public static void setMeasureListener(MeasureListener mListener) {
		MeasureDecoder.mListener = mListener;
	}

	public void decodeMeasures() {
		if (!attributes.isEmpty()) {
			Logging.debug("测量的属性为: ");
			while (iat.hasNext()) {
				Attribute attrib = (Attribute) iat.next();
				decodeAttribute(attrib);
			}		
			if(bloodPressureFlag){
				mListener.getBloodMeasure(new MeasureEvent(this,bloodPressureMeasure));
				bloodPressureFlag = false;
			}
			else if(pulseFlag){
				mListener.getPulseMeasure(new MeasureEvent(this,pulseMeasure));
				pulseFlag = false;
			}else if(oximeterFlag){
				mListener.getOximeterMeasure(new MeasureEvent(this,oximeterMeasure));
				oximeterFlag = false;
			}
		}
	}
	

	public void decodeAttribute(Attribute attrib) {
		switch(attrib.getAttributeID()){
		case Nomenclature.MDC_ATTR_ID_TYPE:
			decodeType((TYPE)attrib.getAttributeType());
			break;
		case Nomenclature.MDC_ATTR_UNIT_CODE:
			decodeUnitCode((OID_Type)attrib.getAttributeType());			
			break;
		case Nomenclature.MDC_ATTR_ID_HANDLE:
			break;
		case Nomenclature.MDC_ATTR_ID_PHYSIO_LIST:
			break;
		default:
			break;
		}
	}

	public void decodeMeasure(ShellMeasure measure) {
		switch (measure.getType()){
		case Nomenclature.MDC_ATTR_NU_CMPD_VAL_OBS_BASIC:
			if(bloodPressureFlag){
				bloodPressureMeasure.setHPressure(measure.getData());
				bloodPressureMeasure.setLPressure(measure.getData());
				bloodPressureMeasure.setAPressure(measure.getData());
			}				
			Logging.debug("数值:"+measure.getData());
			break;
		case Nomenclature.MDC_ATTR_TIME_STAMP_ABS:
			if(bloodPressureFlag)
				bloodPressureMeasure.setDate(decodeAbsoluteTime((AbsoluteTime)measure.getData()));				
			Logging.debug("时间:"+decodeAbsoluteTime((AbsoluteTime)measure.getData()).toString());
			break;
		case Nomenclature.MDC_ATTR_NU_VAL_OBS_BASIC:
			if(oximeterFlag){
				oximeterMeasure.setOximeter(measure.getData());
			}
			Logging.debug("数值:"+measure.getData());
			break;
		default:
			break;
		}
	}

	public void decodeUnitCode(OID_Type unit) {
		switch (unit.getValue().getValue()) {
		case Nomenclature.MDC_DIM_MMHG:
			if(bloodPressureFlag)
				bloodPressureMeasure.setUint("mmHg");
			break;
		case Nomenclature.MDC_DIM_BEAT_PER_MIN:
			if(pulseFlag)
				pulseMeasure.setUint("搏/分");
			break;
		case Nomenclature.MDC_DIM_PERCENT:
			if(oximeterFlag)
				oximeterMeasure.setUint("%");
			break;
		default:
			Logging.debug("默认单位:" + unit.getValue().getValue());
			break;
		}
	}

	public void decodeType(TYPE type) {
		switch (type.getCode().getValue().getValue()) {
			case Nomenclature.MDC_PRESS_BLD_NONINV:
				System.out.println("Nonivasive blood pressure");
				bloodPressureMeasure = new BloodpressureMeasure();
				bloodPressureFlag = true;
				bloodPressureMeasure.setType("Nonivasive blood pressure");
				while (ims.hasNext()) {
					ShellMeasure measure = (ShellMeasure) ims.next();
					decodeMeasure(measure);
				}
				break;
			case Nomenclature.MDC_PULS_RATE_NON_INV:
				System.out.println("Pulse");
				pulseMeasure = new PulseMeasure();
				pulseFlag = true;
				while (ims.hasNext()) {
					ShellMeasure measure = (ShellMeasure) ims.next();
					decodeMeasure(measure);
				}
				break;
			case Nomenclature.MDC_PULS_OXIM_SAT_O2:
				System.out.println("Oximeter");
				oximeterMeasure = new OximeterMeasure();
				oximeterFlag = true;
				oximeterMeasure.setType("Oximeter");
				while (ims.hasNext()) {
					ShellMeasure measure = (ShellMeasure) ims.next();
					decodeMeasure(measure);
				}
				break;
			default:
				Logging.debug("默认类型:" + type.getCode().getValue().getValue());
				break;
		}
	}

	public Date decodeAbsoluteTime(AbsoluteTime time) {
		int century = time.getCentury().getValue();
		int year = time.getYear().getValue();
		int month = time.getMonth().getValue();
		int day = time.getDay().getValue();
		int hour = time.getHour().getValue();
		int min = time.getMinute().getValue();
		int second = time.getSecond().getValue();
//		String timeString = Integer.toHexString(century)+
//				Integer.toHexString(year)+"-"+
//				Integer.toHexString(month)+"-"+
//				Integer.toHexString(day)+" "+
//				Integer.toHexString(hour)+":"+
//				Integer.toHexString(min)+":"+
//				Integer.toHexString(second);
		String timeString = dateUnitString(century)+
				dateUnitString(year)+"-"+
				dateUnitString(month)+"-"+
				dateUnitString(day)+" "+
				dateUnitString(hour)+":"+
				dateUnitString(min)+":"+
				dateUnitString(second);
		SimpleDateFormat formatDate = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		Date datetime = null;
		try {
			datetime = formatDate.parse(timeString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return datetime;
	}
	private static String dateUnitString(int val){
		if(val<10)
			return "0"+val;
		else return ""+val;
	}
}

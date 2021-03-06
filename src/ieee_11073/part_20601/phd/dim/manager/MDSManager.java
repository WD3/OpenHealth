/*
Copyright (C) 2008-2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Jose Antonio Santos Cadenas <jcaden@libresoft.es>
Author: Santiago Carot-Nemesio <scarot@libresoft.es>

This program is a (FLOS) free libre and open source implementation
of a multiplatform manager device written in java according to the
ISO/IEEE 11073-20601. Manager application is designed to work in
DalvikVM over android platform.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/


package ieee_11073.part_20601.phd.dim.manager;

import ieee_11073.part_10101.Nomenclature;
import ieee_11073.part_20601.asn1.AVA_Type;
import ieee_11073.part_20601.asn1.AbsoluteTime;
import ieee_11073.part_20601.asn1.ApduType;
import ieee_11073.part_20601.asn1.AttrValMap;
import ieee_11073.part_20601.asn1.AttrValMapEntry;
import ieee_11073.part_20601.asn1.BaseOffsetTime;
import ieee_11073.part_20601.asn1.BasicNuObsValue;
import ieee_11073.part_20601.asn1.ConfigId;
import ieee_11073.part_20601.asn1.ConfigObject;
import ieee_11073.part_20601.asn1.ConfigReport;
import ieee_11073.part_20601.asn1.ConfigReportRsp;
import ieee_11073.part_20601.asn1.ConfigResult;
import ieee_11073.part_20601.asn1.DataApdu;
import ieee_11073.part_20601.asn1.FLOAT_Type;
import ieee_11073.part_20601.asn1.GetResultSimple;
import ieee_11073.part_20601.asn1.HANDLE;
import ieee_11073.part_20601.asn1.INT_U16;
import ieee_11073.part_20601.asn1.INT_U32;
import ieee_11073.part_20601.asn1.InvokeIDType;
import ieee_11073.part_20601.asn1.MdsTimeInfo;
import ieee_11073.part_20601.asn1.MetricSpecSmall;
import ieee_11073.part_20601.asn1.OID_Type;
import ieee_11073.part_20601.asn1.ObservationScan;
import ieee_11073.part_20601.asn1.ObservationScanFixed;
import ieee_11073.part_20601.asn1.PersonId;
import ieee_11073.part_20601.asn1.ProdSpecEntry;
import ieee_11073.part_20601.asn1.ProductionSpec;
import ieee_11073.part_20601.asn1.RegCertData;
import ieee_11073.part_20601.asn1.RegCertDataList;
import ieee_11073.part_20601.asn1.ScanReportInfoFixed;
import ieee_11073.part_20601.asn1.ScanReportInfoMPFixed;
import ieee_11073.part_20601.asn1.ScanReportInfoMPVar;
import ieee_11073.part_20601.asn1.ScanReportInfoVar;
import ieee_11073.part_20601.asn1.ScanReportPerFixed;
import ieee_11073.part_20601.asn1.ScanReportPerVar;
import ieee_11073.part_20601.asn1.SetTimeInvoke;
import ieee_11073.part_20601.asn1.SystemModel;
import ieee_11073.part_20601.asn1.TYPE;
import ieee_11073.part_20601.asn1.TypeVer;
import ieee_11073.part_20601.asn1.TypeVerList;
import ieee_11073.part_20601.phd.dim.Attribute;
import ieee_11073.part_20601.phd.dim.DIM;
import ieee_11073.part_20601.phd.dim.DimTimeOut;
import ieee_11073.part_20601.phd.dim.Enumeration;
import ieee_11073.part_20601.phd.dim.InvalidAttributeException;
import ieee_11073.part_20601.phd.dim.MDS;
import ieee_11073.part_20601.phd.dim.Numeric;
import ieee_11073.part_20601.phd.dim.RT_SA;
import ieee_11073.part_20601.phd.dim.TimeOut;

import java.util.Calendar;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import Config.ManagerConfig;
import es.libresoft.mdnf.SFloatType;
import es.libresoft.openhealth.Agent;
import es.libresoft.openhealth.Device;
import es.libresoft.openhealth.error.ErrorCodes;
import es.libresoft.openhealth.events.Event;
import es.libresoft.openhealth.events.EventType;
import es.libresoft.openhealth.events.InternalEventReporter;
import es.libresoft.openhealth.events.MeasureReporter;
import es.libresoft.openhealth.events.MeasureReporterFactory;
import es.libresoft.openhealth.events.MeasureReporterUtils;
import es.libresoft.openhealth.events.application.ExternalEvent;
import es.libresoft.openhealth.logging.Logging;
import es.libresoft.openhealth.messages.MessageFactory;
import es.libresoft.openhealth.utils.ASN1_Tools;
import es.libresoft.openhealth.utils.ASN1_Values;
import es.libresoft.openhealth.utils.DIM_Tools;
import es.libresoft.openhealth.utils.RawDataExtractor;

public class MDSManager extends MDS {

	private boolean delayConfRsp = false;
	ApduType cfgrsp;
	/**
	 * Used only in extended configuration when the agent configuration is unknown
	 */
	public MDSManager(Device device) {
		super(device);
	}

	public MDSManager (byte[] system_id, ConfigId devConfig_id){
		super(system_id,devConfig_id);
	}

	public MDSManager(Hashtable<Integer, Attribute> attributeList)
		throws InvalidAttributeException {
		super(attributeList);
	}

	public void lockConfRsp() {
		this.delayConfRsp = true;
	}

	public void delayConfigRsp(ApduType cfgrsp) {
		this.cfgrsp = cfgrsp;
	}

	public boolean isLockConfRsp() {
		return this.delayConfRsp;
	}

	public void configureMDS(Collection<ConfigObject> config) throws InvalidAttributeException {
		Iterator<ConfigObject> i = config.iterator();
		ConfigObject confObj;

		while (i.hasNext()){
			confObj = i.next();
			Hashtable<Integer,Attribute> attribs;
			//Get Attributes
			try {
				attribs = getAttributes(confObj.getAttributes(), getDeviceConf().getEncondigRules());
			} catch (Exception e) {
				e.printStackTrace();
				throw new InvalidAttributeException(e);
			}

			//Generate attribute Handle:
			HANDLE handle = new HANDLE();
			handle.setValue(new INT_U16(new Integer
					(confObj.getObj_handle().getValue().getValue())));
			Attribute attr = new Attribute(Nomenclature.MDC_ATTR_ID_HANDLE,
					handle);
			//Set Attribute Handle to the list
			attribs.put(Nomenclature.MDC_ATTR_ID_HANDLE, attr);

			checkGotAttributes(attribs);
			int classId = confObj.getObj_class().getValue().getValue();
			switch (classId) {
			case Nomenclature.MDC_MOC_VMS_MDS_SIMP : // MDS Class
				addCheckedAttributes(attribs);
				break;
			case Nomenclature.MDC_MOC_VMO_METRIC : // Metric Class
				throw new UnsupportedOperationException("Unsoportedd Metric Class");
			case Nomenclature.MDC_MOC_VMO_METRIC_NU : // Numeric Class
				addNumeric(new Numeric(attribs));
				break;
			case Nomenclature.MDC_MOC_VMO_METRIC_SA_RT: // RT-SA Class
				addRT_SA(new RT_SA(attribs));
				break;
				//throw new UnsupportedOperationException("Unsoportedd RT-SA Class");
			case Nomenclature.MDC_MOC_VMO_METRIC_ENUM: // Enumeration Class
				addEnumeration(new Enumeration(attribs));
				break;
			case Nomenclature.MDC_MOC_VMO_PMSTORE: // PM-Store Class
				// To certificate, comment this line:
				//addPM_Store(new MPM_Store(attribs));
				throw new UnsupportedOperationException("Unsupported PM-Store Class");
			case Nomenclature.MDC_MOC_PM_SEGMENT: // PM-Segment Class
				throw new UnsupportedOperationException("Unsoportedd PM-Segment Class");
			case Nomenclature.MDC_MOC_SCAN: // Scan Class
				throw new UnsupportedOperationException("Unsoportedd Scan Class");
			case Nomenclature.MDC_MOC_SCAN_CFG: // CfgScanner Class
				throw new UnsupportedOperationException("Unsoportedd CfgScanner Class");
			case Nomenclature.MDC_MOC_SCAN_CFG_EPI: // EpiCfgScanner Class
				// To certificate, comment this line:
				//addScanner(new MEpiCfgScanner(attribs));
				throw new UnsupportedOperationException("Unsupported EpicCfgScanner Class");
			case Nomenclature.MDC_MOC_SCAN_CFG_PERI: // PeriCfgScanner Class
				throw new UnsupportedOperationException("Unsoportedd PeriCfgScanner Class");
			}
		}
	}

	private boolean is_supported(int conf_id) {

		for (int i = 0; i < ManagerConfig.supported_spec.length; i++)
			if (ManagerConfig.supported_spec[i] == conf_id)
				return true;

		if ((conf_id >= ASN1_Values.CONF_ID_EXTENDED_CONFIG_START ) &&
				(conf_id <= ASN1_Values.CONF_ID_EXTENDED_CONFIG_END))
			return true;

		return false;
	}

	@Override
	public ConfigReportRsp MDS_Configuration_Event(ConfigReport config) {
		int configId = config.getConfig_report_id().getValue();

		try {
			if (!is_supported(configId))
				throw new Exception("Unsuppoted specialization: " + configId);

			configureMDS(config.getConfig_obj_list().getValue());
			/* Store current configuration */
			storeConfiguration();
			return generateConfigReportRsp(configId,
					ASN1_Values.CONF_RESULT_ACCEPTED_CONFIG);
		} catch (Exception e) {
			e.printStackTrace();
			clearObjectsFromMds();
			return  generateConfigReportRsp(configId,
						ASN1_Values.CONF_RESULT_UNSUPPORTED_CONFIG);
		}

	}

	@Override
	public void MDS_Dynamic_Data_Update_Fixed(ScanReportInfoFixed info) {

		try{
			String system_id = DIM_Tools.byteArrayToString(
					(byte[])getAttribute(Nomenclature.MDC_ATTR_SYS_ID).getAttributeType());

			Iterator<ObservationScanFixed> i= info.getObs_scan_fixed().iterator();
			ObservationScanFixed obs;
			while (i.hasNext()) {
				obs=i.next();

				//Get DIM from Handle_id
				DIM elem = getObject(obs.getObj_handle());
				if (elem == null) {
					Logging.error("MDS_Dynamic_Data_Update_Fixed: Not found DIM for handle " + obs.getObj_handle().getValue().getValue());
					continue;
				}

				AttrValMap avm = (AttrValMap)elem.getAttribute(Nomenclature.MDC_ATTR_ATTRIBUTE_VAL_MAP).getAttributeType();
				Iterator<AttrValMapEntry> it = avm.getValue().iterator();
				RawDataExtractor de = new RawDataExtractor(obs.getObs_val_data());
				MeasureReporter mr = MeasureReporterFactory.getDefaultMeasureReporter();
				MeasureReporterUtils.addAttributesToReport(mr, elem);
				while (it.hasNext()){
					AttrValMapEntry attr = it.next();
					int attrId = attr.getAttribute_id().getValue().getValue();
					int length = attr.getAttribute_len();
					byte[] attrValue = de.getData(length);
					try {
						//mr.addMeasure(attrId, RawDataExtractor.decodeRawData(attrId,de.getData(length), this.getDeviceConf().getEncondigRules()));
						Object data = RawDataExtractor.decodeRawData(attrId, attrValue, this.getDeviceConf().getEncondigRules());
						if (!RawDataExtractor.updateAttrValue(elem, attrId, data))
							mr.addMeasure(attrId, RawDataExtractor.decodeMeasure(attrId, attrValue, this.getDeviceConf().getEncondigRules()));
					}catch(Exception e){
						Logging.error("Error: Can not get attribute " + attrId);
						e.printStackTrace();
					}
				}
				InternalEventReporter.receivedMeasure((Agent) device, mr);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void MDS_Dynamic_Data_Update_Var(ScanReportInfoVar info) {
		try{
			String system_id = DIM_Tools.byteArrayToString(
					(byte[])getAttribute(Nomenclature.MDC_ATTR_SYS_ID).getAttributeType());

			Iterator<ObservationScan> i= info.getObs_scan_var().iterator();
			ObservationScan obs;
			MeasureReporter mr = MeasureReporterFactory.getDefaultMeasureReporter();

			while (i.hasNext()) {
				obs=i.next();
				//Get Numeric from Handle_id
				Numeric numeric = getNumeric(obs.getObj_handle());
				MeasureReporterUtils.addAttributesToReport(mr,numeric);
				if (numeric == null)
					throw new Exception("Numeric class not found for handle: " + obs.getObj_handle().getValue().getValue());

				Iterator<AVA_Type> it = obs.getAttributes().getValue().iterator();
				while (it.hasNext()){
					AVA_Type att = it.next();
					Integer att_id = att.getAttribute_id().getValue().getValue();
					byte[] att_value = att.getAttribute_value();
					Object data = RawDataExtractor.decodeRawData(att_id, att_value, this.getDeviceConf().getEncondigRules());
					if (!RawDataExtractor.updateAttrValue(numeric, att_id, data))
						mr.addMeasure(att_id, RawDataExtractor.decodeMeasure(att_id, att_value, this.getDeviceConf().getEncondigRules()));
				}
				InternalEventReporter.receivedMeasure((Agent) device, mr);
			}
		}catch (Exception e){
			e.printStackTrace();
		}

	}

	//----------------------------------------PRIVATE-----------------------------------------------------------
	private void checkGotAttributes(Hashtable<Integer,Attribute> attribs){
		Iterator<Integer> i = attribs.keySet().iterator();
		while (i.hasNext()){
			int id = i.next();
			attribs.get(id);
			Logging.debug("Checking attribute: " + DIM_Tools.getAttributeName(id) + " " + id);
			Attribute attr = attribs.get(id);
			switch (id){
			case Nomenclature.MDC_ATTR_ID_TYPE :
				TYPE t = (TYPE) attribs.get(new Integer(id)).getAttributeType();
				Logging.debug("partition: " + t.getPartition().getValue());
				Logging.debug("code: " + t.getCode().getValue().getValue());
				Logging.debug("ok.");
				break;
			case Nomenclature.MDC_ATTR_TIME_ABS:
			case Nomenclature.MDC_ATTR_TIME_STAMP_ABS :
				AbsoluteTime time = (AbsoluteTime) attr.getAttributeType();
				Logging.debug("century: " + Integer.toHexString(time.getCentury().getValue()));
				Logging.debug("year: " + Integer.toHexString(time.getYear().getValue()));
				Logging.debug("month: " + Integer.toHexString(time.getMonth().getValue()));
				Logging.debug("day: "+ Integer.toHexString(time.getDay().getValue()));
				Logging.debug("hour: " + Integer.toHexString(time.getHour().getValue()));
				Logging.debug("minute: " + Integer.toHexString(time.getMinute().getValue()));
				Logging.debug("second: " + Integer.toHexString(time.getSecond().getValue()));
				Logging.debug("sec-fraction: " + Integer.toHexString(time.getSec_fractions().getValue()));
				break;
			case Nomenclature.MDC_ATTR_UNIT_CODE:
				OID_Type oid = (OID_Type)attribs.get(new Integer(id)).getAttributeType();
				Logging.debug("oid: " + oid.getValue().getValue());
				Logging.debug("ok.");
				break;
			case Nomenclature.MDC_ATTR_METRIC_SPEC_SMALL:
				MetricSpecSmall mss = (MetricSpecSmall)attribs.get(new Integer(id)).getAttributeType();
				//Logging.debug("partition: " + getHexString(mss.getValue().getValue()));
				Logging.debug("ok.");
				break;
			case Nomenclature.MDC_ATTR_NU_VAL_OBS_BASIC :
				BasicNuObsValue val = (BasicNuObsValue)attribs.get(new Integer(id)).getAttributeType();
				try {
						SFloatType sf = new SFloatType(val.getValue().getValue());
						Logging.debug("BasicNuObsValue: " + sf.doubleValueRepresentation());
					} catch (Exception e) {
						e.printStackTrace();
					}
				Logging.debug("ok.");
				break;
			case Nomenclature.MDC_ATTR_ATTRIBUTE_VAL_MAP:
				AttrValMap avm = (AttrValMap)attribs.get(new Integer(id)).getAttributeType();
				Iterator<AttrValMapEntry> iter = avm.getValue().iterator();
				while (iter.hasNext()){
					AttrValMapEntry entry = iter.next();
					Logging.debug("--");
					Logging.debug("attrib-id: " + entry.getAttribute_id().getValue().getValue());
					Logging.debug("attrib-len: " + entry.getAttribute_len());
				}
				Logging.debug("ok.");
				break;
			case Nomenclature.MDC_ATTR_SYS_TYPE_SPEC_LIST:
				TypeVerList sysTypes = (TypeVerList) attr.getAttributeType();
				Iterator<TypeVer> it = sysTypes.getValue().iterator();
				Logging.debug("Spec. list values:");
				while (it.hasNext()) {
					Logging.debug("\t" + it.next().getType().getValue().getValue());
				}
				break;
			case Nomenclature.MDC_ATTR_DEV_CONFIG_ID:
				ConfigId configId = (ConfigId) attr.getAttributeType();
				Logging.debug("Dev config id: " + configId.getValue());
				break;
			case Nomenclature.MDC_ATTR_SYS_ID:
				byte[] octet = (byte[]) attr.getAttributeType();
				String sysId = new String(octet);
				Logging.debug("Sys id: " + sysId);
				break;
			case Nomenclature.MDC_ATTR_ID_MODEL:
				SystemModel systemModel = (SystemModel) attr.getAttributeType();
				Logging.debug("System manufactures: " + new String(systemModel.getManufacturer()));
				Logging.debug("System model number: " + new String(systemModel.getModel_number()));
				break;
			case Nomenclature.MDC_ATTR_ID_HANDLE:
				HANDLE handle = (HANDLE) attr.getAttributeType();
				Logging.debug("Id handle: " + handle.getValue().getValue());
				break;
			case Nomenclature.MDC_ATTR_REG_CERT_DATA_LIST:
				Logging.debug("Reg cert. data list: ");
				RegCertDataList regList = (RegCertDataList) attr.getAttributeType();
				Iterator<RegCertData> regIt = regList.getValue().iterator();
				while (regIt.hasNext()) {
					RegCertData cert = regIt.next();
					Logging.debug("\t" + cert.getAuth_body_and_struc_type().getAuth_body().getValue() +
								" " + cert.getAuth_body_and_struc_type().getAuth_body_struc_type().getValue());
				}
				break;
			case Nomenclature.MDC_ATTR_MDS_TIME_INFO:
				Logging.debug("Mds time information:");
				MdsTimeInfo timeInfo = (MdsTimeInfo) attr.getAttributeType();
				byte[] capabilities = timeInfo.getMds_time_cap_state().getValue().getValue();

				for (int i1 = 0; i1 < capabilities.length; i1++) {
					String binary = Integer.toBinaryString(capabilities[i1]);
					if (binary.length() > 8)
						binary = binary.substring(binary.length() - 8, binary.length());
					Logging.debug("\t" +binary);	
				}

				Logging.debug("\t" + timeInfo.getTime_sync_protocol().getValue().getValue().getValue());
				Logging.debug("\t" + timeInfo.getTime_sync_accuracy().getValue().getValue());
				Logging.debug("\t" + timeInfo.getTime_resolution_abs_time());
				Logging.debug("\t" + timeInfo.getTime_resolution_rel_time());
				Logging.debug("\t" + timeInfo.getTime_resolution_high_res_time().getValue());
				break;
			case Nomenclature.MDC_ATTR_ID_PROD_SPECN:
				Logging.debug("Production specification:");
				ProductionSpec ps = (ProductionSpec) attr.getAttributeType();
				Iterator<ProdSpecEntry> itps = ps.getValue().iterator();
				while (itps.hasNext()) {
					ProdSpecEntry pse = itps.next();
					Logging.debug("\tSpec type: " + pse.getSpec_type());
					Logging.debug("\tComponent id: " + pse.getComponent_id().getValue().getValue());
					Logging.debug("\tProd spec: " + new String(pse.getProd_spec()));
				}
				break;
			case Nomenclature.MDC_ATTR_TIME_BO:
				BaseOffsetTime boTime = (BaseOffsetTime) attr.getAttributeType();
				Logging.debug("BaseOffsetTime: " + boTime.getBo_time_offset().getValue());
				break;
			default:
				Logging.debug(">>>>>>>Id " + id + " not implemented yet");
				break;
			}
		}
	}

	/**
	 * Generate a response for configuration
	 * @param result Reponse configuration
	 * @return
	 */
	private ConfigReportRsp generateConfigReportRsp (int report_id, int result) {
		ConfigReportRsp configRsp = new ConfigReportRsp();
		ConfigId confId = new ConfigId (new Integer(report_id));
		ConfigResult confResult = new ConfigResult(new Integer(result));
		configRsp.setConfig_report_id(confId);
		configRsp.setConfig_result(confResult);
		return configRsp;
	}

	public void GET (Event event) {
		HANDLE handle = new HANDLE();
		handle.setValue(new INT_U16(0));
		DataApdu data = MessageFactory.PrstRoivCmpGet(this, handle);
		ApduType apdu = MessageFactory.composeApdu(data, getDeviceConf());

		try{
			InvokeIDType invokeId = data.getInvoke_id();
			getStateHandler().send(apdu);
			DimTimeOut to = new DimTimeOut(TimeOut.MDS_TO_GET, invokeId.getValue(), getStateHandler()) {

				@Override
				public void procResponse(DataApdu data) {
					Logging.debug("Received response for get MDS");
					ExternalEvent<Boolean, Object> event = null;
					try {
						event = (ExternalEvent<Boolean, Object>) this.getEvent();
					} catch (ClassCastException e) {

					}

					if (!data.getMessage().isRors_cmip_getSelected()) {
						//TODO: Unexpected response format
						Logging.debug("Unexpected response format");
						if (event != null)
							event.processed(new Boolean(false), ErrorCodes.UNEXPECTED_ERROR);
						return;
					}

					GetResultSimple grs = data.getMessage().getRors_cmip_get();

					if (grs.getObj_handle().getValue().getValue() != 0) {
						//TODO: Unexpected object handle, should be reserved value 0
						Logging.debug("Unexpected object handle, should be reserved value 0");
						if (event != null)
							event.processed(new Boolean(false), ErrorCodes.UNEXPECTED_ERROR);
						return;
					}

					try {
						Hashtable<Integer, Attribute> attribs;
						attribs = getAttributes(grs.getAttribute_list(), getDeviceConf().getEncondigRules());
						checkGotAttributes(attribs);
						addCheckedAttributes(attribs);

						/* Store received configuration */
						byte[] sys_id = (byte[]) getAttribute(Nomenclature.MDC_ATTR_SYS_ID).getAttributeType();
						storeConfiguration(sys_id, getDeviceConf());
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (event != null)
						event.processed(new Boolean(true), ErrorCodes.NO_ERROR);

					//When mds-time-mgr-set-time-bit is set is needed Set_Time
					//chapter 8.12.2.1 of 11073-20601a-2010
					Agent a = (Agent) getDevice();
					a.sendEvent(new Event(EventType.REQ_SET_TIME));
				}

				@Override
				protected void expiredTimeout(){
					super.expiredTimeout();
					ExternalEvent<Boolean, Object> event;
					try {
						event = (ExternalEvent<Boolean, Object>) this.getEvent();
					} catch (ClassCastException e) {
						return;
					}

					if (event == null)
						return;

					event.processed(new Boolean(false), ErrorCodes.TIMEOUT_MDS_GET);
				}
			};
			to.setEvent(event);
			to.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void MDS_DATA_REQUEST() {
		Logging.debug("TODO: Implement MDS_DATA_REQUEST");
	}

	private AbsoluteTime getAbsTime() {
		AbsoluteTime t = new AbsoluteTime();
		Calendar c = Calendar.getInstance();

		t.setCentury(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD( (byte)(c.get(Calendar.YEAR)/100) )));
		t.setYear(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD( (byte)(c.get(Calendar.YEAR)%100) )));
		t.setMonth(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD( (byte)(c.get(Calendar.MONTH)+1) )));
		t.setDay(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD(( byte)c.get(Calendar.DAY_OF_MONTH) )));
		t.setHour(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD( (byte)c.get(Calendar.HOUR_OF_DAY) )));
		t.setMinute(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD( (byte)c.get(Calendar.MINUTE) )));
		t.setSecond(ASN1_Tools.toIntU8(ASN1_Tools.byteToBCD( (byte)c.get(Calendar.SECOND) )));
		t.setSec_fractions(ASN1_Tools.toIntU8( (byte)0) );

		return t;
	}

	private void send_pending_config_rsp() {
		if (delayConfRsp) {
			if (cfgrsp != null) {
				getStateHandler().send(cfgrsp);
				cfgrsp = null;
			}
			delayConfRsp = false;
		}
	}

	@Override
	public void Set_Time(Event event) {
		//Check needed capabilities
		Attribute attr = getAttribute(Nomenclature.MDC_ATTR_MDS_TIME_INFO);
		if (attr == null) {
			Logging.debug("Set_Time: Request of setTime in agent without attr MDC_ATTR_MDS_TIME_INFO");
			send_pending_config_rsp();
			return;
		}
		MdsTimeInfo timeInfo = (MdsTimeInfo)attr.getAttributeType();
		byte[] timeCap = timeInfo.getMds_time_cap_state().getValue().getValue();

		if (ASN1_Tools.isSetBit(timeCap, ASN1_Values.mds_time_capab_set_clock) != 1 ||
			ASN1_Tools.isSetBit(timeCap, ASN1_Values.mds_time_mgr_set_time) != 1
			) {
			Logging.debug("Set_Time: Request of setTime in agent that not support it in MDC_ATTR_MDS_TIME_INFO[" +  timeCap + "]");
			send_pending_config_rsp();
			return;
		}

		//Compose Apdu
		SetTimeInvoke timeInv = new SetTimeInvoke();

		INT_U32 cero = new INT_U32();
		FLOAT_Type accuracy = new FLOAT_Type();
		cero.setValue((long) 0);
		accuracy.setValue(cero);

		timeInv.setDate_time(getAbsTime());
		timeInv.setAccuracy(accuracy);
		DataApdu data = MessageFactory.PrstRoivCmipConfirmedAction(this, timeInv);
		if (data == null) {
			Logging.error("Set_Time: Error creating DataApdu for setTime, is null");
			send_pending_config_rsp();
			return;
		}

		ApduType apdu = MessageFactory.composeApdu(data, getDeviceConf());

		try {
			InvokeIDType invokeId = data.getInvoke_id();
			getStateHandler().send(apdu);
			send_pending_config_rsp();

			DimTimeOut to = new DimTimeOut(TimeOut.MDS_TO_CA, invokeId.getValue(), getStateHandler()) {

				@Override
				public void procResponse(DataApdu data) {
					Logging.debug("Set_Time: Received response for setTime on MDS");
					ExternalEvent<Boolean, Object> event = null;
					try {
						event = (ExternalEvent<Boolean, Object>) this.getEvent();
					} catch (ClassCastException e) {
					}

					if (!data.getMessage().isRors_cmip_confirmed_actionSelected()) {
						//TODO: Unexpected response format
						Logging.debug("Set_Time: Unexpected response format");
						if (event != null)
							event.processed(new Boolean(false), ErrorCodes.UNEXPECTED_ERROR);
							return;
					}

					//TODO: Check the content of response

					if (event != null)
						event.processed(new Boolean(true), ErrorCodes.NO_ERROR);
				}

				@Override
				protected void expiredTimeout(){
					super.expiredTimeout();
					ExternalEvent<Boolean, Object> event;
					try {
						event = (ExternalEvent<Boolean, Object>) this.getEvent();
					} catch (ClassCastException e) {
						return;
					}

					if (event == null)
						return;

					event.processed(new Boolean(false), ErrorCodes.TIMEOUT_MDS_CONF_ACION);
				}
			};
			to.setEvent(event);
			to.start();

		} catch (Exception e) {
			e.printStackTrace();
			send_pending_config_rsp();
		}
	}

	@Override
	public void MDS_Dynamic_Data_Update_MP_Fixed(ScanReportInfoMPFixed info) {
		Logging.debug("MDS_Dynamic_Data_Update_MP_Fixed");
		try {

			String system_id = DIM_Tools.byteArrayToString(
					(byte[])getAttribute(Nomenclature.MDC_ATTR_SYS_ID).getAttributeType());

			Iterator<ScanReportPerFixed> i = info.getScan_per_fixed().iterator();
			ScanReportPerFixed srpf;

			while (i.hasNext()) {
				srpf = i.next();

				PersonId pi = srpf.getPerson_id();
				Logging.debug("PersonId: " + pi.getValue());

				Iterator<ObservationScanFixed> i_o = srpf.getObs_scan_fixed().iterator();
				ObservationScanFixed obs;

				while (i_o.hasNext()){
					obs = i_o.next();

					//Get DIM from Handle_id
					DIM elem = getObject(obs.getObj_handle());

					AttrValMap avm = (AttrValMap)elem.getAttribute(Nomenclature.MDC_ATTR_ATTRIBUTE_VAL_MAP).getAttributeType();
					Iterator<AttrValMapEntry> it = avm.getValue().iterator();
					RawDataExtractor de = new RawDataExtractor(obs.getObs_val_data());
					MeasureReporter mr = MeasureReporterFactory.getDefaultMeasureReporter();
					MeasureReporterUtils.addAttributesToReport(mr, elem);

					Attribute at = new Attribute(Nomenclature.MDC_ATTR_PM_SEG_PERSON_ID, pi);
					mr.set_attribute(at);
					while (it.hasNext()){
						AttrValMapEntry attr = it.next();
						int attrId = attr.getAttribute_id().getValue().getValue();
						int length = attr.getAttribute_len();
						byte[] attrValue = de.getData(length);
						try {
							//mr.addMeasure(attrId, RawDataExtractor.decodeRawData(attrId, de.getData(length), this.getDeviceConf().getEncondigRules()));
							Object data = RawDataExtractor.decodeRawData(attrId, attrValue, this.getDeviceConf().getEncondigRules());
							if (!RawDataExtractor.updateAttrValue(elem, attrId, data))
								mr.addMeasure(attrId, RawDataExtractor.decodeMeasure(attrId, attrValue, this.getDeviceConf().getEncondigRules()));
						}catch(Exception e){
							Logging.error("Error: Cannot get attribute " + attrId);
							e.printStackTrace();
						}
					}

					InternalEventReporter.receivedMeasure((Agent) device, mr);
					mr.clearMeasures();
					mr.clearAttributes();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	@Override
	public void MDS_Dynamic_Data_Update_MP_Var(ScanReportInfoMPVar info) {
		Logging.debug("MDS_Dynamic_Data_Update_MP_Var");
		try{
			String system_id = DIM_Tools.byteArrayToString(
					(byte[])getAttribute(Nomenclature.MDC_ATTR_SYS_ID).getAttributeType());

			Iterator<ScanReportPerVar> i = info.getScan_per_var().iterator();
			ScanReportPerVar srpv;

			while  (i.hasNext())
			{
				srpv = i.next();
				PersonId pi = srpv.getPerson_id();
				Logging.debug("PersonId: " + pi.getValue());

				Iterator<ObservationScan> i_o = srpv.getObs_scan_var().iterator();
				ObservationScan obs;

				MeasureReporter mr = MeasureReporterFactory.getDefaultMeasureReporter();

				while (i_o.hasNext()) {
					obs = i_o.next();
					//Get Numeric from Handle_id
					Numeric numeric = getNumeric(obs.getObj_handle());

					MeasureReporterUtils.addAttributesToReport(mr,numeric);

					Attribute at = new Attribute(Nomenclature.MDC_ATTR_PM_SEG_PERSON_ID, pi);
					mr.set_attribute(at);

					if (numeric == null)
						throw new Exception("Numeric class not found for handle: " + obs.getObj_handle().getValue().getValue());

					Iterator<AVA_Type> it = obs.getAttributes().getValue().iterator();
					while (it.hasNext()){
						AVA_Type att = it.next();
						Integer att_id = att.getAttribute_id().getValue().getValue();
						byte[] att_value = att.getAttribute_value();
						//mr.addMeasure(attrId, RawDataExtractor.decodeRawData(attrId, de.getData(length), this.getDeviceConf().getEncondigRules()));
						Object data = RawDataExtractor.decodeRawData(att_id, att_value, this.getDeviceConf().getEncondigRules());
						if (!RawDataExtractor.updateAttrValue(numeric, att_id, data))
							mr.addMeasure(att_id, RawDataExtractor.decodeMeasure(att_id, att_value, this.getDeviceConf().getEncondigRules()));
					}
					InternalEventReporter.receivedMeasure((Agent) device, mr);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

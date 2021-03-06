package com.pku.wireless;

import ieee_11073.part_20601.phd.channel.tcp.TcpManagerChannel;

import java.io.File;

import cmdTester.ShellConfigStorage;
import cmdTester.ShellMeasureReporter;
import es.libresoft.openhealth.events.InternalEventReporter;
import es.libresoft.openhealth.events.MeasureReporterFactory;
import es.libresoft.openhealth.logging.Logging;
import es.libresoft.openhealth.storage.ConfigStorageFactory;

public class TCPManager {
	private int port;
	private String fileName;
	private TcpManagerChannel channelTCP;
	private String sysId;
	private OnSysIdListener listener;
	
	public TCPManager(int port, String fileName){
		this.port = port;
		this.fileName = fileName;
	}
	public void setOnSysIdListener(OnSysIdListener listener){
		this.listener = listener;
	}
	public void start(){
		try {
			/* uncomment next line to get HDP support for agents */
			// HDPManagerChannel chanHDP = new HDPManagerChannel();
			/* uncomment next line to get TCP support for agents */
			channelTCP = new TcpManagerChannel(port);
			//Set the event manager handler to get internal events from the manager thread
			InternalEventReporter.setDefaultEventManager(new EventManager());
			//Set target platform to android to report measures using IPC mechanism
			MeasureReporterFactory.setDefaultMeasureReporter(new ShellMeasureReporter());

			//set ConfigStorage
			File directory = new File(fileName);
			ShellConfigStorage mShellConfigStorage = new ShellConfigStorage(directory.getCanonicalPath());
			mShellConfigStorage.setOnSysIdListener(new OnSysIdListener() {
				
				@Override
				public void getSysId(String id) {
					// TODO Auto-generated method stub
					listener.getSysId(id);
				}
			});
			ConfigStorageFactory.setDefaultConfigStorage(mShellConfigStorage);
			
			/* Start TCP server */
			channelTCP.start();

			/*Logging.debug("Push any key to exit");
			System.in.read();

			//chanHDP.finish();
			channelTCP.finish();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void finish(){
		channelTCP.finish();
	}
}

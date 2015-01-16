//Hongqiao Gao
package cmdTester;

import ieee_11073.part_20601.phd.channel.tcp.TcpAgentChannel;
import ieee_11073.part_20601.phd.channel.tcp.TcpManagerChannel;

import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.bn.utils.ConvertToByteArray;

import com.pku.wireless.TCPAgent;

import Config.BloodPressureAgentTest;
import Config.BloodPressureAgent;
import Config.ECGAgent;
import Config.OximeterAgent;
import Config.OximeterAgentTest;
import es.libresoft.openhealth.Agent;
import es.libresoft.openhealth.events.Event;
import es.libresoft.openhealth.events.EventType;
import es.libresoft.openhealth.events.InternalEventManager;
import es.libresoft.openhealth.events.InternalEventReporter;
import es.libresoft.openhealth.events.MeasureReporter;
import es.libresoft.openhealth.events.MeasureReporterFactory;
import es.libresoft.openhealth.logging.ILogging;
import es.libresoft.openhealth.logging.Logging;

public class AgentShell {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logging.setDefaultLogGenerator(new ILogging() {

			@Override
			public void error(String str) {
				System.err.println(str);
			}

			@Override
			public void debug(String str) {
				System.out.println(str);
			}

			@Override
			public void info(String str) {
				System.out.println(str);
			}
		});
		Logging.debug("Starting CmdAgent.");
		try {
			String id = "0010000EEACA0605";
			TCPAgent tcpAgent = new TCPAgent(new BloodPressureAgentTest(id),"127.0.0.1", 9999);
			tcpAgent.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

package chord;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class Configuration {
	public static int MAX_NUMBER_OF_NODES;
	public static int INITIAL_NUMBER_OF_NODES;
	public static int SUCCESSORS_SIZE;
	public static boolean SYNCHRONOUS_MODE;
	public static int MAX_TRANSMISSION_DELAY;
	public static int AVG_STABILIZE_INTERVAL;
	public static int AVG_FIX_FINGERS_INTERVAL;
	public static int REQUEST_TIMEOUT_THRESHOLD;
	
	public static void load() {
		Parameters p = RunEnvironment.getInstance().getParameters();
		Configuration.MAX_NUMBER_OF_NODES = p.getInteger("max_number_of_nodes");
		Configuration.INITIAL_NUMBER_OF_NODES = p.getInteger("initial_number_of_nodes");
		Configuration.SYNCHRONOUS_MODE = p.getBoolean("synchronous_mode");
		Configuration.MAX_TRANSMISSION_DELAY = p.getInteger("max_transmission_delay");
		Configuration.SUCCESSORS_SIZE = p.getInteger("successors_size");
		Configuration.AVG_STABILIZE_INTERVAL = p.getInteger("avg_stabilize_interval");
		Configuration.AVG_FIX_FINGERS_INTERVAL = p.getInteger("avg_fix_fingers_interval");
		Configuration.REQUEST_TIMEOUT_THRESHOLD = p.getInteger("request_timeout_threshold");
	}
}

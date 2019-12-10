package chord;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class Configuration {
	public static int MAX_NUMBER_OF_NODES;
	public static int MIN_NUMBER_OF_NODES;
	public static int INITIAL_NUMBER_OF_NODES;
	public static int SUCCESSORS_SIZE;
	public static boolean SYNCHRONOUS_MODE;
	public static int MAX_TRANSMISSION_DELAY;
	public static int AVG_STABILIZE_INTERVAL;
	public static int AVG_FIX_FINGERS_INTERVAL;
	public static int REQUEST_TIMEOUT_THRESHOLD;
	public static double CRASH_PROBABILITY;
	public static double JOIN_PROBABILITY;
	public static double LOOKUP_PROBABILITY;
	public static int VISUALIZATION_TIMEOUT_SHOWTIME;
	public static int VISUALIZATION_CRASHED_SHOWTIME;
	
	public static void load() {
		Parameters p = RunEnvironment.getInstance().getParameters();
		Configuration.MAX_NUMBER_OF_NODES = p.getInteger("max_number_of_nodes");
		Configuration.MIN_NUMBER_OF_NODES = p.getInteger("min_number_of_nodes");
		Configuration.INITIAL_NUMBER_OF_NODES = p.getInteger("initial_number_of_nodes");
		Configuration.SYNCHRONOUS_MODE = p.getBoolean("synchronous_mode");
		Configuration.MAX_TRANSMISSION_DELAY = p.getInteger("max_transmission_delay");
		Configuration.SUCCESSORS_SIZE = p.getInteger("successors_size");
		Configuration.AVG_STABILIZE_INTERVAL = p.getInteger("avg_stabilize_interval");
		Configuration.AVG_FIX_FINGERS_INTERVAL = p.getInteger("avg_fix_fingers_interval");
		Configuration.REQUEST_TIMEOUT_THRESHOLD = p.getInteger("request_timeout_threshold");
		Configuration.CRASH_PROBABILITY = p.getDouble("crash_probability");
		Configuration.JOIN_PROBABILITY = p.getDouble("join_probability");
		Configuration.LOOKUP_PROBABILITY = p.getDouble("lookup_probability");
		Configuration.VISUALIZATION_TIMEOUT_SHOWTIME = p.getInteger("visualization_timeout_showtime");
		Configuration.VISUALIZATION_CRASHED_SHOWTIME = p.getInteger("visualization_crashed_showtime");
	}
}

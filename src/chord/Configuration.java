package chord;

public class Configuration {
	public static int MAX_NUMBER_OF_NODES;
	public static int INITIAL_NUMBER_OF_NODES;
	public static int SUCCESSORS_SIZE;
	public static boolean SYNCHRONOUS_MODE;
	public static int MAX_TRANSMISSION_DELAY;
	public static int AVG_STABILIZE_INTERVAL;
	public static int AVG_FIX_FINGERS_INTERVAL;
	public static double REQUEST_TIMEOUT_THRESHOLD;
	
	public static void load() {
		//Parameters p = RunEnvironment.getInstance().getParameters();
		//Configuration.EVENT_VISUAL_TIME = p.getInteger("event_visual_time");
	}
}

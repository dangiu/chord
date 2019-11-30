package chord;

public class Configuration {
	public static int NUMBER_OF_NODES;
	public static boolean SYNCHRONOUS_MODE;
	public static int MAX_TRANSMISSION_DELAY;
	
	public static void load() {
		//Parameters p = RunEnvironment.getInstance().getParameters();
		//Configuration.EVENT_VISUAL_TIME = p.getInteger("event_visual_time");
	}
}

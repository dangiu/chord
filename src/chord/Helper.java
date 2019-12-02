package chord;

public class Helper {
	
	public static double log2(double x) {
		return (Math.log(x) / Math.log(2));
	}
	
	public static int computeFingerTableSize(int numberOfNodes) {
		return (int) (Helper.log2(numberOfNodes));
	}
	
	public static Node getNodeById(int id) {
		return null;
		//TODO implement
	}
	
	public static double getCurrentTick() {
		return 0;
		//TODO implement
	}
}

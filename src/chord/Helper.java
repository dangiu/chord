package chord;

import repast.simphony.engine.environment.RunEnvironment;

public class Helper {
	
	public static double log2(double x) {
		return (Math.log(x) / Math.log(2));
	}
	
	public static int computeFingerTableSize(int numberOfNodes) {
		return (int) (Helper.log2(numberOfNodes));
	}
	
	public static int computeFingerStart(int index, int numberOfNodes) {
		return (int) (Math.pow(2, index) % Math.pow(2, computeFingerTableSize(numberOfNodes)));
	}
	
	public static Node getNodeById(int id) {
		return null;
		//TODO implement
	}
	
	public static double getCurrentTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}
	
	public static boolean belongs(int id, int lowerBound, boolean lIncluded, int upperBound, boolean uIncluded) {
		if(lowerBound <= upperBound) {
			/**
			 *      <----->
			 * 0    A     B    Max
			 */
			if(lIncluded && uIncluded) {
				if(lowerBound <= id && id <= upperBound)
					return true;
				else
					return false;
			} else if (lIncluded && !uIncluded) {
				if(lowerBound <= id && id < upperBound)
					return true;
				else
					return false;
			} else if (!lIncluded && uIncluded) {
				if(lowerBound < id && id <= upperBound)
					return true;
				else
					return false;
			} else if (!lIncluded && !uIncluded) {
				if(lowerBound < id && id < upperBound)
					return true;
				else
					return false;
			}
		} else {
			/**
			 * <---->     <---->
			 * 0    B     A    Max
			 */
			if(lIncluded && uIncluded) {
				if(lowerBound <= id || id <= upperBound)
					return true;
				else
					return false;
			} else if (lIncluded && !uIncluded) {
				if(lowerBound <= id ||id < upperBound)
					return true;
				else
					return false;
			} else if (!lIncluded && uIncluded) {
				if(lowerBound < id || id <= upperBound)
					return true;
				else
					return false;
			} else if (!lIncluded && !uIncluded) {
				if(lowerBound < id || id < upperBound)
					return true;
				else
					return false;
			}
		}
		assert false; //this code should never be reached
		return false;
	}
}

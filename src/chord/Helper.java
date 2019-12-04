package chord;

import java.util.Iterator;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.util.collections.IndexedIterable;

public class Helper {
	
	public static double log2(double x) {
		return (Math.log(x) / Math.log(2));
	}
	
	public static int computeFingerTableSize(int numberOfNodes) {
		return (int) (Helper.log2(numberOfNodes));
	}
	
	public static int computeFingerStart(int index, int nId, int numberOfNodes) {
		return (int) (nId + Math.pow(2, index) % Math.pow(2, computeFingerTableSize(numberOfNodes)));
	}
	
	public static Node getNodeById(int id, Context c) {
		Node target = null;
		IndexedIterable<Node> collection =  c.getObjects(Node.class);
		Iterator<Node> iterator = collection.iterator();
		
		while(iterator.hasNext() & target == null) {
			Node node = iterator.next();
			if(node.getId() == id) {
				target = node;
			}
		}
		
		return target;
	}
	
	public static double getCurrentTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}
	
	public static boolean belongs(int id, int lowerBound, boolean lIncluded, int upperBound, boolean uIncluded) {
		if(id == -1) {
			//id -1 is considered to be the "nil" value, it does never belong to any interval
			return false;
		}
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

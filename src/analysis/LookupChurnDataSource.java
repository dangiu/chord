package analysis;

import java.util.Iterator;

import chord.Configuration;
import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the data about the fraction of lookup
 * failed due to churn rate
 * Should only be called once at the end of the run
 * @author danie
 *
 */
public class LookupChurnDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "lookup_churn";
	}

	@Override
	public Class<?> getDataType() {
		return String.class;
	}

	@Override
	public Class<?> getSourceType() {
		return Collector.class;
	}

	@Override
	public Object get(Iterable<?> objs, int size) {
		assert size <= 1;
		Iterator<?> it = objs.iterator();
		if(it.hasNext()) {
			Collector c = (Collector) it.next();
			int nodes = Configuration.INITIAL_NUMBER_OF_NODES;
			double churn = Configuration.CRASH_PROBABILITY + Configuration.JOIN_PROBABILITY;
			int avgStabInterval = Configuration.AVG_STABILIZE_INTERVAL;
			int avgFixFingerInterval = Configuration.AVG_FIX_FINGERS_INTERVAL;
			double fractionLookupFailed = c.getFractionOfFailedLookup();
			String result = "";
			result += "\n";
			result += "initNodes,churn,avgStab,avgFixFing,fractionFailedLookups\n";
			result += nodes + "," + churn + "," + avgStabInterval + "," + avgFixFingerInterval + "," + fractionLookupFailed + "\n";
			return result;
		}
		
		return "Error in getting LookupChurnData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

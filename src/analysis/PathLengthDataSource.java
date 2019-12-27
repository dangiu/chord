package analysis;

import java.util.Iterator;

import chord.Configuration;
import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the data the length
 * of a path for a lookup request
 * Should only be called at the end of a run
 * @author danie
 *
 */
public class PathLengthDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "path_len";
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
			double avgPathLen = c.getAvgLookupPathLength();
			double bestPathLen = c.getBestLookupPathLength();
			double worstPathLen = c.getWorstLookupPathLength();
			String result = "";
			result += "\n";
			result += "nodes,avgPathLen,bestPathLen,worstPathLen\n";
			result += nodes + "," + avgPathLen + "," + bestPathLen + "," + worstPathLen + "\n";
			return result;
		}
		
		return "Error in getting PathLengthData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

package analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import chord.Configuration;
import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the data about the number of occurrences
 * of different path length during lookup requests
 * Should only be called at the end of a run
 * @author danie
 *
 */
public class PathLengthOccDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "path_len_occ";
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
			HashMap<Integer, Integer> pathLenOcc = c.getPathLengthOccurrences();
			String result = "";
			result += "\n";
			result += "nodes=" + nodes + "\n";
			result += "patLen,occ\n";
			Iterator<Entry<Integer, Integer>> pit = pathLenOcc.entrySet().iterator();
			while(pit.hasNext()) {
				Entry<Integer, Integer> e = pit.next();
				result += e.getKey() + "," + e.getValue() + "\n";
			}
			return result;
		}
		
		return "Error in getting PathLengthOccData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

package analysis;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import chord.Configuration;
import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the number of occurrences
 * of key count managed by different nodes
 * Should only be called once at the end of the run
 * @author danie
 *
 */
public class OccCountDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "occ_count";
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
			int maxIds = Configuration.MAX_NUMBER_OF_NODES;
			TreeMap<Integer, Integer> tm = c.getCountOccurrences();
			
			String result = "";
			result += "\n";
			result += "nodes=" + nodes + "\n";
			result += "maxIds=" + maxIds + "\n";
			result += "keyCount,occ\n";
			
			Iterator<Entry<Integer, Integer>> it2 = tm.descendingMap().entrySet().iterator();
			while(it2.hasNext()) {
				Entry<Integer, Integer> e = it2.next();
				result += e.getKey() + "," + e.getValue() + "\n";
			}
			return result;
		}
		
		return "Error in getting OccCountData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

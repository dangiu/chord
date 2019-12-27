package analysis;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the data about the number of vales lost due to crash
 * Must be called only once at the end of the run
 * @author danie
 *
 */
public class ValuesLostDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "values_lost";
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
			TreeMap<Integer, Integer> tm = c.getLostValues();
			String result = "";
			result += "\n";
			result += "runIndex,lostValues\n";
			Iterator<Entry<Integer, Integer>> it2 = tm.entrySet().iterator();
			while(it2.hasNext()) {
				Entry<Integer, Integer> currEntry = it2.next();
				result += currEntry.getKey() + "," + currEntry.getValue() + "\n";
			}
			return result;
		}
		
		return "Error in getting ValuesLostData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

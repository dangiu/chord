package analysis;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import chord.Configuration;
import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the data the knowledge of
 * a newly joined process as tick passes
 * Should only be called once at the end of the run
 * @author danie
 *
 */
public class JoinDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "join";
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
			double joinStartTick = c.getJoinStartTick();
			double joinEndTick = c.getJoinEndTick();
			TreeMap<Double, Integer> joinData = c.getKnowledgeOfJoined();

			String result = "";
			result += "\n";
			result += "nodes=" + nodes + "\n";
			result += "joinStartTick=" + joinStartTick + "\n";
			result += "joinEndTick=" + joinEndTick + "\n";
			result += "tick,count\n";
			
			Iterator<Entry<Double, Integer>> jIt = joinData.entrySet().iterator();
			while(jIt.hasNext()) {
				Entry<Double, Integer> e = jIt.next();
				result += e.getKey() + "," + e.getValue() + "\n";
			}
			return result;
		}
		
		return "Error in getting JoinData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

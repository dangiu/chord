package analysis;

import java.util.Iterator;

import chord.Configuration;
import repast.simphony.data2.AggregateDataSource;

/**
 * This data source collects the data about the probability of losing
 * values computed in a given amount of runs, with a percentage of nodes crashing
 * Must be called only once at the end of the run
 * @author danie
 *
 */
public class ProbLossDataSource implements AggregateDataSource {

	@Override
	public String getId() {
		return "prob_loss";
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
			float probOfLoss = c.getLossProbability();
			int nodes = Configuration.INITIAL_NUMBER_OF_NODES;
			int succSize = Configuration.SUCCESSORS_SIZE;
			int failedPercentage = Analysis.CRASHED_PERCENTAGE;
			String result = "";
			result += "\n";
			result += "nodes, failedPercentage, succSize, probOfLoss\n";
			result += nodes + "," + failedPercentage + "," + succSize + "," + probOfLoss + "\n";
			return result;
		}
		
		return "Error in getting ProbLossData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

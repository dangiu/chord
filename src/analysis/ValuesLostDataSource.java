package analysis;

import java.util.Iterator;

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
			String row =  ""+c.getLostValuesCount();
			return row;
		}
		
		return "Error in getting MessagePropagationData";
	}

	@Override
	public void reset() {
		//no need for reset, only called at the end of the run
	}

}

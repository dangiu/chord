package analysis;

import java.util.HashSet;
import java.util.Iterator;

import chord.Configuration;
import repast.simphony.random.RandomHelper;

/**
 * Agent that supports the operation of data collection needed to perform analysis of the protocol
 * 
 * @author danie
 * 
 */
public class Collector {
	
	private int valuesLostCount;
	
	public Collector() {
		//initialize structures needed to collect data
		this.valuesLostCount = 0;
	}
	
	public void computeLostValuesCount(Object[] arr, int percentageCrashed) {
		this.valuesLostCount = 0;
		
		//create array of integers
		int[] ids = new int[arr.length];
		for(int i = 0; i < arr.length; i++) {
			ids[i] = (int) arr[i];
		}
		//compute number of crashed
		float temp = (float) ids.length * ((float) percentageCrashed / (float) 100);
		int crashedN = (int) temp;
		//generate set of crashed nodes
		HashSet<Integer> crashed = new HashSet<Integer>();
		while(crashed.size() < crashedN) {
			int i = RandomHelper.nextIntFromTo(0, ids.length - 1);
			crashed.add(ids[i]);
		}
		//check for nodes that lost values
		HashSet<Integer> lossyNodes = new HashSet<Integer>();
		for(int i = 0; i < ids.length; i++) {
			boolean isLossy = true;
			//check node status and successors status (they must all be crashed in order to loose keys)
			for(int j = i; j < Configuration.SUCCESSORS_SIZE + 1 + i; j++) {
				if(!crashed.contains(ids[j%ids.length]))
					isLossy = false;
			}
			if(isLossy)
				lossyNodes.add(ids[i]);
		}
		//compute lost keys count
		Iterator<Integer> it = lossyNodes.iterator();
		while(it.hasNext()) {
			int curr = it.next();
			int currIndex = -1;
			for(int i = 0; i < ids.length; i++) {
				if(ids[i] == curr) currIndex = i;
			}
			//special cases where the id we are considering is the first in the array
			if(currIndex == 0) {
				int part1 = curr;
				int part2 = Configuration.MAX_NUMBER_OF_NODES - ids[ids.length - 1];
				int tempLost = part1 + part2;
				this.valuesLostCount = this.valuesLostCount + tempLost;
			} else {
				int pred = -1;
				for(int i = 0; i < ids.length - 1; i++) {
					if(ids[i + 1] == curr) pred = ids[i];
				}
				int tempLost = curr - pred;
				this.valuesLostCount = this.valuesLostCount + tempLost;
			}
		}
	}
	
	public int getLostValuesCount() {
		return this.valuesLostCount;
	}
	
}

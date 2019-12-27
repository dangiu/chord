package analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.UUID;

import chord.Configuration;
import chord.Helper;
import repast.simphony.random.RandomHelper;

/**
 * Agent that supports the operation of data collection needed to perform analysis of the protocol
 * 
 * @author danie
 * 
 */
public class Collector {
	
	private TreeMap<Integer, Integer> lostValuesPerRun;
	private HashMap<UUID, HashSet<Integer>> lookupPathLength; //used to store information about lookup in progress
	private HashMap<UUID, HashSet<Integer>> lookupPathLengthStable; //used to store information about lookup terminated
	
	private HashSet<UUID> lookupInProgress;	//used to store information about lookup fail due to churn rate
	private HashSet<UUID> lookupFailed;	//used to store information about lookup fail due to churn rate
	private HashSet<UUID> lookupCompleted;	//used to store information about lookup fail due to churn rate
	
	private Integer joinedNode;	//stores the id of the joined process
	private Double joinStartTick;	//stores the tick in which the process began the join
	private Double joinEndTick;	//stores the tick in which the process completed the join
	private HashMap<Double, HashSet<Integer>> tickKnowledgeOfJoined;	//stores the knowledge of all processes about the newly joined
																		//process for each tick since joinStartTick
	
	private TreeMap<Integer, Integer> countOcc; //stores information about the number of occurrences for a certain keycount
	
	
	public Collector() {
		//initialize structures needed to collect data
		lostValuesPerRun = new TreeMap<Integer, Integer>();
		lookupPathLength = new HashMap<UUID, HashSet<Integer>>();
		lookupPathLengthStable = new HashMap<UUID, HashSet<Integer>>();
		lookupInProgress = new HashSet<UUID>();
		lookupFailed = new HashSet<UUID>();
		lookupCompleted = new HashSet<UUID>();
		joinedNode = null;
		joinStartTick = null;
		joinEndTick = null;
		tickKnowledgeOfJoined = new HashMap<Double, HashSet<Integer>>();
		countOcc = new TreeMap<Integer, Integer>();
		
	}
	
	public int computeLostValuesCount(Object[] arr, int percentageCrashed) {
		int valuesLostCount = 0;
		
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
				valuesLostCount = valuesLostCount + tempLost;
			} else {
				int pred = -1;
				for(int i = 0; i < ids.length - 1; i++) {
					if(ids[i + 1] == curr) pred = ids[i];
				}
				int tempLost = curr - pred;
				valuesLostCount = valuesLostCount + tempLost;
			}
		}
		return valuesLostCount;
	}
	
	public void notifyLostValue(int run, int currRunLoss) {
		this.lostValuesPerRun.put(run, currRunLoss);		
	}
	
	public TreeMap<Integer, Integer> getLostValues() {
		return this.lostValuesPerRun;
	}
	
	/**
	 * Computes the probability of loosing values given the data collected during the simulation
	 * @return
	 */
	public float getLossProbability() {
		int runCount = this.lostValuesPerRun.size();
		int failCount = 0;
		//count number of failed runs
		Iterator<Integer> it = this.lostValuesPerRun.values().iterator();
		while(it.hasNext()) {
			int currValue = it.next();
			if(currValue > 0)
				failCount++;
		}
		float lossProb = (float) failCount / (float) runCount;
		return lossProb;
	}
	
	/**
	 * Store the fact that a new lookup query was generated
	 * @param queryId
	 * @param nodeId
	 */
	public void notifyLookupGeneration(UUID queryId, int nodeId) {
		HashSet<Integer> nodes = this.lookupPathLength.get(queryId);
		if(nodes == null) {
			nodes = new HashSet<Integer>();
		}
		nodes.add(nodeId);
		this.lookupPathLength.put(queryId, nodes);
	}
	
	/**
	 * Notifies about a message received that might be used for the prupose
	 * of performing a lookup, if it is, store the fact that this
	 * node is part of the lookup process
	 * @param queryId
	 * @param nodeId
	 */
	public void notifyPossibleLookup(UUID queryId, int nodeId) {
		HashSet<Integer> nodes = this.lookupPathLength.get(queryId);
		if(nodes != null) {
			//queryId is part of a lookup process, add this node to the list
			nodes.add(nodeId);
			this.lookupPathLength.put(queryId, nodes);
		}
	}
	
	/**
	 * Notifies about the end of a lookup and computes the path size
	 * @param queryId
	 */
	public void notifyLookupOver(UUID queryId) {
		HashSet<Integer> nodes = this.lookupPathLength.get(queryId);
		if(nodes != null) {
			this.lookupPathLengthStable.put(queryId, nodes);
		}
	}
	
	/**
	 * Compute the average path length for a lookup request
	 * @return average path length
	 */
	public double getAvgLookupPathLength() {
		Iterator<HashSet<Integer>> it = this.lookupPathLengthStable.values().iterator();
		double avgPathLength = 0;
		while(it.hasNext()) {
			HashSet<Integer> currPath = it.next();
			avgPathLength += currPath.size();
		}
		avgPathLength = avgPathLength / this.lookupPathLengthStable.size();
		return avgPathLength;
	}
	
	/**
	 * Compute the shortest path length for a lookup request
	 * @return shortest path length
	 */
	public double getBestLookupPathLength() {
		Iterator<HashSet<Integer>> it = this.lookupPathLengthStable.values().iterator();
		double bestPathLength = Double.MAX_VALUE;
		while(it.hasNext()) {
			HashSet<Integer> currPath = it.next();
			if(currPath.size() < bestPathLength)
				bestPathLength = currPath.size();
		}
		return bestPathLength;
	}
	
	/**
	 * Compute the longest path length for a lookup request
	 * @return longest path length
	 */
	public double getWorstLookupPathLength() {
		Iterator<HashSet<Integer>> it = this.lookupPathLengthStable.values().iterator();
		double worstPathLength = 0;
		while(it.hasNext()) {
			HashSet<Integer> currPath = it.next();
			if(currPath.size() > worstPathLength)
				worstPathLength = currPath.size();
		}
		return worstPathLength;
	}
	
	/**
	 * Compute the number of occurrences of each possible path length size for lookup queries
	 * @return
	 */
	public HashMap<Integer, Integer> getPathLengthOccurrences() {
		HashMap<Integer, Integer> lenOcc = new HashMap<Integer, Integer>();
		Iterator<HashSet<Integer>> it = this.lookupPathLengthStable.values().iterator();
		while(it.hasNext()) {
			HashSet<Integer> currPath = it.next();
			int currPathSize = currPath.size();
			int occ = lenOcc.getOrDefault(currPathSize, 0);
			lenOcc.put(currPathSize, occ + 1);
		}
		return lenOcc;
	}
	
	/**
	 * Count lookups currently in the system
	 * @param queryId
	 */
	public void notifyLookupInProgress(UUID queryId) {
		this.lookupInProgress.add(queryId);
	}
	
	/**
	 * Count lookups successfully completed
	 * @param queryId
	 */
	public void notifyLookupCompleted(UUID queryId) {
		this.lookupInProgress.remove(queryId);
		this.lookupCompleted.add(queryId);
	}
	
	/**
	 * Count lookups failed
	 * @param queryId
	 */
	public void notifyLookupFailed(UUID queryId) {
		this.lookupInProgress.remove(queryId);
		this.lookupFailed.add(queryId);
	}
	
	/**
	 * Computes the fraction of lookup failed over all total lookup performed (which are not in progress anymore)
	 * @return
	 */
	public double getFractionOfFailedLookup() {
		double failedSize = this.lookupFailed.size();
		double totalSize = failedSize + this.lookupCompleted.size();
		return failedSize/totalSize;
	}
	
	/**
	 * Notifies the collector about the begin of the join process of a node
	 */
	public void notifyJoinStart(int id) {
		this.joinedNode = id;
		this.joinStartTick = Helper.getCurrentTick();
	}
	
	/**
	 * Notifies the collector about the end of the join process of a node
	 */
	public void notifyJoinComplete(int id) {
		if(id == this.joinedNode) {
			this.joinEndTick = Helper.getCurrentTick();
		}
	}
	
	/**
	 * Notifies the collector about the knowledge of the joined node by some other node
	 */
	public void notifyKnowledgeJoin(int callerId, int[] ss, int[] ft, int pr) {
		//check if join process has actually began, otherwise no need to store data
		if(this.joinedNode != null) {
			double currTick = Helper.getCurrentTick();
			//check if caller knows about joined node
			boolean callerKnowsJoined = false;
			for(int i = 0; i < ss.length; i++) {
				if(ss[i] == this.joinedNode) {
					callerKnowsJoined = true;
				}
			}
			for(int i = 0; i < ft.length; i++) {
				if(ft[i] == this.joinedNode) {
					callerKnowsJoined = true;
				}
			}
			if(pr == this.joinedNode)
				callerKnowsJoined = true;
			if(callerKnowsJoined) {
				//add myself to the list (of the current tick) of processe that know about joined node
				HashSet<Integer> procKnow = this.tickKnowledgeOfJoined.get(currTick);
				if(procKnow == null) {
					procKnow = new HashSet<Integer>();
				}
				procKnow.add(callerId);
				this.tickKnowledgeOfJoined.put(currTick, procKnow);
			}
		}
	}
	
	/**
	 * Returns the tick in which the joining process started
	 * @return
	 */
	public double getJoinStartTick() {
		return this.joinStartTick;
	}
	
	/**
	 * Returns the tick in which the joining process ended
	 * @return
	 */
	public double getJoinEndTick() {
		return this.joinEndTick;
	}

	/**
	 * Returns a map containing the number of processes that know about the
	 * newly joined process, for each tick since the joining process started
	 * @return
	 */
	public TreeMap<Double, Integer> getKnowledgeOfJoined() {
		TreeMap<Double, Integer> tm = new TreeMap<Double, Integer>();
		Iterator<Double> keyIt = this.tickKnowledgeOfJoined.keySet().iterator();
		while(keyIt.hasNext()) {
			Double currKey = keyIt.next();
			HashSet<Integer> currValue = this.tickKnowledgeOfJoined.get(currKey);
			if(currValue == null) {
				tm.put(currKey, 0);
			} else {
				tm.put(currKey, currValue.size());
			}
		}
		return tm;
	}
	
	/**
	 * Computes how many key each node has to store
	 * @param ids
	 */
	public int[] computeKeyCountPerNode(Object[] arr) {		
		//create array of integers
		int[] ids = new int[arr.length];
		for(int i = 0; i < arr.length; i++) {
			ids[i] = (int) arr[i];
		}
		
		//create array to store count
		int[] countPerId = new int[ids.length];
		
		//compute count for each node
		for(int i = 0; i < ids.length; i++) {
			//special cases where the id we are considering is the first in the array
			int tempCount = 0;
			if(i == 0) {
				int part1 = ids[i];
				int part2 = Configuration.MAX_NUMBER_OF_NODES - ids[ids.length - 1];
				tempCount = part1 + part2;
			} else {
				tempCount = ids[i] - ids[i - 1];
			}
			countPerId[i] = tempCount;
		}
		return countPerId;
	}

	/**
	 * Notifies the collector about the count of keys that each node must manage
	 * @param counts
	 */
	public void notifyCountPerId(int[] counts) {
		for(int i = 0; i < counts.length; i++) {
			int currOcc = this.countOcc.getOrDefault(counts[i], 0);
			currOcc++;
			this.countOcc.put(counts[i], currOcc);
		}
	}
	
	/**
	 * Returns the count of the differet occurrances of keys managed
	 * @return
	 */
	public TreeMap<Integer, Integer> getCountOccurrences() {
		return this.countOcc;
	}
}

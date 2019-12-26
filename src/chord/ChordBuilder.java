package chord;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import analysis.Analysis;
import analysis.Collector;
import analysis.Analysis.AnalysisType;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import visualization.ChordSpaceAdder;
import visualization.Visualization;

/**
 * Implementation of a custom ContextBuilder from the Repast Simphony framework, used to initialize the simulation
 * and to handle aspects regarding the supervision of the system (make node crash/join/generate lookups)
 * @author Ohm
 *
 */
public class ChordBuilder implements ContextBuilder<Object> {
	
	public Context<Object> context;
	public int currentProcessId;
	public HashMap<Node, Double> crashedNodes;
	public Visualization vis;
	public Collector coll;

	@Override
	public Context build(Context<Object> context) {
		context.setId("chord");
		//load parameters from repast simulator
		Configuration.load();
		
		//VISUALIZATION
		//create projections
		NetworkBuilder<Object> builder = new NetworkBuilder("node_network", context, true);
		Network<Object> network = builder.buildNetwork();
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(new HashMap<>());
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, new ChordSpaceAdder<Object>(), new repast.simphony.space.continuous.WrapAroundBorders(), 20, 20);
		vis = new Visualization(network, context);
		context.add(vis);
		
		crashedNodes = new HashMap<>(); //store nodes that crash and tick at which they do (in order to remove them later from the view)
		
		this.context = context;
		
		//DATA COLLECTION
		this.coll = new Collector();
		context.add(coll);
		
		//create processes
		TreeSet<Integer> initialIds = new TreeSet<Integer>();
		while(initialIds.size() < Configuration.INITIAL_NUMBER_OF_NODES) {
			int newId = RandomHelper.nextIntFromTo(0, Configuration.MAX_NUMBER_OF_NODES - 1);
			initialIds.add(newId);
		}
		
		//print array of generated nodes for debugging purposes
		System.out.println("Nodes: " + initialIds);
		
		Object[] idsArray = initialIds.toArray();
		
		/*
		 * Analysis 1: values lost with 50% crash
		 */
		if(Analysis.isActive(AnalysisType.VALUES_LOST)) {
			int percentage = Analysis.CRASHED_PERCENTAGE;			
			int currRun = 0;	
			while(currRun < 100000) { //test over 100000 runs
				//generate ids
				//create processes
				TreeSet<Integer> ii = new TreeSet<Integer>();
				while(ii.size() < Configuration.INITIAL_NUMBER_OF_NODES) {
					int newId = RandomHelper.nextIntFromTo(0, Configuration.MAX_NUMBER_OF_NODES - 1);
					ii.add(newId);
				}
				Object[] idsArr = ii.toArray();
				int currRunLoss = coll.computeLostValuesCount(idsArr, percentage);
				coll.notifyLostValue(currRun, currRunLoss);
				currRun++;
			}			
		}
		
		/*
		 * Analysis 7: load balancing
		 */
		if(Analysis.isActive(AnalysisType.LOAD_BALANCING)) {			
			int currRun = 0;	
			while(currRun < 1000) {
				//generate ids
				//create processes
				TreeSet<Integer> ii = new TreeSet<Integer>();
				while(ii.size() < Configuration.INITIAL_NUMBER_OF_NODES) {
					int newId = RandomHelper.nextIntFromTo(0, Configuration.MAX_NUMBER_OF_NODES - 1);
					ii.add(newId);
				}
				Object[] idsArr = ii.toArray();
				int[] countPerId = coll.computeKeyCountPerNode(idsArr);
				coll.notifyCountPerId(countPerId);
				currRun++;
			}			
		}
		
		for(int i = 0; i < idsArray.length; i++) {
			int currId = (int) idsArray[i]; //node id
			//compute node successor
			int currSucc = -1;
			if(i == (idsArray.length - 1))
				currSucc = (int) idsArray[0];
			else
				currSucc = (int) idsArray[i + 1];
			
			//create node finger table
			int[] currFingerTable = new int[Helper.computeFingerTableSize(Configuration.MAX_NUMBER_OF_NODES)];
			for(int j = 0; j < currFingerTable.length; j++) {
				currFingerTable[j] = -1;
			}
			currFingerTable[0] = currSucc;
			
			//create node successors array
			int[] successors = new int[Configuration.SUCCESSORS_SIZE];
			for(int j = 0; j < successors.length; j++) {
				successors[j] = -1;
			}
			successors[0] = currSucc;
			
			Node currNode = new Node(currId, currFingerTable, -1, successors, true, vis, this.coll);
			context.add(currNode);
		}
		
		//schedule the exectution of the method supervior() of ChordBuilder
		RunEnvironment.getInstance().getCurrentSchedule().schedule(ScheduleParameters.createRepeating(1, 1, ScheduleParameters.LAST_PRIORITY), ()-> supervisor());
		
		return context;
	}
	
	/**
	 * Manages general aspect of the simulation such as:
	 * - generation of new nodes
	 * - crashing of existing nodes
	 * - generation of lookup requests
	 */
	public void supervisor() {
		if(Helper.getCurrentTick() < 100)
			//at the beginning, wait for the network to stabilize
			//before performing any operation
			return;
		
		//possibly generate lookup requests (only for active nodes)
		if(RandomHelper.nextDouble() < Configuration.LOOKUP_PROBABILITY) {
			//take a random node
			Iterator<Object> it = context.getRandomObjects(Node.class, 1).iterator();
			if(it.hasNext()) {
				Node n = (Node)it.next();
				if(n.isActive()) //generate lookup request only if node is active
					n.lookup();
			} 
		}

		//possibly make new nodes join the network
		if(RandomHelper.nextDouble() < Configuration.JOIN_PROBABILITY) {
			this.performJoin();
		}
		
		//possibly make existing nodes crash
		if(RandomHelper.nextDouble() < Configuration.CRASH_PROBABILITY) {
			this.performCrash();
		}
		
		//remove from context nodes crashed by more than threshold
		Iterator<Map.Entry<Node, Double>> it = this.crashedNodes.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Node, Double> entry = it.next();
			if(Helper.getCurrentTick() - entry.getValue() > Configuration.VISUALIZATION_CRASHED_SHOWTIME) {
				Node p = entry.getKey();
				context.remove(p);
				it.remove();
			}
		}
		
		//check for residual crashed nodes that might have become isolated at some point
		Iterator<Object> longIt = context.getObjects(Node.class).iterator();
		while(longIt.hasNext()) {
			Node n = (Node) longIt.next();
			if(!n.isActive()) {
				this.crashedNodes.putIfAbsent(n, Helper.getCurrentTick());
			} else {
				this.crashedNodes.remove(n);
			}
		}
		
		/*
		 * Analysis 6: join
		 */
		if(Analysis.isActive(AnalysisType.JOIN)) {
			if(Helper.getCurrentTick() == 200) {
				performJoin();
			}
		}
	}
	
	/**
	 * Take a node (if exist) and ask it to join
	 * @return true if a node joined, false otherwise
	 */
	private boolean performJoin() {
		HashSet<Integer> eligibleIds= new HashSet<Integer>();
		//fill with all possible ids
		for(int i = 0; i < Configuration.MAX_NUMBER_OF_NODES; i++)
			eligibleIds.add(i);
		//remove ids already used
		Iterator<Object> nodeIt = context.getObjects(Node.class).iterator();
		while(nodeIt.hasNext()) {
			Node curr = (Node) nodeIt.next();
			eligibleIds.remove(curr.getId());
		}
		//take random id from remaining
		Object[] eligibleIdsArray = eligibleIds.toArray(); 
		int randIndex = RandomHelper.nextIntFromTo(0, eligibleIdsArray.length - 1);
		int newId = (int) eligibleIdsArray[randIndex];
		
		//take random entry point
		Iterator<Object> randNodeIt = context.getRandomObjects(Node.class, context.size()).iterator();
		while(randNodeIt.hasNext()) {
			Node currRand = (Node) randNodeIt.next();
			if(currRand.isActive()) {
				//generate create new node from chosen id
				//create node finger table
				int[] ft = new int[Helper.computeFingerTableSize(Configuration.MAX_NUMBER_OF_NODES)];
				for(int j = 0; j < ft.length; j++) {
					ft[j] = -1;
				}
				//create node successors array
				int[] ss = new int[Configuration.SUCCESSORS_SIZE];
				for(int j = 0; j < ss.length; j++) {
					ss[j] = -1;
				}
				Node newNode = new Node(newId, ft, -1, ss, false, vis, this.coll);
				//add node to context
				this.context.add(newNode);
				//tell node to join
				newNode.join(currRand.getId());
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Take a node (if exist) and ask it to crash
	 * @return true if a node was crashed, false otherwise
	 */
	private boolean performCrash() {
		int activeNodeCount = 0;
		//count number of active nodes
		Iterator<Object> nodeIt = context.getObjects(Node.class).iterator();
		while(nodeIt.hasNext()) {
			Node curr = (Node) nodeIt.next();
			if(curr.isActive())
				activeNodeCount++;
		}
		if(activeNodeCount > Configuration.MIN_NUMBER_OF_NODES) {
			//try to take a random active node and make it crash
			Iterator<Object> randNodeIt = context.getRandomObjects(Node.class, context.size()).iterator();
			while(randNodeIt.hasNext()) {
				Node currRand = (Node) randNodeIt.next();
				if(currRand.isActive()) {
					currRand.crash();
					this.crashedNodes.put(currRand, Helper.getCurrentTick());
					return true;
				}
			}
		}
		return false;
	}
}

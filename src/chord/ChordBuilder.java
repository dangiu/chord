package chord;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

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

public class ChordBuilder implements ContextBuilder<Object> {
	
	public Context<Object> context;
	public int currentProcessId;
	public HashMap<Node, Double> crashedNodes;
	public Visualization vis;

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
		
		//create processes
		TreeSet<Integer> initialIds = new TreeSet<Integer>();
		while(initialIds.size() < Configuration.INITIAL_NUMBER_OF_NODES) {
			int newId = RandomHelper.nextIntFromTo(0, Configuration.MAX_NUMBER_OF_NODES - 1);
			initialIds.add(newId);
		}
		
		//print array of generated nodes for debugging purposes
		System.out.println("Nodes: " + initialIds);
		
		Object[] idsArray = initialIds.toArray();
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
			
			Node currNode = new Node(currId, currFingerTable, -1, successors, true, vis);
			context.add(currNode);
		}
		
		
		/*
		//OLD DEBUG STUFF
		//add extra node (id 0) that will join the network later
		int[] ft = new int[Helper.computeFingerTableSize(Configuration.MAX_NUMBER_OF_NODES)];
		for(int j = 0; j < ft.length; j++) {
			ft[j] = -1;
		}
		int[] ss = new int[Configuration.SUCCESSORS_SIZE];
		for(int j = 0; j < ss.length; j++) {
			ss[j] = -1;
		}
		
		
		Node sleepingNode = new Node(0, ft, -1, ss, false, vis);
		context.add(sleepingNode);
		*/
		
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
			boolean generated = false;
			while(!generated) {
				//take a random entry point
				Iterator<Object> it = context.getRandomObjects(Node.class, 1).iterator();
				if(it.hasNext()) {
					Node entry = (Node)it.next();
					if(entry.isActive()) {
						//generate random id for new node
						int newId = RandomHelper.nextIntFromTo(0, Configuration.MAX_NUMBER_OF_NODES - 1);
						//check that newId is not already in the network
						Iterator<Object> it2 = context.getObjects(Node.class).iterator();
						boolean alreadyPresent = false;
						while(it2.hasNext()) {
							Node curr = (Node) it2.next();
							if(curr.getId() == newId)
								alreadyPresent = true;
						}
						if(!alreadyPresent) {
							//actually create new node
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
							Node newNode = new Node(newId, ft, -1, ss, false, vis);
							//add node to context
							this.context.add(newNode);
							//tell node to join
							newNode.join(entry.getId());
							//set boolean generated to true in order to end the process
							generated = true;
						}
					}
				}
			}
		}
		
		//possibly make existing nodes crash
		if(RandomHelper.nextDouble() < Configuration.CRASH_PROBABILITY) {
			boolean removed = false;
			while(!removed) {
				//take a random node
				Iterator<Object> it = context.getRandomObjects(Node.class, 1).iterator();
				if(it.hasNext()) {
					Node target =  (Node)it.next();
					if(target.isActive()) {
						//count active nodes
						Iterator<Object> it2 = context.getObjects(Node.class).iterator();
						int count = 0;
						while(it2.hasNext()) {
							Node n = (Node) it2.next();
							if(n.isActive())
								count++;
						}
						if(count > Configuration.MIN_NUMBER_OF_NODES) {
							target.crash();
							this.crashedNodes.put(target, Helper.getCurrentTick());
						}
						removed = true;
					}				
				}
			}
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
	}
}

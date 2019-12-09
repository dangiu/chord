package chord;


import java.util.HashMap;
import java.util.TreeSet;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import visualization.ChordSpaceAdder;
import visualization.Visualization;

public class ChordBuilder implements ContextBuilder<Object> {
	
	public Context<Object> context;
	public int currentProcessId;
	public HashMap<Process, Double> unsubscribedProcesses;

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
		Visualization vis = new Visualization(network, context);
		context.add(vis);
		
				
		unsubscribedProcesses = new HashMap<>();
		
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
		
		
		return context;
	}
}
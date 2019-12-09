package visualization;

import java.awt.Color;
import java.util.Iterator;
import java.util.UUID;

import chord.Helper;
import chord.Node;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.graph.Network;

public class Visualization {
	
	public enum EdgeType {SUCCESSOR, QUERY}
	public Network<Object> network; 
	public Context<Object> context;
	public final Color[] colors = new Color[] {Color.CYAN, Color.YELLOW, Color.MAGENTA};
	private UUID currentQuery;
	private Color currentColor;
	private int currentOriginator;
	
	
	public Visualization(Network<Object> network, Context<Object> context) {
		this.network = network;
		this.context = context;
		this.currentQuery = null;
		this.currentColor = null;
		this.currentOriginator = -1;
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void startStep() {
		//remove all the current edges 
		this.network.removeEdges();
		
		//add all successors of active processes
		Iterator<Object> it = context.getAgentLayer(Object.class).iterator();
		while(it.hasNext()) {
			Object agent = it.next();
			if(agent instanceof Node) {
				Node n = (Node) agent;
				Node nSucc = Helper.getNodeById(n.getSuccs()[0], this.context);
				
				if(n.isActive()) {
					this.addEdge(n, nSucc, EdgeType.SUCCESSOR);
				}
			}
		}
		
		/*
		//if EVENT_VISUAL_TIME timeout is expired, a new event has to be considered
		if(this.getCurrentTick() - this.currentVisEventTick > Configuration.EVENT_VISUAL_TIME) {
			// set currentVisEvent to an unexisting event -> reset the visualization
			this.currentVisEvent = new Event(new EventId(UUID.randomUUID(), -1), 0); 
			//Send to each process the new event to consider
			Iterator<Object> it = context.getAgentLayer(Object.class).iterator();
			while(it.hasNext()) {
				Object agent = it.next();
				if(agent instanceof Process) {
					((Process)agent).setCurrentVisualEvent(this.currentVisEvent);
				}
			}
			newEvent = false;
		}
		// Update edges
		for(Link entry : this.currentLinks) {
			try {
				if(!entry.target.isUnsubscribed)
					this.network.addEdge(entry.source, entry.target, Double.valueOf(entry.type.ordinal()));
			} catch(Exception e) {
				// some process is removed from the context, I can not show that edge
			}
		}

		// Empty the list of links and the list of new Events at every step
		this.currentLinks.clear();
		*/
	}
	
	public UUID getCurrentVisualizedQuery() {
		return this.currentQuery;
	}
	
	public int getCurrentOriginator() {
		return this.currentOriginator;
	}
	
	public void notifyNewQuery(UUID queryId, int callerId) {
		if(this.currentQuery == null) {
			this.currentQuery = queryId;
			this.currentOriginator = callerId;
			this.changeColor();
		}
	}
	
	public void notifyQueryCompleted(UUID queryId) {
		if(this.currentQuery == queryId) {
			this.currentQuery = null;
			this.currentOriginator = -1;
		}
	}
	
	public Color getCurrentColor() {
		return this.currentColor;
	}
	
	public void changeColor() {
		if(this.currentColor == null) {
			this.currentColor = colors[0];
			return;
		}
		for(int i = 0; i < this.colors.length; i++) {
			if(this.currentColor == colors[i]) {
				int newColorIndex = (i + 1) % this.colors.length;
				this.currentColor = colors[newColorIndex];
				return;
			}
		}
	}
	
	public void addEdge(Node source, Node target, EdgeType type) {
		try {
			this.network.addEdge(source, target, Double.valueOf(type.ordinal()));
		} catch(Exception e) {
			// some process is removed from the context, I can not show that edge
		}
	}
}

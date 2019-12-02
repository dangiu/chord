package chord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import messages.Message;
import repast.simphony.random.RandomHelper;
import requests.Request;

public class Node {
	private ConcurrentLinkedQueue<Message> messageQueue;
	private HashMap<UUID, ArrayList<Request>> issuedRequests;
	private int id;
	private int[] fingerTable;
	private int predecessor;
	private int[] successors;
	
	public Node(int id, int[] fingerTable, int predecessor, int[] successors) {
		this.messageQueue = new ConcurrentLinkedQueue<Message>();
		this.issuedRequests = new HashMap<UUID, ArrayList<Request>>();
		
		this.id = id;
		
		int fingerTableSize = Helper.computeFingerTableSize(Configuration.NUMBER_OF_NODES);
		fingerTable = new int[fingerTableSize];
		
		this.predecessor = predecessor;
		this.successors = successors.clone();
	}
	
	public void step() {
		// TODO implement
	}
	
	private void send(Message m, int destination) {
		if(Configuration.SYNCHRONOUS_MODE) {
			m.setProcessingTick(Helper.getCurrentTick() + 1); // process message on next tick
		} else {
			double randDelay = RandomHelper.nextIntFromTo(1, Configuration.MAX_TRANSMISSION_DELAY);
			m.setProcessingTick(Helper.getCurrentTick() + randDelay);
		}
		
		Node target = Helper.getNodeById(destination);
		target.appendMessage(m);
	}
	
	/**
	 * Extracts a single message from the queue of incoming messages
	 * @return null if there is no message to process, Message to process otherwise
	 */
	private Message receive() {
		Iterator<Message> it = messageQueue.iterator();
		while(it.hasNext()) {
			Message m = it.next();
			if(m.getProcessingTick() == Helper.getCurrentTick()) { // check if message must be processed this tick
				it.remove();
				return m;
			}
		}
		return null;
	}
	
	public void appendMessage(Message m) {
		this.messageQueue.add(m);
	}
	
	private void handleFindSuccessor(int id) {
		// create and send request for find_predecessor(id)
		// TODO implement
	}
	
	private void resumeFindSuccessor1(int fpId) {
		
	}
	
	private void resumeFindSuccessor2(int sId) {
		
	}
	
	private void handleFindPredecessor(int id) {
		
	}
	
	private void resumeFindPredecessor1(int sId) {
		
	}
	
	private void resumeFindPredecessor2(int cpfId) {
		
	}
	
	private void handleClosestPrecedingFinger(int id) {
		
	}
	
	private void handleJoin(int id) {
		
	}
	
	private void resumeJoin(int fsId) {
		
	}
	
	// TODO define stabilize() implementation
	
	private void handleNotify(int id) {
		
	}
	
	// TODO define fix_finger() implementation
	
}

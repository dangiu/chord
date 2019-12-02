package chord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import messages.ClosestPrecedingFingerMessage;
import messages.ClosestPrecedingFingerReplyMessage;
import messages.FindPredecessorMessage;
import messages.FindPredecessorReplyMessage;
import messages.FindSuccessorMessage;
import messages.FindSuccessorReplyMessage;
import messages.Message;
import messages.NotifyMessage;
import messages.PredecessorMessage;
import messages.PredecessorReplyMessage;
import messages.SuccessorMessage;
import messages.SuccessorReplyMessage;
import repast.simphony.random.RandomHelper;
import requests.FindPredecessorRequest;
import requests.FindSuccessorRequest;
import requests.Request;
import requests.Request.RequestType;

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
		m.setSenderId(this.id); //set the sender as myself (node id that is calling the send() method)
		if(destination == this.id) {
			//message is sent to myself
			//this corresponds to a local call
			//message must be processed in the same tick in which it was sent
			m.setProcessingTick(Helper.getCurrentTick());
			this.appendMessage(m);
		} else {
			if(Configuration.SYNCHRONOUS_MODE) {
				m.setProcessingTick(Helper.getCurrentTick() + 1); // process message on next tick
			} else {
				double randDelay = RandomHelper.nextIntFromTo(1, Configuration.MAX_TRANSMISSION_DELAY);
				m.setProcessingTick(Helper.getCurrentTick() + randDelay);
			}
			
			Node target = Helper.getNodeById(destination);
			target.appendMessage(m);
		}
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
	
	private void handleSuccessor(SuccessorMessage m) {
		UUID queryId = m.getQueryId();
		//check if there is a request already associated with that queryId
		//this should never happen since we are being asked to give our successor (which is immediate)
		ArrayList<Request> requests = this.issuedRequests.get(queryId);
		
		if(requests != null) {
			System.err.println("handleSuccessor - a request with the same queryId already exists!");
			assert false;
		}
		
		//get our successor and send it back to the source of the message
		int mySuccessor = this.fingerTable[0];
		SuccessorReplyMessage reply = new SuccessorReplyMessage(m.getQueryId(), mySuccessor);
		this.send(reply, m.getSender());
	}
	
	private void handleSuccessorReply(SuccessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.issuedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request lastRequest = requests.remove(requests.size());
			
			if(lastRequest.getType() == RequestType.FIND_SUCCESSOR) {
				this.resumeFindSuccessor2(m.getSId());
			} else if (lastRequest.getType() == RequestType.FIND_PREDECESSOR) {
				FindPredecessorRequest fpr = (FindPredecessorRequest) lastRequest;
				this.resumeFindPredecessor1(fpr.getId(), fpr.getNPrime(), m.getSId());
			} else {
				System.err.println("handleSuccessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	private void handlePredecessor(PredecessorMessage m) {
		UUID queryId = m.getQueryId();
		//check if there is a request already associated with that queryId
		//this should never happen since we are being asked to give our predecessor (which is immediate)
		ArrayList<Request> requests = this.issuedRequests.get(queryId);
		
		if(requests != null) {
			System.err.println("handlePredecessor - a request with the same queryId already exists!");
			assert false;
		}
		
		//get our predecessor and send it back to the source of the message
		int myPredecessor = this.predecessor;
		PredecessorReplyMessage reply = new PredecessorReplyMessage(m.getQueryId(), myPredecessor);
		this.send(reply, m.getSender());
	}
	
	private void handlePredecessorReply(PredecessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.issuedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request lastRequest = requests.remove(requests.size());
			
			if(lastRequest.getType() == RequestType.STABILIZE) {
				this.resumeStabilize(m.getPId());
			} else {
				System.err.println("handlePredecessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	private void handleFindSuccessor(FindSuccessorMessage m) {
		UUID queryId = m.getQueryId();
		
		//suspend method execution and create entry in request table to resume find_successor()
		FindSuccessorRequest frs = new FindSuccessorRequest(Helper.getCurrentTick(), m.getQueryId(), m.getSender());
		
		ArrayList<Request> currentList = this.issuedRequests.get(queryId);
		
		//if not existent, initialize empty list
		if(currentList == null) {
			//initialize empty list for that queryId, insert request, update issuedRequests
			currentList = new ArrayList<Request>();
		}
		//add request at the end of the list
		currentList.add(currentList.size(), frs);
		
		//update issuedRequests
		this.issuedRequests.replace(queryId, currentList);
		
		//create and send FindPredecessorMessage (perform local call to find_predecessor())
		FindPredecessorMessage fpm = new FindPredecessorMessage(queryId, m.getTargetId());
		this.send(fpm, this.id); //call is local, the message is sent to myself
	}
	
	private void handleFindSuccessorReply(FindSuccessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.issuedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request lastRequest = requests.remove(requests.size());
			
			if(lastRequest.getType() == RequestType.JOIN) {
				//find_successor() was called during the join() method, proceed with execution
				this.resumeJoin(m.getFsId());
			} else if(lastRequest.getType() == RequestType.FIX_FINGER) {
				//find_successor() was called during the fix_fingers() method, proceed with execution
				this.resumeFixFingers(m.getFsId());
			} else {
				System.err.println("handleFindSuccessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	/**
	 * On resume we need the queryId in order to recover the corresponding request entry and proceed with execution
	 * during this method we don't pop the FIND_SUCCESSOR request from the request list with the specified queryId
	 * because that request is not handled yet! We will remove it in the method resumeFindSuccessor2() instead!
	 * @param queryId of the entry in the issuedRequests table that we need to resume
	 * @param fpId
	 */
	private void resumeFindSuccessor1(UUID queryId, int fpId) {
		ArrayList<Request> requests = this.issuedRequests.get(queryId);
		if(requests == null) {
			//we are resuming the execution of a method from a series of nested requests
			//the entry must exist otherwise there is some error
			System.err.println("resumeFindSuccessor1 - cannot resume method execution!");
			assert false;
		}
		//get last request (the caller)
		Request r = requests.get(requests.size());
		if(r.getType() != Request.RequestType.FIND_SUCCESSOR) {
			System.err.println("resumeFindSuccessor1 - impossible RequestType retrieved!");
		} else {
			//request for the successor of the returned node
			SuccessorMessage sm = new SuccessorMessage(queryId);
			this.send(sm, fpId);
		}
	}
	
	
	//TODO EACH RESUME METHOD DOES REQUIRE A QUERY ID TO BE PASSED
	//IN ORDER TO UNDERESTAND WHICH REQUEST SHOULD BE RESUMED
	//(AND POSSIBLY REMOVED FROM THE LIST OF ISSUED REQUESTS)
	
	private void resumeFindSuccessor2(int sId) {
		
	}
	
	private void handleFindPredecessor(FindPredecessorMessage m) {
		
	}
	
	private void handleFindPredecessorReply(FindPredecessorReplyMessage m) {
		
	}
	
	private void resumeFindPredecessor1(int id, int nPrime, int nPrimeSucc) {
		
	}
	
	private void resumeFindPredecessor2(int cpfId) {
		
	}
	
	private void handleClosestPrecedingFinger(ClosestPrecedingFingerMessage m) {
		
	}
	
	private void handleClosestPrecedingFingerReply(ClosestPrecedingFingerReplyMessage m) {
		
	}
	
	public void join(int id) {
		// ask (from exteranl) for a node to join the network
	}
	
	private void resumeJoin(int fsId) {
		
	}
	
	private void stabilize() {
		
	}
	
	private void resumeStabilize(int pId) {
		
	}
	
	private void handleNotify(NotifyMessage m) {
		
	}
	
	private void fixFingers() {
		
	}
	
	private void resumeFixFingers(int fsId) {
		
	}
	
}

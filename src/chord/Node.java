package chord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
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
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import requests.FindPredecessorRequest;
import requests.FindSuccessorRequest;
import requests.FixFingerRequest;
import requests.GetMoreSuccessorsRequest;
import requests.JoinRequest;
import requests.Request;
import requests.Request.RequestType;
import requests.StabilizeRequest;

public class Node {
	private ConcurrentLinkedQueue<Message> messageQueue;
	private HashMap<UUID, ArrayList<Request>> suspendedRequests;
	private int id;
	private int[] fingerTable;
	private int predecessor;
	private int[] successors;
	private boolean active;
	
	public Node(int id, int[] fingerTable, int predecessor, int[] successors, boolean active) {
		this.messageQueue = new ConcurrentLinkedQueue<Message>();
		this.suspendedRequests = new HashMap<UUID, ArrayList<Request>>();
		
		this.id = id;
		
		this.fingerTable = fingerTable.clone();
		
		this.predecessor = predecessor;
		this.successors = successors.clone();
		
		this.active = active;
	}
	
	public int getId() {
		return this.id;
	}
	
	@ScheduledMethod(start=1 , interval=1)
	public void step() {
		if(this.active) {
			//check if the node wants to perform some operations (e.g. join/lookup/leave)
			
			
			//check if stabilize must be executed
			//this must be done before the message processing phase, since this method can generate messages that must be processed in the current tick
			int randStabilize = RandomHelper.nextIntFromTo(1, Configuration.AVG_STABILIZE_INTERVAL);
			if(randStabilize == Configuration.AVG_STABILIZE_INTERVAL) {
				this.stabilize();
			}
			
			//check if fix fingers must be executed
			//this must be done before the message processing phase, since this method can generate messages that must be processed in the current tick
			int randFixFingers = RandomHelper.nextIntFromTo(1, Configuration.AVG_FIX_FINGERS_INTERVAL);
			if(randFixFingers == Configuration.AVG_FIX_FINGERS_INTERVAL) {
				this.fixFingers();
			}
			
			//process incoming messages
			Message m = this.receive();
			while(m != null) {
				if(m.getType() == Message.MessageType.CLOSEST_PRECEDING_FINGER) {
					this.handleClosestPrecedingFinger((ClosestPrecedingFingerMessage) m);
				} else if(m.getType() == Message.MessageType.CLOSEST_PRECEDING_FINGER_REPLY) {
					this.handleClosestPrecedingFingerReply((ClosestPrecedingFingerReplyMessage) m);
				} else if(m.getType() == Message.MessageType.FIND_PREDECESSOR) {
					this.handleFindPredecessor((FindPredecessorMessage) m);
				} else if(m.getType() == Message.MessageType.FIND_PREDECESSOR_REPLY) {
					this.handleFindPredecessorReply((FindPredecessorReplyMessage) m);
				} else if(m.getType() == Message.MessageType.FIND_SUCCESSOR) {
					this.handleFindSuccessor((FindSuccessorMessage) m);
				} else if(m.getType() == Message.MessageType.FIND_SUCCESSOR_REPLY) {
					this.handleFindSuccessorReply((FindSuccessorReplyMessage) m);
				} else if(m.getType() == Message.MessageType.NOTIFY) {
					this.handleNotify((NotifyMessage) m);
				} else if(m.getType() == Message.MessageType.PREDECESSOR) {
					this.handlePredecessor((PredecessorMessage) m);
				} else if(m.getType() == Message.MessageType.PREDECESSOR_REPLY) {
					this.handlePredecessorReply((PredecessorReplyMessage) m);
				} else if(m.getType() == Message.MessageType.SUCCESSOR) {
					this.handleSuccessor((SuccessorMessage) m);
				} else if(m.getType() == Message.MessageType.SUCCESSOR_REPLY) {
					this.handleSuccessorReply((SuccessorReplyMessage) m);
				} else {
					System.err.println("step - unknown message received");
				}
				m = this.receive(); //continue with next message
			}
			
			//clean timed out requests
			this.cleanTimedOutRequests();
			
		}
		
		
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
			
			Node target = Helper.getNodeById(destination, ContextUtils.getContext(this));
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
			if(m.getProcessingTick() <= Helper.getCurrentTick()) { // check if message must be processed this tick
				it.remove();
				return m;
			}
		}
		return null;
	}
	
	public void appendMessage(Message m) {
		this.messageQueue.add(m);
	}
	
	private void cleanTimedOutRequests() {
		Iterator<Entry<UUID, ArrayList<Request>>> it = suspendedRequests.entrySet().iterator();
		while(it.hasNext()) {
			Entry<UUID, ArrayList<Request>> curr = it.next();
			ArrayList<Request> requests = curr.getValue();
			Iterator<Request> it2 = requests.iterator();
			while(it2.hasNext()) {
				Request r = it2.next();
				if((Helper.getCurrentTick() - r.getCreationTick()) > Configuration.REQUEST_TIMEOUT_THRESHOLD) {
					//request is timed out, remove it
					it2.remove();
					
					//StabilizeRequests and GetMoreSuccessorsRequest tell us when a node crashed
					//and needs to be removed form fingertable/successors array
					int crashedId = -1;
					if(r.getType() == Request.RequestType.GET_MORE_SUCCESSORS) {
						crashedId = ((GetMoreSuccessorsRequest) r).getQueriedId();
					} else if(r.getType() == Request.RequestType.STABILIZE) {
						crashedId = ((StabilizeRequest) r).getQueriedId();
					}
					if(crashedId != -1) {
						//remove failed successor from successors, if present (and shift everything back)
						for(int i = 0; i < this.successors.length; i++) {
							if(this.successors[i] == crashedId) {
								for(int j = i; j < this.successors.length - 1; j++) {
									this.successors[j] = this.successors[j + 1];
								}
							}
						}
						//if it was our immediate successor also need to change the first entry in the finger table
						this.fingerTable[0] = successors[0];
						
						//fix finger table, if present the finger is changed with the previous one
						//queries will be a bit slower but still correct
						for(int i = 1; i < this.fingerTable.length; i++) { //start from i=1, i=0 already handled
							if(this.fingerTable[i] == crashedId) {
								this.fingerTable[i] = this.fingerTable[i - 1];
							}
						}
					}
				}
			}
			if(requests.size() == 0) {
				//there are no other requests with that queryId
				//remove the whole entry form suspendedRequests to save memory
				it.remove();
			}
		}
	}
	
	private void handleSuccessor(SuccessorMessage m) {
		//get our successor and send it back to the source of the message
		int mySuccessor = this.fingerTable[0];
		SuccessorReplyMessage reply = new SuccessorReplyMessage(m.getQueryId(), mySuccessor);
		this.send(reply, m.getSender());
	}
	
	private void handleSuccessorReply(SuccessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.FIND_SUCCESSOR) {
				this.resumeFindSuccessor2((FindSuccessorRequest) relatedRequest, m.getSId());
			} else if (relatedRequest.getType() == RequestType.FIND_PREDECESSOR) {
				this.resumeFindPredecessor1((FindPredecessorRequest) relatedRequest, m.getSId());
			} else if (relatedRequest.getType() == RequestType.GET_MORE_SUCCESSORS) {
				this.resumeGetMoreSuccessors((GetMoreSuccessorsRequest) relatedRequest, m.getSId());
			} else {
				System.err.println("handleSuccessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	private void handlePredecessor(PredecessorMessage m) {
		//get our predecessor and send it back to the source of the message
		int myPredecessor = this.predecessor;
		PredecessorReplyMessage reply = new PredecessorReplyMessage(m.getQueryId(), myPredecessor);
		this.send(reply, m.getSender());
	}
	
	private void handlePredecessorReply(PredecessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.STABILIZE) {
				this.resumeStabilize((StabilizeRequest) relatedRequest, m.getPId());
			} else {
				System.err.println("handlePredecessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	private void handleFindSuccessor(FindSuccessorMessage m) {
		UUID queryId = m.getQueryId();
		
		//suspend method execution and create entry in request table to resume find_successor()
		FindSuccessorRequest frs = new FindSuccessorRequest(Helper.getCurrentTick(), m.getQueryId(), m.getSender());
		
		ArrayList<Request> currentList = this.suspendedRequests.get(queryId);
		
		//if not existent, initialize empty list
		if(currentList == null) {
			//initialize empty list for that queryId, insert request, update issuedRequests
			currentList = new ArrayList<Request>();
		}
		//add request at the end of the list
		currentList.add(currentList.size(), frs);
		
		//update issuedRequests
		this.suspendedRequests.put(queryId, currentList);
		
		//create and send FindPredecessorMessage (perform local call to find_predecessor())
		FindPredecessorMessage fpm = new FindPredecessorMessage(queryId, m.getTargetId());
		this.send(fpm, this.id); //call is local, the message is sent to myself
	}
	
	private void handleFindSuccessorReply(FindSuccessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.JOIN) {
				//find_successor() was called during the join() method, proceed with execution
				this.resumeJoin((JoinRequest) relatedRequest, m.getFsId());
			} else if(relatedRequest.getType() == RequestType.FIX_FINGER) {
				//find_successor() was called during the fix_fingers() method, proceed with execution
				this.resumeFixFingers((FixFingerRequest) relatedRequest, m.getFsId());
			} else {
				System.err.println("handleFindSuccessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	private void resumeFindSuccessor1(FindSuccessorRequest relatedRequest, int fpId) {
		//append request to suspendedRequests and send message to get successor of fpId (request needs to be suspended again)
		ArrayList<Request> requests = this.suspendedRequests.get(relatedRequest.getQueryId());
		requests.add(relatedRequest);
		this.suspendedRequests.put(relatedRequest.getQueryId(), requests);
		//request for the successor of the returned node
		SuccessorMessage sm = new SuccessorMessage(relatedRequest.getQueryId());
		this.send(sm, fpId);
	}
	
	
	private void resumeFindSuccessor2(FindSuccessorRequest relatedRequest, int sId) {
		//execution of find_successor() is completed, send result back to the requester
		FindSuccessorReplyMessage reply = new FindSuccessorReplyMessage(relatedRequest.getQueryId(), sId);
		this.send(reply, relatedRequest.getRequesterId());
	}
	
	private void handleFindPredecessor(FindPredecessorMessage m) {
		int id = m.getTargetId();
		int nPrime = this.id;
		//suspend execution to get the successor of nPrime (even though nPrime is myself, we do this for consistency)
		FindPredecessorRequest fpr = new FindPredecessorRequest(Helper.getCurrentTick(), m.getQueryId(), m.getSender(), id, nPrime);
		ArrayList<Request> requests = this.suspendedRequests.get(m.getQueryId());
		if(requests == null) {
			requests = new ArrayList<Request>();
		}
		requests.add(fpr);
		this.suspendedRequests.put(m.getQueryId(), requests);
		//send successor message to nPrime
		SuccessorMessage sm = new SuccessorMessage(m.getQueryId());
		this.send(sm, nPrime);
	}
	
	private void handleFindPredecessorReply(FindPredecessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.FIND_SUCCESSOR) {
				//find_predecessor() was called during the find_successor() method, proceed with execution
				this.resumeFindSuccessor1((FindSuccessorRequest) relatedRequest, m.getFpId());
			} else {
				System.err.println("handleFindPredecessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	private void resumeFindPredecessor1(FindPredecessorRequest relatedRequest, int nPrimeSucc) {
		int id = relatedRequest.getId();
		int nPrime = relatedRequest.getNPrime();
		if(!Helper.belongs(id, nPrime, false, nPrimeSucc, true)) {
			//must resuspend the request and ask for closestPrecedingFinger to nPrime
			ArrayList<Request> requests = suspendedRequests.get(relatedRequest.getQueryId());
			if(requests == null) {
				requests = new ArrayList<Request>();
			}
			requests.add(relatedRequest);
			this.suspendedRequests.put(relatedRequest.getQueryId(), requests);
			//ask closest preceding finger to nPrime
			ClosestPrecedingFingerMessage cpfm = new ClosestPrecedingFingerMessage(relatedRequest.getQueryId(), id);
			this.send(cpfm, nPrime);
		} else {
			//we have found the immediate predecessor of the targetId, can send a reply back to the requester and terminate
			FindPredecessorReplyMessage fprm = new FindPredecessorReplyMessage(relatedRequest.getQueryId(), nPrime);
			this.send(fprm, relatedRequest.getRequesterId());
		}
	}
	
	private void resumeFindPredecessor2(FindPredecessorRequest relatedRequest, int cpfId) {
		//discovered closest finger, create new relatedRequest with new nPrime
		//add it in the table and resuspend execution in order to ask for successor of the new nPrime
		//this corresponds to beginning a new interation of the "while" loop in the pseudocode
		FindPredecessorRequest updatedRequest = new FindPredecessorRequest(relatedRequest.getCreationTick(), relatedRequest.getQueryId(), relatedRequest.getRequesterId(), relatedRequest.getId(), cpfId);
		
		ArrayList<Request> requests = this.suspendedRequests.get(updatedRequest.getQueryId());
		if(requests == null) {
			requests = new ArrayList<Request>();
		}
		requests.add(updatedRequest);
		this.suspendedRequests.put(updatedRequest.getQueryId(), requests);
		//send successor message to new nPrime
		SuccessorMessage sm = new SuccessorMessage(updatedRequest.getQueryId());
		this.send(sm, updatedRequest.getNPrime());
	}
	
	private void handleClosestPrecedingFinger(ClosestPrecedingFingerMessage m) {
		int cpfId = -1;
		//get closest preceding finger and send it back to the source of the message
		for(int i = this.fingerTable.length - 1; i > 0; i--) {
			if(Helper.belongs(fingerTable[i], this.id, false, m.getTargetId(), false)) {
				//closest preceding finger found
				cpfId = fingerTable[i];
				break;
			}
		}
		if(cpfId == -1) {
			//default case: closest preceding finger is me
			cpfId = this.id;
		}
		//send back reply
		ClosestPrecedingFingerReplyMessage reply = new ClosestPrecedingFingerReplyMessage(m.getQueryId(), cpfId);
		this.send(reply, m.getSender());
	}
	
	private void handleClosestPrecedingFingerReply(ClosestPrecedingFingerReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.FIND_PREDECESSOR) {
				//closest_preceding_finger() was called during find_predecessor(), resume method execution
				this.resumeFindPredecessor2((FindPredecessorRequest) relatedRequest, m.getCpfId());
			} else {
				System.err.println("handleClosestPrecedingFingerReply - impossible RequestType retrieved!");
			}
		}
	}
	
	/**
	 * Ask a node to join the network
	 * @param entryPointId, entry point used to join
	 */
	public void join(int entryPointId) {
		if(!this.active) {
			//empty message queue
			this.messageQueue.clear();
			//empty suspendedRequests
			this.suspendedRequests.clear();
			//reset finger table
			for(int i = 0; i < fingerTable.length; i++) {
				this.fingerTable[i] = -1;
			}
			//reset successors
			for(int i = 0; i < this.successors.length; i++) {
				this.successors[i] = -1;
			}
			
			this.predecessor = -1;
			//suspend execution and ask our entry point to find the successor of our id
			UUID queryId = UUID.randomUUID();
			JoinRequest jr = new JoinRequest(Helper.getCurrentTick(), queryId, this.id);
			ArrayList<Request> requests = this.suspendedRequests.get(queryId);
			if(requests == null) {
				requests = new ArrayList<Request>();
			}
			requests.add(jr);
			this.suspendedRequests.put(queryId, requests);
			FindSuccessorMessage fsm = new FindSuccessorMessage(queryId, this.id);
			this.send(fsm, entryPointId);
		} else {
			System.err.println("join - process is already active!");
		}
	}
	
	private void resumeJoin(JoinRequest relatedRequest, int fsId) {
		this.fingerTable[0] = fsId;
		successors[0] = fsId;
	}
	
	private void stabilize() {
		//count number of entries in successors array
		int successorsCount = 0;
		int lastSuccessor = -1;
		for(int i = 0; i < this.successors.length; i++) {
			if(this.successors[i] != -1) {
				successorsCount++;
				lastSuccessor = this.successors[i];
			}
		}
		if(successorsCount < this.successors.length) {
			//need to fill successors array
			//suspend execution and ask last entry to give me it's successor
			UUID queryId = UUID.randomUUID();
			GetMoreSuccessorsRequest gmsr = new GetMoreSuccessorsRequest(Helper.getCurrentTick(), queryId, this.id, lastSuccessor);
			ArrayList<Request> requests = this.suspendedRequests.get(queryId);
			if(requests == null) {
				requests = new ArrayList<Request>();
			}
			requests.add(gmsr);
			this.suspendedRequests.put(queryId, requests);
			SuccessorMessage sm = new SuccessorMessage(queryId);
			this.send(sm, lastSuccessor);
		}
		//proceed with stabilizing each valid entry in the successor array
		for(int i = 0; i < this.successors.length; i++) {
			if(this.successors[i] != -1) {
				//stabilize procedure
				UUID queryId = UUID.randomUUID();
				StabilizeRequest sr = new StabilizeRequest(Helper.getCurrentTick(), queryId, this.id, this.successors[i]);
				ArrayList<Request> requests = this.suspendedRequests.get(queryId);
				if(requests == null) {
					requests = new ArrayList<Request>();
				}
				requests.add(sr);
				this.suspendedRequests.put(queryId, requests);
				PredecessorMessage pm = new PredecessorMessage(queryId);
				this.send(pm, this.successors[i]);
			}
		}
	}
	
	private void resumeGetMoreSuccessors(GetMoreSuccessorsRequest relatedRequest, int sId) {
		int queriedId = relatedRequest.getQueriedId();
		//scroll the successor vector and check if the queriedId still exists
		//if it does, check whether entry after it is empty, add it if it is
		for(int i = 0; i < this.successors.length - 1; i++) { //if the queriedId is in the last position we don't care, its successor will not be added anyway
			if(this.successors[i] == queriedId && this.successors[i + 1] == -1) {
				this.successors[i + 1] = sId;
				break;
			}
		}
	}
	
	private void resumeStabilize(StabilizeRequest relatedRequest, int pId) {
		//check if the queriedId was the first entry in the successors array (index 0)
		if(relatedRequest.getQueriedId() == this.successors[0]) {
			//if it is, we might have a new successor and the finger table must be changed
			//also the notify needs to be called in that case
			if(Helper.belongs(pId, this.id, false, this.successors[0], false)) {
				this.fingerTable[0] = pId; //update finger table
				int oldEntry = -1;
				int newEntry = pId;
				for(int j = 0; j < this.successors.length; j++) {
					oldEntry = this.successors[j];
					this.successors[j] = newEntry;
					newEntry = oldEntry;
				}
			}
			//call the notify (no need for suspension since method is over)
			NotifyMessage nm = new NotifyMessage(UUID.randomUUID(), this.id);
			this.send(nm, this.fingerTable[0]);
		} else {
			//otherwise just add the element (if needed) and shift everything back
			for(int i = 1; i < this.successors.length; i++) { //can start form i=1 since i=0 is already covered by the code before
				int queriedId = relatedRequest.getQueriedId();
				if(this.successors[i] == queriedId) {
					if(Helper.belongs(pId, this.successors[i - 1], false, this.successors[i], false)) {
						//if we have actually discovered a new node, add it and shift everything
						int oldEntry = -1;
						int newEntry = pId;
						for(int j = i; j < this.successors.length; j++) {
							oldEntry = this.successors[j];
							this.successors[j] = newEntry;
							newEntry = oldEntry;
						}
					}
				}
			}
			//no notify messages need to be sent in this case
		}
	}
	
	private void handleNotify(NotifyMessage m) {
		int nPrime = m.getId();
		if(this.predecessor == -1 || Helper.belongs(nPrime, this.predecessor, false, this.id, false)) {
			//short-circuit evaluation used, this way Helper.belongs() is never called if this.predecessor == -1
			this.predecessor = nPrime;
		}
	}
	
	/**
	 * As done in the paper, this method will attempt to fix all fingers in a single call
	 */
	private void fixFingers() {
		for(int i = 0; i < this.fingerTable.length; i++) {
			UUID queryId = UUID.randomUUID();
			FixFingerRequest ffr = new FixFingerRequest(Helper.getCurrentTick(), queryId, this.id, i);
			ArrayList<Request> requests = this.suspendedRequests.get(queryId);
			if(requests == null) {
				requests = new ArrayList<Request>();
			}
			requests.add(ffr);
			this.suspendedRequests.put(queryId, requests);
			FindSuccessorMessage fsm = new FindSuccessorMessage(queryId, Helper.computeFingerStart(i, this.id, Configuration.MAX_NUMBER_OF_NODES));
			this.send(fsm, this.id);
		}
	}
	
	private void resumeFixFingers(FixFingerRequest relatedRequest, int fsId) {
		int fingerIndex = relatedRequest.getFingerIndex();
		this.fingerTable[fingerIndex] = fsId;
	}
	
}

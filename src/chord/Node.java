package chord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import analysis.Analysis;
import analysis.Collector;
import analysis.Analysis.AnalysisType;
import messages.ClosestPrecedingFingerMessage;
import messages.ClosestPrecedingFingerReplyMessage;
import messages.FindPredecessorMessage;
import messages.FindPredecessorReplyMessage;
import messages.FindSuccessorMessage;
import messages.FindSuccessorReplyMessage;
import messages.Message;
import messages.NotifyMessage;
import messages.PingMessage;
import messages.PingReplyMessage;
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
import requests.JoinRequest;
import requests.LookupRequest;
import requests.PingRequest;
import requests.Request;
import requests.Request.RequestType;
import requests.Stabilize1Request;
import requests.Stabilize2Request;
import visualization.Visualization;

/**
 * Main agent of the Chord protocol, implements protocol functionality
 * @author Ohm
 *
 */
public class Node {
	private ConcurrentLinkedQueue<Message> messageQueue;
	private HashMap<UUID, ArrayList<Request>> suspendedRequests;
	private int id;
	private int[] fingerTable;
	private int predecessor;
	private int[] successors;
	private boolean active;
	
	//attributes for visualization
	public Visualization vis;
	public boolean visQueryTimeout;
	public double visQueryTimeoutTick;
	
	//attributes for data collection
	public Collector coll;
	
	/**
	 * Create a valid node that can participate in the protocol
	 * @param id specifying position inside ID space
	 * @param fingerTable
	 * @param predecessor
	 * @param successors
	 * @param active whether process participates in the protocol, when false, the join process must be manually done
	 * @param vis used by the process to update visualization
	 * @param coll used by the process to send data to the collector
	 */
	public Node(int id, int[] fingerTable, int predecessor, int[] successors, boolean active, Visualization vis, Collector coll) {
		this.messageQueue = new ConcurrentLinkedQueue<Message>();
		this.suspendedRequests = new HashMap<UUID, ArrayList<Request>>();
		
		this.id = id;
		
		this.fingerTable = fingerTable.clone();
		
		this.predecessor = predecessor;
		this.successors = successors.clone();
		
		this.active = active;
		
		//visualization attributes
		this.vis = vis;
		this.visQueryTimeout = false;
		this.visQueryTimeoutTick = -1;	
		
		//data collection
		this.coll = coll;
	}
	
	/**
	 * Returns id of the node
	 * @return
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * Returns whether node is active or not
	 * @return
	 */
	public boolean isActive() {
		return this.active;
	}
	
	/**
	 * Single step of execution of a node, scheduled by repast simphony
	 */
	@ScheduledMethod(start=1 , interval=1)
	public void step() {		
		if(this.active) {
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
				} else if(m.getType() == Message.MessageType.PING) {
					this.handlePing((PingMessage) m);
				} else if(m.getType() == Message.MessageType.PING_REPLY) {
					this.handlePingReply((PingReplyMessage) m);
				} else {
					System.err.println("step - unknown message received");
				}
				m = this.receive(); //continue with next message
			}
			
			//clean timed out requests
			this.cleanTimedOutRequests();
			
			//check if become isolated
			if(successors[0] == this.id || successors[0] == -1) {
				this.crash();
			}
			
			/*
			 * Analysis 6: join
			 */
			if(Analysis.isActive(AnalysisType.JOIN)) {
				this.coll.notifyKnowledgeJoin(this.id, this.successors, this.fingerTable, this.predecessor);
			}
			
		} else if(!this.active) {
			//if node is inactive, but it's trying to join
			//we need to check if our entry point has answered our query
			//and resume the join procedure
			//otherwise we just discard the messages
			Message m = this.receive();
			while(m != null) {
				if(m.getType() == Message.MessageType.FIND_SUCCESSOR_REPLY) {
					FindSuccessorReplyMessage fsrm = (FindSuccessorReplyMessage) m;
					UUID queryId = fsrm.getQueryId();
					ArrayList<Request> requests = this.suspendedRequests.get(queryId);
					if(requests != null) {
						if(requests.size() == 0) break;
						Request joinRequest = requests.remove(requests.size() - 1);
						if(joinRequest.getType() == RequestType.JOIN) {
							this.resumeJoin((JoinRequest) joinRequest, fsrm.getFsId());
						}
					}
				}
				m = this.receive(); //continue with next message
			}
		}
		
	}
	
	/**
	 * Sends message to other node
	 * @param m message to be sent
	 * @param destination node
	 */
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
			
			//update visualization
			if(m.getQueryId() == this.vis.getCurrentVisualizedQuery()) {
				if(
						m.getType() == Message.MessageType.CLOSEST_PRECEDING_FINGER
						||
						m.getType() == Message.MessageType.FIND_PREDECESSOR
						||
						m.getType() == Message.MessageType.FIND_SUCCESSOR
						||
						m.getType() == Message.MessageType.NOTIFY
						||
						m.getType() == Message.MessageType.PING
						||
						m.getType() == Message.MessageType.PREDECESSOR
						||
						m.getType() == Message.MessageType.SUCCESSOR
					) {
					this.vis.addEdge(this, target, Visualization.EdgeType.QUERY);
				} else if(
						m.getType() == Message.MessageType.CLOSEST_PRECEDING_FINGER_REPLY
						||
						m.getType() == Message.MessageType.FIND_PREDECESSOR_REPLY
						||
						m.getType() == Message.MessageType.FIND_SUCCESSOR_REPLY
						||
						m.getType() == Message.MessageType.PING_REPLY
						||
						m.getType() == Message.MessageType.PREDECESSOR_REPLY
						||
						m.getType() == Message.MessageType.SUCCESSOR_REPLY
						) {
					this.vis.addEdge(this, target, Visualization.EdgeType.REPLY);
				}
			}
				
			if(target != null)	//target might be crashed
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
				
				//analysis 3: path length
				if(Analysis.isActive(AnalysisType.PATH_LENGTH)) {
					this.coll.notifyPossibleLookup(m.getQueryId(), this.id);
				}
				
				return m;
			}
		}
		return null;
	}
	
	/**
	 * Appends message to queue of the current node
	 * @param m
	 */
	public void appendMessage(Message m) {
		this.messageQueue.add(m);
	}
	
	/**
	 * Handles timed out requests, removing them and updating finger table and successors list when necessary
	 */
	private void cleanTimedOutRequests() {
		Iterator<Entry<UUID, ArrayList<Request>>> it = suspendedRequests.entrySet().iterator();
		while(it.hasNext()) {
			Entry<UUID, ArrayList<Request>> curr = it.next();
			ArrayList<Request> requests = curr.getValue();
			Iterator<Request> it2 = requests.iterator();
			while(it2.hasNext()) {
				Request r = it2.next();
				if((Helper.getCurrentTick() - r.getCreationTick()) > Configuration.REQUEST_TIMEOUT_THRESHOLD) {
					//visualization: check if timed out request is being visualized
					if(r.getQueryId() == this.vis.getCurrentVisualizedQuery()) {
						this.visQueryTimeout = true;
						this.visQueryTimeoutTick = Helper.getCurrentTick();
						//reset visualization in order to visualize new query
						this.vis.notifyQueryCompleted(r.getQueryId());
					}
					
					//request is timed out, remove it
					it2.remove();
					
					//analysis 5: churn rate vs lookup
					if(Analysis.isActive(AnalysisType.CHURN_LOOKUP)) {
						if(r.getType() == Request.RequestType.LOOKUP) {
							this.coll.notifyLookupFailed(r.getQueryId());
						}
					}
				}
				if((Helper.getCurrentTick() - r.getCreationTick()) > Configuration.STABILIZATION_TIMEOUT_THRESHOLD) {
					//Stabilize1Request, Stabilize2Request, PingRequest
					//tell us when a node crashed and needs to be removed
					//from successors, fingerTable or predecessor
					int crashedId = -1;
					if(r.getType() == Request.RequestType.STABILIZE2) {
						crashedId = ((Stabilize2Request) r).getQueriedId();
					} else if(r.getType() == Request.RequestType.STABILIZE1) {
						crashedId = ((Stabilize1Request) r).getQueriedId();
					} else if(r.getType() == Request.RequestType.PING) {
						crashedId = ((PingRequest) r).getQueriedId();
					}
					
					//a node has actually crashed
					if(crashedId != -1) {
						//remove failed successor from successors, if present (and shift everything back)
						for(int i = 0; i < this.successors.length; i++) {
							if(this.successors[i] == crashedId) {
								for(int j = i; j < this.successors.length - 1; j++) {
									this.successors[j] = this.successors[j + 1];
								}
							}
						}
						//update immediate successor in the finger table in case it has changed
						this.fingerTable[0] = successors[0];
						
						//fix finger table, if present the finger is changed with the previous one
						//queries will be a bit slower but still correct
						for(int i = 1; i < this.fingerTable.length; i++) { //start from i=1, i=0 already handled
							if(this.fingerTable[i] == crashedId) {
								this.fingerTable[i] = this.fingerTable[i - 1];
							}
						}
						//if the crashed node is our predecessor, set predecessor to nil value (-1)
						if(this.predecessor == crashedId) {
							this.predecessor = -1;
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
	
	/**
	 * Handler for the SuccessorMessage messages
	 * @param m
	 */
	private void handleSuccessor(SuccessorMessage m) {
		//get our successor and send it back to the source of the message
		SuccessorReplyMessage reply = new SuccessorReplyMessage(m.getQueryId(), this.successors);
		this.send(reply, m.getSender());
	}
	
	/**
	 * Handler for the SuccessorReplyMessage messages
	 * @param m
	 */
	private void handleSuccessorReply(SuccessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			if(requests.size() == 0) return;
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.FIND_SUCCESSOR) {
				this.resumeFindSuccessor2((FindSuccessorRequest) relatedRequest, m.getSId());
			} else if (relatedRequest.getType() == RequestType.FIND_PREDECESSOR) {
				this.resumeFindPredecessor1((FindPredecessorRequest) relatedRequest, m.getSId());
			} else if (relatedRequest.getType() == RequestType.STABILIZE2) {
				this.resumeStabilize2((Stabilize2Request) relatedRequest, m.getSuccessors());
			} else {
				System.err.println("handleSuccessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	/**
	 * Handler for the PredecessorMessage messages
	 * @param m
	 */
	private void handlePredecessor(PredecessorMessage m) {
		//get our predecessor and send it back to the source of the message
		int myPredecessor = this.predecessor;
		PredecessorReplyMessage reply = new PredecessorReplyMessage(m.getQueryId(), myPredecessor);
		this.send(reply, m.getSender());
	}
	
	/**
	 * Handler for the PredecessorReplyMessage messages
	 * @param m
	 */
	private void handlePredecessorReply(PredecessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			if(requests.size() == 0) return;
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.STABILIZE1) {
				this.resumeStabilize1((Stabilize1Request) relatedRequest, m.getPId());
			} else {
				System.err.println("handlePredecessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	/**
	 * Handler for the FindSuccessorMessage messages
	 * @param m
	 */
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
	
	/**
	 * Handler for the FindSuccessorReplyMessage messages
	 * @param m
	 */
	private void handleFindSuccessorReply(FindSuccessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			if(requests.size() == 0) return;
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.JOIN) {
				//find_successor() was called during the join() method, proceed with execution
				this.resumeJoin((JoinRequest) relatedRequest, m.getFsId());
			} else if(relatedRequest.getType() == RequestType.FIX_FINGER) {
				//find_successor() was called during the fix_fingers() method, proceed with execution
				this.resumeFixFingers((FixFingerRequest) relatedRequest, m.getFsId());
			} else if(relatedRequest.getType() == RequestType.LOOKUP) {
				//find_successor() was called during the lookup() method, proceed with execution
				this.resumeLookup((LookupRequest) relatedRequest, m.getFsId());
			} else {
				System.err.println("handleFindSuccessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	/**
	 * Resumes execution of FindSuccessor method after predecessor has been found
	 * @param relatedRequest
	 * @param fpId
	 */
	private void resumeFindSuccessor1(FindSuccessorRequest relatedRequest, int fpId) {
		//append request to suspendedRequests and send message to get successor of fpId (request needs to be suspended again)
		ArrayList<Request> requests = this.suspendedRequests.get(relatedRequest.getQueryId());
		requests.add(relatedRequest);
		this.suspendedRequests.put(relatedRequest.getQueryId(), requests);
		//request for the successor of the returned node
		SuccessorMessage sm = new SuccessorMessage(relatedRequest.getQueryId());
		this.send(sm, fpId);
	}
	
	
	/**
	 * Resumes execution of FindSuccessor method after successor of predecessor has been found
	 * @param relatedRequest
	 * @param sId
	 */
	private void resumeFindSuccessor2(FindSuccessorRequest relatedRequest, int sId) {
		//execution of find_successor() is completed, send result back to the requester
		FindSuccessorReplyMessage reply = new FindSuccessorReplyMessage(relatedRequest.getQueryId(), sId);
		this.send(reply, relatedRequest.getRequesterId());
	}
	
	/**
	 * Handler for the FindPredecessorMessage messages
	 * @param m
	 */
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
	
	/**
	 * Handler for the FindPredecessorReplyMessage messages
	 * @param m
	 */
	private void handleFindPredecessorReply(FindPredecessorReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			if(requests.size() == 0) return;
			Request relatedRequest = requests.remove(requests.size() - 1);
			
			if(relatedRequest.getType() == RequestType.FIND_SUCCESSOR) {
				//find_predecessor() was called during the find_successor() method, proceed with execution
				this.resumeFindSuccessor1((FindSuccessorRequest) relatedRequest, m.getFpId());
			} else {
				System.err.println("handleFindPredecessorReply - impossible RequestType retrieved!");
			}
		}
	}
	
	/**
	 * Resumes execution of FindPredecessor method after successor of n has been found
	 * @param relatedRequest
	 * @param nPrimeSucc
	 */
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
	
	/**
	 * Resumes execution of FindPredecessor method after closest preceding finger has been found
	 * @param relatedRequest
	 * @param cpfId
	 */
	private void resumeFindPredecessor2(FindPredecessorRequest relatedRequest, int cpfId) {
		if(relatedRequest.getNPrime() == cpfId) {
			//special case where the node remained isolated and keeps looping forever
			return; //just return therefore removeing request
		}
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
	
	/**
	 * Handler for the ClosestPrecedingFingerMessage messages
	 * @param m
	 */
	private void handleClosestPrecedingFinger(ClosestPrecedingFingerMessage m) {
		int cpfId = -1;
		//get closest preceding finger and send it back to the source of the message
		for(int i = this.fingerTable.length - 1; i >= 0; i--) {
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
	
	/**
	 * Handler for the ClosestPrecedingFingerReplyMessage messages
	 * @param m
	 */
	private void handleClosestPrecedingFingerReply(ClosestPrecedingFingerReplyMessage m) {
		UUID queryId = m.getQueryId();
		//this is a reply, a request associated with it should exist
		//if it does not exist the request already timed out and we can ignore the message
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests != null) {
			//get last request for that queryId and resume correct method execution
			//while passing context saved in request table and data from message
			if(requests.size() == 0) return;
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
		/*
		 * Analysis 6: join
		 */
		if(Analysis.isActive(AnalysisType.JOIN)) {
			this.coll.notifyJoinStart(this.id);
		}
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
			//reset predecessor
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
	
	/**
	 * Resumes execution of Join after entry point has replied
	 * @param relatedRequest
	 * @param fsId
	 */
	private void resumeJoin(JoinRequest relatedRequest, int fsId) {
		/*
		 * Analysis 6: join
		 */
		if(Analysis.isActive(AnalysisType.JOIN)) {
			this.coll.notifyJoinComplete(this.id);
		}
		this.fingerTable[0] = fsId;
		successors[0] = fsId;
		//join procedure completed set flag active
		this.active = true;
		this.stabilize();
	}
	
	/**
	 * Stabilization procedure executed periodically
	 */
	private void stabilize() {
		//check if our predecessor is still alive if we have one
		//if he replies ok, otherwise we will timeout and set it to -1
		if(this.predecessor != -1) {
			UUID queryId = UUID.randomUUID();
			PingMessage ping = new PingMessage(queryId);
			this.send(ping, this.predecessor);
			PingRequest pr = new PingRequest(Helper.getCurrentTick(), queryId, this.id, this.predecessor);
			ArrayList<Request> requests = this.suspendedRequests.get(queryId);
			if(requests == null) {
				requests = new ArrayList<Request>();
			}
			requests.add(pr);
			this.suspendedRequests.put(queryId, requests);
		}
		
		//check if another stabilize call is in progress
		//either Stabilize1Request or Stabilize2Request
		//only one stabilize call must be active at any time!
		boolean stabilizeAlreadyInProgress = false;
		Iterator<ArrayList<Request>> it = this.suspendedRequests.values().iterator();
		while(it.hasNext() && !stabilizeAlreadyInProgress) {
			ArrayList<Request> currRequests = it.next();
			Iterator<Request> it2 = currRequests.iterator();
			while(it2.hasNext() && !stabilizeAlreadyInProgress) {
				Request r = it2.next();
				if(r.getType() == Request.RequestType.STABILIZE1 || r.getType() == Request.RequestType.STABILIZE2) {
					stabilizeAlreadyInProgress = true;
				}
			}
		}
		if(!stabilizeAlreadyInProgress) {
			//start stabilize process
			//ask our successor for it's predecessor and suspend execution
			UUID queryId = UUID.randomUUID();
			Stabilize1Request sr = new Stabilize1Request(Helper.getCurrentTick(), queryId, this.id, successors[0]);
			ArrayList<Request> requests = this.suspendedRequests.get(queryId);
			if(requests == null) {
				requests = new ArrayList<Request>();
			}
			requests.add(sr);
			this.suspendedRequests.put(queryId, requests);
			//send message to get the predecessor of our successor
			PredecessorMessage pm = new PredecessorMessage(queryId);
			if(successors[0] == -1) {
				//all our successors crashed, we are isolated
				this.crash();
			} else {
				this.send(pm, successors[0]);
			}
		}
	}
	
	/**
	 * Resumes execution of stabilization procedure after predecessor of current successor has been found
	 * @param relatedRequest
	 * @param pId
	 */
	private void resumeStabilize1(Stabilize1Request relatedRequest, int pId) {
		//pseudocode of stabilize() but suspend execution before notify in order to ask for successors list
		if(Helper.belongs(pId, this.id, false, this.successors[0], false)) {
			//new node joined, update successors[0] and fingertable[0]
			this.successors[0] = pId;
			fingerTable[0] = pId;
		}
		//suspend execution and ask for successors list in order to get more successors
		UUID queryId = UUID.randomUUID();
		Stabilize2Request gmsr = new Stabilize2Request(Helper.getCurrentTick(), queryId, this.id, this.successors[0]);
		ArrayList<Request> requests = this.suspendedRequests.get(queryId);
		if(requests == null) {
			requests = new ArrayList<Request>();
		}
		requests.add(gmsr);
		this.suspendedRequests.put(queryId, requests);
		//send message to get the predecessor of our successor
		SuccessorMessage sm = new SuccessorMessage(queryId);
		this.send(sm, this.successors[0]);
	}
	
	/**
	 * Resumes execution of stabilization procedure after successor has replied with successors lists
	 * @param relatedRequest
	 * @param pId
	 */
	private void resumeStabilize2(Stabilize2Request relatedRequest, int[] sSuccessors) {
		//second part of the stabilize() procedure, update successors list and send notify to successor[0]
		//update successors list based on sSuccessors
		for(int i = 1; i < this.successors.length; i++) {
			if(sSuccessors[i - 1] == this.id) {
				//corner case where im inside the successor array 
				//because is longer than the number of nodes in the system
				//remove the rest of the successors (set them to -1) and break
				for(int j = i; j < this.successors.length; j++) {
					this.successors[j] = -1;
				}
				break;
			}
			this.successors[i] = sSuccessors[i - 1];
		}
		
		//send notify
		NotifyMessage nm = new NotifyMessage(UUID.randomUUID(), this.id);
		this.send(nm, successors[0]);
	}
	
	/**
	 * Handler for the NotifyMessage messages
	 * @param m
	 */
	private void handleNotify(NotifyMessage m) {
		int nPrime = m.getId();
		if(this.predecessor == -1 || Helper.belongs(nPrime, this.predecessor, false, this.id, false)) {
			//short-circuit evaluation used, this way Helper.belongs() is never called if this.predecessor == -1
			this.predecessor = nPrime;
		}
	}
	
	/**
	 * Fix finger procedure, as in the paper, this method will attempt to fix all fingers in a single call
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
	
	/**
	 * Resume execution of fix finger after findSuccessor has completed
	 * @param relatedRequest
	 * @param fsId
	 */
	private void resumeFixFingers(FixFingerRequest relatedRequest, int fsId) {
		int fingerIndex = relatedRequest.getFingerIndex();
		this.fingerTable[fingerIndex] = fsId;
	}
	
	/**
	 * Handler for the PingMessage messages
	 * @param m
	 */
	private void handlePing(PingMessage m) {
		PingReplyMessage reply = new PingReplyMessage(m.getQueryId());
		this.send(reply, m.getSender());
	}
	
	/**
	 * Handler for the PingReplyMessage messages
	 * @param m
	 */
	private void handlePingReply(PingReplyMessage m) {
		//remove entry from suspendedRequest, pinged node is still alive
		this.suspendedRequests.remove(m.getQueryId());
	}
	
	/**
	 * Crash the node without notifying other nodes
	 */
	public void crash() {
		//set active to false
		this.active = false;
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
		//reset predecessor
		this.predecessor = -1;
		
		//update visualization in case I am originator of current visualized query
		if(this.vis.getCurrentOriginator() == this.id)
			this.vis.resetVisualizedQuery();
	}
	
	/**
	 * Tell the current node to perform a lookup for the specified key
	 * @param key
	 */
	public void lookup(int key) {		
		//suspend execution and store LookupRequest
		UUID queryId = UUID.randomUUID();
		
		//analysis 3: path length
		if(Analysis.isActive(AnalysisType.PATH_LENGTH)) {
			this.coll.notifyLookupGeneration(queryId, this.id);
		}
		
		//analysis 5: churn rate vs lookup
		if(Analysis.isActive(AnalysisType.CHURN_LOOKUP)) {
			this.coll.notifyLookupInProgress(queryId);
		}
		
		//update visualization
		this.vis.notifyNewQuery(queryId, this.id);
		
		//create loockup request
		LookupRequest lr = new LookupRequest(Helper.getCurrentTick(), queryId, this.id, key);
		
		if(Helper.belongs(key, this.predecessor, false, this.id, true)) {
			//I am the node that holds the key, lookup already completed
			this.resumeLookup(lr, this.id);
		} else {			
			ArrayList<Request> currentList = this.suspendedRequests.get(queryId);
			//if not existent, initialize empty list
			if(currentList == null) {
				//initialize empty list for that queryId, insert request, update issuedRequests
				currentList = new ArrayList<Request>();
			}
			//add request at the end of the list
			currentList.add(currentList.size(), lr);
			
			//update issuedRequests
			this.suspendedRequests.put(queryId, currentList);
			
			//create and send FindSuccessorMessage (perform local call to find_successor())
			FindSuccessorMessage fsm = new FindSuccessorMessage(queryId, key);
			this.send(fsm, this.id); //call is local, the message is sent to myself
		}
	}
	
	/**
	 * Wrapper methods, tell the current node to perform a a lookup for a random id
	 */
	public void lookup() {
		//generate random key
		int key = RandomHelper.nextIntFromTo(0, Configuration.MAX_NUMBER_OF_NODES - 1);
		//call lockup
		this.lookup(key);
	}
	
	/**
	 * Resume execution of lookup, this method is called once the lookup has been completed and the result obtained
	 * @param relatedRequest
	 * @param fsId
	 */
	public void resumeLookup(LookupRequest relatedRequest, int fsId) {
		//fsId is the id of the node that holds the key we are looking for (first successor of key)
		//update visualization
		this.vis.notifyQueryCompleted(relatedRequest.getQueryId());
		
		//analysis 3: path length
		if(Analysis.isActive(AnalysisType.PATH_LENGTH)) {
			this.coll.notifyLookupOver(relatedRequest.getQueryId());
		}
		
		//analysis 5: churn rate vs lookup
		if(Analysis.isActive(AnalysisType.CHURN_LOOKUP)) {
			this.coll.notifyLookupCompleted(relatedRequest.getQueryId());
		}
	}
	
	/**
	 * Returns an array representing the clone of the successors list
	 * @return
	 */
	public int[] getSuccs() {
		return this.successors.clone();
	}
}

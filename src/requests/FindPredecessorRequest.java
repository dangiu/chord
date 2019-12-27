package requests;

import java.util.UUID;

public class FindPredecessorRequest extends Request {
	
	private int id;
	private int nPrime;

	public FindPredecessorRequest(double creationTick, UUID queryID, int requesterId, int id, int nPrime) {
		super(creationTick, queryID, requesterId, Request.RequestType.FIND_PREDECESSOR);
		this.id = id;
		this.nPrime = nPrime;
	}

	public int getId() {
		return this.id;
	}
	
	public int getNPrime() {
		return this.nPrime;
	}
	
}

package requests;

import java.util.UUID;

public class Stabilize1Request extends Request {

	private int queriedId;
	
	public Stabilize1Request(double creationTick, UUID queryID, int requesterId, int queriedId) {
		super(creationTick, queryID, requesterId, Request.RequestType.STABILIZE1);
		this.queriedId = queriedId;
	}
	
	public int getQueriedId() {
		return this.queriedId;
	}
	
}

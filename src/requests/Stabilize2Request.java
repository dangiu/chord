package requests;

import java.util.UUID;

public class Stabilize2Request extends Request {
	
	private int queriedId;

	public Stabilize2Request(double creationTick, UUID queryID, int requesterId, int queriedId) {
		super(creationTick, queryID, requesterId, Request.RequestType.STABILIZE2);
		this.queriedId = queriedId;
	}
	
	public int getQueriedId() {
		return this.queriedId;
	}

}

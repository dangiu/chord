package requests;

import java.util.UUID;

public class StabilizeRequest extends Request {

	private int queriedId;
	
	public StabilizeRequest(double creationTick, UUID queryID, int requesterId, int queriedId) {
		super(creationTick, queryID, requesterId, Request.RequestType.STABILIZE);
		this.queriedId = queriedId;
	}
	
	public int getQueriedId() {
		return this.queriedId;
	}
	
}

package requests;

import java.util.UUID;

public class FixFingerRequest extends Request {

	private int queriedId;
	
	public FixFingerRequest(double creationTick, UUID queryID, int requesterId, int queriedId) {
		super(creationTick, queryID, requesterId, Request.RequestType.FIX_FINGER);
		this.queriedId = queriedId;
	}
	
	public int getQueriedId() {
		return this.queriedId;
	}

}

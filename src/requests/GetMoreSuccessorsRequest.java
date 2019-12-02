package requests;

import java.util.UUID;

public class GetMoreSuccessorsRequest extends Request {
	
	private int queriedId;

	public GetMoreSuccessorsRequest(double creationTick, UUID queryID, int requesterId, int queriedId) {
		super(creationTick, queryID, requesterId, Request.RequestType.GET_MORE_SUCCESSORS);
		this.queriedId = queriedId;
	}
	
	public int getQueriedId() {
		return this.queriedId;
	}

}

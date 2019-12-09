package requests;

import java.util.UUID;

public class LookupRequest extends Request {
	
	private int lookedKey;

	public LookupRequest(double creationTick, UUID queryID, int requesterId, int lookedKey) {
		super(creationTick, queryID, requesterId, Request.RequestType.LOOKUP);
		this.lookedKey = lookedKey;
	}

	public int getLookedKey() {
		return this.lookedKey;
	}
	
}

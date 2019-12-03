package requests;

import java.util.UUID;

public class FixFingerRequest extends Request {

	private int fingerIndex;
	
	public FixFingerRequest(double creationTick, UUID queryID, int requesterId, int fingerIndex) {
		super(creationTick, queryID, requesterId, Request.RequestType.FIX_FINGER);
		this.fingerIndex = fingerIndex;
	}
	
	public int getFingerIndex() {
		return this.fingerIndex;
	}

}

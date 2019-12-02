package requests;

import java.util.UUID;

public class FindSuccessorRequest extends Request {

	public FindSuccessorRequest(double creationTick, UUID queryID, int requesterId) {
		super(creationTick, queryID, requesterId, Request.RequestType.FIND_SUCCESSOR);
	}

}

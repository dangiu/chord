package requests;

import java.util.UUID;

public class JoinRequest extends Request {

	public JoinRequest(double creationTick, UUID queryID, int requesterId) {
		super(creationTick, queryID, requesterId, Request.RequestType.JOIN);
	}

}

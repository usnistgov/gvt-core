package gov.nist.hit.gvt.exception;

public class NoUserFoundException extends Exception {

	private static final long serialVersionUID = 4262180673193951190L;

	public NoUserFoundException(String errorMessage) {
		super(errorMessage);
	}

	public NoUserFoundException(Exception error) {
		super(error);
	}
	
}

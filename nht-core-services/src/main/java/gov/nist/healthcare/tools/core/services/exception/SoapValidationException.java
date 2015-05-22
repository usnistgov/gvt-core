package gov.nist.healthcare.tools.core.services.exception;


public class SoapValidationException extends ValidationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SoapValidationException(String message) {
		super(message);
	}

	public SoapValidationException(RuntimeException exception) {
		super(exception);
	}

	public SoapValidationException(ValidationException e) {
		super(e);
	}
	
	public SoapValidationException(Exception e) {
		super(e);
	}
}
package gov.nist.healthcare.tools.core.services.xml;


public class XmlFormatterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public XmlFormatterException(String message) {
		super(message);
	}

	public XmlFormatterException(RuntimeException exception) {
		super(exception);
	}

	public XmlFormatterException(Exception e) {
		super(e);
	}
}

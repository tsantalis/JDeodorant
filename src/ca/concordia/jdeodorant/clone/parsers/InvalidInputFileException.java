package ca.concordia.jdeodorant.clone.parsers;

public class InvalidInputFileException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public InvalidInputFileException() {
		super();
	}

	public InvalidInputFileException(String message) {
		super(message);
	}
	
	public InvalidInputFileException(Throwable throwable) {
		super(throwable);
	}
	
}

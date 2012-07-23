package chat.server;

@SuppressWarnings("serial")
public class UnknownCommandException extends Exception {
	private String message;
	
	public UnknownCommandException(String m)
	{
		message = m;
	}
	
	public String getMessage()
	{
		return message;
	}

}

package chat;

import java.io.Serializable;

public class MessageHandler implements Serializable{
	private static final long serialVersionUID = 207524322L;
	private String timestamp;
	private String userLabel;
	private String message;

	/**
	 * Constructor for messageHandler generates a time stamp for the message
	 */
	public MessageHandler(){
		timestamp = "";
	}
	
	/**
	 * @return Timestamp of the message in the form hh:mm
	 */
	public String getTimestamp(){
		return timestamp;
	}
	
	/**
	 * @param aTimestamp The Timestamp of the message is set to the form hh:mm
	 */
	public void setTimestamp(String aTimestamp){
		timestamp = aTimestamp;
	}	
	
	/**
	 * @return The message that was sent
	 */
	public String getMessage(){
		return message;
	}
	
	/**
	 * Checks the hashmap of users to return the users current username
	 * @return
	 */
	public String getUser(){
		return userLabel;
	}
	
	/**
	 * @param aMessage The message to be set for the messageHandler
	 */
	public void setMessage(String aMessage){
		message = aMessage;
	}
	
	/**
	 * @param ip The IP address of the user
	 */
	public void setUserLabel(String label){
		userLabel = label;
	}

	/**
	 * @return True if the message Object is a command
	 */
	public boolean isCommand() {
		return message.charAt(0) == '/';
	}
	
	/**
	 * Returns a string representation of the message object
	 */
	public String toString(){
	    StringBuffer buff = new StringBuffer();
	    buff.append("("+timestamp.toString()+")");
	    buff.append(getUser()+" : ");
	    buff.append(getMessage());
	    buff.append('\n');
	    return buff.toString();
	}
}

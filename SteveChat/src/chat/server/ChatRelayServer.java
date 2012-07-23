/**
 * This package handles everything regarding the server and server side problems
 */
package chat.server;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import chat.MessageHandler;

/**
 * Server for the Chat relay system, will handle recieving and sending of messages from multiple clients simultaneously
 * @author Calvin Nefdt - 207524322	
 */
public class ChatRelayServer extends JFrame{

	private static final long serialVersionUID = 2228986130831416873L;

	//The port that the server functions on
	private static final int SERVER_PORT = 9999;
	
	//Second close forces immediate server shutdown
	boolean force = false;

	//External storage files for chat logs and error logs
	BufferedWriter errorLog = null;
	private File outputFile = new File("ChatLog.txt");
	JTextArea debugArea = new JTextArea();

	//HashMaps used to keep track of user names and associated details necessary for functionality
	private HashMap<InetAddress, String> userList = new HashMap<InetAddress, String>(10); //maps names to IP addresses
	private HashMap<InetAddress, PrintWriter> outputList = new HashMap<InetAddress,PrintWriter>(10); //maps IP's to server output
	private ArrayList<PrintWriter> serverOutput = new ArrayList<PrintWriter>();
	private int counter = 0;
	ServerSocket ss;

	/**
	 * Constructor for the Server that implements all files for storing data and opens the Socket for the server to
	 * handle clients, as well as a small window for debugging purposes
	 * @throws IOException If for some reason the chat or error logs could not be handled properly
	 */
	public ChatRelayServer(){
		super("Server debug");
		boolean listening = true;
		add(new JScrollPane(debugArea));
		debugArea.setEditable(false);
		setSize(300, 300);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setVisible(true);

		//Add specific functionality to the server Window for shuting down
		this.addWindowListener(new WindowListener(){
			@Override
			public void windowActivated(WindowEvent arg0){}

			//Override the close event
			@Override
			public void windowClosed(WindowEvent arg0) {
				try {
					int result = JOptionPane.showConfirmDialog(null, "Immediate shutdown? (Saying no will shut the server down in 30s");
					
					if (result == JOptionPane.CANCEL_OPTION)
						return;
					debug("Server received shutdown request, stopping all functionality");
					if (result == JOptionPane.YES_OPTION)  
						shutdown(0, true);
					else if (result == JOptionPane.NO_OPTION)
						shutdown(30, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				try {
					int result = JOptionPane.showConfirmDialog(null, "Immediate shutdown? (Saying no will shut the server down in 30s");
					
					if (result == JOptionPane.CANCEL_OPTION)
						return;
					debug("Server received shutdown request, stopping all functionality");
					if (result == JOptionPane.YES_OPTION)  
						shutdown(0, true);
					else if (result == JOptionPane.NO_OPTION)
						shutdown(30, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void windowDeactivated(WindowEvent arg0){}

			@Override
			public void windowDeiconified(WindowEvent arg0) {}

			@Override
			public void windowIconified(WindowEvent arg0) {}

			@Override
			public void windowOpened(WindowEvent arg0) {}
		});

		try{
			//Create serversocket for listening to clients and listen until terminated
			ss = new ServerSocket(SERVER_PORT);
			
			debug("Server started (" + ss.getInetAddress() + ")");
			debug("^ 0.0.0.0 = listening on every available interface");
			while(listening){
				new ChatRelayServerThread(ss.accept()).start();
			}
			ss.close();
		}
		catch(IOException e){
			debug("Error starting server. Ensure that the server directory has write priveleges and that the port("+ SERVER_PORT +") is not in use");
		}
		
	}

	/**
	 * Used for debugging purposes, outputs the passes string as a message on the server window
	 * @param m The message to output
	 */
	void debug(String m)
	{
		debugArea.setText(debugArea.getText() + m + "\n");
	}
	
	/**
	 * Broadcast message to all clients connected to this server
	 * @param m The message to be broadcast
	 */
	void broadcastMessage(String m)
	{
		for(int i=0; i<serverOutput.size(); i++){
			serverOutput.get(i).println(m);
			serverOutput.get(i).flush();
		}
	}

	/**
	 * Used to shutdown the server
	 * @param sec The amount of time before a shutdown occurs
	 * @throws IOException If the server socket can not be found to close 
	 */
	private void shutdown(int sec, boolean force) throws IOException {
		if (!force)
		{
			force = true; //Next shutdown attempt will force the server to close immediately
			broadcastMessage("Server has received a shutdown request and will shut down in 30 seconds.");
			int seconds = sec+1;
			while (seconds > 1)
			{
				seconds--;
				try {
					Thread.sleep(1000);
					if (seconds % 5 == 0)
						broadcastMessage(seconds + "s until server shutdown");
				} catch (InterruptedException e) {
					broadcastMessage("Server closed.");
					broadcastMessage("/disconnect");
					System.exit(0);
				}
			}
		}
		broadcastMessage("Server closed.");
		broadcastMessage("/disconnect");
		if (ss != null)
			ss.close();
		System.exit(0);
	}

	/**
	 * Main Method for running the server
	 */
	public static void main(String[] args) {
		new ChatRelayServer();
	}

	//==============================================================================================================================================
	/**
	 * Class used to handle multiple threads of incoming users
	 * @author Calvin Nefdt - 207524322
	 */
	public class ChatRelayServerThread extends Thread {
		private Socket clientConnection = null;
		private ObjectInputStream clientInput = null;
		private BufferedWriter chatWriter = null;
		private PrintWriter clientSpecificOutput = null;
		String clientName;

		/**
		 * Constructor implements all connections and input or output streams
		 * @param socket The socket that the client has connected through
		 */
		public ChatRelayServerThread(Socket socket){
			try{
				clientConnection = socket;
				clientInput = new ObjectInputStream(clientConnection.getInputStream());
				chatWriter = new BufferedWriter(new FileWriter(outputFile, true));
				clientSpecificOutput = new PrintWriter(clientConnection.getOutputStream(), true);
				serverOutput.add(clientSpecificOutput);
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}

		/**
		 * Runs each thread for each socket that connects to the server and monitors for incoming messages
		 * kills the connection when the client disconnects
		 */
		public void run(){
			MessageHandler message;
			try {
				while((message = (MessageHandler)clientInput.readObject()) != null){
					message.setTimestamp((new SimpleDateFormat ("HH:mm")).format(new Date()));
					message.setUserLabel(userList.get(clientConnection.getInetAddress()));
					if(message.isCommand()){
						try {
							handleCommand(message.getMessage());
						} catch (UnknownCommandException e) {
							clientSpecificOutput.println(e.getMessage());
						}
						catch (ArrayIndexOutOfBoundsException e) {
							try
							{
							clientSpecificOutput.println("Missing paramater for command " + message.getMessage().substring(1, message.getMessage().indexOf(" ")));
							}
							catch (StringIndexOutOfBoundsException e2)
							{
								clientSpecificOutput.println("Missing paramater for command " + message.getMessage().substring(1, message.getMessage().length()));
							}
						}
					}
					else{
						String tempMes = message.toString();
						broadcastMessage(tempMes);
						chatWriter.write(message.toString());
						chatWriter.flush();
					}
				}
				killConnection();

			} catch (IOException e) {
				//client disconnected
				String temp = clientName + " has disconnected from the server";
				debug(clientName + " disconnected");
				userList.remove(clientName);
				serverOutput.remove(clientSpecificOutput);
				broadcastMessage(temp);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Will carry out commands passed through to the server from the Client
		 * @throws IOException 
		 */
		private void handleCommand(String command) throws IOException, UnknownCommandException, ArrayIndexOutOfBoundsException {
			//For user connected
			
			//if (command.equals("/updatelist") || command.equals("/join"))
				//throw new UnrecognisedCommandException
			
			if (command.startsWith("/connect"))
			{
				clientName = command.split(" ")[1];
				
				//clientName has to be unique
				boolean nameExists = false;
				
				for (String name: userList.values())
				{
					if (clientName.equals(name))
						nameExists = true;
				}
				
				if (nameExists)
				{
					clientName = clientName.concat("_clone");
					clientSpecificOutput.println("Your username is already in use and has been changed to " + clientName);
					clientSpecificOutput.println("/nick " + clientName);
				}
				
				//Add user to current user list and notify all clients as necessary and store info in the chatlog
				userList.put(clientConnection.getInetAddress(), clientName);
				outputList.put(clientConnection.getInetAddress(), clientSpecificOutput);
				String temp = clientName + " has connected to the server from " + clientConnection.getInetAddress().toString();
				debug(clientName + " connected");
				//send join command back to client
				clientSpecificOutput.println("/join " + clientName);
				broadcastMessage(temp);
				
				chatWriter.write(temp+"\n");
				chatWriter.flush();

				sendUserList();

			}

			//User disconnected
			else if (command.equals("/disconnect"))
				killConnection();

			//User wants to change nickname
			else if (command.startsWith("/nick"))
			{
				String newName = command.split(" ")[1];
				
				for (String name: userList.values())
				{
					if (newName.equals(name))
						throw new UnknownCommandException("Username already in use!");
				}
				
				InetAddress tempAddress = clientConnection.getInetAddress();
				userList.remove(clientName);
				userList.put(tempAddress, newName);

				debug(clientName + " changed name to " + newName);

				String temp = clientName + " is now known as " + newName;
				clientName = newName;
				clientSpecificOutput.println("/nick " + newName);

				broadcastMessage(temp);

				sendUserList();

				chatWriter.write(temp.toString() + "\n");
				chatWriter.flush();
			}
			
			//User query of another user
			else if (command.startsWith("/whois"))
			{
				String queryName = command.split(" ")[1];
								
				if(userList.containsValue(queryName)){					
					
					InetAddress targetAddress = null;
					
					for (Entry<InetAddress, String> entry : userList.entrySet()) {
				        if (queryName.equals(entry.getValue())) {
				            targetAddress = entry.getKey();
				        }
				    }					
					
					clientSpecificOutput.println(queryName + ":  " + targetAddress.toString());				
				}
				
				else
					clientSpecificOutput.println("The user is not present in the channel");
				
				chatWriter.flush();
			}
			
			//displays a list of commands and what they do
			else if(command.equals("/cmdlist")){
				clientSpecificOutput.println(    " /disconnect - disconnect from server\n"
												+" /whois <NICK> - obtain client IP for <NICK>\n"
												+" /msg <NICK> <MESSAGE>- send private messages to <NICK>\n"
												+" /nick <NAME> - change nick to <NAME>\n"
												+" /me <EMOTE> - emote a message that will start with your name\n"
												+" /slap <NAME> - perform the slap emote\n"
												+" /help - opens readme.txt");
				
				chatWriter.flush();				
			}
			
			//Open the readme.txt
			else if(command.startsWith("/help")){
				try {
					Runtime.getRuntime().exec("cmd /c start Notepad.exe readme.txt");
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Unable to open notepad");
				}
			}
			
			//Performs slap emote
			else if(command.startsWith("/slap")){
				String[] split = command.split(" ");
				String targetUsername = split[1];
				String message = "";
				
				if(userList.containsValue(targetUsername)==true){				
					 message = "#  " + clientName + " slaps " + targetUsername + " around a bit with a large trout";
				
					 for(int i=0; i<serverOutput.size(); i++){
						serverOutput.get(i).println(message);
						serverOutput.get(i).flush();
					}
				}
				
				else{
					clientSpecificOutput.println("That user does not exist");
				}
				
				chatWriter.flush();
			}
			
			//Send a private message to a user
			else if (command.startsWith("/msg"))
			{
				String[] split = command.split(" ");
				String targetUsername = split[1];
				
				if (targetUsername.equals(clientName))
				{
					clientSpecificOutput.println("Cannot whisper to yourself!");
					return;
				}
				
				if (split.length < 3)
					throw new ArrayIndexOutOfBoundsException(); //no message
				
				StringBuilder message = new StringBuilder();
				for (int i = 2; i < split.length; i++)
					message.append(split[i] + " ");
				
				InetAddress targetAddress = null;
				
				for (Entry<InetAddress, String> entry : userList.entrySet()) {
			        if (targetUsername.equals(entry.getValue())) {
			            targetAddress = entry.getKey();
			        }
			    }
				
				if (targetAddress == null)
					throw new UnknownCommandException("User " + targetUsername + " not connected to server.");
				
				clientSpecificOutput.println("You whisper to " + targetUsername + ": " + message.toString());
				
				outputList.get(targetAddress).println(clientName + " whispers : " + message.toString());

				String temp = clientName + "->" + targetUsername + " : " + message.toString();
				chatWriter.write(temp.toString() + "\n");
				chatWriter.flush();
				
				debug(temp);
			}
			
			//Show details about the user who used the command
			else if (command.startsWith("/me")){				
				String[] split = command.split(" ");
				if (split.length < 2)
					throw new ArrayIndexOutOfBoundsException(); //no argument
				StringBuilder message = new StringBuilder();
				
				message.append("#  "+clientName+" ");
				
				for (int i = 1; i < split.length; i++)
					message.append(split[i] + " ");
				
				for(int i=0; i<serverOutput.size(); i++){
					serverOutput.get(i).println(message);
					serverOutput.get(i).flush();
				}			
			}
			
			//For anything else the command will be unrecognised
			else
				throw new UnknownCommandException("Unrecognised command \"" + command.substring(1, command.length()) + "\"");
		}

		/**
		 * Method to send the list of currently connected users
		 */
		private void sendUserList() {
			String list = "";

			ArrayList<String> list_ = new ArrayList<String>(userList.values());

			list = list.concat(list_.get(0));
			for (int i = 1; i < userList.size(); i++)
			{
				list = list.concat("," + list_.get(i));
			}

			broadcastMessage("/updatelist " + list);
			
		}

		/**
		 * Method to close all input and output streams as well as the socket
		 */
		private void killConnection() throws IOException{
			String temp = userList.get(clientConnection.getInetAddress())+" has disconnected from the server";
			userList.remove(clientConnection.getInetAddress());
			counter--;
			serverOutput.remove(clientSpecificOutput);
			outputList.remove(clientConnection.getInetAddress());
			broadcastMessage(temp);
			chatWriter.write(temp+"\n");
			chatWriter.flush();
			
			debug(clientName + " disconnected");

			clientSpecificOutput.println("/disconnect");

			clientConnection.close();
			clientInput.close();
			chatWriter.close();
			clientSpecificOutput.close();
		}
	}

}
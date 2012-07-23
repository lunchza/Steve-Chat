/**
 * Steve Chat Client v0.1a
 * @author - Peter Pretorius
 * 
 */
package chat.client;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

import chat.MessageHandler;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client extends JFrame implements ActionListener
{

	private static final long serialVersionUID = -5696664560294113917L;

	//server port (defaults to 9999)
	private int server_port = 9999;

	//server IP (defaults to 127.0.0.1)
	private String server_ip = "127.0.0.1";

	//The socket that connects to the server
	private Socket logConnection = null;
	
	//dimensions of the frame, used to resize the image
	int frameWidth, frameHeight;

	private ObjectOutputStream outputStream = null;
	private BufferedReader inputStream = null;
	private ReceiveMessages receiveMessages = null;
	
	//Stores all past client messages in a clipboard
	private UnlimitedClipboard messageHistory;

	//The regular expression for validating IP addresses
	private static final String IPADDRESS_PATTERN = 
		"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	//The client-side list of users connected to the server
	private ArrayList<String> userList;

	//program version
	final static String VERSION = "v0.1b";

	//This client's name. Defaults to Steve
	String clientName = "Steve";

	//determines whether or not this user is connected to the server
	private boolean connected;

	//gui declarations
	JMenuBar menuBar;
	
	JMenuItem configure_menu;
	JMenuItem exit_menu;
	JMenuItem help_menu;
	JMenuItem about_menu;
	JMenuItem connect_menu;
	
	JTextArea messageArea;
	JTextField messageField;
	JButton sendButton;
	JTextArea usersArea;

	//configure window GUI components
	JFrame configureFrame;
	JTextField nameField;
	JTextField IPField;
	JTextField portField;
	JButton okButton;
	JButton cancelButton;
	JButton connectButton;
	

	@SuppressWarnings("serial")
	public Client()
	{
		super("Steve Chat Client " + VERSION);
		userList = new ArrayList<String>();
		messageHistory = new UnlimitedClipboard();

		//gui initialisations
		messageArea = new JTextArea();
		messageArea.setBackground(new Color(235, 235, 235));
		messageField = new JTextField();
		messageField.setFocusTraversalKeysEnabled(false); //needed for TAB name completion
		sendButton = new JButton("Send");
		connectButton = new JButton("Connect");
		
		//chat side 
		usersArea =new JTextArea();
		usersArea.setBackground(new Color(235, 235, 235));
		
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");
		
		configure_menu  = new JMenuItem("Configure");
		connect_menu = new JMenuItem("Connect");
		exit_menu  = new JMenuItem("Exit");
		help_menu = new JMenuItem("Help");
		about_menu = new JMenuItem("About");
		
		//send panel bottom
		JPanel sendPanel = new JPanel();

		//gui component properties
		messageArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		messageArea.setEditable(false);
				
		//Set auto scrolling for the message area
		DefaultCaret caret = (DefaultCaret)messageArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		messageField.setPreferredSize(new Dimension(800, 25));
		usersArea.setPreferredSize(new Dimension(150, 800));
		usersArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		usersArea.setEditable(false);
		
		setJMenuBar(menuBar);
		
		//GrachicsEnvironment.getMaximumWindowBounds is used instead of ToolKit to
		//accommodate the height of the start menu (if one is present)
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();  
		Rectangle gebounds = ge.getMaximumWindowBounds();
		frameWidth = gebounds.width;
		frameHeight = gebounds.height;
		setSize(gebounds.getSize());
		
		
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		setLayout(new BorderLayout());
		
		sendPanel.add(connectButton);
		sendPanel.add(messageField);
		sendPanel.add(sendButton);

		//adding menu components
		fileMenu.add(connect_menu);
		fileMenu.add(configure_menu);
		fileMenu.add(exit_menu);

		helpMenu.add(help_menu);
		helpMenu.add(about_menu);

		menuBar.add(fileMenu);
		menuBar.add(helpMenu);

		//adding gui components to the frame
		add(new JScrollPane(messageArea), BorderLayout.CENTER);
		add(usersArea, BorderLayout.EAST);
		add(sendPanel, BorderLayout.SOUTH);
		
		//Allows the use of up to revisit past messages using the clipboard
		Action previousMessage = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		    	String previousMSG = messageHistory.undo();
				if (previousMSG != null)
					messageField.setText(previousMSG);
		    }
		};
		
		//Allows the use of down to undo the last clipboard retrieval
		Action nextMessage = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		    	String nextMSG = messageHistory.redo();
				if (nextMSG != null)
					messageField.setText(nextMSG);
				else
					messageField.setText("");
		    }
		};
		
		//Allows the use of TAB to do tab completion on names
		Action tabComplete = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		    	String curText = messageField.getText();
		    	
		    	//non-empty user-list, non-empty message
		    	if (userList.size() == 0 || curText == null || curText == " ")
		    		return;
		    	
		    	String partialName = null;
		    	
		    	//name entered as part of a command or mid-sentence
		    	if (curText.contains(" "))
		    		partialName = curText.substring(curText.lastIndexOf(" ")+1, curText.length());
		    	
		    	//message starts with name
		    	else
		    		partialName = curText;
		    	
		    	//Only partial names longer than 2 letters will be considered
		    	if (partialName.length() < 3)
		    		return;
		    	
		    	for (String name: userList)
		    	{
		    		String tempName = name; //preserve case
		    		if (name.toLowerCase().startsWith(partialName.toLowerCase()))
		    		{
		    			//complete name
		    			messageField.setText(curText.substring(0, curText.lastIndexOf(" ")+1).concat(tempName + " "));
		    			break;
		    		}
		    	}
		    }
		};
		
		/*
		 * Add the above 3 actions to the message field
		 */
		messageField.getInputMap().put(KeyStroke.getKeyStroke("UP"),
        "Go to previous message");
		messageField.getActionMap().put("Go to previous message",
         previousMessage);
		
		messageField.getInputMap().put(KeyStroke.getKeyStroke("DOWN"),
        "Go to next message");
		messageField.getActionMap().put("Go to next message",
         nextMessage);
		
		messageField.getInputMap().put(KeyStroke.getKeyStroke("TAB"),
        "Complete name");
		messageField.getActionMap().put("Complete name",
         tabComplete);
		
		//event listeners
		messageField.addKeyListener
		(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				//simulate pressing the send button
				//when enter is pressed
				if (key == KeyEvent.VK_ENTER) {  
					actionPerformed(new ActionEvent(sendButton, 20, null));				
				}
			}			
		}
		);
		
		//set up menu action listeners
		sendButton.addActionListener(this);
		connectButton.addActionListener(this);
		connect_menu.addActionListener(this);
		configure_menu.addActionListener(this);
		exit_menu.addActionListener(this);
		help_menu.addActionListener(this);
		about_menu.addActionListener(this);

		setVisible(true);
		messageField.requestFocus();
		this.setIconImage(Toolkit.getDefaultToolkit().getImage("Picture/SC.jpg"));
		
	}

	public static void main(String[] a)
	{
		new Client();
	}
	
	public void actionPerformed(ActionEvent evt)
	{
		//User clicked on connect
		if (evt.getSource() == connect_menu)
		{
			
			//If not already connected
			if (!connected)
			{
				//connection runs in its own thread to avoid locking up the client
				new Thread()
				{
					public void run()
					{
						//Will attempt to connect to the server 3 times
						int count = 0;
						while(count < 3)
						{
							String retryText = (count == 0) ? "" : "(Retry # " + count + ")";
							appendMessage("Attempting to connect to server"+ retryText);
							try {
								//establish connection to server
								logConnection = new Socket(server_ip, server_port);
								
								outputStream = new ObjectOutputStream(logConnection.getOutputStream());
								inputStream = new BufferedReader(new InputStreamReader(logConnection.getInputStream()));
								receiveMessages = new ReceiveMessages();
								//start receiving messages
								receiveMessages.start();
								//set connected flag
								connected = true;
								//breaks out of the while loop after this iteration
								count = 99;
								
								//The following lines of code send a "/connect" message to the server, which
								//lets the server know the name of this client
								String tempBuffer = messageField.getText();
								messageField.setText("/connect " + clientName);
								actionPerformed(new ActionEvent(sendButton, 20, null));
								messageHistory.removeLast(); //user doesn't need to see the /connect
								messageField.setText(tempBuffer);

								//get user list from server
								updateUserList();

								//All instances of "connect" change to "disconnect"
								connect_menu.setText("Disconnect");
								connectButton.setText("Disconnect");
								messageField.requestFocus();
							} catch (UnknownHostException e1) {
								appendMessage("Server refused connection\n");
							} catch (IOException e1) {
								appendMessage("Unable to find server at " + server_ip + ":" + server_port + "\n");
							}
							count++;
						}
						
						//connection attempt timeout
						if (count == 3)
							appendMessage("Connection timed out.");
					}
				}.start();
			}

			else
			{
				disconnect();
			}
		}

		if (evt.getSource() == configure_menu)
		{
			//show configuration window
			if (configureFrame == null)
				showConfigureGUI();
		}

		if (evt.getSource() == exit_menu)
		{
			//Disconnect from server and close client
			disconnect();
			System.exit(0);
		}

		if (evt.getSource() == help_menu)
		{
			//Display readme file in notepad
			try {
				Runtime.getRuntime().exec("cmd /c start Notepad.exe readme.txt");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Unable to open notepad");
			}
		}

		if (evt.getSource() == about_menu)
		{
			//about
			JOptionPane.showMessageDialog(null, "Steve chat " + VERSION + "\nWritten by Calvin Nefdt, Aimee Booysens, Peter Pretorius, Darren Osterloh and Tristan Goring\nComp313 project 2012");
		}

		//send message to server
		if (evt.getSource() == sendButton)
		{
			String input = messageField.getText();

			//don't accept empty messages
			if (input.length() < 1)
				return;
			
			messageHistory.update(input);

			//server down
			if (connected && logConnection.isClosed())
			{
				disconnect();
			}

			//send message via the message handler
			if (connected)
			{
				MessageHandler message = new MessageHandler();
					try{
						message.setMessage(input);
						message.setUserLabel(logConnection.getLocalAddress().toString());
						outputStream.writeObject(message);
						outputStream.flush();
						outputStream.reset();
					}
					catch(IOException e1){
						e1.printStackTrace();
					}
				messageField.setText("");
			}

			else 
			{
				if (isCommand(input))
				{
					//connect to the server if the user types /connect
					if (input.equals("/connect"))
						actionPerformed(new ActionEvent(connect_menu, 20, null));

					else
						appendMessage("Not connected to server!");
				}
				else
					appendMessage("Not connected to server!");

				messageField.setText("");
			}
		}
		
		if (evt.getSource() == connectButton){			
			//connecting via the menu or button does the exact same thing
			actionPerformed(new ActionEvent(connect_menu, 20, null));
	}

		if (evt.getSource() == okButton)
		{
			if (!connected)
			{
				String nameText = nameField.getText();
				String ipText = IPField.getText();
				ipText = ipText.trim();
				String portText = portField.getText();

				//IP address validation using regular expressions
				Pattern ipPattern = Pattern.compile(IPADDRESS_PATTERN);
				Matcher matcher = ipPattern.matcher(ipText);
				if(!matcher.matches())
				{
					JOptionPane.showMessageDialog(null, "Invalid IP address!");
					return;
				}

				//check for a number-only port
				try
				{
					server_port = Integer.parseInt(portText);
					if (server_port < 0)
					{
						throw new NumberFormatException();
					}
				}
				catch (NumberFormatException e)
				{
					JOptionPane.showMessageDialog(null, "Invalid Port number");
					server_port = 9999;
					return;
				}

				clientName = nameText;
				server_ip = ipText;

				configureFrame.dispose();
				configureFrame = null;
			}

			else
				JOptionPane.showMessageDialog(null, "Can't change settings while connected to server");
		}

		//cancel button clicked
		if (evt.getSource() == cancelButton)
		{
			configureFrame.dispose();
			configureFrame = null;
		}
	}

	/**
	 * Disconnects this client from the server
	 * @throws IOException 
	 */
	private void disconnect() {
		connected = false;
		connect_menu.setText("Connect");
		connectButton.setText("Connect");
		appendMessage("Disconnected from server");
		userList.clear();
		updateUserList();
		
		try
		{
			logConnection.close();
			outputStream.close();
			inputStream.close();
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	//append a message onto the message area
	private void appendMessage(String m)
	{
		messageArea.setText(messageArea.getText() + m + "\n");
	}

	//update user text area
	private void updateUserList() {
		usersArea.setText("");
		for (String user: userList)
		{
			usersArea.setText(usersArea.getText() + user + "\n");
		}		
	}

	//Determinies if a String m is a command
	private boolean isCommand(String m)
	{
		return m.charAt(0) == '/';
	}

	private void showConfigureGUI()
	{
		//Generated by JGuiMaker v1.2b
		//JGuiMaker written by Peter Pretorius
		configureFrame = new JFrame("Configuration");
		configureFrame.setSize(300,330);
		configureFrame.setLocationRelativeTo(null);
		configureFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		configureFrame.setResizable(false);
		configureFrame.setLayout(null);
		JLabel nameLabel = new JLabel("Your name:");
		JLabel IPlabel = new JLabel("Server IP:");
		JLabel portLabel = new JLabel("Server Port:");
		nameField = new JTextField(clientName);
		IPField = new JTextField(server_ip);
		portField = new JTextField(""+server_port);
		okButton = new JButton("ok");
		cancelButton = new JButton("cancel");

		//gui component properties
		nameLabel.setBounds(41, 38, 64, 25);
		nameField.setBounds(129, 38, 150, 25);
		IPlabel.setBounds(41, 91, 55, 25);
		IPField.setBounds(129, 91, 127, 25);
		portLabel.setBounds(41, 145, 68, 25);
		portField.setBounds(129, 140, 50, 25);
		okButton.setBounds(46, 230, 75, 50);
		cancelButton.setBounds(184, 230, 75, 50);

		configureFrame.add(nameLabel);
		configureFrame.add(nameField);
		configureFrame.add(IPlabel);
		configureFrame.add(IPField);
		configureFrame.add(portLabel);
		configureFrame.add(portField);
		configureFrame.add(okButton);
		configureFrame.add(cancelButton);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		configureFrame.setVisible(true);
		configureFrame.requestFocus();
	}

	//This thread runs in the background and waits for commands from the server
	private class ReceiveMessages extends Thread {

		public void run(){
			String inputLine;
			try {
				while((inputLine = inputStream.readLine()) != null) {
					//This is a hack. TODO: Find out why the server is sending a blank message
					if (inputLine.equals(""))
						continue;

					//server sent command directive
					if (isCommand(inputLine))
					{
						//join command
						if (inputLine.startsWith("/join"))
						{
							String username = inputLine.split(" ")[1];
							userList.add(username);
							updateUserList();
						}

						//received disconnect command from server
						if (inputLine.startsWith("/disconnect"))
						{
							disconnect();
						}

						//server accepted nick change, change nick client-side
						if (inputLine.startsWith("/nick"))
						{
							String newName = inputLine.split(" ")[1];
							userList.remove(clientName);
							clientName = newName;
							userList.add(clientName);
							updateUserList();
						}

						//server is sending updated userlist
						if (inputLine.startsWith("/updatelist"))
						{
							String list = inputLine.split(" ")[1];
							userList.clear();
							String[] names = list.split(",");

							for (int i = 0; i < names.length; i++)
								userList.add(names[i]);

							updateUserList();
						}
					}
					//server broadcasted something
					else
						appendMessage(inputLine);
				}

				logConnection.close();
				outputStream.close();
				inputStream.close();

			} catch (IOException e) {

			}
		}
	}
}
/*
@SuppressWarnings("serial")
private class ContentPanel extends JTextArea {
	  Image image = null;
	  ImageIcon backgroundImage = null;

	  public ContentPanel(String s) {
		  backgroundImage = new ImageIcon(s); 		  
		  image = backgroundImage.getImage();
	  }

	  public void paint(Graphics g)  
	  {  
	   setOpaque(false);
	   g.drawImage(image,0,0,this);  
	   super.paint(g);  
	  }
	}
}*/
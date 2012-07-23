/**
 * Written by Peter Pretorius
 * Unlimited for now (why not?) but possibly could change with a maximum limit, after
 * which the first half of the clipboard is discarded.
 */
package chat.client;

import java.util.ArrayList;

public class UnlimitedClipboard {

	//clipboard is an arraylist of strings
	ArrayList<String> clipboard;
	
	//where we are in the clipboard
	int position;
	
	//initialise
	public UnlimitedClipboard()
	{
		position = 0;
		clipboard = new ArrayList<String>();
	}
	
	//new clipboard entry
	public void update(String s)
	{
		clipboard.add(s);
		position = clipboard.size();
	}
	
	//returns the command at current position - 1
	public String undo()
	{
		if (position == 0)
			return null;
		/*
		if(position == 1)
		{
			String first = clipboard.get(0);
			clipboard = new ArrayList<String>();
			return first;
		}*/
		return clipboard.get(--position);
	}
	
	//trim the last added command
	public void removeLast()
	{
		if (clipboard.size() != 0)
			clipboard.remove(clipboard.size()-1);
	}
	
	//determines if this clipboard is empty
	public boolean isEmpty()
	{
		return clipboard.isEmpty();
	}
	
	//returns the command at position + 1
	public String redo()
	{
		if(position < clipboard.size()-1)
		{
			return clipboard.get(++position);
		}
		
		else return null;
	}
	
}

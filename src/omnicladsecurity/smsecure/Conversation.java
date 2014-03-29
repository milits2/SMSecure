package omnicladsecurity.smsecure;

import java.io.UnsupportedEncodingException;

public class Conversation {
	// The Conversation class contains the pertinent information for a conversation.
	// They are loaded upon opening a conversation and populate themselves.
	OneTimePad localPad, contactPad;
	String contactNumber;
	
	public Conversation(String withNumber) {
		contactNumber = withNumber;
		
		// Load the one-time pads from memory
		// TODO
	}
	
	public String readMessage(String message) {
		// Encrypted messages will be of the form |~|offset|message
		if(message.startsWith("|~|")) {
			// Find the offset.
			int offset;
			String[] components = message.split("|");
			if(components.length != 3) {
				return "";
			}
			try {
				offset = Integer.parseInt(components[1]);
			} catch(NumberFormatException e) {
				return "";
			}
			
			// Decrypt the message.
			contactPad.setOffset(offset);
			return contactPad.decrypt(components[2].getBytes());
		}
		// If it doesn't have the prefix, skip the message.
		return "";
	}
	
	public String sendMessage(String message) {
		// Encrypted messages will be of the form |~|offset|message
		String prefix = "|~|" + localPad.getOffset() + "|";
		String suffix;
		try {
			suffix = new String(localPad.encrypt(message), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			suffix = "";
		}
		return prefix + suffix;
	}
}

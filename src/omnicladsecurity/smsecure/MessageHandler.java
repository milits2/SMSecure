package omnicladsecurity.smsecure;

import java.io.UnsupportedEncodingException;

public class MessageHandler {
	OneTimePad localPad, contactPad;
	
	public MessageHandler(String conversationNumber) {
		// Load the pads for the associated conversation.
		// TODO actually load the pads
	}
	
	void setLocalPad(OneTimePad newPad) {
		// TODO
	}
	
	OneTimePad getLocalPad() {
		return localPad;
	}
	
	void setContactPad(OneTimePad newPad) {
		// TODO
	}
	
	OneTimePad getContactPad() {
		return contactPad;
	}
	
	public String decryptText(String message) {
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
	
	public String encryptText(String message) {
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

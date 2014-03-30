package omnicladsecurity.smsecure;

import android.content.Context;

public class MessageHandler {
	OneTimePad localPad, contactPad;
	Context context;
	
	public MessageHandler(Context context, String conversationNumber) {
		// Load the pads for the associated conversation.
		// TODO actually load the pads
		localPad = new OneTimePad(8000);
		contactPad = new OneTimePad(8000);
		
		this.context = context;
	}
	
	void setLocalPad(OneTimePad newPad) {
		// TODO
	}
	
	OneTimePad getLocalPad() {
		return localPad;
	}
	
	void setContactPad(OneTimePad newPad) {
		// Loads a pad from an SD card.
		// TODO
	}
	
	OneTimePad getContactPad() {
		return contactPad;
	}
	
	public boolean canSendMessage(String message) {
		return (localPad.offset + message.length()) < localPad.pad.length;
	}
	
	public String encryptText(String message) {
		// Encrypted messages will be of the form |~|offset|message
		String prefix = "|~|" + localPad.getOffset() + "|";
		String suffix = localPad.encrypt(message);
		return prefix + suffix;
	}
	
	public String decryptText(String message) {
		// Encrypted messages will be of the form |~|offset|message
		if(message.startsWith("|~|")) {
			// Find the offset/verify that it's properly formed.
			int offset;
			String[] components = message.split("\\|");
			if(components.length < 4) {
				return message;
			}
			try {
				offset = Integer.parseInt(components[2]);
			} catch(NumberFormatException e) {
				return message;
			}
			
			// Decrypt the message.
			contactPad.setOffset(offset);
			return "[S]" + contactPad.decrypt(components[3].toCharArray());
		}
		// If it doesn't have the prefix, skip the message.
		return message;
	}
}

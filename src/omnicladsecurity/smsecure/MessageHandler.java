package omnicladsecurity.smsecure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

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
	

public void storeNumbers(String[] numbers) {
	String filename = "conversations.txt";
	File numberList = new File(context.getFilesDir(), filename);


	try {
    	OutputStream os = new FileOutputStream(numberList);
		for(String number: numbers) {
			os.write(number.getBytes());
		}
        os.close();
	}    catch (IOException e) {
        // Unable to create file for unknown reasons, seriously I'm doing
		//like the exact same thing as jake argh
		//except context, apparently he didn't need to do that because external memory
        Log.w("ExternalStorage", "Error writing " + numberList, e);
   	}
  }

public void storeOffest(String number, int offset) {
	String filename = "pad.txt";
	File path = new File(context.getFilesDir()+"/"+number);
	path.mkdir();  //make it and/or make sure it exists
	File offsetFile = new File(context.getFilesDir()+"/"+number, filename);
	String writeOffset = ( (Integer) offset).toString();
	
	try {
    	OutputStream os = new FileOutputStream(offsetFile);
    	os.write(writeOffset.getBytes());
        os.close();
	}    catch (IOException e) {
        // Unable to create file
        Log.w("ExternalStorage", "Error writing " + offsetFile, e);
   	}
  }

public void storePad(String number) {
	String filename = "pad.txt";
	File path = new File(context.getFilesDir()+"/"+number);
	path.mkdir();  //make it and/or make sure it exists
	File padFile = new File(context.getFilesDir()+"/"+number, filename);
	OneTimePad padData;
	padData = new OneTimePad(1024);
	
	try {
    	OutputStream os = new FileOutputStream(padFile);
    	os.write(new String(padData.pad).getBytes());
        os.close();
	}    catch (IOException e) {
        // Unable to create file
        Log.w("ExternalStorage", "Error writing " + padFile, e);
   	}
  }

public File retrievePad(String number) {
	File path = new File(context.getFilesDir()+"/"+number);
	File pad = new File(path, "pad.txt");
	
	return pad;
  }

}

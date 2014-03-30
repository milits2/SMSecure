package omnicladsecurity.smsecure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MessageHandler {
	OneTimePad localPad, contactPad;
	Context context;
	String contactNumber;
	
	public MessageHandler(Context context, String conversationNumber) {
		this.contactNumber = conversationNumber;
		this.context = context;
		// Pads will be null if they aren't populated yet
		localPad = loadPadByName("local");
		contactPad = loadPadByName("contact");
	}
	
	void setLocalPad(OneTimePad pad) {
		localPad = pad;
		if(localPad != null) {
			storePadByName(localPad, "local");
		}
	}
	
	OneTimePad getLocalPad() {
		return localPad;
	}
	
	void setContactPad(OneTimePad pad) {
		contactPad = pad;
		if(contactPad != null) {
			storePadByName(localPad, "contact");
		}
	}
	
	void storePadByName(OneTimePad pad, String name) {
		String routeName = context.getFilesDir() + "/" + contactNumber;
		File path = new File(routeName);
		path.mkdir();
		
		String fileName = name + "Pad.dat";
		File padFile = new File(routeName, fileName);
		
		if(padFile.exists()) {
			padFile.delete();
		}
		
		// Store the pad.pad contents in the file.
		String padContents = new String(pad.pad);
		
		try {
			FileWriter writer = new FileWriter(padFile);
			writer.write(padContents);
			writer.close();
		} catch(IOException e) {
			Log.w("InternalStorage", "Error writing to " + routeName + "/" + fileName, e);
		}
		
		// Store the offset as a preference.		
		SharedPreferences prefs = context.getSharedPreferences("padOffsets", Context.MODE_PRIVATE);
		SharedPreferences.Editor writer = prefs.edit();
		
		String key = name + "Offset" + contactNumber;
		writer.putInt(key, pad.offset);
		writer.commit();
	}
	
	OneTimePad loadPadByName(String name) {
		String routeName = context.getFilesDir() + "/" + contactNumber;
		String fileName = name + "Pad.dat";
		File padFile = new File(routeName, fileName);
		
		if(!padFile.exists()) return null;

		String padContents = "";
		try {
			FileInputStream streamer = new FileInputStream(padFile);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(streamer));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			reader.close();
			padContents = builder.toString();
		} catch(IOException e) {
			Log.w("InternalStorage", "Error reading from " + routeName + "/" + fileName, e);
		}

		// Load the offset from preferences
		SharedPreferences prefs = context.getSharedPreferences("padOffsets", Context.MODE_PRIVATE);
		String key = name + "Offset" + contactNumber;
		int offset = prefs.getInt(key, 0);
		
		return new OneTimePad(padContents, offset);
	}
	
	public boolean canSendMessage(String message) {
		if(localPad == null) return false;
		return (localPad.offset + message.length()) < localPad.pad.length;
	}
	
	public String encryptText(String message) {
		// Encrypted messages will be of the form |~|offset|message
		String prefix = "|~|" + localPad.getOffset() + "|";
		String suffix = localPad.encrypt(message);
		return prefix + suffix;
	}
	
	public String decryptText(String message) {
		if(contactPad == null) {
			return "[Get contact's pad] " + message;
		}
		
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
			
			if(offset + message.length() >= contactPad.pad.length) {
				return "[Get contact's pad] " + message;
			}
			
			// Decrypt the message.
			contactPad.setOffset(offset);
			return "[S]" + contactPad.decrypt(components[3].toCharArray());
		}
		// If it doesn't have the prefix, skip the message.
		return message;
	}
}

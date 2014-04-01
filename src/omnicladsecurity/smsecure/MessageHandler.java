package omnicladsecurity.smsecure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
//import android.telephony.TelephonyManager;

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
		return loadContactPad();
	}
	
	void setContactPad() {
		storeContactPad();
	}
	
	
	void storeContactPad() {
		SharedPreferences prefs = context.getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
    	//This only works if phone has internal storage and an SD card mounted
    	File path = new File("/storage/sdcard1");
    	File file = new File(path, localNumber + "-" + contactNumber + ".txt");
    	
    	OneTimePad pad;
    	pad = localPad;
    	
    	try {
            path.mkdirs();

            OutputStream os = new FileOutputStream(file);
            os.write(new String(pad.pad).getBytes());
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            //Not sure if use of context is correct here
            MediaScannerConnection.scanFile(context,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
	}
	
	OneTimePad loadContactPad(){
		SharedPreferences prefs = context.getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
	    File path = new File("/storage/sdcard1");
	    File file = new File(path, contactNumber + "-" + localNumber + ".txt");
	    
		if(!file.exists()) {
			// TODO make this not silently fail
	    	return null;
		}
	    
	    String padContents = "";
		try {
			FileInputStream streamer = new FileInputStream(file);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(streamer));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			reader.close();
			padContents = builder.toString();
		} catch(IOException e) {
			Log.w("ExternalStorage", "Error reading from " + contactNumber + "-" + localNumber, e);
		}

		boolean deleted = file.delete();
		if(!deleted)
		{
			return new OneTimePad(null, 0);
		}
		return new OneTimePad(padContents, 0);
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

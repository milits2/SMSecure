package omnicladsecurity.smsecure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
//import android.telephony.TelephonyManager;

public class MessageHandler {
	public OneTimePad localPad, contactPad;
	public Context context;
	public String contactNumber;
	
	public MessageHandler(Context context, String conversationNumber) {
		this.contactNumber = conversationNumber;
		this.context = context;
		// Pads will be null if they aren't populated yet
		localPad = loadPadByName("local");
		contactPad = loadPadByName("contact");
	}
	
	public void setLocalPad(OneTimePad pad) {
		// Store the local pad in internal memory
		localPad = pad;
		if(localPad != null) {
			storePadByName(localPad, "local");
		}
	}
	
	public void shareLocalPad() {
		// Save the local pad onto external memory for contact
		SharedPreferences prefs = context.getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
    	//This only works if phone has internal storage and an SD card mounted
		File path = Environment.getExternalStorageDirectory();
    	//File path = new File("/storage/sdcard1");
    	File file = new File(path, localNumber + "-" + contactNumber + ".txt");
    	
    	OneTimePad pad;
    	pad = localPad;
    	
    	try {
            path.mkdirs();
            Toast.makeText(context, path.getPath() + " successsfully created", Toast.LENGTH_LONG).show();
            

            OutputStream os = new FileOutputStream(file);
            os.write(new String(pad.pad).getBytes());
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            // Not sure if use of context is correct here
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
            Toast.makeText(context, e.getMessage() , Toast.LENGTH_LONG).show();
        }
	}
	
	public void loadContactPad() {
		// Load the contact's pad from external memory
		SharedPreferences prefs = context.getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
		File path = Environment.getExternalStorageDirectory();
	    File file = new File(path, contactNumber + "-" + localNumber + ".txt");
	    
		if(!file.exists()) {
			// TODO make this not silently fail	    	
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

		file.delete();
		
		contactPad = new OneTimePad(padContents, 0);
		storePadByName(contactPad, "contact");
	}
	
	public void storePadByName(OneTimePad pad, String name) {
		// Store a pad in internal memory
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
	
	public OneTimePad loadPadByName(String name) {
		// Load a pad from internal memory
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
		// Verify that the pad is sufficient to send a message
		if(localPad == null) return false;
		return (localPad.offset + message.length()) < localPad.pad.length;
	}
	
	public String encryptText(String message) {
		// Encrypted messages will be of the form |~|offset|message
		String prefix = "|~|" + localPad.getOffset() + "|";
		String suffix = localPad.encrypt(message);
		
		// Update the new offset		
		SharedPreferences prefs = context.getSharedPreferences("padOffsets", Context.MODE_PRIVATE);
		SharedPreferences.Editor writer = prefs.edit();
		
		String key = "localOffset" + contactNumber;
		writer.putInt(key, localPad.getOffset());
		writer.commit();
		
		return prefix + suffix;
	}
	
	public String decryptText(String message) {
		// Unwrap and decrypt a received message
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

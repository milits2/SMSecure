package omnicladsecurity.smsecure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Conversation {
	// The Conversation class contains the pertinent information for a conversation.
	// They are loaded upon opening a conversation and populate themselves.
	MessageHandler handler;
	String contactNumber;
	Context context;
	
	public Conversation(Context context, String withNumber) {
		this.contactNumber = withNumber;
		this.context = context;
		
		this.handler = new MessageHandler(context, withNumber);
	}
	
	public String getContactNumber() {
		return contactNumber;
	}
	
	public List<String> loadTextMessages() {
    	Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
    	if(cursor.isAfterLast()) {
    		return new ArrayList<String>();
    	}
    	
    	cursor.moveToFirst();
    	Boolean correctAddress = false;    	
    	List<String> returnList = new ArrayList<String>();
    	
    	do {
    	   for(int idx = 0; idx < cursor.getColumnCount(); idx++) {
    		   if (cursor.getColumnName(idx).equals("address") ) {
    			   if (cursor.getString(idx).equals(contactNumber)) {
    				   correctAddress = true;
				   }
    			   else {
    				   correctAddress = false;
    			   }
			   }    		   
    		   if (cursor.getColumnName(idx).equals( "body") && correctAddress) {
    			   String ciphertext = cursor.getString(idx);
    			   String plaintext = handler.decryptText(ciphertext);
    			   returnList.add(plaintext);
			   }			   
    	   }
    	} while(cursor.moveToNext());
    	
    	Collections.reverse(returnList);
    	return returnList;
	}
	
	public String prepareTextMessage(String text) {
		return handler.encryptText(text);
	}
}

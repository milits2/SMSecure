package omnicladsecurity.smsecure;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;


public class Conversation {
	public MessageHandler handler;
	public String contactNumber;
	public Context context;
	
	public Conversation(Context context, String withNumber) {
		this.contactNumber = withNumber;
		this.context = context;
		
		this.handler = new MessageHandler(context, withNumber);
	}
	
	public String getContactNumber() {
		return contactNumber;
	}
	
	public List<SMSMessage> loadTextMessages() {
		// Load the text messages in from the phone for display.
		Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
		if(cursor.isAfterLast()) {
			return new ArrayList<SMSMessage>();
		}
		
		cursor.moveToFirst();
		List<SMSMessage> messageList = new ArrayList<SMSMessage>();
		
		do {
			for(int idx = 0; idx < cursor.getColumnCount(); idx++) {
				if (cursor.getColumnName(idx).equals("address") &&
					cursor.getString(idx).equals(contactNumber)) {
					SMSMessage message = new SMSMessage();
			   
					for(idx = idx + 1; idx < cursor.getColumnCount(); idx++) {
						if (cursor.getColumnName(idx).equals("body")) {
							String ciphertext = cursor.getString(idx);
							String plaintext = handler.decryptText(ciphertext);
							message.message = plaintext;
						}
					   
						if (cursor.getColumnName(idx).equals("date")) {
							message.date = new Date(cursor.getLong(idx));
						}
					}
				   
					if (message.message != null) {
						messageList.add(message);
					}
				}
			}
		} while(cursor.moveToNext());
		
		Collections.reverse(messageList);
		return messageList;
	}
	
	public String prepareTextMessage(String text) {
		// Encrypt and wrap the text message for sending
		return handler.encryptText(text);
	}

	public void shareButtonClick() {
		// Save the one-time pad onto external memory
		handler.shareLocalPad();
	}
	
	public void loadButtonClick(){
		// Load the one-time pad from external memory
		handler.loadContactPad();
	}
}

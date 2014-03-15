package omnicladsecurity.smsecure;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Hub extends Activity {
	OneTimePad pad;
	static String[] numbers = {"7164005384", "7165746024", "7168675309"};
	Button[] conversationLinks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openHub();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hub, menu);
        return true;
    }
    
    public void loadConversationLinks() {
    	LinearLayout linkLayout = (LinearLayout)findViewById(R.id.linkLayout);
    	// Load the list of conversations we have.
    	conversationLinks = new Button[numbers.length];
    	int i = 0;
    	for(String number: numbers) {
    		conversationLinks[i] = new Button(this);
    		conversationLinks[i].setTag(number);
    		conversationLinks[i].setText("Conversation with " + number);
    		conversationLinks[i].setOnClickListener(clickConversation);
    		linkLayout.addView(conversationLinks[i]);
    		++i;
    	}
    }
    
    public void openHub() {
    	setContentView(R.layout.activity_hub);
        loadConversationLinks();
    }
    
    public void openConversation(String phoneNumber) {
    	setContentView(R.layout.message);
    	TextView number = (TextView)findViewById(R.id.phoneNumberTextBox);
    	number.setText(phoneNumber);
    }
    
    OnClickListener clickConversation = new OnClickListener() {
    	@Override
    	public void onClick(View view) {
    		openConversation(view.getTag().toString());
		}
    };

    public void sendMessageButtonClick(View view)
    {
    	TextView phoneNumber = (TextView)findViewById(R.id.phoneNumberTextBox);
    	TextView message = (TextView)findViewById(R.id.messageTextBox);
    	
    	sendTextMessage(message.getText().toString(), phoneNumber.getText().toString());
    	   	
    }
    
    public void readTextMessages(View view)
    {
    	TextView phoneNumber = (TextView)findViewById(R.id.phoneNumberTextBox);
    	Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
    	cursor.moveToFirst();
    	String msgData = "";
    	Boolean correctAddress = false;
    	
    	do{
    	   
    	   for(int idx=0;idx<cursor.getColumnCount();idx++)
    	   {
    		   
    		   if (cursor.getColumnName(idx).equals("address") )
			   {
    			   if (cursor.getString(idx).equals(phoneNumber.getText().toString()))
				   {
    				   correctAddress = true;
				   }
    			   else
    			   {
    				   correctAddress = false;
    			   }
			   }
    		   
    		   if (cursor.getColumnName(idx).equals( "body") && correctAddress)
			   { 
    			   msgData += " " + cursor.getString(idx);
			   }
			   
    	   }
    	}while(cursor.moveToNext());    
    	
    	TextView smsTextBox = (TextView)findViewById(R.id.smsTextBox);
    	
    	smsTextBox.setText(msgData);
    }
    
    public void sendTextMessage(String text, String address) {
    	SmsManager smsManager = SmsManager.getDefault();
    	smsManager.sendTextMessage(address, null, text, null, null);
    }
    
    public void hubScreen(View view) {
    	openHub();
    }
    
    public void generateOneTimePad(View view) {
    	TextView padDisplay = (TextView)findViewById(R.id.padContents);
    	pad = new OneTimePad(1024);
    	padDisplay.setText("Pad created.");
    }
    
    public void encryptMessage(View view) {
    	EditText input = (EditText)findViewById(R.id.cleantext);
    	TextView output = (TextView)findViewById(R.id.ciphertext);
    	pad.cipher = pad.encrypt(input.getText().toString());
    	
    	output.setText("Message encrypted.");
    }
    
    public void decryptMessage(View view) {
    	TextView output = (TextView)findViewById(R.id.plaintext);
    	output.setText(pad.decrypt(pad.cipher));
    }
}

package omnicladsecurity.smsecure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Hub extends Activity {	
	// Dynamic elements
	List<TextView> messageLog;
	Contacts contacts;
	
	Conversation activeConversation;
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        contacts = new Contacts(this.getApplicationContext());
        openHub();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hub, menu);
        return true;
    }
    
    /********\
    |* Hub management
    \********/
    
    public void openHub() {
    	setContentView(R.layout.activity_hub);
        loadConversationLinks();
    }
    
    public void loadConversationLinks() {
    	LinearLayout linkLayout = (LinearLayout)findViewById(R.id.linkLayout);
    	// Remove any links it currently has.
    	linkLayout.removeAllViewsInLayout();
    	
    	// Load the list of conversations we have.
    	String[] numbers = contacts.getNumbersArray();    	
    	for(String number: numbers) {
    		Button conversationLink = new Button(this);
    		conversationLink.setTag(number);
    		conversationLink.setText("Conversation with " + number);
    		conversationLink.setOnClickListener(clickConversation);
    		linkLayout.addView(conversationLink);
    	}
    }
    
    public void addConversationButton(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("Add Conversation");
		// Set up the input
		final EditText numberInput = new EditText(this);
		numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(numberInput);
		// Set up the buttons
		builder.setPositiveButton("Add", new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	String inputNumber = numberInput.getText().toString();

		    	// Refresh the screen if addNumber succeeds
		    	if(contacts.addNumber(inputNumber)) {
		    		loadConversationLinks();
		    	}
		    }
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.cancel();
		    }
		});
		builder.show();
    }
    
    OnClickListener clickConversation = new OnClickListener() {
    	@Override
    	public void onClick(View view) {
    		openConversation(view.getTag().toString());
		}
    };
    
    public void openConversation(String phoneNumber) {
    	setContentView(R.layout.activity_conversation);
    	activeConversation = new Conversation(this.getApplicationContext(), phoneNumber);
    	
    	TextView number = (TextView)findViewById(R.id.phoneNumber);
    	number.setText(phoneNumber);

    	loadMessageLog();
    }
    
    /********\
    |* Conversation management
    \********/
    
    public void loadMessageLog() {
    	List<String> messages = activeConversation.loadTextMessages();
    	LinearLayout messageLayout = (LinearLayout)findViewById(R.id.messageLayout);
    	messageLog = new ArrayList<TextView>();

    	boolean colorOn = true;;
    	for(String message: messages) {
    		TextView temp = new TextView(this);
    		temp.setText(message);
    		temp.setTextSize(36);
    		temp.setPadding(6, 12, 0, 6);
    		
    		if(colorOn) {
    			temp.setBackgroundColor(Color.parseColor("#EFEFFF"));
    		}
    		else {
    			temp.setBackgroundColor(Color.parseColor("#DFDFFF"));
    		}
    		colorOn = !colorOn;
    		
    		messageLayout.addView(temp);
    	}
    }
    
    public void backToHubButtonClick(View view) {
    	openHub();
    }
    
    public void sendTextMessageButtonClick(View view) {
    	TextView text = (TextView)findViewById(R.id.messageText);
    	String message = text.getText().toString();
    	
    	if(!activeConversation.handler.canSendMessage(message)) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    	builder.setMessage("Insufficient remaining one-time pad. Create and share a new one-time pad.")
		        .setCancelable(false)
		        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		                 dialog.cancel();
		            }
		        });
	    	builder.show();
    	}
    	else if(message.length() > 0) {
    		sendTextMessage(message, activeConversation.getContactNumber());
    		text.setText("");
    	}
    }
    
    public void sendTextMessage(String text, String address) {
    	SmsManager smsManager = SmsManager.getDefault();
    	String messageText = activeConversation.prepareTextMessage(text);
    	smsManager.sendTextMessage(address, null, messageText, null, null);
    }

    public void generateOneTimePadButtonClick(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("Generate new local pad of the entered size?");
		// Set up the input
		final EditText numberInput = new EditText(this);
		numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(numberInput);
		// Set up the buttons
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	int inputNumber = Integer.parseInt(numberInput.getText().toString());
		    	inputNumber = Math.min(8192, Math.max(1024, inputNumber));
		    	
		    	activeConversation.handler.setLocalPad(new OneTimePad(inputNumber));
		    }
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.cancel();
		    }
		});
		builder.show();
    }
    
    /********\
    |* SD card storage
    |* TODO make it not use a dummy pad
    \********/
	
    public void shareOneTimePadButtonClick(View view) {
    	Conversation.shareButtonClick();
    }
    
    OnClickListener shareOneTimePadButtonClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			Conversation.shareButtonClick();
		}
	};

    public void loadOneTimePadButtonClick(View view) {
    	Conversation.loadButtonClick();
    }
    
    OnClickListener loadOneTimePadButtonClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			Conversation.loadButtonClick();
		}
	};

    File getExternalStoragePad() {
    	
    	File path = new File("/storage/sdcard1");
    	File file = new File(path, "Pad.txt");
    	
    	return file;
    }
}

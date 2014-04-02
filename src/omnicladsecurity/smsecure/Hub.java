package omnicladsecurity.smsecure;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
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
    	TableLayout linkLayout = (TableLayout)findViewById(R.id.linkLayout);
    	// Remove any links it currently has.
    	linkLayout.removeAllViewsInLayout();
    	
    	// Load the list of conversations we have.
    	String[] numbers = contacts.getNumbersArray();    	
    	for(String number: numbers) {
    		TableRow row = new TableRow(this);
    		row.setTag(number);
    		
    		Button conversationLink = new Button(this);
    		conversationLink.setTag(number);
    		conversationLink.setText("Text with " + number);
    		conversationLink.setOnClickListener(clickConversation);
    		row.addView(conversationLink);
    		
    		Button removeLink = new Button(this);
    		removeLink.setTag(number);
    		removeLink.setText("-");
    		removeLink.setOnClickListener(removeConversation);
    		row.addView(removeLink);
    		
    		linkLayout.setColumnStretchable(0, true);
    		
    		linkLayout.addView(row);
    	}
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
    
    OnClickListener removeConversation = new OnClickListener() {
    	@Override
    	public void onClick(View view) {
			contacts.removeNumber(view.getTag().toString());
			loadConversationLinks();
    	}
    };
    
    public void addConversationButtonClick(View view) {
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
    
    public void setLocalNumberButtonClick(View view) {
    	setLocalNumber();
    }
    
    /********\
    |* Conversation management
    \********/
    
    public void loadMessageLog() {
    	List<SMSMessage> messages = activeConversation.loadTextMessages();
    	LinearLayout messageLayout = (LinearLayout)findViewById(R.id.messageLayout);
    	messageLog = new ArrayList<TextView>();

    	boolean colorOn = true;;
    	
    	String previousDate = "text";
    	
    	for(SMSMessage message: messages) {
    		
    		String messageDate = new SimpleDateFormat("MM/dd/yyyy").format(message.date);
    		String messageTime = new SimpleDateFormat("hh:mm:ss.SSS").format(message.date);
    		
    		if (!previousDate.equals(messageDate))
    		{
    			previousDate = messageDate;
    			
    			TextView padding = new TextView(this);
    			padding.setText(previousDate);
        		padding.setPadding(10, 10, 10, 10);
        		padding.setTextSize(36);
        		padding.setBackgroundColor(Color.parseColor("#AFAFFF"));
        		
        		messageLayout.addView(padding);
    		}
    		
    		
    		TextView messageTimeView = new TextView(this);
			messageTimeView.setText(messageTime);
			messageTimeView.setPadding(10, 0, 0, 10);
			messageTimeView.setTextSize(16);
			messageTimeView.setBackgroundColor(Color.parseColor("#F6F6FF"));
    				
    		
    		TextView temp = new TextView(this);
    		temp.setText(message.message);
    		temp.setTextSize(24);
    		temp.setPadding(10, 10, 10, 0);

    		if(colorOn) {
    			temp.setBackgroundColor(Color.parseColor("#EFEFFF"));
    			messageTimeView.setBackgroundColor(Color.parseColor("#EFEFFF"));
    			
    		}
    		else {
    			temp.setBackgroundColor(Color.parseColor("#DFDFFF"));
    			messageTimeView.setBackgroundColor(Color.parseColor("#DFDFFF"));
    		}
    		colorOn = !colorOn;
    		 		
    		messageLayout.addView(temp);
    		messageLayout.addView(messageTimeView);
    		
			
			
    		
    		
    	}
    	
    	//ScrollView messagePane = (ScrollView)findViewById(R.id.messagePane);
    	//messagePane.fullScroll(ScrollView.FOCUS_DOWN);
    	
    	findViewById(R.id.messagePane).post(new Runnable() {            
    	    @Override
    	    public void run() {
    	    	((ScrollView) findViewById(R.id.messagePane)).fullScroll(View.FOCUS_DOWN);              
    	    }
    	});
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
    
    public void setLocalNumber() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("Set Local Number");
		// Set up the input
		final EditText numberInput = new EditText(this);
		numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(numberInput);
		// Set up the buttons
		builder.setPositiveButton("Set", new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	String inputNumber = numberInput.getText().toString();
				
		    	if(inputNumber.length() == 10) {
					SharedPreferences prefs = getApplicationContext().getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
					SharedPreferences.Editor writer = prefs.edit();
					writer.putString("localNumber", inputNumber);
					writer.commit();
		    	}
		    }
		});
		builder.show();
    }
    
    /********\
    |* SD card storage
    |* TODO make it not use a dummy pad
    \********/
	
    public void shareOneTimePadButtonClick(View view) {
		SharedPreferences prefs = this.getApplicationContext().getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
		if(localNumber == null) {
			setLocalNumber();
		}
		
    	Conversation.shareButtonClick();
    }
    
    OnClickListener shareOneTimePadButtonClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			Conversation.shareButtonClick();
		}
	};

    public void loadOneTimePadButtonClick(View view) {
		SharedPreferences prefs = this.getApplicationContext().getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
		if(localNumber == null) {
			setLocalNumber();
		}
		
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

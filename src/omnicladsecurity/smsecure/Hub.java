package omnicladsecurity.smsecure;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
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
	private Contacts contacts;
	private Conversation activeConversation;
	private Guardian guardian;
	private Map<String, Integer> conversationMap;
	private IntentFilter intentFilter;
	public BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String senders = intent.getExtras().getString("senders");
			if(activeConversation != null && activeConversation.contactNumber.equals(senders)) {
				loadMessageLog();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		intentFilter = new IntentFilter();
		intentFilter.addAction("SMS_RECEIVED_ACTION");
		contacts = new Contacts(this.getApplicationContext());
		guardian = new Guardian(this.getApplicationContext());
		openHub();
	}
	
	@Override
	protected void onResume() {
		registerReceiver(intentReceiver, intentFilter);
	    super.onResume();
	    checkPassword();
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(intentReceiver);
		super.onPause();
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
		// Transition to the Hub UI view
		setContentView(R.layout.activity_hub);
		
		conversationMap = new HashMap<String, Integer>();
		loadMessagesNumbers();
		
		loadConversationLinks();
		activeConversation = null;
	}
	
	public void checkPassword() {
		// If no password exists, just let them in.
		if(guardian.attemptPassword(null)) {
			return;
		}

		TableLayout linkLayout = (TableLayout)findViewById(R.id.linkLayout);
		linkLayout.setVisibility(View.INVISIBLE);
		// If it exists, request it in a dialog.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter Password");
		builder.setCancelable(false);
		
		// Set up the input
		final EditText passwordInput = new EditText(this);
		passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(passwordInput);
		
		// Set up the buttons
		builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() { 
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String password = passwordInput.getText().toString();
				if(guardian.attemptPassword(password)) {
					TableLayout linkLayout = (TableLayout)findViewById(R.id.linkLayout);
					linkLayout.setVisibility(View.VISIBLE);
					return;
				}
				else finish();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
				dialog.cancel();
			}
		});
		builder.show();
	}
	
	public void loadMessagesNumbers()
	{
		// Counts number of messages and stores in hash table 
		Cursor cursor = this.getApplicationContext().getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
		if(cursor.isAfterLast()) {
			return;
		}
		
		cursor.moveToFirst();
		
		do {
		   for(int idx = 0; idx < cursor.getColumnCount(); idx++) {							 
			   if (cursor.getColumnName(idx).equals("address")) {	 
				   if (!conversationMap.containsKey(cursor.getString(idx))){
					   conversationMap.put(cursor.getString(idx), 1);
				   } else {
					   conversationMap.put(cursor.getString(idx),conversationMap.get(cursor.getString(idx)) + 1);
				   }
				   break;  
				   
			   }																			
		   }
		} while(cursor.moveToNext());
	}
	
	public void loadConversationLinks() {
		// Load and display the links to each conversation
		TableLayout linkLayout = (TableLayout)findViewById(R.id.linkLayout);
		// Remove any links it currently has.
		linkLayout.removeAllViewsInLayout();
		
		// Load the list of conversations we have.
		String[] numbers = contacts.getNumbersArray();
		if(numbers.length > 0) {
			for(String number: numbers) {
				TableRow row = new TableRow(this);
				row.setTag(number);
				
				Button conversationLink = new Button(this);
				conversationLink.setTag(number);
				
				String formattedNumber = "(" + number.substring(0, 3) + ")" +
										 number.substring(3, 6) + "-" +
										 number.substring(6, 10);
				conversationLink.setText("Text " + formattedNumber);
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
		else {
			TableRow row = new TableRow(this);
			linkLayout.addView(row);
		}
	}
	
	OnClickListener clickConversation = new OnClickListener() {
		// A dynamic listener to open conversations
		@Override
		public void onClick(View view) {
			openConversation(view.getTag().toString());
		}
	};
	
	OnClickListener removeConversation = new OnClickListener() {
		// A dynamic listener to remove conversations
		@Override
		public void onClick(View view) {
			contacts.removeNumber(view.getTag().toString());
			loadConversationLinks();
		}
	};
	
	public void openConversation(String phoneNumber) {
		// Transition to the Conversation UI view
		setContentView(R.layout.activity_conversation);
		activeConversation = new Conversation(this.getApplicationContext(), phoneNumber);
		setConversationHeader();

		loadMessageLog();
	}
	
	public void setConversationHeader() {
		TextView number = (TextView)findViewById(R.id.phoneNumber);
		String phoneNumber = activeConversation.contactNumber;
		String formattedNumber = "(" + phoneNumber.substring(0, 3) + ")" +
								 phoneNumber.substring(3, 6) + "-" +
								 phoneNumber.substring(6, 10);
		int padLeft = activeConversation.handler.padRemaining();
		String padRemaining = padLeft >= 0 
				? "[" + Integer.toString(padLeft) + "% of pad left]"
				: "[No pad]";
		number.setText("Texts with " + formattedNumber + " " + padRemaining);		
	}
	
	public void addConversationButtonClick(View view) {
		// Add a new conversation and contact to the list
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Add Conversation");
		
		// Set up the input
		final EditText numberInput = new EditText(this);
		numberInput.setInputType(InputType.TYPE_CLASS_PHONE);
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
	
	public void hubSettingsButtonClick(View view) {
		CharSequence options[] = new CharSequence[] {
				"Set local number",
				"Set external memory location",
				"Set SMSecure password",
				"Remove SMSecure password"
				};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Hub Settings");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
				case 0: // Set local number
					setLocalNumber();
					break;
				case 1: // Set external memory location
					setExternalMemoryLocation();
					break;
				case 2: // Set password
					setPassword();
					break;
				case 3: // Remove password
					guardian.removePassword();
					break;
				}
			}
		});
		builder.show();
	}
	
	public void setPassword() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set Password");
		
		// Set up the input
		final EditText passwordInput = new EditText(this);
		passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(passwordInput);
		
		// Set up the buttons
		builder.setPositiveButton("Set", new DialogInterface.OnClickListener() { 
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String newPassword = passwordInput.getText().toString();
				guardian.setPassword(newPassword);
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
	
	public void appTutorialButtonClick(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
					"Create a conversation with the +Convo button, then " +
					"be sure to create a one-time pad and share it with your " +
					"conversation partner via the options found in the pad manager. " +
					"More specific instructions can be found in the external tutorial."
				)
		.setCancelable(false)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				 dialog.cancel();
			}
		});
		builder.show();
	}
	
	/********\
	|* Conversation management
	\********/
	
	public void loadMessageLog() {
		// Load the text message history for display.
		List<SMSMessage> messages = activeConversation.loadTextMessages();
		LinearLayout messageLayout = (LinearLayout)findViewById(R.id.messageLayout);

		boolean colorOn = true;;
		
		String previousDate = "spaceholdertext";
		
		for(SMSMessage message: messages) {		
			String messageDate = new SimpleDateFormat("MM/dd/yyyy").format(message.date);
			String messageTime = new SimpleDateFormat("hh:mm:ss.SSS").format(message.date);
			
			if (!previousDate.equals(messageDate)) {
				previousDate = messageDate;
				
				TextView padding = new TextView(this);
				padding.setText(previousDate);
				padding.setPadding(10, 10, 10, 10);
				padding.setTextSize(36);
				padding.setBackgroundColor(Color.parseColor("#AFAFFF"));
				
				messageLayout.addView(padding);
			}		
			
			// Create and format the box for a given text message
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
	
	public void padManagerButtonClick(View view) {
		CharSequence options[] = new CharSequence[] {
				"Generate one-time pad",
				"Share pad via SD card",
				"Load pad via SD card"
				};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pad Manager");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
				case 0: // Generate one-time pad
					generateOneTimePad();
					break;
					
				case 1: // Share pad via SD card
					shareOneTimePad();
					break;
					
				case 2: // Load pad via SD card
					loadOneTimePad();
				}
			}
		});
		builder.show();
	}
	
	public void sendTextMessageButtonClick(View view) {
		// UI handling for sending a text message to conversation partner
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
			setConversationHeader();
		}
	}
	
	public void sendTextMessage(String text, String address) {
		// Prepare and issue the text message
		SmsManager smsManager = SmsManager.getDefault();
		String messageText = activeConversation.prepareTextMessage(text);
		smsManager.sendTextMessage(address, null, messageText, null, null);
	}
	
	public void setLocalNumber() {
		// Store the local number for the host user's phone
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set Local Number");
		
		// Set up the input
		final EditText numberInput = new EditText(this);
		numberInput.setInputType(InputType.TYPE_CLASS_PHONE);
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
	
	public void setExternalMemoryLocation() {
		// Store the local number for the host user's phone
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set External Memory Location");
		
		// Set up the input
		final EditText locationInput = new EditText(this);
		locationInput.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(locationInput);
		
		// Set up the buttons
		builder.setPositiveButton("Set", new DialogInterface.OnClickListener() { 
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String inputLocation = locationInput.getText().toString();
				
				SharedPreferences prefs = getApplicationContext().getSharedPreferences("externalMemoryLocation", Context.MODE_PRIVATE);
				SharedPreferences.Editor writer = prefs.edit();
				writer.putString("externalLocation", inputLocation);
				writer.commit();
			}
		});
		builder.show();
	}
	
	public void generateOneTimePad() {
		// Create a new one-time pad for a given conversation
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose pad size in bytes.");
			
		// Set up the input
		final EditText numberInput = new EditText(this);
		numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(numberInput);
		
		// Set up the buttons
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() { 
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int inputNumber;
				// Valid sizes: 1KB to 1MB
				int sizeMin = 1024, sizeMax = 1024*1024;
				
				if (numberInput.getText().toString().length() <= 9){
					inputNumber = Integer.parseInt(numberInput.getText().toString());									
				}
				else {
					inputNumber = sizeMax;	
				}
				
				inputNumber = Math.min(sizeMax, Math.max(sizeMin, inputNumber));	
				activeConversation.handler.setLocalPad(new OneTimePad(inputNumber));
				setConversationHeader();
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
	
	public void shareOneTimePad() {
		// Save a one-time pad to SD
		SharedPreferences prefs = this.getApplicationContext().getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
		if(localNumber == null) {
			setLocalNumber();
		}
		
		activeConversation.shareButtonClick();
	}
	
	public void loadOneTimePad() {
		// Load a one-time pad from SD
		SharedPreferences prefs = this.getApplicationContext().getSharedPreferences("localPhoneNumber", Context.MODE_PRIVATE);
		String localNumber = prefs.getString("localNumber", null);
		
		if(localNumber == null) {
			setLocalNumber();
		}
		
		activeConversation.loadButtonClick();
	}
}

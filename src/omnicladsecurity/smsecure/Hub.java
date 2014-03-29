package omnicladsecurity.smsecure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
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
	static String[] numbers = {"7164005384", "7168675309", "8029993641"};
	
	Conversation activeConversation;
		
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
    	int i = 0;
    	for(String number: numbers) {
    		Button conversationLink = new Button(this);
    		conversationLink.setTag(number);
    		conversationLink.setText("Conversation with " + number);
    		conversationLink.setOnClickListener(clickConversation);
    		linkLayout.addView(conversationLink);
    		++i;
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
		    	String input = numberInput.getText().toString();
		    	if(input.length() == 10) {
		    		addConversation(numberInput.getText().toString());
		    	}
		    	//numbers.add(input.getText().toString());
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
    
    public void addConversation(String number) {
    	// TODO add memory interaction
    	String[] newNumbers = new String[numbers.length + 1];
    	for(int i = 0; i < numbers.length; ++i) {
    		newNumbers[i+1] = numbers[i];
    	}
    	newNumbers[0] = number;
    	numbers = newNumbers;
    	
    	loadConversationLinks();
    }
    
    public void editConversationsButton(View view) {
    	// TODO implement
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
    	sendTextMessage(text.getText().toString(), activeConversation.getContactNumber());	
    }
    
    public void sendTextMessage(String text, String address) {
    	SmsManager smsManager = SmsManager.getDefault();
    	String messageText = activeConversation.prepareTextMessage(text);
    	smsManager.sendTextMessage(address, null, messageText, null, null);
    }
    
    /********\
    |* SD card storage
    |* TODO make it not use a dummy pad
    \********/

    public void generateOneTimePadButtonClick(View view) {
    	createExternalStoragePad();
    }
        
    OnClickListener clickSDStorage = new OnClickListener() {
    	@Override
    	public void onClick(View view) {
    		createExternalStoragePad();
		}
    };
    
    void createExternalStoragePad() {
    	//Create a path where we will place the one time pad
    	//this is in public directory because removal of external storage deletes private app data
    	OneTimePad pad;
    	//File path = Environment.getExternalStorageDirectory();
    	File path = new File("/storage/sdcard1");
    	File file = new File(path, "Pad.txt");
    	
    	pad = new OneTimePad(1024);
    	
    	try {
            // Make sure the directory exists
            path.mkdirs();

            OutputStream os = new FileOutputStream(file);
            os.write(new String(pad.pad).getBytes());
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
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
    
    File getExternalStoragePad() {
    	File path = new File("/storage/sdcard1");
    	File file = new File(path, "Pad.txt");
    	
    	return file;
    }
}

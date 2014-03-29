package omnicladsecurity.smsecure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Hub extends Activity {
	OneTimePad pad;
	static String[] numbers = {"7164005384", "7165746024", "7168675309"};
	
	Button[] conversationLinks;
	List<TextView> messageLog;
		
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
    
    public void loadMessageLog() {
    	List<String> messages = readTextMessages();
    	LinearLayout messageLayout = (LinearLayout)findViewById(R.id.messageLayout);
    	messageLog = new ArrayList<TextView>();

    	for(String message: messages) {
    		TextView temp = new TextView(this);
    		temp.setText(message);
    		messageLayout.addView(temp);
    	}
    }
    
    public void openHub() {
    	setContentView(R.layout.activity_hub);
        loadConversationLinks();
    }
    
    public void openConversation(String phoneNumber) {
    	setContentView(R.layout.activity_conversation);
    	
    	TextView number = (TextView)findViewById(R.id.phoneNumber);
    	number.setText(phoneNumber);

    	loadMessageLog();
    }
    
    OnClickListener clickConversation = new OnClickListener() {
    	@Override
    	public void onClick(View view) {
    		openConversation(view.getTag().toString());
		}
    };

    public void generateOneTimePadButtonClick(View view) {
    	createExternalStoragePad();
    }
    
    public List<String> readTextMessages() {
    	TextView phoneNumber = (TextView)findViewById(R.id.phoneNumber);
    	Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
    	cursor.moveToFirst();

    	Boolean correctAddress = false;
    	
    	List<String> returnList = new ArrayList<String>();
    	
    	do{
    	   for(int idx=0; idx<cursor.getColumnCount(); idx++) {
    		   if (cursor.getColumnName(idx).equals("address") ) {
    			   if (cursor.getString(idx).equals(phoneNumber.getText().toString())) {
    				   correctAddress = true;
				   }
    			   else {
    				   correctAddress = false;
    			   }
			   }
    		   
    		   if (cursor.getColumnName(idx).equals( "body") && correctAddress) { 
    			   returnList.add(cursor.getString(idx));
			   }
			   
    	   }
    	}while(cursor.moveToNext());    
    	
    	return returnList;
    }
    
    public void sendTextMessage(String text, String address) {
    	SmsManager smsManager = SmsManager.getDefault();
    	smsManager.sendTextMessage(address, null, text, null, null);
    }
    
    public void hubScreen(View view) {
    	openHub();
    }
    
    /********\
    |* Start of SD card storage
    |* TODO make it not use a dummy pad
    \********/
        
    OnClickListener clickSDStorage = new OnClickListener() {
    	@Override
    	public void onClick(View view) {
    		createExternalStoragePad();
		}
    };
    
    void createExternalStoragePad( ) {
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
            os.write(pad.pad);
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
    /********\
    |* End of SD card storage
    \********/
}

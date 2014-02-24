package omnicladsecurity.smsecure;

import omnicladsecurity.smsecure.R;

import omnicladsecurity.smsecure.OneTimePad;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Hub extends Activity {
	OneTimePad pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hub, menu);
        return true;
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

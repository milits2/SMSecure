package omnicladsecurity.smsecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Bundle bundle = intent.getExtras();
		if(bundle != null) {
			Object[] pdus = (Object[])bundle.get("pdus");
			SmsMessage[] messages = new SmsMessage[pdus.length];
			
			// List all senders to only update if needed
			String senders = "";
			for(int i = 0; i < messages.length; ++i) {
				messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
				senders += messages[i].getOriginatingAddress();
			}
			
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction("SMS_RECEIVED_ACTION");			
			broadcastIntent.putExtra("senders", senders);			
			context.sendBroadcast(broadcastIntent);
		}
	}
}

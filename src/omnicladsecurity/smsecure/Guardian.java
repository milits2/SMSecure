package omnicladsecurity.smsecure;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.SharedPreferences;

public class Guardian {
	public Context context;
	
	public Guardian(Context context) {
		this.context = context;
	}
	
	public boolean attemptPassword(String password) {
		SharedPreferences prefs = context.getSharedPreferences("guardian", Context.MODE_PRIVATE);
		String hashed = prefs.getString("password", null);
		
		// If there's no recorded password, just accept.
		if(hashed == null) return true;
		// Otherwise, check the hash.
		return verifyHash(hashText(password), hashed);
	}
	
	public void setPassword(String password) {
		SharedPreferences prefs = context.getSharedPreferences("guardian", Context.MODE_PRIVATE);
		SharedPreferences.Editor writer = prefs.edit();
		
		writer.putString("password", hashText(password));
		writer.commit();
	}
	
	public void removePassword() {
		SharedPreferences prefs = context.getSharedPreferences("guardian", Context.MODE_PRIVATE);
		SharedPreferences.Editor eraser = prefs.edit();
		
		eraser.remove("password");
		eraser.commit();
	}
	
	private String hashText(String input) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
		byte[] hash = md.digest(input.getBytes());
		// Convert from the byte array to a hex string
		String hashed = "";
		for(int i = 0; i < hash.length; ++i) {
			hashed += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
		}
		
		return hashed;
	}
	
	private boolean verifyHash(String attempt, String hashed) {
		String hashAttempt = hashText(attempt);
		return hashAttempt.equals(hashed);
	}
}

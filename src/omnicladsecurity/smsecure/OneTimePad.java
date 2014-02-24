package omnicladsecurity.smsecure;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class OneTimePad {
	public byte[] pad;
	public byte[] cipher;
	
	public OneTimePad(int padLength) {
		// Generate padLength characters.
		pad = new byte[padLength];
		Random pGen = new Random();
		pGen.nextBytes(pad);
	}
	
	public byte[] encrypt(String plaintext) {
		byte[] plain = plaintext.getBytes();
		cipher = new byte[plain.length];
		int offset = 0;
		for(byte b: plain) {
			cipher[offset] = (byte)(b + (byte)pad[offset]);
			++offset;
		}
		return cipher;
	}
	
	public String decrypt(byte[] ciphertext) {
		byte[] plain = new byte[ciphertext.length];
		int offset = 0;
		for(byte b: ciphertext) {
			plain[offset] = (byte)(b - pad[offset]);
			++offset;
		}
		
		String visible;
		try {
			visible = new String(plain, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			visible = "ERROR";
		}
		return visible;
	}
}

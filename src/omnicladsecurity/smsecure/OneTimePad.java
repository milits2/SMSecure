package omnicladsecurity.smsecure;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class OneTimePad {
	public byte[] pad;
	int offset;
	
	public OneTimePad(int padLength) {
		// Generate padLength characters.
		pad = new byte[padLength];
		Random pGen = new Random();
		pGen.nextBytes(pad);
		
		offset = 0;
	}
	
	public byte[] encrypt(String plaintext) {
		// Offset is updated by encrypt.
		byte[] plain = plaintext.getBytes();
		byte[] cipher = new byte[plain.length];
		int encryptShift = offset;
		for(byte b: plain) {
			cipher[offset - encryptShift] = (byte)(b + (byte)pad[offset]);
			++offset;
		}
		return cipher;
	}
	
	public String decrypt(byte[] ciphertext) {
		// Decrypt must have offset set before it is run.
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
	
	public int getOffset() {
		return offset;
	}
	public void setOffset(int newOffset) {
		offset = newOffset;
	}
}

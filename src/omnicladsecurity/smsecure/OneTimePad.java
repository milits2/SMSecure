package omnicladsecurity.smsecure;

import java.util.Random;

public class OneTimePad {
	public char[] pad;
	public int offset;
	
	public OneTimePad(int padLength) {
		this.pad = new char[padLength];
		Random pGen = new Random();		
		for(int i = 0; i < padLength; ++i) {
			this.pad[i] = (char)pGen.nextInt(Character.MAX_VALUE);
		}
		
		this.offset = 0;
	}
	
	public OneTimePad(String padContents, int offset) {
		this.pad = padContents.toCharArray();
		this.offset = offset;
	}
	
	public String encrypt(String plaintext) {
		// Encrypt a string with the pad
		char[] plain = plaintext.toCharArray();
		char[] cipher = new char[plain.length];
		int encryptShift = offset;
		for(char b: plain) {
			cipher[offset - encryptShift] = (char)(b + pad[offset]);
			++offset;
		}
		return new String(cipher);
	}
	
	public String decrypt(char[] ciphertext) {
		// Decrypt a string the pad
		// Needs to have offset set before running
		char[] plain = new char[ciphertext.length];
		int encryptShift = offset;
		for(char b: ciphertext) {
			plain[offset - encryptShift] = (char)(b - pad[offset]);
			++offset;
		}
		return new String(plain);
	}
	
	public int getOffset() {
		return offset;
	}
	public void setOffset(int newOffset) {
		offset = newOffset;
	}
}

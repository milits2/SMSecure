package omnicladsecurity.smsecure.tests;

import static org.junit.Assert.*;

import omnicladsecurity.smsecure.OneTimePad;

import org.junit.Test;

public class OneTimePadTest {

	@Test
	public void testEncDec() {
		OneTimePad otp = new OneTimePad(2048);
		
		// Tests for text, numbers, punctuation, and combination
		String[] cleanText = {
				"vriska",
				"413612",
				"@(#)^@#@}{:@#$",
				"fluo413~!asdf`"
		};
		
		for(int i = 0; i < cleanText.length; ++i) {
			int offset = otp.getOffset();
			String cipher = otp.encrypt(cleanText[i]);
			// Reset offset to emulate properly
			otp.setOffset(offset);
			
			assertEquals("enc/dec test #" + i,
					cleanText[i],
					otp.decrypt(cipher.toCharArray()));
		}
	}
}

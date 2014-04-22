package omnicladsecurity.smsecure.tests;

import static org.junit.Assert.assertEquals;
import omnicladsecurity.smsecure.Guardian;

import org.junit.Test;

public class GuardianTest {
	
	@Test
	public void hashTest() {
		Guardian crusader = new Guardian(null);
		
		// Tests for text, numbers, punctuation, and combination
		String[] cleanText = {
				"vriska",
				"413612",
				"@(#)^@#@}{:@#$",
				"fluo413~!asdf`"
		};
		
		for(int i = 0; i < cleanText.length; ++i) {
			String hashed = crusader.hashText(cleanText[i]);
			System.out.println(hashed);
			String hashAgain = crusader.hashText(cleanText[i]);
			assertEquals("hash test #" + i, hashed, hashAgain);
		}
	}
}

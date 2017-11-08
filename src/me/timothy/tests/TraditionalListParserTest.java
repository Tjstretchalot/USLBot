package me.timothy.tests;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

import me.timothy.bots.USLTraditionalListParser;
import me.timothy.bots.USLTraditionalListParser.TraditionalScammerParsed;

/**
 * Tests the USLTraditionalListParser
 * 
 * @author Timothy
 */
public class TraditionalListParserTest {
	@Test
	public void testSomeLines() {
		final Object[][] tests = new Object[][] {
			{ "* /u/zerkeruk #scammer", "zerkeruk", "#scammer" },
			{ "* /u/Zman9596 #sketchy GCX rule 2. likely compromised account (banned by /r/giftcardexchange)", "Zman9596", "#sketchy GCX rule 2. likely compromised account (banned by /r/giftcardexchange)" },
			{ "* /u/hearmyside Threatening, harassing, or inciting violence: #scammer alt of greenyoda9 (banned by /r/hardwareswap)", "hearmyside", "Threatening, harassing, or inciting violence: #scammer alt of greenyoda9 (banned by /r/hardwareswap)" } 
		};
		
		USLTraditionalListParser parser = new USLTraditionalListParser();
		for(final Object[] test : tests) {
			String testString = (String)test[0];
			String testUser = (String)test[1];
			String testDesc = (String)test[2];
			
			TraditionalScammerParsed parsed = parser.parseLine(testString);
			assertEquals(testUser, parsed.username);
			assertEquals(testDesc, parsed.description);
		}
	}
	
	@Test
	public void testFromFile() throws IOException {
		File tempFile = File.createTempFile("tradlistparsertest", "txt");
		tempFile.deleteOnExit();
		
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
			bw.write("* /u/hazman117 #sketchy GCX rule 2 (banned by /r/giftcardexchange)");
			bw.newLine();
			bw.write("* /u/Gulogomi #sketchy GCX rule 2 (banned by /r/giftcardexchange)");
			bw.newLine();
			bw.write("* /u/honaent #sketchy trading alt (banned by /r/giftcardexchange)");
		}
		
		boolean[] found = new boolean[3];

		USLTraditionalListParser parser = new USLTraditionalListParser();
		
		parser.parseFile(tempFile, (parsed) -> {
			if(parsed.username.equals("hazman117")) {
				assertEquals("#sketchy GCX rule 2 (banned by /r/giftcardexchange)", parsed.description);
				found[0] = true;
			}else if(parsed.username.equals("Gulogomi")) {
				assertEquals("#sketchy GCX rule 2 (banned by /r/giftcardexchange)", parsed.description);
				found[1] = true;
			}else {
				assertEquals("honaent", parsed.username);
				assertEquals("#sketchy trading alt (banned by /r/giftcardexchange)", parsed.description);
				found[2] = true;
			}
		});
		
		for(int i = 0; i < found.length; i++) {
			assertTrue("i=" + i, found[i]);
		}
	}
}

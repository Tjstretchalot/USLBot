package me.timothy.bots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.function.Consumer;

import me.timothy.bots.models.Person;
import me.timothy.bots.models.TraditionalScammer;

/**
 * Parses the traditional content_md of the wikipedia page saved as a 
 * local file with \\r\\n replaced with actual \r\ns
 * 
 * @author Timothy
 */
public class USLTraditionalListParser {
	/**
	 * Length to trim of the front
	 */
	private static final int TRIM_LENGTH = "* /u/".length();
	
	/** What is parsed from each line */
	public final class TraditionalScammerParsed {
		/** Who was banned */
		public final String username;
		/** Why they were banned */
		public final String description;
		
		/**
		 * @param username the username parsed
		 * @param description the description parsed
		 */
		public TraditionalScammerParsed(String username, String description) {
			this.username = username;
			this.description = description;
		}
	}
	/**
	 * Parse one line and return the traditional scammer
	 * 
	 * @param line the line to parse
	 * @return the traditional scammer
	 */
	public TraditionalScammerParsed parseLine(String line) {
		line = line.substring(TRIM_LENGTH);
		
		StringBuilder username = new StringBuilder();
		for(int i = 0; i < line.length(); i++) {
			if(line.charAt(i) != ' ') {
				username.append(line.charAt(i));
			}else {
				break;
			}
		}
		
		String description = line.substring(username.length() + 1);
		
		return new TraditionalScammerParsed(username.toString(), description);
	}
	
	/**
	 * Go through the file line by line and call handler(lineParsed) on each
	 * line. 
	 * 
	 * @param file The file
	 * @param handler The handler
	 */
	public void parseFile(File file, Consumer<TraditionalScammerParsed> handler) throws IOException {
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			String ln;
			while((ln = br.readLine()) != null) {
				handler.accept(parseLine(ln));
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		if(args.length != 1) {
			System.err.println("Expected 1 argument (file name) but got " + args.length);
			System.exit(1);
		}
		
		File file = new File(args[0]);
		if(!file.exists()) {
			System.err.println("File " + args[0] + " does not exist");
			System.exit(1);
		}
		
		USLFileConfiguration config = new USLFileConfiguration();
		config.load();
		
		USLDatabase database = new USLDatabase();
		database.connect(config.getProperty("database.username"), config.getProperty("database.password"), config.getProperty("database.url"));
		database.validateTableState();
		
		new USLTraditionalListParser().parseFile(file, (handler) -> {
			Person person = database.getPersonMapping().fetchOrCreateByUsername(handler.username);
			
			if(database.getTraditionalScammerMapping().fetchByPersonID(person.id) != null) {
				System.err.println("Got duplicate for person " + person.username + ", ignoring");
				return;
			}
			
			TraditionalScammer scammer = new TraditionalScammer(-1, person.id, "grandfathered", handler.description, new Timestamp(System.currentTimeMillis()));
			database.getTraditionalScammerMapping().save(scammer);
		});
	}
}

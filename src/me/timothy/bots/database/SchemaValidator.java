package me.timothy.bots.database;

// Alternative name: SchemaManager?
/**
 * A schema validator is capable of validating a particular
 * schema for a mapping, for example a table in a relational
 * database, as well as create it and purge/destroy it (where appropriate).
 * 
 * @author Timothy
 */
public interface SchemaValidator {
	/**
	 * <p>Validates the schema to ensure it exists (or create it
	 * if possible). If the schema <i>does</i> exist, then verifies
	 * it matches what's expected. This should be sufficient enough
	 * to prevent versioning errors without being unnecessarily restrictive.</p>
	 * 
	 * <p>Things that should be verified:</p>
	 * <ul>
	 * <li>That the fields/columns exist and are of the right type</li>
	 * <li>That fields that <i>must</i> have certain behavior (e.g. default to null, autoincrement)
	 * in order for the mapper to function correctly do.</li>
	 * </ul>
	 * 
	 * <p>Things that should <i>not</i> be verified:</p>
	 * <ul>
	 * <li>Foreign keys - These shouldn't be depended on, and removing these is 
	 * reasonable performance tweak in some instances</li>
	 * <li>Maximum field sizes - Except where the size requirement is nonintuitive, maximum 
	 * field sizes can often be tweaked for performance/usability reasons</li>
	 * </ul>
	 * 
	 * @throws IllegalStateException if the schema exists, but is not compatible
	 */
	public void validateSchema() throws IllegalStateException;
	
	/**
	 * Purges the schema; deleting everything in the schema and the
	 * schema itself.
	 */
	public void purgeSchema();
}

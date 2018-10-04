package javax.json.serialization;

/**
 * Define the behavior of the JsonObjectBuilder when the received value
 * is <code>null</code>.
 * 
 * @author Wesley Egberto
 */
public enum NullValueSerializationBehavior {
	/**
	 * Define the behavior to throw a NullPointerException when
	 * the received value is <code>null</code>.
	 * This is the default behavior.
	 */
	FORBIDDEN,
	/**
	 * Define the behavior to serialize the
	 * the received value as <code>JsonValue.NULL</code>.
	 */
	SERIALIZE,
	/**
	 * Define the behavior to ignore the value when
	 * it is <code>null</code>.
	 */
	IGNORE
}

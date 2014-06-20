package de.comlineag.sbm.persistence;

/**
 * 
 * @author MLeinemann
 * 
 *         Fehler der anzeigt das die Werte nicht entschluesselt werden konnten
 */
public class NoBase64EncryptedValue extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6300138787724088083L;

	public NoBase64EncryptedValue() {

	}

	public NoBase64EncryptedValue(String s) {
		super(s);
	}
}

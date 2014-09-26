package de.comlineag.snc.helper;

/**
 * 
 * @author 		Christian Gunether
 * @category 	helper class
 * @version 	0.1					- 25.09.2014
 * @status		productive
 *
 * @description creates a hashcode consisting of a string and an int 
 * 				which can be used as a alphanumerical id of an object in a map
 * 
 * @changelog	0.1 (Chris)			skeleton created
 */
public class StringIntKey {

    private final String s;
    private final int i;

    static StringIntKey valueOf( String string , int value ) {
        return new StringIntKey( string, value );
    }
    private StringIntKey( String string, int value ) {
        this.s = string;
        this.i = value;
    }
    public boolean equals( Object o ) {
        if( o != null && o instanceof StringIntKey ){
            StringIntKey other = ( StringIntKey ) o;
            return this.s == other.s && this.i == other.i;
        }

        return false;
    }
    
    public int hashCode() {
        return s != null ? s.hashCode() * 37 + i : i;
    }
}

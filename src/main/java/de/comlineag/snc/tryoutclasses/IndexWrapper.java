package de.comlineag.snc.tryoutclasses;

/**
 * 
 * @author 		Christian Guenther
 * @category 	helper class
 * @version		0.1				- 30.09.2014
 * @status		productive
 * 
 * @description IndexWrapper is a helper class that can hold a start and end-index.
 * 				It is, for example, used by the GenericWebParser class to find and return
 * 				the occurrences of a given word within a given text.  
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */
public class IndexWrapper {
	 
    private int start;
    private int end;
 
    public IndexWrapper(int start, int end) {
        this.start = start;
        this.end = end;
    }
 
    public int getEnd() {
        return end;
    }
 
    public int getStart() {
        return start;
    }
 
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + end;
        result = prime * result + start;
        return result;
    }
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndexWrapper other = (IndexWrapper) obj;
        if (end != other.end)
            return false;
        if (start != other.start)
            return false;
        return true;
    }
 
}
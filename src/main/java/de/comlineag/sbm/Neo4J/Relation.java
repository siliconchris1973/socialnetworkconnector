package de.comlineag.sbm.Neo4J;

/**
*
* @author Christian Guenther
* @category Handler
*
* @description this class can be used as an object representing a relationship between nodes
* 			   to actually create this relationship in the graph, a JSON conform string is created 
* 				
* @version 1.0
*
*/
public class Relation
{
    public static final String OUT = "out";
    public static final String IN = "in";
    public static final String BOTH = "both";
    private String type;
    private String direction;

    public String toJsonCollection()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "{ " );
        sb.append( " \"type\" : \"" + type + "\"" );
        if ( direction != null )
        {
            sb.append( ", \"direction\" : \"" + direction + "\"" );
        }
        sb.append( " }" );
        return sb.toString();
    }

    public Relation( String type, String direction )
    {
        setType( type );
        setDirection( direction );
    }

    public Relation( String type )
    {
        this( type, null );
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setDirection( String direction )
    {
        this.direction = direction;
    }
}
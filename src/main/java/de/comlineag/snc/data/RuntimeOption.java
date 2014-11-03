package de.comlineag.snc.data;

/**
*
* @author 		Christian Guenther
* @category 	data
* @version		0.1				- 23.10.2014
* @status		productive
*
* @description 	representation of the runtime configuration data. The class is used by
* 				RuntimeOptionsParser to setup a map of options. The options come from
* 				the runtime configuration xml-file SNC_Runtime_Configuration-1.0.xml
*				RuntimeOptionsParser is in turn used by the RuntimeConfiguration class
*
* @changelog	0.1 (Chris)		class created
* 
*/
public class RuntimeOption {
    private String name;
    private String type;
    private String value;
    private String constant;
    
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getType() {return type;}
    public void setType(String type) {this.type = type;}
    public String getValue() {return value;}
    public void setValue(String value) {this.value = value;}
    public String getConstant() {return constant;}
    public void setConstant(String constant) {this.constant = constant;}
}
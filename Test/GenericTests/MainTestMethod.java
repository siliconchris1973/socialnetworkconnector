package GenericTests;

import de.comlineag.sbm.persistence.IniFileConfigurationPersistence;

public class MainTestMethod {

	public static void main(String[] args) {
		IniFileConfigurationPersistence Configuration = new IniFileConfigurationPersistence();
		
		String term1;
		term1 = Configuration.getConfigurationElement("term1", "trackTerms");
		
		System.out.print("the value of term1 is " + term1);
		
	}

}

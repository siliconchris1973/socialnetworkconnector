In this directory you can find the files for the actual crawler configuration. The SNC has multiple ways of setting up the constraints on what to track or search within the different social networks, called a configuration manager.

You can activate the configuration manager to use in the applicationContext.xml file (one directory up from here). In there you chose exactly one of the available configuration manager and also how to retrieve the configuration db. The configuration db can either be a valid db engine or a file from this directory. Be aware that in case you want to setup SNC to track a completely new domain, I would recommend creating a new domain driven configuration and copy the existing files prior editing - you will later see why.


As of this release there are three different file based configuration managers available (from simple to sophisticated):
	1. INI file		 - very simple options on what to track
	2. simple XML	 - more options on what and where to track
	3. complex XML - very sophisticated and fine grained options

INI file configuration
is a very simple ini file in which you can setup constraints for 3 different areas: term, language and site. Every constraint given applies to all crawlers and there is no possibility to distinguish between different customers or domains. This restriction can have a serious impact and can, in the worst case, lead to runtime errors. 
Consider this: you setup a constraint against a site from one network, say Lithium, and the crawler for Facebook now tries to read this site (which probably doesn't even exist in Facebook universe) - bang.
In addition, there is also no type safety guaranteed, as the values given are all handled as Strings. Because of this with ini file based configuration you cannot setup a constraint against specific users or geo locations. 

For the above given reasons, you should really only consider using ini file based configuration in the most simplest scenario, possibly with only constraining one crawler on some keywords. In any other case, you should at least opt for the simple xml configuration option.
The standard configuration file is
	* CrawlerConfiguration.ini


Simple XML configuration
Is a simple xml based file. With this, on top of what the ini file based option gives you, you get the ability to setup all 5 categories (term, user, language, site and geo location). The constraints can be configured per network or for all networks.
Constraints given in the section for ALL networks are appended to the constraints provided for a specific network (e.g. Twitter) and the different crawlers are then instantiated with the combined constraints (or search parameter). 
The standard configuration file is
	* SimpleCrawlerConfiguration.xml


Complex XML and Domain Driven configuration
puts additional possibilities to simple xml configuration. With this option, you can setup a configuration for a structure based on a business (or interest) domain and one or more customers plus constraints for any combination of crawlers. A domain is an area of business, like banking or group of people sharing the same interest). The domain acts as the top level structure and can as such be used to define common track criteria for an entire line of business. A customer belongs to a domain and therefore inherits all constraints based on the domain.

The domain and customer are not configured in the crawler configuration xml file but only referenced therein. They can be found in the bean configuration file GeneralConfiguration.xml in the section for domain and customer configuration.

With complex xml configuration you get the possibility for a complex domain-customer structure. But you don't have to use such a a complex structure. You can also just opt for one or more customers without a domain or one or more domains without a customer. For this scenario, your convenience and to give an example on how to use complex xml file crawler configuration, I provided 3 example files:
	* CustomerSpecificCrawlerConfiguration.xml
	shows how to setup the crawler for different customers but 
	without a domain
	* DomainSpecificCrawlerConfiguration.xml
	shows how to setup the crawler for different domains but
	without any individual customers
	* CustomerAndDomainCrawlerConfiguration.xml
	shows how to setup a domain and a customer with common and
	specific constraints but no hierarchy.


Domain Driven Configuration
To setup a hierarchy between domain and customer, the 4th option  DomainDrivenConfiguration is used. You activate it by choosing DomainDrivenConfiguration in applicationContext.xml and setting the configuration file. 
The standard configuration file is:
	* DomainWithCustomerCrawlerConfiguration.xml
	It shows how to setup a domain as top level element with two
	customer as sub structure with common and	specific constraints

Please take a look at the configuration files itself for an explanation on how to setup constraints according to your needs.

ATTENTION
As of this date (July, 17th 2014) it is possible to configure the SNC for different domains and customers, but it is not possible to start just one instance of the SNC and have it track everything in one. So right now, if you want to track banking related posts and food-related posts, you would need to either copy the whole SNC and start up two instances on two different application servers or run one SNC instance first for banking and later for food. This restriction will be eliminated in Version 1.1
In this directory you can find the files for the actual crawler configuration. The SNC has multiple ways of setting up the constraints on what to track or search within the different social networks.

You can setup the configuration manager to use in the applicationContext.xml file (one directory up from here). In there you activate exactly one of the available configuration persistence manager and also how to retrieve the configuration db. The configuration db can either be a valid db engine or a file from here.


As of this release there are three different file based configuration managers available (from simple to sophisticated):
		1. INI file		- very simple options on what to track
		2. simple XML	- more options on what and where to track
		3. complex XML	- very sophisticated and fine grained options

INI file configuration
is a very simple ini file in which you can setup constraints for 3 different areas: term, language and site. Every constraint given applies to all crawlers and there is no possibility to distinguish between different customers or domains. This restriction can have a serious impact and can in worst case lead to runtime errors. Consider this: you setup a constraint against a site from one network, say Lithium, and the crawler for Facebook now tries to read this site - which probably doesn't even exist in Facebook universe - bang.
In addition, there is also no type safety guaranteed, 
as the values given are all handled as Strings. Because of this 
with ini file based configuration you cannot setup a constraint
against specific users or geo locations. For the above given reasons, you should only consider using ini file based configuration in the most simplest scenario, possibly with only constraining one crawler on some keywords. In any other case, you should at least opt for the simple xml configuration option.
The standard configuration file is
		* CrawlerConfiguration.ini


Simple XML configuration
Is a simple xml based file. With this, on top of what the ini file based option gives you, you get the ability to setup all 5 categories (term, user, language, site and geo location) per network or for all networks.
Constraints given in the section for ALL networks are appended to the constraints provided for a specific network (e.g. Twitter) and the different crawlers are then instantiated with the combined constraints (or search parameter). 
The standard configuration file is
		* CrawlerConfiguration.xml


Complex XML configuration
puts additional possibilities to simple xml configuration. With this option, you can have different crawler configurations for different domains (a domain is an area of business, like banking or group of people sharing the same interest) and/or customers and for every crawler. 
For your convenience I provided 3 examples:
		* CustomerSpecificCrawlerConfiguration.xml
		shows how to setup the file for different customers and
		with no domain
		* DomainSpecificCrawlerConfiguration.xml
		shows how to setup the file for different domains but not
		for individual customers
		* CustomerAndDomainCrawlerConfiguration.xml
		combines both variants

Please take a look at the configuration files itself for 
an explanation on how to setup constraints according to 
your needs.
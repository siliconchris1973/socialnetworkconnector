In this directory you can find the files for the actual
crawler configuration. The SNC has multiple ways of 
setting up the constraints on what to track or search
within the different social networks.

You can setup the configuration manager to use in the 
applicationContext.xml file (one directory up from here).
In there you activate exactly one of the available 
configuration persistence manager and also how to retrieve
the configuration db. The configuration db can either be a
valid db engine or a file from here.


As of this release there are three different file based 
configuration managers available (from simple to 
sophisticated):
		1. INI file		- very simple options on what to track
		2. simple XML	- more options on what and where to track
		3. complex XML	- very sophisticated and fine grained options

INI file configuration
is a very simple ini file in which you can setup constraints 
for the 5 different areas: term, user, language, site and 
geo location. Every constraint given applies to all crawlers 
and there is no possibility to distinguish between different
customers or domains. In addition, there is also no type 
safety guaranteed, as the values given are all handled as
Strings. So there is no guarantee, that a user-id (being
for example a long in one network) is actually treated as
a long.
The standard configuration file is
		* CrawlerConfiguration.ini


Simple XML configuration
is a simple xml based file. With this, on top of what the 
ini file based option gives you, you get the ability to 
setup the 5 categories per network or for all networks.
The standard configuration file is
		* CrawlerConfiguration.xml


Complex XML configuration
puts additional possibilities to simple xml configuration. 
With this option, you can have different crawler 
configurations for different domains an/or customers and 
for every crawler. 
For your convenience I provided 3 examples:
		* CustomerSpecificCrawlerConfiguration.xml
		* DomainSpecificCrawlerConfiguration.xml
		* CustomerAndDomainCrawlerConfiguration.xml

Please take a look at the configuration files itself for 
an explanation on how to setup constraints according to 
your needs.
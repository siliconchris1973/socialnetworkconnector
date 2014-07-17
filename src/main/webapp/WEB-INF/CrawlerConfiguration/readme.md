In this directory you can find the files for the actual
crawler configuration. The SNC has multiple ways of 
setting up the constraints on what to track or search
within the different social networks.

All file based configuration options store the actual
configuration file herein, whereas the db configuration
manager obviously has no files in here.

As of this release there are three different file based 
configuration managers available (from simple to 
sophisticated):
1. INI file
2. simple XML
3. complex XML

INI file configuration
CrawlerConfiguration.ini is a very simple ini file
in which you can setup constraints for the 4 different
areas: term, user, language, site and geo location.
Every constraint given applies to all crawlers and there 
is no possibility to distinguish between different 
customers or domains.

Simple XML configuration
CrawlerConfiguration.xml is a simple xml based file. 
With this option, on top of what the ini file based 
option gives you, you get the ability to setup the 5
categories individual for different crawlers.

Complex XML configuration
CustomerAndDomainCrawlerConfiguration.xml puts additional
options to simple xml configuration. With this option,
you can have different crawler configurations for different 
domains an/or customers and for every crawler. For your
convenience I provided 3 examples:
* CustomerSpecificCrawlerConfiguration.xml
* DomainSpecificCrawlerConfiguration.xml
* CustomerAndDomainCrawlerConfiguration.xml

Please take a look at the configuration for an example on 
how to setup constraints according to your needs.
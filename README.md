SocialNetworkConnector

The Social Network Connector is part of the Social Activity Analyzer.
SNC is used to crawl social networks (like twitter, Facebook et. al.) and grab posts and messages according to constraints configurable - like specific terms, or users.

SNC was designed to be highly configurable. Take a look in the directory 
	webapp/WEB_INF
In there you'll find all the files necessary to adapt SNC to your needs. 

applicationContext.xml
applicationContext.xml is used to setup the basic runtime environment of SNC, like persistence and which configuration manager to use. Take a look at the XML structure, it has loads of comments to show you what is used for what.


CrawlerConfiguration
In CrawlerConfiguration you'll find the files to configure and adapt the crawler to your needs. take a look at the read me.md file there.


GeneralConfiguration.xml 
GeneralConfiguration.xml is used to define things like XML structure for the crawler configuration but also some runtime definition, like warnings and special persistence options.


HANA_Configuration.xml
HANA_Configuration.xml defines the schema and layout of the HANA database and max field sizes, as needed by the HANA persistence manager. You normally do not need to do anything in here.


web.xml
web.xml is used to define certain aspects of the web based administration interfaces for the SNC. You normally do not need to do anything in here.


log4j.xml
Setup your logging preferences.


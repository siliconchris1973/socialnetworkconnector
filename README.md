SocialNetworkConnector

The Social Network Connector is part of the Social Activity Analyzer.
SNC is used to crawl social networks (like twitter, Facebook et. al.) and grab posts and messages according to constraints configurable - like specific terms, or users.

SNC was designed to be highly configurable. Take a look in the directory 
	webapp/WEB_INF
In there you'll find all the files necessary to adapt SNC to your needs. 

applicationContext.xml
applicationContext.xml is used to setup the basic runtime environment of SNC, like persistence and which configuration manager to use. You also define in here, which crawler to start. Take a look at the XML structure, it has loads of comments to show you what is used for what.


SNC_Crawler_Configuration
In SNC_Crawler_Configuration you'll find the files to configure and adapt the crawler to your needs. take a look at the readme.md file there.


SNC_Runtime_Configuration.xml 
SNC_Runtime_Configuration.xml is used to define the overall XML structure for the crawler configuration but also some runtime definition, like warnings and special persistence options.


SNC_HANA_Configuration.xml
HANA_Configuration.xml defines the schema and layout of the HANA database and max field sizes, as needed by the HANA persistence manager. You normally do not need to do anything in here, except if you change the db layout, the schema or the field types of the HANA db.


SNC_Administration-servlet.xml
SNC_Administration-servlet.xml defines the beans needed to setup the MVC for the web based administration and monitoring system for the SNC. You normally do not need to do anything in here.


web.xml
web.xml is used to define certain aspects of the web based administration interfaces for the SNC. You normally do not need to do anything in here.


log4j.xml
Setup your logging preferences.


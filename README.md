SocialNetworkConnector

The Social Network Connector is part of the Social Activity Analyzer.
SNC is used to crawl social networks (like twitter, Facebook et. al.) and grab posts and messages according to constraints configurable - like specific terms, or users.

The idea behind the SNC is it, to have a single crawler that connects to a social network, a website, a forum or an rss feed and take whatever it finds that matches the domain of interest. This domain of interest can be, for example, Banking. So within banking we know a lot of keywords that are common (like Money, quite obvious) and we track every message, every post or every article of the given sites and networks containing any of these words.

In the second step the SNC breaks the different message- or post types apart and creates a uniform post-type of them. After doing so, these posts are passed along to the database where it is stored for further analysis and processing.



SNC was designed to be highly configurable. Take a look in the directory 
	webapp/WEB_INF
In there you'll find all the files necessary to adapt SNC to your needs. 

applicationContext.xml
applicationContext.xml is used to setup the basic runtime environment of SNC, like persistence and which configuration manager to use. You also define in here, which crawler to start. Take a look at the XML structure, it has loads of comments to show you what is used for what.


SNC_Crawler_Configuration
In SNC_Crawler_Configuration you'll find the files to configure and adapt the crawler to your needs. take a look at the readme.md file there.


SNC_Runtime_Configuration.xml 
SNC_Runtime_Configuration.xml is used to define the overall XML structure for the crawler configuration but also some runtime definition, like warnings and general persistence options.


SNC_HANA_Configuration.xml
HANA_Configuration.xml defines the schema and layout of the HANA database and max field sizes, as needed by the HANA persistence manager. You normally do not need to do anything in here, except if you change the db layout, the schema or the field types of the HANA db.


SNC_Administration-servlet.xml
SNC_Administration-servlet.xml defines the beans needed to setup the MVC for the web based administration and monitoring system for the SNC. You normally do not need to do anything in here.


web.xml
web.xml is used to define certain aspects of the web based administration interfaces for the SNC. You normally do not need to do anything in here.


log4j.xml
Setup your logging preferences.


SocialNetworkConnector

The Social Network Connector is part of the Social Activity Analyzer.
SNC is used to crawl social networks (like twitter, Facebook et. al.) and grab posts and messages according to constraints configurable - like specific terms, or users.

The idea behind the SNC is it, to have a single crawler that connects to a social network, a website, a forum or an rss feed and take whatever it finds that matches the domain of interest. This domain of interest can be, for example, Banking. So within banking we know a lot of keywords that are common (like Money, quite obvious) and we track every message, every post or every article of the given sites and networks containing any of these words.

In the second step the SNC breaks the different message- or post types apart and creates a uniform post-type of them. After doing so, these posts are passed along to the database where it is stored for further analysis and processing.



SNC was designed to be highly configurable. Take a look in the directory 
	webapp/WEB_INF
In there you'll find all the files necessary to adapt SNC to your needs. 

applicationContext.xml
applicationContext.xml is used to setup the low-level environment of SNC. In here you define which persistence manager, which configuration manager and what cryptographic support, if any, to use. Additionally the available crawler and their repsective start behaviour is defined in this file. Take a look at the XML structure, it has loads of comments to show you what is used for what.


SNC_Crawler_Configuration
In SNC_Crawler_Configuration you'll find the files to setup the search patterns for the crawler. You should take a look at the readme.md in the directory there.


SNC_Runtime_Configuration.xml 
SNC_Runtime_Configuration.xml is used to adapt the overall runtime behaviour of the SNC. Every tunable aspect of the SNC's runtime behaviour can be configued in this file. You can, for example, configure if the system shall create a json file on disk, in case the creation of a dataset in the db fails. Also threading for the different parts of the SNC can be configured here. Third, the XML structure for the configuration files of the actual crawler is configured in this file. Fourth, some general data definitions are set - like whether or not to truncate specific fields (according to their respective field length) and if text shall be saved with or without markup elements (like html). 


SNC_HANA_Configuration.xml
HANA_Configuration.xml defines the schema and layout of the HANA database and max field sizes, as needed by the HANA persistence manager. You normally do not need to do anything in here, except if you change the db layout, the schema or the field types of the HANA db.


SNC_Administration-servlet.xml
SNC_Administration-servlet.xml defines the beans needed to setup the MVC for the web based administration and monitoring system for the SNC. You normally do not need to do anything in here.


web.xml
web.xml is used to define certain aspects of the web based administration interfaces for the SNC. You normally do not need to do anything in here.


log4j.xml
Setup your logging preferences.


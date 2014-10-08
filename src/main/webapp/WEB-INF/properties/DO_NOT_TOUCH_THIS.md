The files in this directory are needed for dynamic class loading and linking
during program execution. You only need to adapt anything in here, in case 
you develop a new service, say a new parser.

In case you want to change the name or location of this file, you have to 
adapt the setting ParserListFilePath in SNC_Runtime_Configuration.xml!


For parser:
The file Parser.xml contains all available web parser. Upon instantiation it
is queried by ParserControl to get a list of all available web parser. This is
needed, so that WebParserControl can be extended by new parsers. The idea is
that WebParserControl will execute anyone of the available web parser based on
a decision, whether the parser can decode a given site. It determines the
correct parser by asking every parser (defined in Parser.xml) whether or not
it can decode the website in question. The first parser to return true is then
dynamically loaded and the parse-method executed.


To develop a new parser (only for a new web parser as of this moment), you
create a new parser class (e.g. NetmomsParser.class) which implements the
Interface IWebParser. You then implement your specific parser code and most
important implement the method canExecute(). canExecute must return true, in 
case the parser is capable of parsing the specific website, and false in any
other case. After creating the parser, add it to the file Parser.xml and be
sure to add it above the SimpleWebParser as this is is the fallback parser,
which only extracts 30 words before and after a given word, thus can handle
any site and consequently will always return true.



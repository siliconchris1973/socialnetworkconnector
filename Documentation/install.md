install glassfish 4.0

set CDI-param
bin/asadmin set configs.config.server-config.cdi-service.enable-implicit-cdi=false

copy xerces libs to glassfish
cp <your m2 directory>/repository/xerces/xercesImpl/2.11.0/xercesImpl-2.11.0.jar /opt/glassfish4/glassfish/domains/domain1/lib/ext/

cp <your m2 directory>/repository/xml-apis/xml-apis/1.0.b2/xml-apis-1.0.b2.jar /opt/glassfish4/glassfish/domains/domain1/lib/ext/


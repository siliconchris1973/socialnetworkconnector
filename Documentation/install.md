install Apache Tomcat AS 6, 7 or 8

set runtime parameter for JMX Access in Tomcat:
CATALINA_OPTS="${CATALINA_OPTS} -Djava.rmi.server.hostname=JMX_HOST" CATALINA_OPTS="${CATALINA_OPTS} -Djavax.management.builder.initial=" CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote=true" CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote.port=JMX_PORT" CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote.ssl=false" CATALINA_OPTS="${CATALINA_OPTS} -Dcom.sun.management.jmxremote.authenticate=false"

register quartz scheduler in JMX
edit quartz.properties and add:
org.quartz.scheduler.jmx.export = true
org.quartz.scheduler.jmx.objectName = de.comlineag.snc.appstate.MasterControlProgram:type=MCP,name=1

deploy war file to AS

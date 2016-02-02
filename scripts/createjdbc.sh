mqsideleteconfigurableservice IB9NODE -c JDBCProviders -o PPC2
mqsicreateconfigurableservice IB9NODE -c JDBCProviders -o PPC2

mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n connectionUrlFormat -v jdbc:oracle:thin:[user]/[password]@[serverName]:[portNumber]:[connectionUrlFormatAttr1]
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n connectionUrlFormatAttr1 -v XE
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n databaseType -v oracle
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n jarsURL -v /opt/oracle/instantclient_11_2/jlib/
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n maxConnectionPoolSize -v 10
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n jdbcProviderXASupport -v false
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n type4DriverClassName -v oracle.jdbc.OracleDriver

mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n serverName -v 10.211.55.4
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n portNumber -v 1521

mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n databaseName -v PPC2
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n databaseSchemaNames -v useProvidedSchemaNames
mqsichangeproperties IB9NODE -c JDBCProviders -o PPC2 -n securityIdentity -v ppc2@xe

mqsisetdbparms IB9NODE -n jdbc::ppc2@xe -u inw -p inw

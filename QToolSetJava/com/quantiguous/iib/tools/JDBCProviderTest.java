package com.quantiguous.iib.tools;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.w3c.dom.Document;

import com.quantiguous.iib.tools.model.*;


import com.ibm.broker.config.proxy.ConfigurableService;
import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbJavaException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class JDBCProviderTest extends MbJavaComputeNode {
	
	private JAXBContext mJAXBContext = null;
	
	public void onInitialize() throws MbException {
		try {
			mJAXBContext = JAXBContext.newInstance("com.quantiguous.iib.tools.model");
		} catch (JAXBException e) {
			MbUserException mbue = new MbUserException(this, "onInitialize()",
					"", "", e.toString(), null);
			throw mbue;
		}
	}

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");

		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage();

			ConfigurableService[] jdbcProviders = QBrokerProxy.getJDBCProviders();
			
			JdbcProvidersType outJdbcProviders = new JdbcProvidersType();		
			
			for ( ConfigurableService jdbcProvider : jdbcProviders ) {
				Properties properties = jdbcProvider.getProperties();
				JdbcProviderType outJdbcProvider = new JdbcProviderType();
				
				outJdbcProvider.setName(jdbcProvider.getName());
				outJdbcProvider.setConnectionUrlFormat(properties.getProperty("connectionUrlFormat"));
				outJdbcProvider.setDatabaseName(properties.getProperty("databaseName"));
				outJdbcProvider.setDatabaseSchemaNames(properties.getProperty("databaseSchemaNames"));
				outJdbcProvider.setDatabaseType(properties.getProperty("databaseType"));
				outJdbcProvider.setDatabaseVersion(properties.getProperty("databaseVersion"));
				outJdbcProvider.setDescription(properties.getProperty("description"));
				outJdbcProvider.setEnvironmentParms(properties.getProperty("environmentParms"));
				outJdbcProvider.setJarsURL(properties.getProperty("jarsURL"));
				outJdbcProvider.setMaxConnectionPoolSize(properties.getProperty("maxConnectionPoolSize"));
				outJdbcProvider.setPortNumber(properties.getProperty("portNumber"));
				outJdbcProvider.setSecurityIdentity(properties.getProperty("securityIdentity"));
				outJdbcProvider.setServerName(properties.getProperty("serverName"));
				
				/* we now run some tests */
				Connection sqlConnection = null;
				TestResultsType testResults = new TestResultsType();
				TestResultType testResult;
				
				/* we try and connect to the server/port */					
				testResult = testSocketConnect(outJdbcProvider.getServerName(), outJdbcProvider.getPortNumber());
				testResults.getTestResult().add(testResult);

				// if we are able to connect, we check if we can load the JDBC Driver 
				if (testResult.isTestSuccessful()) {
					sqlConnection = establishJDBCConnection(outJdbcProvider.getName(), testResults);
									
					// if we received the Connection object, we do further tests on the database 
					if ( sqlConnection != null ) {
						outJdbcProvider.setMetaData(getMetaData(sqlConnection));
					}
				}

				if ( sqlConnection != null ) {
					try { sqlConnection.close(); } catch (Exception e) { ; }
				}

				/* completed all test */								
				outJdbcProvider.setTestResults(testResults);
				
				/* add to the providers collection */
				outJdbcProviders.getJdbcProvider().add(outJdbcProvider);
			

			}

			Document outDocument = outMessage.createDOMDocument(MbXMLNSC.PARSER_NAME);
			ObjectFactory objectFactory = new ObjectFactory();
	        JAXBElement<JdbcProvidersType> je =  objectFactory.createJdbcProviders(outJdbcProviders);
	        
			mJAXBContext.createMarshaller().marshal(je, outDocument);

			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
				
			
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}
	
	private TestResultType testSocketConnect(String serverName, String portNumber) {
		TestResultType testResult = new TestResultType();
		testResult.setTestName("socketConnection");
		
		Socket client=new Socket();  
		try {
			client.connect(new InetSocketAddress(serverName,Integer.parseInt(portNumber)),2000);
			testResult.setTestSuccessful(true);
			try { client.close();} catch (Exception e) {;}
			return testResult;
		} catch (Exception e) {
			testResult.setTestSuccessful(false);
			testResult.setTestException(e.toString());
			return testResult;
		}
	}
	
	private Connection establishJDBCConnection(String providerName, TestResultsType testResults) {
		TestResultType testResult = new TestResultType();
		Connection sqlConnection = null;
		testResult.setTestName("jdbcConnection");

		try {
			sqlConnection = getJDBCType4Connection(providerName, JDBC_TransactionType.MB_TRANSACTION_AUTO);
			testResult.setTestSuccessful(true);
			testResults.getTestResult().add(testResult);
			return sqlConnection;
			
		} catch (MbException e) {
			testResult.setTestSuccessful(false);
			testResult.setTestException(serialiseMbException(e));
			testResults.getTestResult().add(testResult);
			return null;			
		} catch (Exception e) {
			testResult.setTestSuccessful(false);
			testResult.setTestException(e.toString() + e.getMessage());
			testResults.getTestResult().add(testResult);
			return null;
		} 
	}
	
	private DatabaseMetaDataType getMetaData(Connection sqlConnection) {
		DatabaseMetaDataType metaData = new DatabaseMetaDataType();
		
		try {
			metaData.setSchema(sqlConnection.getCatalog());
			DatabaseMetaData dbMetaData = sqlConnection.getMetaData();
			
			metaData.setDatabaseMajorVersion(dbMetaData.getDatabaseMajorVersion());
			metaData.setDatabaseMinorVersion(dbMetaData.getDatabaseMinorVersion());
			metaData.setDatabaseURL(dbMetaData.getURL());
			metaData.setDriverName(dbMetaData.getDriverName());
			metaData.setDriverVersion(dbMetaData.getDriverVersion());
			metaData.setJdbcMajorVersion(dbMetaData.getJDBCMajorVersion());
			metaData.setJdbcMinorVersion(dbMetaData.getJDBCMinorVersion());
			metaData.setProductName(dbMetaData.getDatabaseProductName());
			metaData.setProductVersion(dbMetaData.getDatabaseProductVersion());
			metaData.setUserName(dbMetaData.getUserName());
			
			ResultSet rs = dbMetaData.getCatalogs();
			
			while (rs.next()) { 
				metaData.getCatalogs().getCatalog().add(rs.getString("TABLE_CAT"));
			}
			
			rs = dbMetaData.getSchemas();

			while (rs.next()) { 
				metaData.getSchemas().getSchema().add(rs.getString("TABLE_SCHEM"));
			}
			
			return metaData;
			
		} catch (Exception e) {
			return metaData;
		} 
	}	
	

	
	private String serialiseMbException(MbException ex) {
		String msg = "";
		if (ex != null) {
			msg += ex.toString();
			msg += ex.getTraceText();
			msg += ex.getMessage();
			

			for (Object insert : ex.getInserts()) {
				msg += insert.toString();
			}
			
			for (StackTraceElement st : ex.getStackTrace()) {
				msg += st.toString();
			}
			
			MbException nestedExceptions[] = ex.getNestedExceptions();
			
			for (MbException nestedEx : nestedExceptions) {
				if ( nestedEx instanceof MbJavaException ) {
					
					msg += ((MbJavaException)ex).getThrowable().toString();
					
				} else {
					msg += serialiseMbException(nestedEx);
				}
			}			
		}
		return msg;
	}
}

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.List;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;


public class X_JavaCompute extends MbJavaComputeNode {

	@SuppressWarnings("unchecked")
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		
		Connection providerConnection = null; // a connection to the provider, separate from the connection to apprise 
		String JDBCProviderName; // this will be the providerName from the topic
		CallableStatement stmt = null;

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// get the provider name from the query string
			// this'll be replaced to the providerName obtained from the topic model
			String fileNameXPath = "HTTP/Input/QueryString/providerName";
			List<MbElement> fileNameElementList = ((List<MbElement>) inAssembly.getLocalEnvironment().getRootElement().evaluateXPath(fileNameXPath));
			JDBCProviderName = fileNameElementList.get(0).getValueAsString();
			
			providerConnection = getJDBCType4Connection(JDBCProviderName, JDBC_TransactionType.MB_TRANSACTION_AUTO);
			String sql = "{call qg_get_contact_detail(?,?,?)}"; // the name of the procedure is the same in all providers
			stmt = providerConnection.prepareCall(sql);
			
			stmt.setString(1, "1234"); // customerId
			stmt.registerOutParameter(2, java.sql.Types.VARCHAR); // mobileNo
			stmt.registerOutParameter(3, java.sql.Types.VARCHAR); // emailId
			
			stmt.execute();
			
			
			String mobileNo = stmt.getString(2);
			String emailId = stmt.getString(3);

			stmt.close();
			
			MbMessage outMessage = new MbMessage();
			copyMessageHeaders(inMessage, outMessage);
			MbElement parserElement = outMessage.getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
			MbElement rootElement = parserElement.createElementAsLastChild(MbElement.TYPE_NAME);
			rootElement.setName("result");
			rootElement.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "mobileNo", mobileNo);
			rootElement.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "emailId", emailId);

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

    private void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
            throws MbException {
    	MbElement outRoot = outMessage.getRootElement();

	    // iterate though the headers starting with the first child of the root
	    // element
	    MbElement header = inMessage.getRootElement().getFirstChild();
	    while (header != null && header.getNextSibling() != null) // stop before
	    // the last
	    // child
	    // (body)
	    {
	            // copy the header and add it to the out message
	            outRoot.addAsLastChild(header.copy());
	            // move along to next header
	            header = header.getNextSibling();
	    }
    }
}

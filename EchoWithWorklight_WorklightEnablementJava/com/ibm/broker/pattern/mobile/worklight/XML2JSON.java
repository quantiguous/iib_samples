package com.ibm.broker.pattern.mobile.worklight;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.*;


public class XML2JSON extends MbJavaComputeNode {

	public void onInitialize() throws MbException{
		Utility.initialize();
	}
	
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbMessage inMessage = inAssembly.getMessage();

		// create new message
		MbMessage outMessage = new MbMessage();
		MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly,
				outMessage);

		try {

			copyMessageHeaders(inMessage, outMessage);

			MbElement jsonDataElement = outMessage.getRootElement().createElementAsLastChild(MbJSON.PARSER_NAME).createElementAsFirstChild(MbElement.TYPE_NAME);
			jsonDataElement.setName(MbJSON.DATA_ELEMENT_NAME);

			MbElement xmlnscElement = inMessage.getRootElement().getLastChild();
			if(null==xmlnscElement ){
				MbUserException mbue = new MbUserException(this, "evaluate()", "",
						"", "No body", null);
				throw mbue;
			}
			jsonDataElement.copyElementTree(xmlnscElement);
			Utility.unsetNameSpaces(jsonDataElement);
			Utility.convertArrays(jsonDataElement);			

			out.propagate(outAssembly);

		} catch (Throwable e) {
			// Example Exception handling	
			MbUserException mbue = new MbUserException(this, "evaluate()", "",
					"", e.toString(), null);
			throw mbue;
		}
	}
	
	public void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
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

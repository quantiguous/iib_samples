package com.ibm.broker.pattern.mobile.worklight;
import java.util.List;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.*;


public class JSON2XML extends MbJavaComputeNode {

	
	private static String destinationListXPathString = "?Destination/?RouterList/DestinationData/labelName";
	private static String requestURIXPathString = "HTTP/Input/RequestLine/RequestURI";
	private static MbXPath destinationListXPath; 
	private static MbXPath requestURIXPath;
	
	public void onInitialize() throws MbException{
		destinationListXPathString = new String("?Destination/?RouterList/DestinationData/labelName");
		requestURIXPathString      = new String("HTTP/Input/RequestLine/RequestURI");
		destinationListXPath       = new MbXPath(destinationListXPathString);
		requestURIXPath            = new MbXPath(requestURIXPathString);
		Utility.initialize();
	}
	
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		

		MbMessage inMessage = inAssembly.getMessage();

		// create new message
		MbMessage outMessage = new MbMessage();
		MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly,
				outMessage);

		MbMessage localEnvironment = outAssembly.getLocalEnvironment();
		MbElement localEnvironmentRoot = localEnvironment.getRootElement();
		List xpathResult = (List)localEnvironmentRoot.evaluateXPath(requestURIXPath); 
		MbElement requestURIElement = (MbElement)xpathResult.get(0);
		String requestURI  = requestURIElement .getValueAsString();
		String httpBaseURL = Utility.getBaseURL();
		String operationName = requestURI.substring(httpBaseURL.length());   
		
		xpathResult = (List)localEnvironmentRoot.evaluateXPath(destinationListXPath);
		MbElement destinationDataElement = (MbElement)xpathResult.get(0);
		destinationDataElement.setValue(operationName);
		
		// optionally copy message headers
		copyMessageHeaders(inMessage, outMessage);

		//copy JSON to XML
		//TODO rename any arrary Item elements			
		MbElement xmlnscElement = outMessage.getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
		MbElement jsonDataElement = inMessage.getRootElement().getLastChild().getFirstElementByPath(MbJSON.DATA_ELEMENT_NAME);
			
		//visit the XML tree and patch up the namespace
		Utility.setNameSpaces(xmlnscElement);
		
		copyChildren(jsonDataElement,xmlnscElement);
		out.propagate(outAssembly);
		
	}
	
	private void createChildAsCopy(MbElement jsonElement,MbElement xmlnscParentElement,String nameOverride) throws MbException{
		int type = jsonElement.getType();
		MbElement childXmlnscElement = xmlnscParentElement.createElementAsLastChild(type);
		if(0!=(MbElement.TYPE_VALUE & type)){
			//copy the value
			childXmlnscElement.setValue(jsonElement.getValue());			
		}		
		String name;		
		if(null==nameOverride){
			name=jsonElement.getName();
		}else{
			name=nameOverride;
		}		
		childXmlnscElement.setName(name);
		copyChildren(jsonElement,childXmlnscElement);
	}
	
	private void copyChildren(MbElement jsonElement,MbElement xmlnscElement) throws MbException{
		MbElement childJsonElement = jsonElement.getFirstChild();
		
		while (null!=childJsonElement ){			
			if(childJsonElement.getSpecificType()==MbJSON.ARRAY){
				String name = childJsonElement.getName();
				MbElement jsonArrayItem =  childJsonElement.getFirstChild();
				while(null!=jsonArrayItem ){
					createChildAsCopy(jsonArrayItem,xmlnscElement,name);
					jsonArrayItem=jsonArrayItem.getNextSibling();
				}
			}else{
				createChildAsCopy(childJsonElement,xmlnscElement,null);
			}
			childJsonElement = childJsonElement.getNextSibling();
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

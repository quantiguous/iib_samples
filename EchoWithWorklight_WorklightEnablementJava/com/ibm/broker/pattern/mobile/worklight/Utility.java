package com.ibm.broker.pattern.mobile.worklight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbJSON;
import com.ibm.broker.plugin.MbXPath;

public class Utility {

	private static final String targetNamespace = "http://EchoService";
	private static final String httpBaseURL = "/Worklight/EchoWithWorklight/";
	private static Map<String,String[]> arrayPaths = createMap();
	private static Map<String,List<MbXPath>> arrayXPaths = null;
	private static String namespaceDeclarationXPathString = "//*[namespace-uri()='http://www.w3.org/2000/xmlns/']";
    private static MbXPath namespaceDeclarationXPath;
	private static Map<String, String[]> createMap() {
		Map<String,String[]> result = new HashMap<String,String[]>();
					
	    result.put("echoResponse",new String[]{});
		return Collections.unmodifiableMap(result);
	}
	
	//this method locks, do not call it often. Hint call it from onInitialize
	static void initialize() throws MbException{
		synchronized(arrayPaths){
			if(null!=arrayXPaths){
				return;
			}
			arrayXPaths = new HashMap <String,List<MbXPath>> ();
			for(Map.Entry<String, String[]> entry : arrayPaths.entrySet() ){
				String key = entry.getKey();
				String [] paths = entry.getValue();
				List <MbXPath> xpathList = new ArrayList<MbXPath>();
				
				for(int index =0 ; index<paths.length ;index++){
					MbXPath xPath = new MbXPath(paths[index]);
					xpathList.add(xPath);
				}
				arrayXPaths.put(key, xpathList);
			}
			namespaceDeclarationXPath=new MbXPath(namespaceDeclarationXPathString);
			
		}
	}

	static List<MbXPath> getPaths(String rootElementName){
		return arrayXPaths.get(rootElementName);
	}

	public static void convertArrays(MbElement dataRoot) throws MbException {
		MbElement rootElement=dataRoot.getFirstChild();
		if(rootElement==null){
			return;
		}

		List<MbXPath> arrayPaths = getPaths(rootElement.getName());
		for(MbXPath mbXPath : arrayPaths){
			
			Object xPathResult = dataRoot.evaluateXPath(mbXPath);
			if(null!=xPathResult){
				List arrayList = (List)xPathResult;
				if(arrayList.size()>0){
					int index=0;
					MbElement nextElement = (MbElement) arrayList.get(index);
					//	create the array element and then move all elements into it
					MbElement arrayElement = nextElement.createElementBefore(MbElement.TYPE_NAME);
					arrayElement.setSpecificType(MbJSON.ARRAY);
					arrayElement.setName(nextElement.getName());
					
					nextElement.detach();
					arrayElement.addAsLastChild(nextElement);
					index++;
					//move any others down too
					while(index<arrayList.size()){
						nextElement=(MbElement) arrayList.get(index);
						nextElement.detach();
						arrayElement.addAsLastChild(nextElement);
						index++;
					}
				}
			}
		}
	}
	
	public static void unsetNameSpaces(MbElement jsonDataRootElement) throws MbException {
			MbElement rootElement=jsonDataRootElement.getFirstChild();
			if(rootElement==null){
				return;
			}	
			
			unsetNameSpacesInner(rootElement);
	}
			
	private static void unsetNameSpacesInner(MbElement element) throws MbException {
			element.setNamespace(""); 
			List<MbElement> namespaceDeclarations = (List<MbElement>)element.evaluateXPath(namespaceDeclarationXPath);
			for(MbElement nextElement : namespaceDeclarations ){
				nextElement.delete();
			}
	}
	
	
	public static void setNameSpaces(MbElement xmlnscElement) throws MbException {
		MbElement child =xmlnscElement.getFirstChild(); 
		if (null!=child){
			child.setNamespace(targetNamespace);
		}
	}


	public static String getBaseURL() {
		return httpBaseURL;
	}
}
	
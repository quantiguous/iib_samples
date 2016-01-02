package com.quantiguous.iib.tools;

import com.ibm.broker.config.proxy.*;

public class QBrokerProxy {
	private static  BrokerProxy b;
	private static Object mutex= new Object();
	
	private static void initialize() {	 
		if(null!=b) {
			return;
		}
		try {
			synchronized (mutex) {
				b = BrokerProxy.getLocalInstance();
				while(!b.hasBeenPopulatedByBroker()) { Thread.sleep(100); } 
			}
		} catch  (ConfigManagerProxyLoggedException e){
			;
		} catch (InterruptedException e) {
			;
		}
	}
	
	private static ConfigurableService[] getConfigurableServices(String serviceType) throws ConfigManagerProxyPropertyNotInitializedException, IllegalArgumentException {
		initialize();
		return b.getConfigurableServices(serviceType);
	}
	
	public static ConfigurableService[] getJDBCProviders() throws ConfigManagerProxyPropertyNotInitializedException, IllegalArgumentException {
		return getConfigurableServices("JDBCProviders");
	}
}

iib samples
===========

A collection of small & big samples created while getting dirty with the IBM Integration Bus!


EchoService
-----------

The echo service demostrates a trivial implementation of a SOAP service, and its REST enablement by applying the Mobile pattern of the toolkit.

The pattern applies some constriaints on the implementation of the SOAP service, which needs careful refactoring to avoid nasty suprises. Some of these are

* the generated code will result in warnings in eclipse, this can be easily fixed (see the commit history for what was changed in this project)
* the JSON2XML node does not seem to apply the namespace correctly, the XML generated is devoid of any namespace, so the coorelation paths used in the SOAP service need to be refactored to avoid referecning the namepace.
* the XML2JSON node keeps the propagate within a try-catch, this means that the any exceptions in sending the reply are garbled up and not reported propertly, moving the propagate out of the try-catch ensures that exception handling is corret.
* Ensure that the EG is configured such that both SoapInput nodes and HTTPInput nodes use the same listener. (either the broker wide listener or the embedded listner).

The last point is especially tricky as by default, SOAPInput uses the embededded HTTP listener, while the HTTPInput uses the broker wide listener. If this is kept as default, then you'll get a nasty error

	Exception. BIP2230E: Error detected whilst processing a message in node 'gen.EchoWithWorklight_EchoService_worklight.HTTP Reply'. : /build/slot2/S000_P/src/WebServices/WSLibrary/ImbWSReplyNode.cpp: 968: ImbWSReplyNode::evaluate: ComIbmWSReplyNode: gen/EchoWithWorklight_EchoService_worklight#FCMComposite_1_5 BIP3745E: The node ''gen.EchoWithWorklight_EchoService_worklight.HTTP Reply'' received a message with an invalid 'HTTP' 'RequestIdentifier'. : /build/slot2/S000_P/src/WebServices/WSLibrary/ImbWSReplyNode.cpp: 882: ImbWSReplyNode::evaluate: ComIbmWSReplyNode: gen/EchoWithWorklight_EchoService_worklight#FCMComposite_1_5 BIP3740E: An attempt was made to use an invalid 'Unknown' message identifier '414d5120494239514d4752202020202042701a54029d1420'. : /build/slot2/S000_P/src/WebServices/WSLibrary/ImbSOAPHandleManager.cpp: 1899: ImbSOAPHandleManager::retrieveInputNodeMessageHandle: :

To run the sample, 
* Use SoapUI Toolkit for the [SOAP Endpoint](http://localhost:7800/EchoService/EchoService)
* Use PostMan for the [HTTP Endpoint](http://localhost:7800/Worklight/EchoWithWorklight/echo)


SadService
-----------

The SadService demostrates transaction management and failure processing in the broker.

TimeOut processing:
To demostrate the timeout processing, configure 2 datasources and create the AUDITRECORDS table (the data design project is also available). 
The operation takes a single paramter, which is the timeoutInteval, this is the time the service takes to respond back (in an attempt to mimic a long operation). This is implemented via the ESQL SLEEP statement.
The SOAPInput node is configured for 5 second timeout, so
* if you invoke the opertion with a timeout less than 5 seconds, the flow completes successfully, and you'll see 3 audit records in the table.
* if you invoke the operation witha  timeout more than 5 seconds, the SOAPInput node timesout and marks the flow to issue a rollback. At this time you will see 2 audit records in the table.

There are 3 compute nodes in the flow, 2 of them are configured for un-coordinated transactions (transation mode = commit), and 1 is configured for a co-ordinated transation (transaction mode - automatic). Irrespective of the whether the msg flow commits or rollsback, 2 nodes will always commit. 1 node however will obey the message flow, and will rollback (in case of a timeout) or commit (in case of a successful completion).

What is important to understand here is that a timeout does not abort the execution of the message flow prematurely, the timeout merely does the following
* replies back to the client on the expiry of the timeout
* fires a rollback instead of a commit as and when the message flow completes.

The message flow infact tries to send a reply as well, but that fails with BIP3745E, because a reply (timeout) has already been sent. You can see this in the trace.

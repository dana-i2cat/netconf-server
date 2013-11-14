package net.i2cat.netconf.server.transport;

import java.util.ArrayList;

import net.i2cat.netconf.errors.NetconfProtocolException;
import net.i2cat.netconf.messageQueue.MessageQueue;
import net.i2cat.netconf.rpc.Capability;
import net.i2cat.netconf.rpc.Error;
import net.i2cat.netconf.rpc.ErrorSeverity;
import net.i2cat.netconf.rpc.ErrorTag;
import net.i2cat.netconf.rpc.ErrorType;
import net.i2cat.netconf.rpc.Hello;
import net.i2cat.netconf.rpc.Operation;
import net.i2cat.netconf.rpc.Query;
import net.i2cat.netconf.rpc.SerializableReply;
import net.i2cat.netconf.transport.TransportContentParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * TRansport content parser for Netconf servers based on {@link TransportContentParser}
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class ServerTransportContentParser extends DefaultHandler2 {

	private Log				log						= LogFactory.getLog(ServerTransportContentParser.class);

	MessageQueue			messageQueue;

	Hello					hello;
	Query					query;
	SerializableReply		reply;
	String					messageId;
	Error					error;

	boolean					insideCapabilityTag		= false;
	StringBuffer			capabilityTagContent	= new StringBuffer();
	ArrayList<Capability>	capabilities;

	boolean					insideSessionIdTag		= false;
	StringBuffer			sessionIdTagContent		= new StringBuffer();

	boolean					insideDataTag			= false;
	StringBuffer			dataTagContent			= new StringBuffer();

	boolean					insideErrorTypeTag		= false;
	StringBuffer			errorTypeTagContent		= new StringBuffer();

	boolean					insideErrorTagTag		= false;
	StringBuffer			errorTagTagContent		= new StringBuffer();

	boolean					insideErrorSeverityTag	= false;
	StringBuffer			errorSeverityTagContent	= new StringBuffer();

	boolean					insideErrorAppTagTag	= false;
	StringBuffer			errorAppTagTagContent	= new StringBuffer();

	boolean					insideErrorPathTag		= false;
	StringBuffer			errorPathTagContent		= new StringBuffer();

	boolean					insideErrorMessageTag	= false;
	StringBuffer			errorMessageTagContent	= new StringBuffer();

	boolean					insideErrorInfoTag		= false;
	StringBuffer			errorInfoTagContent		= new StringBuffer();

	/* Query operation tags (extracted from RFC 4741 Section 7 (http://tools.ietf.org/html/rfc4741#section-7) */
	boolean					insideOperationTag		= false;

	/* extra features from JUNOS (out RFC) */

	boolean					insideInterfaceInfoTag	= false;
	StringBuffer			interfaceInfoTagContent	= new StringBuffer();

	boolean					insideSoftwareInfoTag	= false;
	StringBuffer			softwareInfoTagContent	= new StringBuffer();

	public void setMessageQueue(MessageQueue queue) {
		this.messageQueue = queue;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		// if (insideDataTag && !localName.equalsIgnoreCase("data"))
		// return;

		if (insideDataTag) {
			dataTagContent.append("<" + localName + ">");
		} else if (insideSoftwareInfoTag) {
			softwareInfoTagContent.append("<" + localName + ">");
		}

		// log.debug("startElement <" + uri + "::" + localName + ">");

		if (localName.equalsIgnoreCase("hello")) {
			hello = new Hello();
			capabilities = new ArrayList<Capability>();
		}
		// if (localName.equalsIgnoreCase("capabilities")) {
		// insideCapabilityTag = false;
		// }
		else if (localName.equalsIgnoreCase("capability")) {
			insideCapabilityTag = true;
		} else if (localName.equalsIgnoreCase("session-id")) {
			insideSessionIdTag = true;

			/* Query tags and operations */
		} else if (localName.equalsIgnoreCase("rpc")) {
			query = new Query();

			messageId = attributes.getValue("message-id");
			if (messageId == null)
				throw new SAXException(new NetconfProtocolException("Received <rpc> message without a message ID"));

			query.setMessageId(messageId);
		} else if (localName.equalsIgnoreCase(Operation.GET_CONFIG.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.GET_CONFIG);
		} else if (localName.equalsIgnoreCase(Operation.EDIT_CONFIG.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.EDIT_CONFIG);
		} else if (localName.equalsIgnoreCase(Operation.COPY_CONFIG.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.COPY_CONFIG);
		} else if (localName.equalsIgnoreCase(Operation.DELETE_CONFIG.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.DELETE_CONFIG);
		} else if (localName.equalsIgnoreCase(Operation.LOCK.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.LOCK);
		} else if (localName.equalsIgnoreCase(Operation.UNLOCK.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.UNLOCK);
		} else if (localName.equalsIgnoreCase(Operation.GET.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.GET);
		} else if (localName.equalsIgnoreCase(Operation.CLOSE_SESSION.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.CLOSE_SESSION);
		} else if (localName.equalsIgnoreCase(Operation.KILL_SESSION.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.KILL_SESSION);
		} else if (localName.equalsIgnoreCase(Operation.COMMIT.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.COMMIT);
		} else if (localName.equalsIgnoreCase(Operation.SET_LOGICAL_ROUTER.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.SET_LOGICAL_ROUTER);
		} else if (localName.equalsIgnoreCase(Operation.GET_ROUTE_INFO.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.GET_ROUTE_INFO);
		} else if (localName.equalsIgnoreCase(Operation.GET_INTERFACE_INFO.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.GET_INTERFACE_INFO);
		} else if (localName.equalsIgnoreCase(Operation.GET_SOFTWARE_INFO.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.GET_SOFTWARE_INFO);
		} else if (localName.equalsIgnoreCase(Operation.GET_ROLLBACK_INFO.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.GET_ROLLBACK_INFO);
		} else if (localName.equalsIgnoreCase(Operation.DISCARD.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.DISCARD);
		} else if (localName.equalsIgnoreCase(Operation.OPEN_CONFIG.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.OPEN_CONFIG);
		} else if (localName.equalsIgnoreCase(Operation.CLOSE_CONFIG.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.CLOSE_CONFIG);
		} else if (localName.equalsIgnoreCase(Operation.LOAD_CONFIGURATION.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.LOAD_CONFIGURATION);
		} else if (localName.equalsIgnoreCase(Operation.VALIDATE.getName())) {
			insideOperationTag = true;
			query.setOperation(Operation.VALIDATE);

			/* Reply tags */
		} else if (localName.equalsIgnoreCase("rpc-reply")) {
			reply = new SerializableReply();

			messageId = attributes.getValue("message-id");
			if (messageId == null)
				throw new SAXException(new NetconfProtocolException("Received <rpc-reply> message without a messageId"));

			reply.setMessageId(messageId);
			reply.setOk(false); // defaults to false
		} else if (localName.equalsIgnoreCase("data")) {
			insideDataTag = true;
		} else if (localName.equalsIgnoreCase("ok")) {
			reply.setOk(true);
		} else if (localName.equalsIgnoreCase("rpc-error")) {
			error = new Error();
		} else if (localName.equalsIgnoreCase("error-type")) {
			insideErrorTypeTag = true;
		} else if (localName.equalsIgnoreCase("error-tag")) {
			insideErrorTagTag = true;
		} else if (localName.equalsIgnoreCase("error-severity")) {
			insideErrorSeverityTag = true;
		} else if (localName.equalsIgnoreCase("error-app-tag")) {
			insideErrorAppTagTag = true;
		} else if (localName.equalsIgnoreCase("error-path")) {
			insideErrorPathTag = true;
		} else if (localName.equalsIgnoreCase("error-message")) {
			insideErrorMessageTag = true;
		} else if (localName.equalsIgnoreCase("error-info")) {
			insideErrorInfoTag = true;
		}

		/* extra features from JUNOS (out RFC) */
		else if (localName.equalsIgnoreCase("interface-information")) {
			insideInterfaceInfoTag = true;
		} else if (localName.equalsIgnoreCase("software-information")) {
			// software-information is the root node and leaving it in place
			// makes gives us a well-formed XML document rather than multiple
			// top-level nodes.
			softwareInfoTagContent.append("<" + localName + ">");
			insideSoftwareInfoTag = true;
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);

		log.info(new String(ch, start, length));
		// log.info(new String(ch));

		if (insideCapabilityTag) {
			capabilityTagContent.append(ch, start, length);
			// log.debug("capability content:" + capabilityTagContent);
		} else if (insideSessionIdTag) {
			sessionIdTagContent.append(ch, start, length);
		} else if (insideDataTag) {
			dataTagContent.append(ch, start, length);
		} else if (insideOperationTag) {

		} else if (insideErrorAppTagTag) {
			errorAppTagTagContent.append(ch, start, length);
		} else if (insideErrorInfoTag) {
			errorInfoTagContent.append(ch, start, length);
		} else if (insideErrorMessageTag) {
			errorMessageTagContent.append(ch, start, length);
		} else if (insideErrorPathTag) {
			errorPathTagContent.append(ch, start, length);
		} else if (insideErrorSeverityTag) {
			errorSeverityTagContent.append(ch, start, length);
		} else if (insideErrorTagTag) {
			errorTagTagContent.append(ch, start, length);
		} else if (insideErrorTypeTag) {
			errorTypeTagContent.append(ch, start, length);
		}

		/* extra features from JUNOS (out RFC) */
		else if (insideInterfaceInfoTag) {
			interfaceInfoTagContent.append(ch, start, length);
		} else if (insideSoftwareInfoTag) {
			softwareInfoTagContent.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);

		// log.debug("endElement </" + localName + ">");

		// if (insideDataTag && !localName.equalsIgnoreCase("data"))
		// return;

		if (insideDataTag && !localName.equalsIgnoreCase("data")) {
			dataTagContent.append("</" + localName + ">");
		} else if (insideSoftwareInfoTag && !localName.equalsIgnoreCase("software-information")) {
			softwareInfoTagContent.append("</" + localName + ">");
		}

		if (localName.equalsIgnoreCase("hello")) {
			messageQueue.put(hello);
			hello = null;
		} else if (localName.equalsIgnoreCase("capabilities")) {
			hello.setCapabilities(capabilities);
		} else if (localName.equalsIgnoreCase("capability")) {
			insideCapabilityTag = false;
			capabilities.add(Capability.getCapabilityByNamespace(capabilityTagContent.toString()));
			capabilityTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("session-id")) {
			insideSessionIdTag = false;
			hello.setSessionId(sessionIdTagContent.toString());
			sessionIdTagContent = new StringBuffer();

			/* Query tags and operations */
		} else if (localName.equalsIgnoreCase("rpc")) {
			messageQueue.put(query);
			query = null;
		} else if (localName.equalsIgnoreCase(Operation.GET_CONFIG.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.EDIT_CONFIG.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.COPY_CONFIG.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.DELETE_CONFIG.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.LOCK.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.UNLOCK.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.GET.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.CLOSE_SESSION.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.KILL_SESSION.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.COMMIT.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.SET_LOGICAL_ROUTER.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.GET_ROUTE_INFO.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.GET_INTERFACE_INFO.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.GET_SOFTWARE_INFO.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.GET_ROLLBACK_INFO.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.DISCARD.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.OPEN_CONFIG.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.CLOSE_CONFIG.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.LOAD_CONFIGURATION.getName())) {
			insideOperationTag = false;
		} else if (localName.equalsIgnoreCase(Operation.VALIDATE.getName())) {
			insideOperationTag = false;

			/* Reply tags */
		} else if (localName.equalsIgnoreCase("rpc-reply")) {
			messageQueue.put(reply);
			reply = null;
		} else if (localName.equalsIgnoreCase("data")) {
			insideDataTag = false;
			reply.setContain(dataTagContent.toString());
			reply.setContainName("data");
			dataTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("rpc-error")) {
			reply.addError(error);
		} else if (localName.equalsIgnoreCase("error-type")) {
			insideErrorTypeTag = false;
			error.setType(ErrorType.getErrorTypeByValue(errorTypeTagContent.toString()));
			errorTypeTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("error-tag")) {
			insideErrorTagTag = false;
			error.setTag(ErrorTag.getErrorTagByValue((errorTagTagContent.toString())));
			errorTagTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("error-severity")) {
			insideErrorSeverityTag = false;
			error.setSeverity(ErrorSeverity.getErrorSeverityByValue(errorSeverityTagContent.toString()));
			errorSeverityTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("error-app-tag")) {
			insideErrorAppTagTag = false;
			error.setAppTag(errorAppTagTagContent.toString());
			errorAppTagTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("error-path")) {
			insideErrorPathTag = false;
			error.setPath(errorPathTagContent.toString());
			errorPathTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("error-message")) {
			insideErrorMessageTag = false;
			error.setMessage(errorMessageTagContent.toString());
			errorMessageTagContent = new StringBuffer();
		} else if (localName.equalsIgnoreCase("error-info")) {
			insideErrorInfoTag = false;
			error.setInfo(errorInfoTagContent.toString());
			errorInfoTagContent = new StringBuffer();
		}

		/* get extrafunctionalities */
		else if (localName.equalsIgnoreCase("get-interface-information")) {
			insideInterfaceInfoTag = false;
			reply.setContain(interfaceInfoTagContent.toString());
			reply.setContainName("get-interface-information");
			interfaceInfoTagContent = new StringBuffer();
		}

		else if (localName.equalsIgnoreCase("software-information")) {
			insideSoftwareInfoTag = false;
			// software-information is the root node and leaving it in place
			// makes gives us a well-formed XML document rather than multiple
			// top-level nodes.
			softwareInfoTagContent.append("</" + localName + ">");
			reply.setContain(softwareInfoTagContent.toString());
			reply.setContainName("software-information");
			softwareInfoTagContent = new StringBuffer();
		}
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		// super.error(e);
		log.warn(e.getMessage());
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		// TODO Auto-generated method stub
		// super.fatalError(e);
		log.warn(e.getMessage());
	}
}

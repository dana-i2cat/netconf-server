package net.i2cat.netconf.rpc;

import java.util.Map.Entry;

/**
 * Implementation of {@link Reply} with toXml() method
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class SerializableReply extends Reply {

	private static final long	serialVersionUID	= -115827553610326725L;

	public SerializableReply() {
	}

	/**
	 * Construct a shallow copy of the Reply into a SerializableReply
	 */
	public SerializableReply(Reply reply) {
		this.operation = reply.operation;
		this.errors = reply.errors;
		this.ok = reply.ok;
		this.containName = reply.containName;
		this.contain = reply.contain;
		this.attributes = reply.attributes;
	}

	@Override
	public String toXML() {
		StringBuilder xmlBuilder = new StringBuilder();

		xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		// start rpc-reply beginning with message ID attribute
		xmlBuilder.append("<rpc-reply message-id=\"");
		xmlBuilder.append(getMessageId());
		xmlBuilder.append("\" xmlns:junos=\"http://xml.juniper.net/junos/11.2R1/junos\"");

		// write extra attributes
		if (getAttributes() != null && !getAttributes().isEmpty()) {
			for (Entry<String, String> attributteEntry : getAttributes().entrySet()) {
				xmlBuilder.append(attributteEntry.getKey());
				xmlBuilder.append("=\"");
				xmlBuilder.append(attributteEntry.getValue());
				xmlBuilder.append("\" ");
			}
		}

		// end rpc-reply tag beginning
		xmlBuilder.append(">");

		// write contain
		if (getContainName() != null && getContain() != null) {
			xmlBuilder.append("<" + getContainName() + ">");
			xmlBuilder.append(getContain());
			xmlBuilder.append("</" + getContainName() + ">");
		}

		// OK
		if (ok) {
			xmlBuilder.append("<ok />");
		}

		// errors
		if (getErrors() != null && getErrors().size() > 0) {
			for (Error error : getErrors()) {
				// begin tag
				xmlBuilder.append("<rpc-error>");

				// type
				xmlBuilder.append("<error-type>");
				xmlBuilder.append(error.getType());
				xmlBuilder.append("</error-type>");

				// tag
				xmlBuilder.append("<error-tag>");
				xmlBuilder.append(error.getTag());
				xmlBuilder.append("</error-tag>");

				// severity
				xmlBuilder.append("<error-severity>");
				xmlBuilder.append(error.getSeverity());
				xmlBuilder.append("</error-severity>");

				// app tag (optional)
				if (error.getAppTag() != null) {
					xmlBuilder.append("<error-app-tag>");
					xmlBuilder.append(error.getAppTag());
					xmlBuilder.append("</error-app-tag>");
				}

				// error path (optional)
				if (error.getPath() != null) {
					xmlBuilder.append("<error-path>");
					xmlBuilder.append(error.getPath());
					xmlBuilder.append("</error-path>");
				}

				// message (optional)
				if (error.getMessage() != null) {
					xmlBuilder.append("<error-message>");
					xmlBuilder.append(error.getMessage());
					xmlBuilder.append("</error-message>");
				}

				// error info (optional)
				if (error.getInfo() != null) {
					xmlBuilder.append("<error-info>");
					xmlBuilder.append(error.getInfo());
					xmlBuilder.append("</error-info>");
				}

				// end tag
				xmlBuilder.append("</rpc-error>");
			}
		}

		// close rpc-reply tag
		xmlBuilder.append("</rpc-reply>");
		return xmlBuilder.toString();
	}
}

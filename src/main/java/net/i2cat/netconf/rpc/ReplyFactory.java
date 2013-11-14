package net.i2cat.netconf.rpc;

import java.util.HashMap;

/**
 * Factory class to create base Netconf replies
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class ReplyFactory {

	public static SerializableReply newOk(Query query, HashMap<String, String> attributes) {
		SerializableReply reply = new SerializableReply();
		reply.setMessageId(query.getMessageId());
		reply.setAttributes(attributes);
		reply.setOk(true);
		return reply;
	}

	public static SerializableReply newGetConfigReply(Query query, HashMap<String, String> attributes, String configuration) {
		SerializableReply reply = new SerializableReply();
		reply.setMessageId(query.getMessageId());
		reply.setAttributes(attributes);

		// configuration
		reply.setContain("configuration");
		reply.setContain(configuration);

		return reply;
	}
}

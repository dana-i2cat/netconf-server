package net.i2cat.netconf.server;

import net.i2cat.netconf.rpc.Query;
import net.i2cat.netconf.rpc.Reply;

/**
 * Netconf behaviour defined by a {@link Query} and a {@link Reply}
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class Behaviour {

	private Query	query;
	private Reply	reply;

	public Behaviour(Query query, Reply reply) {
		super();
		this.query = query;
		this.reply = reply;
	}

	public Query getQuery() {
		return query;
	}

	public Reply getReply() {
		return reply;
	}

}

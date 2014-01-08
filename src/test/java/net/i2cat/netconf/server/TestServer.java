package net.i2cat.netconf.server;

import java.net.URI;
import java.util.List;

import net.i2cat.netconf.INetconfSession;
import net.i2cat.netconf.NetconfSession;
import net.i2cat.netconf.SessionContext;
import net.i2cat.netconf.rpc.Error;
import net.i2cat.netconf.rpc.ErrorFactory;
import net.i2cat.netconf.rpc.ErrorSeverity;
import net.i2cat.netconf.rpc.ErrorTag;
import net.i2cat.netconf.rpc.ErrorType;
import net.i2cat.netconf.rpc.Hello;
import net.i2cat.netconf.rpc.Operation;
import net.i2cat.netconf.rpc.Query;
import net.i2cat.netconf.rpc.QueryFactory;
import net.i2cat.netconf.rpc.RPCElement;
import net.i2cat.netconf.rpc.Reply;
import net.i2cat.netconf.rpc.ReplyFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit Tests for {@link Server}
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class TestServer {

	@Test
	public void testBasicServer() {
		Server server = Server.createServer(2222);
		server.startServer();

		try {
			SessionContext sessionContext = new SessionContext();
			sessionContext.setURI(new URI("ssh://user:user@localhost:2222/"));

			INetconfSession session = new NetconfSession(sessionContext);

			session.connect();
		} catch (Exception e) {
			Assert.fail("Error creating session: " + e.getMessage());
		} finally {
			server.stopServer();
		}
	}

	@Test
	public void testMessageStoring() {
		Server server = Server.createServerStoringMessages(2222);
		server.startServer();

		try {
			SessionContext sessionContext = new SessionContext();
			sessionContext.setURI(new URI("ssh://user:user@localhost:2222/"));

			INetconfSession session = new NetconfSession(sessionContext);

			session.connect();

			// send 3 messages
			Query query = QueryFactory.newGetConfig("running", null, null);
			session.sendSyncQuery(query);

			query = QueryFactory.newDeleteConfig("running");
			session.sendSyncQuery(query);

			query = QueryFactory.newCopyConfig("running", null);
			session.sendSyncQuery(query);

			// check stored messages
			List<RPCElement> messages = server.getStoredMessages();

			Assert.assertEquals("Message list must contain 4 messages", (3 + 1), messages.size());
			Assert.assertTrue("First message must be a Hello", messages.get(0) instanceof Hello);
			Assert.assertTrue("Second message must be a Query with GET-CONFIG operation",
					messages.get(1) instanceof Query && ((Query) messages.get(1)).getOperation().equals(Operation.GET_CONFIG));
			Assert.assertTrue("Third message must be a Query with DELETE-CONFIG operation",
					messages.get(2) instanceof Query && ((Query) messages.get(2)).getOperation().equals(Operation.DELETE_CONFIG));
			Assert.assertTrue("Fourth message must be a Query with COPY-CONFIG operation",
					messages.get(3) instanceof Query && ((Query) messages.get(3)).getOperation().equals(Operation.COPY_CONFIG));
		} catch (Exception e) {
			Assert.fail("Error creating session: " + e.getMessage());
		} finally {
			server.stopServer();
		}
	}

	@Test
	public void testBehaviours() {
		Server server = Server.createServerStoringMessages(2222);

		// define behaviours
		Query bQuery = QueryFactory.newGetRouteInformation();
		Reply bReply = new Reply();
		Error error = ErrorFactory.newError(ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, ErrorSeverity.ERROR, null, null, null, null);
		bReply.addError(error);
		Behaviour behaviour = new Behaviour(bQuery, bReply);
		server.defineBehaviour(behaviour);

		// start the server
		server.startServer();

		try {
			SessionContext sessionContext = new SessionContext();
			sessionContext.setURI(new URI("ssh://user:user@localhost:2222/"));

			INetconfSession session = new NetconfSession(sessionContext);

			session.connect();

			// send a message
			Query query = QueryFactory.newGetRouteInformation();
			Reply reply = session.sendSyncQuery(query);

			// assertions
			Assert.assertEquals("Reply must contain 1 error", 1, reply.getErrors().size());
			Assert.assertEquals("Reply error must be of 'type application'", ErrorType.APPLICATION, reply.getErrors().get(0).getType());
			Assert.assertEquals("Error tag must be 'operation-failed'", ErrorTag.OPERATION_FAILED, reply.getErrors().get(0).getTag());
			Assert.assertEquals("Error severity must be 'error'", ErrorSeverity.ERROR, reply.getErrors().get(0).getSeverity());
		} catch (Exception e) {
			Assert.fail("Error creating session: " + e.getMessage());
		} finally {
			server.stopServer();
		}
	}

	@Test
	public void testConsumingBehaviours() {
		Server server = Server.createServerStoringMessages(2222);

		// define behaviours
		Query bQuery = QueryFactory.newGetRouteInformation();
		Reply bReply = new Reply();
		Error error = ErrorFactory.newError(ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, ErrorSeverity.ERROR, null, null, null, null);
		bReply.addError(error);
		Behaviour behaviour = new Behaviour(bQuery, bReply, true);
		server.defineBehaviour(behaviour);

		bQuery = QueryFactory.newDiscardChanges();
		bReply = ReplyFactory.newOk(bQuery, null);
		behaviour = new Behaviour(bQuery, bReply, true);
		server.defineBehaviour(behaviour);

		bQuery = QueryFactory.newGetRouteInformation();
		bReply = ReplyFactory.newOk(bQuery, null);
		behaviour = new Behaviour(bQuery, bReply, true);
		server.defineBehaviour(behaviour);

		// start the server
		server.startServer();

		try {
			SessionContext sessionContext = new SessionContext();
			sessionContext.setURI(new URI("ssh://user:user@localhost:2222/"));

			INetconfSession session = new NetconfSession(sessionContext);

			session.connect();

			// send first query
			Query query = QueryFactory.newGetRouteInformation();
			Reply reply = session.sendSyncQuery(query);

			// assertions
			Assert.assertEquals("Reply must contain 1 error", 1, reply.getErrors().size());
			Assert.assertEquals("Reply error must be of 'type application'", ErrorType.APPLICATION, reply.getErrors().get(0).getType());
			Assert.assertEquals("Error tag must be 'operation-failed'", ErrorTag.OPERATION_FAILED, reply.getErrors().get(0).getTag());
			Assert.assertEquals("Error severity must be 'error'", ErrorSeverity.ERROR, reply.getErrors().get(0).getSeverity());

			// send second query
			query = QueryFactory.newDiscardChanges();
			reply = session.sendSyncQuery(query);

			// assertions
			Assert.assertEquals("Reply must contain 0 errors", 0, reply.getErrors().size());
			Assert.assertEquals("Reply must be OK", true, reply.isOk());

			// send third query
			query = QueryFactory.newGetRouteInformation();
			reply = session.sendSyncQuery(query);

			// assertions
			Assert.assertEquals("Reply must contain 0 errors", 0, reply.getErrors().size());
			Assert.assertEquals("Reply must be OK", true, reply.isOk());

		} catch (Exception e) {
			Assert.fail("Error executing tests: " + e.getMessage());
		} finally {
			server.stopServer();
		}
	}

}

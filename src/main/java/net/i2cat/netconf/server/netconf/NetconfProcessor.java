package net.i2cat.netconf.server.netconf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import net.i2cat.netconf.messageQueue.MessageQueue;
import net.i2cat.netconf.messageQueue.MessageQueueListener;
import net.i2cat.netconf.rpc.Capability;
import net.i2cat.netconf.rpc.Hello;
import net.i2cat.netconf.rpc.Operation;
import net.i2cat.netconf.rpc.Query;
import net.i2cat.netconf.rpc.RPCElement;
import net.i2cat.netconf.rpc.Reply;
import net.i2cat.netconf.rpc.ReplyFactory;
import net.i2cat.netconf.server.Behaviour;
import net.i2cat.netconf.server.BehaviourContainer;
import net.i2cat.netconf.server.MessageStore;
import net.i2cat.netconf.server.transport.ServerTransportContentParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.server.ExitCallback;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Netconf client processor
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class NetconfProcessor implements Runnable, MessageQueueListener {

	private static final String				END_CHAR_SEQUENCE	= "]]>]]>";

	private static final Log				log					= LogFactory.getLog(NetconfProcessor.class);

	// message counter
	private int								messageCounter		= 100;

	// message store
	private MessageStore					messageStore;

	// behaviors
	private BehaviourContainer				behaviourContainer;

	// client streams
	private InputStream						in;
	private OutputStream					out;
	private OutputStream					err;

	// callback
	private ExitCallback					callback;

	// status fields
	private Status							status;

	private String							sessionId;

	// XML parser & handler
	private XMLReader						xmlParser;
	private ServerTransportContentParser	xmlHandler;
	private MessageQueue					messageQueue;

	// message processor thread
	private Thread							messageProcessorThread;

	public NetconfProcessor(InputStream in, OutputStream out, OutputStream err, ExitCallback callback) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.callback = callback;
	}

	public void setMessageStore(MessageStore messageStore) {
		this.messageStore = messageStore;
	}

	public void setBehaviors(BehaviourContainer behaviourContainer) {
		this.behaviourContainer = behaviourContainer;
	}

	@Override
	public void run() {
		// initialize XML parser & handler and message queue
		try {
			messageQueue = new MessageQueue();

			xmlHandler = new ServerTransportContentParser();
			xmlHandler.setMessageQueue(messageQueue);
			messageQueue.addListener(this);

			xmlParser = XMLReaderFactory.createXMLReader();
			xmlParser.setContentHandler(xmlHandler);
			xmlParser.setErrorHandler(xmlHandler);
		} catch (SAXException e) {
			log.error("Cannot instantiate XML parser", e);
			return;
		}

		status = Status.INIT;

		// start message processor
		startMessageProcessor();
		// wait for message processor to continue
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.warn("Error waiting for message processor thread.", e);
		}

		// process messages
		try {
			StringBuilder message = new StringBuilder();

			while (status != Status.SESSION_CLOSED) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				log.debug("Start reading new message...");

				// read message
				String line;
				while ((line = br.readLine()) != null) {
					log.trace("Line read: '" + line + "'");
					if (line.endsWith(END_CHAR_SEQUENCE)) {
						log.trace("Detected end message.");
						// remove end char sequence from message
						line = line.replace(END_CHAR_SEQUENCE, "");
						message.append(line + '\n');
						// process data
						process(message.toString());
						// reset message
						message.setLength(0);
					}
					message.append(line + '\n');
				}
				// exit loop if stream closed
				break;
			}
		} catch (Exception e) {
			log.error("Exception caught in Netconf subsystem", e);
		} finally {
			waitAndInterruptThreads();
			callback.onExit(0);
		}
	}

	private void process(final String message) throws IOException, SAXException {
		log.debug("Starting parser..");
		try {
			log.trace("Parsing message:\n" + message);
			xmlParser.parse(new InputSource(new StringReader(message)));
		} catch (SAXException e) {
			if (e.getMessage().contentEquals("Content is not allowed in trailing section.")) {
				// Using shitty non-xml delimiters forces us to detect
				// end-of-frame by a SAX error.
				// Do nothing will just restart the parser.
				// Blame netconf
			}
			else {
				log.error("Error parsing. Message: \n" + message, e);
				status = Status.SESSION_CLOSED;
			}
			log.info("End of parsing.");
		} catch (Exception e) {
			log.error("Error parsing message", e);
			status = Status.SESSION_CLOSED;
		}
	}

	private void sendHello() throws IOException {
		// create a server hello message
		Hello serverHello = new Hello();
		// generate a random session ID
		sessionId = "" + (int) (Math.random() * (Integer.MAX_VALUE));
		serverHello.setSessionId(sessionId);

		// add only base capability
		ArrayList<Capability> capabilities = new ArrayList<Capability>();
		capabilities.add(Capability.BASE);
		serverHello.setCapabilities(capabilities);

		send(serverHello.toXML());
	}

	public void sendFakeConfig(Query configQuery) throws IOException {
		InputStream configFileIs = this.getClass().getResourceAsStream("/router_configs/router_config_A.xml");
		Reply reply = ReplyFactory.newGetConfigReply(configQuery, null, IOUtils.toString(configFileIs));
		sendReply(reply);
	}

	private void sendCloseSession() throws IOException {
		log.debug("Sending close session.");
		Query query = new Query();
		query.setMessageId("" + messageCounter++);
		query.setOperation(Operation.CLOSE_SESSION);
		sendQuery(query);
	}

	private void sendOk(Query query) throws IOException {
		log.debug("Sending OK.");
		sendReply(ReplyFactory.newOk(query, null));
	}

	public void sendQuery(Query query) throws IOException {
		send(query.toXML());
	}

	public void sendReply(Reply reply) throws IOException {
		send(reply.toXML());
	}

	private void send(String xmlMessage) throws IOException {
		log.trace("Sending message:\n" + xmlMessage);
		out.write(xmlMessage.getBytes("UTF-8"));
		// send final sequence
		out.write((END_CHAR_SEQUENCE + "\n").getBytes("UTF-8"));
		out.flush();
	}

	/**
	 * Netconf session status
	 * 
	 */
	private enum Status {
		INIT(0),
		HELLO_RECEIVED(1),
		CLOSING_SESSION(99),
		SESSION_CLOSED(100);

		private int	index;

		private Status(int index) {
			this.index = index;
		}

		public int getIndex() {
			return index;
		}
	}

	private void startMessageProcessor() {
		log.info("Creating new message processor...");
		messageProcessorThread = new Thread("Message processor") {
			@Override
			public void run() {
				while (status.getIndex() < Status.SESSION_CLOSED.getIndex()) {

					RPCElement message = messageQueue.blockingConsume();

					log.trace("Message body:\n" + message.toXML() + '\n');

					// store message if necessary
					if (messageStore != null) {
						messageStore.storeMessage(message);
					}

					// avoid message processing when session is already closed
					if (status == Status.SESSION_CLOSED) {
						log.warn("Session is closing or is already closed, message will not be processed");
						return;
					}

					// process message
					try {
						// user defined behaviours
						if (message instanceof Query) {
							Query query = (Query) message;
							List<Behaviour> behaviours = behaviourContainer.getBehaviours();
							if (behaviours != null) {
								Behaviour behaviour = null;
								for (Behaviour b : behaviours) {
									if (b.getQuery().getOperation().equals(query.getOperation())) {
										behaviour = b;
										break;
									}
								}
								if (behaviour != null) {
									log.info("Behaviour matched.");
									if (behaviour.isConsume()) {
										log.info("Behaviour matched. Sending reply...");
										behaviours.remove(behaviour);
									}
									log.info("Sending matched reply...");
									behaviour.getReply().setMessageId(query.getMessageId());
									sendReply(behaviour.getReply());
									// next iteration
									continue;
								}
							}
						}

						// default message processing
						if (message instanceof Hello) {
							if (status.getIndex() < Status.HELLO_RECEIVED.getIndex()) {
								status = Status.HELLO_RECEIVED;
								// send hello
								log.debug("Sending hello...");
								sendHello();
							} else {
								log.error("Hello already received. Aborting");
								sendCloseSession();
								status = Status.CLOSING_SESSION;
							}
						} else if (message instanceof Query) {
							Query query = (Query) message;
							Operation operation = query.getOperation();

							if (operation.equals(Operation.CLOSE_SESSION)) {
								log.info("Close-session received.");
								status = Status.CLOSING_SESSION;
								sendOk(query);
								status = Status.SESSION_CLOSED;
								log.info("Session closed.");
								// next iteration
								continue;
							} else if (operation.equals(Operation.GET_CONFIG)) {
								log.info("Get-config received.");
								sendFakeConfig(query);
								// next iteration
								continue;
							} else {
								log.info("Unknown query received, replying OK");
								sendOk(query);
								// next iteration
								continue;
							}
						} else if (message instanceof Reply) {
							if (status == Status.CLOSING_SESSION) {
								log.info("Client confirms the close session request.");
								status = Status.SESSION_CLOSED;
								// next iteration
								continue;
							} else {
								log.error("Unknown reply received!");
								// next iteration
								continue;
							}
						} else {
							log.warn("Unknown message: " + message.toXML());
							// next iteration
							continue;
						}
					} catch (IOException e) {
						log.error("Error sending reply", e);
						break;
					}
				}
				log.trace("Message processor ended");
			}
		};
		messageProcessorThread.start();
		log.info("Message processor started.");
	}

	public void waitAndInterruptThreads() {
		// wait for thread
		try {
			messageProcessorThread.join(2000);
		} catch (InterruptedException e) {
			log.error("Error waiting for thread end", e);
		}

		// kill thread if it don't finish naturally
		if (messageProcessorThread != null && messageProcessorThread.isAlive()) {
			log.debug("Killing message processor thread");
			messageProcessorThread.interrupt();
		}
	}

	@Override
	public void receiveRPCElement(RPCElement element) {
		log.info("Message received");
	}
}

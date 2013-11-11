package net.i2cat.netconf.messageQueue;

import net.i2cat.netconf.rpc.RPCElement;

/**
 * Working Message Queue extending {@link MessageQueue}
 * 
 * @author logoff
 * 
 */
public class WorkingMessageQueue extends MessageQueue {

	@Override
	public RPCElement consume() {
		synchronized (queue) {
			RPCElement element = null;
			if (queue.keySet().iterator().hasNext()) {
				element = queue.remove(queue.keySet().iterator().next()); // get first (older)
			}
			if (element != null)
				log.debug("Consuming message");
			return element;
		}
	}
}

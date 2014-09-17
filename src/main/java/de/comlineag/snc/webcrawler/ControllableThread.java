package de.comlineag.snc.webcrawler;


/**
 * @author 		Christian Guenther / Andreas Hess <andreas.hess@ucd.ie>, 11/02/2003
 * @category 	controller
 * @version		0.1				- 16.09.2014
 * @status		in development
 *  
 * @description	Abstract class that denotes a thread that can cooperate with a
 * 				ThreadController and has a iQueue, a depth level and a iMessageReceiver.
 */

abstract public class ControllableThread extends Thread {
	protected int level;
	protected int id;
	protected iQueue iQueue;
	protected ThreadController tc;
	protected iMessageReceiver mr;
	public void setId(int _id) {
		id = _id;
	}
	public void setLevel(int _level) {
		level = _level;
	}
	public void setQueue(iQueue _queue) {
		iQueue = _queue;
	}
	public void setThreadController(ThreadController _tc) {
		tc = _tc;
	}
	public void setMessageReceiver(iMessageReceiver _mr) {
		mr = _mr;
	}
	public ControllableThread() {
	}
	public void run() {
		// pop new urls from the iQueue until iQueue is empty
		for (Object newTask = iQueue.pop(level);
			 newTask != null;
			 newTask = iQueue.pop(level)) {
			// Tell the message receiver what we're doing now
			mr.receiveMessage(newTask, id);
			// Process the newTask
			process(newTask);
			// If there are less threads running than it could, try
			// starting more threads
			if (tc.getMaxThreads() > tc.getRunningThreads()) {
				try {
					tc.startThreads();
				} catch (Exception e) {
					System.err.println("[" + id + "] " + e.toString());
				}
			}
		}
		// Notify the ThreadController that we're done
		tc.finished(id);
	}

	/**
	 * The thread invokes the process method for each object in the iQueue
	 */
	public abstract void process(Object o);
}

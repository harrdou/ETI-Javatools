package ca.gc.ssc.eti.msa;

import java.util.Set;
import java.util.TreeSet;

/**
 * An enumeration of all of the standard Ironport sender groups configured
 * on every Partner-local MSA
 * @author Doug
 *
 */
public enum SenderGroup {
	PROD_ALL("PROD_ALL", 587),
	PROD_EXTERNAL("PROD_EXTERNAL", 587),
	PROD_INTERNAL("PROD_INTERNAL", 587),
	PROD_MFD_INTERNAL("PROD_MFD_INTERNAL", 587),
	CTE_ALL("CTE_ALL", 587),
	CTE_EXTERNAL("CTE_EXTERNAL", 587),
	CTE_INTERNAL("CTE_INTERNAL", 587),
	
	PROD_ALL_25("PROD_ALL", 25),
	PROD_EXTERNAL_25("PROD_EXTERNAL", 25),
	PROD_INTERNAL_25("PROD_INTERNAL", 25),
	CTE_ALL_25("CTE_ALL", 25),
	CTE_EXTERNAL_25("CTE_EXTERNAL", 25),
	CTE_INTERNAL_25("CTE_INTERNAL", 25);

	// Sender group name
	String name;
	// Listener port
	int port;
	// A set of hosts that belong to the sender group
	Set<Host> hosts;

	/**
	 * Construct a SenderGroup 
	 * @param name the name
	 * @param port the listener port
	 */
	SenderGroup(String name, int port) {
		this.name = name;
		this.port = port;
		hosts = new TreeSet<Host>();
	}
	
	public static void reset() {
		for (SenderGroup group : SenderGroup.values()) {
			group.hosts = new TreeSet<Host>();
		}
	}	


	/**
	 * Add a Host to the sender group
	 * @param host
	 */
	void addHost(Host host) {
		hosts.add(host);
	}

	/**
	 * Determine if a Host belongs to a sender group
	 * @param host
	 * @return
	 */
	boolean contains(Host host) {
		return hosts.contains(host);
	}

	/**
	 * Get the set of Hosts that belong to the sender group
	 * @return
	 */
	Set<Host> getHosts() {
		return hosts;
	}
}
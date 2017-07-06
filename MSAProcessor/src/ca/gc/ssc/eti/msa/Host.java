package ca.gc.ssc.eti.msa;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Represents a Server or Device (with an IP address and host name) that hosts
 * one or more SMTP clients. 
 * @author Doug
 *
 */
/**
 * @author Doug
 *
 */
public class Host implements Comparable<Host> {

	private String fqdn;
	private List<SMTPClient> smtpClients = new ArrayList<SMTPClient>();
	private SenderGroup senderGroup;
		
	/**
	 * Get the fqdn
	 * @return the fqdn
	 */
	String getFqdn() {
		return fqdn;
	}
	/**
	 * Set the fqdn
	 * @param fqdn the fqdn to set
	 */
	void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}
	/**
	 * Add an SMTPClient to the Host
	 * @param smtpCleint the SMTP Client to add
	 */
	void addSmtpClient(SMTPClient smtpClient) {
		smtpClients.add(smtpClient);
	}
	/**
	 * Return a List of all hosted SMTP Clients 
	 * @return the smtpClients
	 */
	List<SMTPClient> getSmtpClients() {
		return smtpClients;
	}


	/**
	 * Look at all of the hosted SMTP Clients and determine
	 * which IronPort sender group the host should belong to.
	 */
	void setSenderGroups() {
		final Pattern printerPattern = Pattern.compile("(?i)^(?:[A-Za-z]+)\\.(?:MultifunctionPrinter-ImprimanteMultifonction|ImprimanteMultifonction-MultifunctionPrinter)\\.(?:[A-Za-z]+)@(ctst\\.)?canada\\.ca");
		
		if (this.isProduction()) {
			if (this.usesPort587()) {
				if (this.sendsExternally() && this.sendsInternally()) {
					setSenderGroup(SenderGroup.PROD_ALL);
				} else if (this.sendsExternally() && !this.sendsInternally()) {
					setSenderGroup(SenderGroup.PROD_EXTERNAL);
				}  else if (this.getEmailAddresses().length == 1 &&
						    printerPattern.matcher(this.getEmailAddresses()[0]).matches()) {
					setSenderGroup(SenderGroup.PROD_MFD_INTERNAL);
				}
				else {
					setSenderGroup(SenderGroup.PROD_INTERNAL);
				}
			} else { // Port 25
				if (this.sendsExternally() && this.sendsInternally()) {
					setSenderGroup(SenderGroup.PROD_ALL_25);
				} else if (this.sendsExternally() && !this.sendsInternally()) {
					setSenderGroup(SenderGroup.PROD_EXTERNAL_25);
				}  else {
					setSenderGroup(SenderGroup.PROD_INTERNAL_25);
				}
			}
		} else { // Non-Production
			if (this.usesPort587()) {
				if (this.sendsExternally() && this.sendsInternally()) {
					setSenderGroup(SenderGroup.CTE_ALL);
				} else if (this.sendsExternally() && !this.sendsInternally()) {
					setSenderGroup(SenderGroup.CTE_EXTERNAL);
				}  else {
					setSenderGroup(SenderGroup.CTE_INTERNAL);
				}
			} else { // Port 25
				if (this.sendsExternally() && this.sendsInternally()) {
					setSenderGroup(SenderGroup.CTE_ALL_25);
				} else if (this.sendsExternally() && !this.sendsInternally()) {
					setSenderGroup(SenderGroup.CTE_EXTERNAL_25);
				}  else {
					setSenderGroup(SenderGroup.CTE_INTERNAL_25);
				}
			}
		}
	}
	
	/**
	 * Set the sender group for the host, and then add the host to
	 * the applicable SenderGroup object.
	 * @param senderGroup the senderGroup to set
	 */
	private void setSenderGroup(SenderGroup senderGroup) {
		senderGroup.addHost(this);
		this.senderGroup = senderGroup;
	}

	/**
	 * Get the SenderGroup that the host belongs to
	 * @return the senderGroup
	 */
	SenderGroup getSenderGroup() {
		return senderGroup;
	}
	
	/**
	 * determine if the host needs to send externally
	 * @return true if the host needs to send externally
	 */
	boolean sendsExternally() {
		for (SMTPClient client : smtpClients) {
			if (client.sendsExternally()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * determine if the host needs to send internally
	 * @return true if the host needs to send internally
	 */
	boolean sendsInternally() {
		for (SMTPClient client : smtpClients) {
			if (client.sendsInternally()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * determine if the host is a production environment
	 * @return true if the host is a production environment
	 */
	boolean isProduction() {
		for (SMTPClient client : smtpClients) {
			if (client.isProductionEnv()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * determine if the host needs port 587
	 * @return true if the host if the host needs port 587
	 */
	boolean usesPort587() {
		for (SMTPClient client : smtpClients) {
			if (!client.needsPort25()) {
				return true;
			}
		}
		return false;
	}
		
	/**
	 * determine if the host needs port 25
	 * @return true if the host if the host needs port 25
	 * @return
	 */
	boolean usesPort25() {
		for (SMTPClient client : smtpClients) {
			if (client.needsPort25()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get all of the email addresses used by
	 * all SMTP clients on the host.
	 * @return A String array of unique email addresses
	 */
	String[] getEmailAddresses() {
		Set<String> uniqueAddresses = new HashSet<String>();
		for (SMTPClient client : smtpClients) {
			uniqueAddresses.addAll(client.getEmailAddresses());
		}		
		
		return uniqueAddresses.toArray(new String[0]);
	}
	
	/**
	 * Determine if any of the SMTPClients on a host have
	 * conflicting recipient restrictions
	 * @return true if there are and DLP conflicts
	 */
	boolean hasDLPConflict() {
		int hasInternalOnlyClients = 0;
		int hasExternalOnlyClients = 0;
		int hasUnrestrictedClients = 0;
		
		for (SMTPClient client : smtpClients) {
			if (client.sendsInternally() && !client.sendsExternally()) {
				hasInternalOnlyClients = 1;
			} else if ((!client.sendsInternally() && client.sendsExternally())) {
				hasExternalOnlyClients = 1;
			} else {
				hasUnrestrictedClients = 1;
			}
		}
	return (hasInternalOnlyClients + hasExternalOnlyClients + hasUnrestrictedClients) > 1;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Host other) {
		return this.fqdn.compareTo((other).getFqdn());
	}

}
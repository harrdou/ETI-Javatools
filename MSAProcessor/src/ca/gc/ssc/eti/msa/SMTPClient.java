package ca.gc.ssc.eti.msa;

import java.util.*;

/**
 * A class to represent one SMTP Client (an application or a device). corresponds
 * to one row in an MSA worksheet.
 * @author Doug Harris
 *
 */
public class SMTPClient {

	// Client properties
	private Set<String> emailAddresses;
	private String fqdn;
	private boolean productionEnv;
	private boolean sendsExternally;
	private boolean sendsInternally;
	private boolean needsPort25;
	
	/**
	 * Construct an SMTPClient
	 */
	SMTPClient() {
		emailAddresses = new HashSet<String>();
	}


	/**
	 * Get the email addresses used by the client
	 * @return the emailAddresses
	 */
	Set<String> getEmailAddresses() {
		return emailAddresses;
	}

	/**
	 * Set the email addresses used by the client
	 * @param emailAddresses
	 *            the emailAddresses to set
	 */
	void setEmailAddresses(Set<String> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	/**
	 * Add an email address
	 * @param emailAddress
	 *            the emailAddress to add
	 */
	void addEmailAddresses(String emailAddress) {
		this.emailAddresses.add(emailAddress);
	}

	/**
	 * Get the host name
	 * @return the fqdn
	 */
	String getFqdn() {
		return fqdn;
	}

	/**
	 * Set the host name
	 * @param fqdn
	 *            the fqdn to set
	 */
	void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}

	/**
	 * Determine if the client is a production environment
	 * @return the productionEnv
	 */
	boolean isProductionEnv() {
		return productionEnv;
	}

	/**
	 * set whether the client is a production environment
	 * @param productionEnv
	 *            the productionEnv to set
	 */
	void setProductionEnv(boolean productionEnv) {
		this.productionEnv = productionEnv;
	}

	/**
	 * Determine if the client is allowed to send externally
	 * @return the sendsExternally
	 */
	boolean sendsExternally() {
		return sendsExternally;
	}

	/**
	 * Specify if the client is allowed to send externally
	 * @param sendsExternally
	 *            the sendsExternally to set
	 */
	void setSendsExternally(boolean sendsExternally) {
		this.sendsExternally = sendsExternally;
	}

	/**
	 * Determine if the client is allowed to send internally
	 * @return the sendsInternally
	 */
	boolean sendsInternally() {
		return sendsInternally;
	}

	/**
	 * Specify if the client is allowed to send internally
	 * @param sendsInternally
	 *            the sendsInternally to set
	 */
	void setSendsInternally(boolean sendsInternally) {
		this.sendsInternally = sendsInternally;
	}

	/**
	 * Determine if the client needs to use port 25
	 * @return true if it needs port 25, false otherwise
	 */
	boolean needsPort25() {
		return needsPort25;
	}

	/**
	 * Specify if the client needs to use port 25
	 * @param needsPort25
	 *            true if it needs port 25, false otherwise
	 */
	void setNeedsPort25(boolean needsPort25) {
		this.needsPort25 = needsPort25;
	}
}

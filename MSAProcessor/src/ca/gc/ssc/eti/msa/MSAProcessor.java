package ca.gc.ssc.eti.msa;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.*;

import javax.swing.JProgressBar;

import org.apache.commons.csv.*;
import org.apache.commons.cli.*;

/**
 * A program to automate the data validation of MSA integration worksheets and
 * automatically produce a set of Ironport configuration changes.
 * 
 * @author Doug Harris
 *
 */

public class MSAProcessor {
	
	private static final Pattern CTE_ADDRESSES = Pattern.compile(".*@ctst\\.canada.ca$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	public static void main(String[] args) {
		System.err.println(Messages.getString("MSAProcessor.1")); //$NON-NLS-1$
		System.err.println();
		
		// Instantiate an NSAProcessor
		MSAProcessor processor = new MSAProcessor();

		// Parse the command line arguments
		CommandLine cmdline = processor.parseArgs(args);

		// Parse the CSV file and build a Set of SMTPClient objects for each row 
		Set<SMTPClient> clients = processor.parseFile(System.out, cmdline.getArgs()[0], cmdline.getArgs()[1], cmdline.getArgs()[2]);
		
		// Sort all of the clients, group them by host and determine the sender group for each host  
		Map<String,Host> hosts = processor.sortClients(clients);

		// Print a report of any hosts with conflicting Data Loss Prevention requirements 
		processor.printDLPConflicts(System.out, hosts);
		
		// Optionally perform a DNS check for every host and report any issues 
		if (cmdline.hasOption('d')) {
			processor.printDNSIssues(System.out, hosts);
		}

		// Optionally produce the IronPort configuration instructions
		if (cmdline.hasOption('i')) {
			if (cmdline.hasOption('m')) { // Multitenant  names?
				processor.printSenderGroups(System.out, cmdline.getArgs()[1]);
			} else {
				processor.printSenderGroups(System.out, null);
			}
			processor.printDictionary(System.out, hosts, cmdline.getArgs()[1].toLowerCase(), cmdline.getArgs()[2].toLowerCase());
		}

	}

	/**
	 * Parses the contents of the CSV file, reports any errors, and builds a Set of SMTPClient
	 * objects (one for each row in the worksheet). 
	 * @param out The PrintStream for error and warning messages
	 * @param fileName The name of the CSV file
	 * @param acronymE English Partner Acronym
	 * @param acronymF French Partner Acronym
	 * @return
	 */
	public Set<SMTPClient> parseFile(PrintStream out, String fileName, String acronymE, String acronymF) {
		Set<SMTPClient> clients = new HashSet<SMTPClient>();
	
		// Build and compile the regular expressions for validating the data fields
		final Pattern VALID_ENVIRONMENTS = Pattern.compile(
				"^(Development|Testing|Production)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

		final Pattern VALID_HOSTNAMES = Pattern
				.compile("^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))+$"); //$NON-NLS-1$
		
		final Pattern VALID_FQDNS = Pattern
				.compile("^[^\\s,:;/\\\\]{0,63}(\\.[^\\s,:;/\\\\]{0,63})+$"); //$NON-NLS-1$

		final Pattern IP_ADDRESS = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"); //$NON-NLS-1$

		String acronymPattern;
		if (acronymE.equals(acronymF)) {
			acronymPattern = acronymE;
		} else {
			acronymPattern = "(?:" + acronymE + "|" + acronymF +")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		final Pattern VALID_ADDRESSES = Pattern.compile(
				"^" //$NON-NLS-1$
				+ acronymPattern
				+ "\\.[A-Z0-9!#$%&'*+\\-\\./=?^_`{|}~]+\\." //$NON-NLS-1$
				+ acronymPattern
				+ "@(ctst\\.)?canada\\.ca$", //$NON-NLS-1$
				Pattern.CASE_INSENSITIVE);

		int errors = 0;
		int warnings = 0;
		
		BufferedReader reader;
		CSVFormat format;

		try {
			// Open the file
			reader = new BufferedReader(new FileReader(fileName));

			// Read the headers and determine the worksheet type
			int headerLines = 0;
			String line;
			do {
				line = reader.readLine();
				if (line == null) {
					out.println(Messages.getString("MSAProcessor.10")); //$NON-NLS-1$
					System.exit(0);
				}
				headerLines++;
				
				if (line.startsWith("Application Name") || line.startsWith("Nom de l�application")) { //$NON-NLS-1$ //$NON-NLS-2$
					format = CSVFormat.EXCEL.withHeader("name", "ename", "etype", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
							"addrs", "internal", "external", "cte", "fqdn", "port"); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					break;
				} else if (line.startsWith("Device Type") || line.startsWith("Type de dispositif")) { //$NON-NLS-1$ //$NON-NLS-2$
					format = CSVFormat.EXCEL.withHeader("etype", "addrs", "internal", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							"external", "cte", "fqdn", "port"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					break;
					}
			} while (true);
			
			CSVParser parser = format.parse(reader);

			for (CSVRecord record : parser) {
				if (record.get(0).isEmpty())
					continue;
				
				SMTPClient client = new SMTPClient();

				// Parse the FQDN
				String fqdn = record.get("fqdn").trim().toLowerCase(); //$NON-NLS-1$
				if (!VALID_FQDNS.matcher(fqdn).matches() || IP_ADDRESS.matcher(fqdn).matches()) {
					out.println(Messages.getString("MSAProcessor.32") + (headerLines + record.getRecordNumber()) //$NON-NLS-1$
							+ Messages.getString("MSAProcessor.33") + fqdn); //$NON-NLS-1$
					errors++;
				} else if (!VALID_HOSTNAMES.matcher(fqdn).matches()) {
					out.println(Messages.getString("MSAProcessor.42") + (headerLines + record.getRecordNumber()) //$NON-NLS-1$
							+ Messages.getString("MSAProcessor.66") + fqdn); //$NON-NLS-1$
					warnings++;
				}
				client.setFqdn(fqdn);

				// Parse the Environment/Device Type
				String envType = record.get("etype"); //$NON-NLS-1$
				if (!VALID_ENVIRONMENTS.matcher(envType).matches()) {
					out.println(Messages.getString("MSAProcessor.32") + (headerLines + record.getRecordNumber()) //$NON-NLS-1$
							+ Messages.getString("MSAProcessor.36") + envType); //$NON-NLS-1$
					errors++;
				} else if (envType.equalsIgnoreCase("Production")) { //$NON-NLS-1$
					client.setProductionEnv(true);
				} else {
					client.setProductionEnv(false);
				}

				// Parse the email addresses to be authorized
				String addresses = record.get("addrs").trim(); //$NON-NLS-1$
				Set<String> addressSet = new HashSet<String>();
				addressSet.addAll(((List<String>) Arrays.asList(addresses
						.split("[\\s,;\\t\\xa0]+")))); //$NON-NLS-1$
				for (String address : addressSet) {
					if (address.trim().length() == 0) {
						continue;
					}
					if (!VALID_ADDRESSES.matcher(address).matches()) {
						out.println(Messages.getString("MSAProcessor.32") //$NON-NLS-1$
								+ (headerLines + record.getRecordNumber())
								+ Messages.getString("MSAProcessor.41") + address); //$NON-NLS-1$
						errors++;
					} else {
						if (address.indexOf('@') > 64) {
							out.println(Messages.getString("MSAProcessor.42") //$NON-NLS-1$
									+ (headerLines + record.getRecordNumber())
									+ Messages.getString("MSAProcessor.43") + address); //$NON-NLS-1$
							warnings++;
						}
						if (CTE_ADDRESSES.matcher(address).matches()) {
							if (client.isProductionEnv()) {
								out.println(Messages.getString("MSAProcessor.42") //$NON-NLS-1$
										+ (headerLines + record.getRecordNumber())
										+ Messages.getString("MSAProcessor.45") + address); //$NON-NLS-1$
								warnings++;
							}
						} else if (!client.isProductionEnv()) {
							out.println(Messages.getString("MSAProcessor.42") //$NON-NLS-1$
									+ (headerLines + record.getRecordNumber())
									+ Messages.getString("MSAProcessor.47") + address); //$NON-NLS-1$
							warnings++;
							}
						client.addEmailAddresses(address.toLowerCase());
						}
					}

				// Parse the allowed Recipient types 
				String internal = record.get("internal"); //$NON-NLS-1$
				if (internal == null || internal.isEmpty()) {
					out.println(Messages.getString("MSAProcessor.32") + (headerLines + record.getRecordNumber()) //$NON-NLS-1$
							+ Messages.getString("MSAProcessor.50")); //$NON-NLS-1$
					errors++;
				} else if (internal.matches("[YyOo].*")) { //$NON-NLS-1$
					client.setSendsInternally(true);
				} else {
					client.setSendsInternally(false);
				}

				String external = record.get("external"); //$NON-NLS-1$
				if (external == null || external.isEmpty()) {
					out.println(Messages.getString("MSAProcessor.32") + (headerLines + record.getRecordNumber()) //$NON-NLS-1$
							+ Messages.getString("MSAProcessor.54")); //$NON-NLS-1$
					errors++;
				} else if (external.matches("[YyOo].*")) { //$NON-NLS-1$
					client.setSendsExternally(true);
				} else {
					client.setSendsExternally(false);
				}

				String cte = record.get("cte"); //$NON-NLS-1$
				if (cte != null && cte.matches("[YyOo].*")) { //$NON-NLS-1$
					client.setSendsInternally(true);
				}

				if (!client.sendsExternally() && !client.sendsInternally()) {
					out.println(Messages.getString("MSAProcessor.32") + (headerLines + record.getRecordNumber()) //$NON-NLS-1$
							+ Messages.getString("MSAProcessor.59")); //$NON-NLS-1$
					errors++;
				}
				
				// Parse the port (this column must be added to the worksheet manually as required)
				String port = record.get("port"); //$NON-NLS-1$
				if ("25".equals(port.trim())) { //$NON-NLS-1$
					client.setNeedsPort25(true);
				}

				clients.add(client);
			}

		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(0);
		}

		out.println(Messages.getString("MSAProcessor.62") + errors); //$NON-NLS-1$
		out.println(Messages.getString("MSAProcessor.63") + warnings); //$NON-NLS-1$

		return clients;
	}
	

	/**
	 * Sorts all of the SMTP clients and groups then together into Host objects (one per FQDN).
	 * Then determines the appropriate IronPort sender group for each Host. 
	 * @param clients The set of SMTPClient object to sort
	 * @return A Map of each FQDNs and itS constructed Host object 
	 */
	public Map<String,Host> sortClients(Set<SMTPClient> clients) {
		
		// Populate the list of hosts
		Map<String,Host> hosts = new HashMap<String,Host>();

		for (SMTPClient client : clients) {
			Host host = hosts.get(client.getFqdn());
			if (host == null) {
				host = new Host();
				host.setFqdn(client.getFqdn());
				hosts.put(host.getFqdn(), host);
			}
			host.addSmtpClient(client);
		}

		// Determine the Sender Groups
		for (Host host : hosts.values()) {
			host.setSenderGroups();
		}
		return hosts;
	}
	
	/**
	 * Prints a report of any Hosts that have multiple SMTP clients with
	 * conflicting DLP recipient restrictions
	 * @param out
	 * @param hosts
	 */
	public void printDLPConflicts(PrintStream out, Map<String,Host> hosts) {

		// Build a Sorted set so they will be listed alphabetically 
		SortedSet<String> dlpConflicts = new TreeSet<String>();
		
		for (Host host : hosts.values()) {
			if (host.hasDLPConflict()) {
				dlpConflicts.add(host.getFqdn());
			}
		}
		
		// Print the header
		if (!dlpConflicts.isEmpty()) {
			out.println();
			underline(
					out,
					Messages.getString("MSAProcessor.64") //$NON-NLS-1$
							+ dlpConflicts.size()
							+ Messages.getString("MSAProcessor.65"), //$NON-NLS-1$
					'=');
		}

		// Print the list of hosts
		for (String conflictingHost : dlpConflicts)
			out.println(conflictingHost);

	}

	/**
	 * Produces a report of all the new IronPort sender group entries
	 * @param out The PrintStream to send the report to
	 * @param acronymE English Partner Acronym (for multi-tenant sender group names)
	 */
	public void printSenderGroups(PrintStream out, String acronymE) {

		for (SenderGroup senderGroup : SenderGroup.values()) {
			// Ignore Sender Groups with no new entries
			if (senderGroup.getHosts().isEmpty())
				continue;

			String senderGroupName = senderGroup.name;
			// Insert the Partner Acronym in the sender group name if needed
			if (acronymE != null) {
				senderGroupName = senderGroupName.replaceFirst("_", "_" + acronymE + "_"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			
			// Print the header
			underline(out,
					senderGroup.getHosts().size()
							+ Messages.getString("MSAProcessor.0") //$NON-NLS-1$
							+ senderGroup.port
							+ Messages.getString("MSAProcessor.2") //$NON-NLS-1$
							+ senderGroupName, '=');
			
			// Print the fqdn entries to be added
			for (Host host : senderGroup.getHosts()) {
				out.println(host.getFqdn());
			}
			out.println();
		}
	}
	
	
	/**
	 * Escapes any regex special characters in a string.
	 * @param aString
	 * @return
	 */
	private String escapeForRegex(String aString) {
		StringBuilder result = new StringBuilder();
		for (int i = 0 ; i < aString.length(); i++) {
			char c = aString.charAt(i);
			if ("^$*+?|/.".indexOf(c) >= 0) { //$NON-NLS-1$
				result.append('\\');
			}
			result.append(c);
		}
	return result.toString();	
	}
	
	/**
	 * Produce a single regular expression that will match all of the prefixes
	 * of a given set of email addresses. 
	 * @param addresses A set of email addresses 
	 * @return A regular expression
	 */
	private String optimizedPrefixRegex(Set<String> addresses) {
		
		final Pattern capturePattern = Pattern.compile("^(([A-Za-z]+)\\.([^@]+)\\.([A-Za-z]+))(@[^ ]+)"); //$NON-NLS-1$

		StringBuilder regex = new StringBuilder();
		
		// first see if the partner acronyms are all consistent
		String firstAcronym = null, secondAcronym = null;
		boolean consistentAcronyms = true;
		for (String address : addresses) {
			Matcher matcher = capturePattern.matcher(address);
			matcher.matches();
			if (firstAcronym == null) {
				firstAcronym = matcher.group(2);
				secondAcronym = matcher.group(4);
			} else if (!firstAcronym.equals(matcher.group(2)) ||
					   !secondAcronym.equals(matcher.group(4))) {
				consistentAcronyms = false;
			}
		}
		
		if (consistentAcronyms) { // Optimize matching the 1st acronym
			regex.append(firstAcronym + "\\."); //$NON-NLS-1$
		}
		
		if (addresses.size() > 1) {
			// Start an alternation group
			regex.append("(?:"); //$NON-NLS-1$
		}

		Iterator<String> i = addresses.iterator();
		while  (i.hasNext()) {
			String address = i.next();
			Matcher matcher = capturePattern.matcher(address);
			matcher.matches();

			if (consistentAcronyms) {
				// Just need the service name
				regex.append(escapeForRegex(matcher.group(3)));
			} else {
				// Need the whole thing
				regex.append(escapeForRegex(matcher.group(1)));
			}
			
			if (i.hasNext()) {
				regex.append("|"); //$NON-NLS-1$
			}
		}
		
		if (addresses.size() > 1) {
			// close the alternation group
			regex.append(")"); //$NON-NLS-1$
		}

		if (consistentAcronyms) { // Optimize matching the 2n acronym
			regex.append("\\." + secondAcronym); //$NON-NLS-1$
		}
		
		return regex.toString();
	}

	/**
	 * Prints a report of all the new IronPort Dictionary entries
	 * needed to allow the use of authorized email addresses (other
	 * than the Partner DoNotReply address).
	 * @param out The PrintStream to send the report to
	 * @param hosts The Map of FQDNs to Host objects
	 * @param acronymE English Partner Acronym
	 * @param acronymF French Partner Acronym
	 */
	
 	public void printDictionary(PrintStream out, Map<String,Host> hosts, String acronymE, String acronymF) {
		
		Map<String,Set<String>> addressDictionary = new TreeMap<String,Set<String>>();
		
		// Build a set of possible DoNotReply addresses
		Set<String> dnrPrefixes = new TreeSet<String>();
		dnrPrefixes.add(acronymE + ".donotreply-nepasrepondre." + acronymF); //$NON-NLS-1$
		dnrPrefixes.add(acronymF + ".nepasrepondre-donotreply." + acronymE); //$NON-NLS-1$
		
		// Build the set of non-DNR addresses
		for (Host host : hosts.values()) {
			for (String address : host.getEmailAddresses()) {
				// Ignore Multifunction Printer addresses
				if (host.getSenderGroup() == SenderGroup.PROD_MFD_INTERNAL)
					continue;

				// Ignore DoNotReply addresses
				if (dnrPrefixes.contains(address.substring(0,
						address.indexOf('@')).toLowerCase()))
					continue;

				Set<String> clientAddresses  = addressDictionary.get(host.getFqdn());
				if (clientAddresses == null) {
					clientAddresses = new TreeSet<String>();
					addressDictionary.put(host.getFqdn(), clientAddresses);
				}
				clientAddresses.add(address);
			}
		}

		// Print the header
		if (!addressDictionary.isEmpty()) {
			out.println();
			underline(out, addressDictionary.size()
					+ Messages.getString("MSAProcessor.3"), '='); //$NON-NLS-1$
			
			// Print the list of regular expressions to add to the IronPort Dictionary
			for (Map.Entry<String, Set<String>> entry : addressDictionary.entrySet()) {
				// Start with the escaped host name and separating space
				out.print("^" + entry.getKey().replace(".", "\\.") + "\\ "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				// Then consume/ignore the display name
				out.print("(?:[^<,]+<)??<?"); //$NON-NLS-1$

				// Sort out the CTE addresses from the Prod addresses
				Set<String> cteAddresses = new TreeSet<String>();
				Set<String> prodAddresses = new TreeSet<String>();
				for (String address : entry.getValue()) {
					if (CTE_ADDRESSES.matcher(address).matches()) {
						cteAddresses.add(address);
					} else {
						prodAddresses.add(address);
					}
				}
				
				// Sift out any that are duplicated in both CTE and PROD
				Set<String> testInProdAddresses = new TreeSet<String>();
				Iterator<String> i = prodAddresses.iterator();
				while (i.hasNext()) {
					String prodAddress = i.next(); 
					String prefix = prodAddress.substring(0, prodAddress.indexOf('@'));
					String cteAddress = prefix + "@ctst.canada.ca"; //$NON-NLS-1$
					if (cteAddresses.contains(cteAddress)) {
						testInProdAddresses.add(prodAddress);
						i.remove();
						cteAddresses.remove(cteAddress);
					}
				}
				
				// Determine if an alternation group is required
				int nonEmptySetCount = (prodAddresses.isEmpty() ? 0 : 1);
				nonEmptySetCount += (cteAddresses.isEmpty() ? 0 : 1);
				nonEmptySetCount += (testInProdAddresses.isEmpty() ? 0 : 1);
				if (nonEmptySetCount > 1) { // Need an alternation group 
					out.print("(?:"); //$NON-NLS-1$
				} 
				
				// Add the prod-only addresses
				if (!prodAddresses.isEmpty()) {
					out.print(optimizedPrefixRegex(prodAddresses) + "@canada\\.ca"); //$NON-NLS-1$
				}
				
				// Add the addresses in both CTE and Prod
				if (!testInProdAddresses.isEmpty()) {
					if (!prodAddresses.isEmpty()) {
						out.print("|"); //$NON-NLS-1$
					}
					out.print(optimizedPrefixRegex(testInProdAddresses) + "@(?:ctst\\.)?canada\\.ca"); //$NON-NLS-1$
				}
				
				// Add the CTE-only addresses
				if (!cteAddresses.isEmpty()) {
					if (!testInProdAddresses.isEmpty() || !prodAddresses.isEmpty()) {
						out.print("|"); //$NON-NLS-1$
					}
					out.print(optimizedPrefixRegex(cteAddresses) + "@ctst\\.canada\\.ca"); //$NON-NLS-1$
				}

				if (nonEmptySetCount > 1) { // Close the alternation group 
					out.print(")"); //$NON-NLS-1$
				}
				
				out.println(">?\\s*$"); //$NON-NLS-1$
			}
		}
					
	}
	

 	
 	/**
	 * Performs both forward and reverse DNS lookups for every host name
	 * and reports any issues
	 * @param out The PrintStream to send the report to
	 * @param hosts The Map of fqdns and their Host objects
	 */
	public void printDNSIssues (PrintStream out, Map<String,Host> hosts) {
		printDNSIssues(out, hosts, null);
	}
	
	public void printDNSIssues (PrintStream out, Map<String,Host> hosts, JProgressBar progress) {

		DNSResolver resolver = new DNSResolver();
		
		// Build a list to hold the error messages so we can count them
		List<String> errors = new ArrayList<String>(hosts.size());

		if (progress != null) {
			progress.setValue(0);
			progress.setMaximum(hosts.size());
		}
		
		for (Host host : hosts.values()) {
			
			// Do a forward lookup to find all registered IPs
			InetAddress[] addresses;
			try {
				addresses = InetAddress.getAllByName(host.getFqdn());
			} catch (UnknownHostException e) {
				errors.add(Messages.getString("MSAProcessor.99") + host.getFqdn()); //$NON-NLS-1$
				if (progress != null) {
					progress.setValue(progress.getValue() + 1);
					progress.repaint();
				}
				continue;
			}
			
			// Do a FCrDNS check on each IP address found 
			for (InetAddress address : addresses) {
				// Build the PTR record name to look for
				StringBuilder ptrName = new StringBuilder();
				byte[] octets = address.getAddress();
				for (int i = octets.length - 1; i >= 0; i--) {
					ptrName.append(0xff & octets[i]);
					ptrName.append(".");//$NON-NLS-1$
				}
				if (address instanceof Inet4Address) {
					ptrName.append("in-addr."); //$NON-NLS-1$
				} else {
					ptrName.append("ip6."); //$NON-NLS-1$
				}
				ptrName.append("arpa."); //$NON-NLS-1$
				
				// Do the reverse lookup
				List<String> ptrRecords = resolver.lookup(ptrName.toString(), "PTR"); //$NON-NLS-1$
				
				if (ptrRecords.size() == 0) {
					errors.add(Messages.getString("MSAProcessor.6") + address.getHostAddress() + Messages.getString("MSAProcessor.7") + host.getFqdn()); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				if (!ptrRecords.contains(host.getFqdn() + ".")) { //$NON-NLS-1$
					errors.add(Messages.getString("MSAProcessor.9") + host.getFqdn() + Messages.getString("MSAProcessor.11") + address.getHostAddress() + Messages.getString("MSAProcessor.12")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (ptrRecords.size() > 1) {
					errors.add(Messages.getString("MSAProcessor.13") + address.getHostAddress() + Messages.getString("MSAProcessor.14") + host.getFqdn() //$NON-NLS-1$ //$NON-NLS-2$
					 + "\n   " //$NON-NLS-1$
					 + Messages.getString("MSAProcessor.16")); //$NON-NLS-1$
				
					for (String hostName : ptrRecords) {
						hostName = hostName.substring(0, hostName.length() - 1); // Strip trailing dot
						if (!hostName.equals(host.getFqdn())) {
							// Forward-check the other host name
							Set<InetAddress> otherAddresses = new HashSet<InetAddress>();
							try {
								otherAddresses.addAll(Arrays.asList(InetAddress.getAllByName(hostName)));
							} catch (UnknownHostException e) {
								// Ignore. Set will be empty
							}
							boolean forwardMatch = false;
							for (InetAddress otherAddress : otherAddresses) {
								if (otherAddress.getHostAddress().equals(address.getHostAddress())) {
									forwardMatch = true;
									break;
								}
							}
							if (! forwardMatch) {
								errors.add(Messages.getString("MSAProcessor.17") + hostName + Messages.getString("MSAProcessor.18")); //$NON-NLS-1$ //$NON-NLS-2$
							}
							
							// Check if the non-matching duplicate is also being whitelisted
							Host otherHost = hosts.get(hostName);
							if (otherHost == null) {
								errors.add(Messages.getString("MSAProcessor.19") + hostName + Messages.getString("MSAProcessor.20") + address.getHostAddress() + Messages.getString("MSAProcessor.21")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							} else {
								// Check for mismatched Sender Groups
								if (otherHost.getSenderGroup() != host.getSenderGroup()) {
									errors.add(Messages.getString("MSAProcessor.22") + hostName + Messages.getString("MSAProcessor.23")); //$NON-NLS-1$ //$NON-NLS-2$
								}
								
								// Check for mismatched email addresses
								Set<String> emailAddresses = new HashSet<String>(Arrays.asList(host.getEmailAddresses()));
								Set<String> otherEmailAddresses = new HashSet<String>(Arrays.asList(otherHost.getEmailAddresses()));
								if (! otherEmailAddresses.equals(emailAddresses)) {
									errors.add(Messages.getString("MSAProcessor.24") + hostName + Messages.getString("MSAProcessor.25")); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						}
					}
				}
			}

			if (progress != null) {
				progress.setValue(progress.getValue() + 1);
				progress.repaint();
			}
		}
		
		if (!errors.isEmpty()) {
			// Print the header
			underline(out, errors.size()
					+ Messages.getString("MSAProcessor.106"), '='); //$NON-NLS-1$

			// Print the errors
			for (String error : errors) {
				out.println(error);
				}
			}
		else {
			out.println(Messages.getString("MSAProcessor.5")); //$NON-NLS-1$
		}
	}

	/**
	 * Parse the command line arguments and print help information if they are invalid
	 * @param args the command line arguments
	 * @return The parsed CommandLine object
	 */
	CommandLine parseArgs(String[] args) {
		
		Options options = new Options();
		
		options.addOption("d", Messages.getString("MSAProcessor.108"), false, Messages.getString("MSAProcessor.109")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		options.addOption("i", "ironport", false, Messages.getString("MSAProcessor.112")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		options.addOption("m", Messages.getString("MSAProcessor.4"), false, Messages.getString("MSAProcessor.115"));   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		
	
		CommandLineParser parser = new DefaultParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(options, args, true);
			if (cl.getArgs().length != 3)
				throw new ParseException(Messages.getString("MSAProcessor.116")); //$NON-NLS-1$
		} catch (ParseException pe) {
			System.err.println(Messages.getString("MSAProcessor.117")); //$NON-NLS-1$
			System.err.println();
			HelpFormatter formatter = new HelpFormatter();
			PrintWriter pw = new PrintWriter(System.err);
			formatter.printOptions(pw, 80, options, 5, 5);
			pw.flush();
			System.exit(0);
		}
		return cl;
	}

	/**
	 * Print a report heading and underline it
	 * @param out The PrintStream to print the heading to
	 * @param text The heading text
	 * @param underline The character to underline with
	 */
	void underline(PrintStream out, String text, char underline) {
		out.println(text);
		for (int i = 0; i < text.length(); i++) {
			out.print(underline);
		}
		out.println();
	}
}

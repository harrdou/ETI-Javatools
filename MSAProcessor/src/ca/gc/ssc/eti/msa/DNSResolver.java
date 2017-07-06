package ca.gc.ssc.eti.msa;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;

/**
 * An object that can perform DNS lookups
 * @author Doug Harris
 *
 */

class DNSResolver {
	InitialDirContext dns;

	/**
	 * Construct a DNSResolver object and connect to the DNS service
	 */
	DNSResolver() {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory"); //$NON-NLS-1$
		env.put("com.sun.jndi.dns.timeout.initial", "1000"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("com.sun.jndi.dns.timeout.retries", "1"); //$NON-NLS-1$ //$NON-NLS-2$
//		env.put(Context.PROVIDER_URL, "dns://127.0.0.1");

		try {
			dns = new InitialDirContext(env);
		} catch (NamingException e1) {
			System.err.println (Messages.getString("DNSResolver.5")); //$NON-NLS-1$
			return;
		}
	}
	
	
	/**
	 * Perform a DNS query
	 * @param name the name to look for
	 * @param recordType the DNS record type
	 * @return The query result, or null if no record is found
	 */
	List<String> lookup (String name, String recordType) {
		List<String> result = new ArrayList<String>();
		try {
			Attributes attrs = dns.getAttributes(name, new String[] {recordType});
			Attribute attr = attrs.get(recordType);
			
			if (attr == null || attr.size() == 0) {
				return result;
			}
					
			NamingEnumeration<?> e = attr.getAll();
			while  (e.hasMore()) {
				result.add(((String) e.next()).toLowerCase());
			}
		} catch (NamingException e1) {
			return result;
		}					
		return result;
	}

}

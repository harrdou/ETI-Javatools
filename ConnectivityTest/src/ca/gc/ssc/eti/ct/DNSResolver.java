package ca.gc.ssc.eti.ct;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;

class DNSResolver {
	InitialDirContext dns;

	DNSResolver() {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
		env.put("com.sun.jndi.dns.timeout.initial", "1000");
		env.put("com.sun.jndi.dns.timeout.retries", "1");
	
		try {
			dns = new InitialDirContext(env);
		} catch (NamingException e1) {
			System.err.println ("FATAL Error, unable to connect to DNS");
			System.exit(0);
		}
	}
	
	String getLocalServers() {
		try {
			return  (String) dns.getEnvironment().get("java.naming.provider.url");
		} catch (NamingException e) {
			return ("NOT FOUND");
		}
	}
	
	String lookup (String name, String recordType) {
		StringBuilder result = new StringBuilder();
		try {
			Attributes attrs = dns.getAttributes(name, new String[] {recordType});
			Attribute attr = attrs.get(recordType);
			
			if (attr.size() == 0) {
				return null;
			}
					
			NamingEnumeration<?> e = attr.getAll();
			result.append(e.next());
			while  (e.hasMore()) {
				result.append(", " + e.next());
			}
		} catch (NamingException e1) {
			return null;
		}					
		return result.toString();
	}

}

package ca.gc.ssc.eti.ct;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class ConnectivityTest {

	private static String[] aliases = {
		"autodiscover.canada.ca",
		"youremail-votrecourriel.canada.ca",
		"ss.youremail-votrecourriel.canada.ca",
		"ctst.youremail-votrecourriel.canada.ca",
		"votrecourriel-youremail.canada.ca",
		"youremail.canada.ca",
		"votrecourriel.canada.ca",
		"emailportal-portailcourriel.canada.ca",
		"portailcourriel-emailportal.canada.ca",
		"emailportal.canada.ca",
		"portailcourriel.canada.ca",
		"ms.emailportal-portailcourriel.canada.ca",
		"am.emailportal-portailcourriel.canada.ca",
		"aq.emailportal-portailcourriel.canada.ca",
		"ar.emailportal-portailcourriel.canada.ca",
		"mq.emailportal-portailcourriel.canada.ca",
		"si.emailportal-portailcourriel.canada.ca",
		"bl.emailportal-portailcourriel.canada.ca",
		"dr.emailportal-portailcourriel.canada.ca",
		"it.emailportal-portailcourriel.canada.ca",
		"sc.emailportal-portailcourriel.canada.ca",
		"webmail-courrielweb.canada.ca",
		"courrielweb-webmail.canada.ca",
		"webmail.canada.ca",
		"courrielweb.canada.ca",
		"autodiscover.ctst.canada.ca"
	};
	
	private static Site[] ALL_SITES = {Site.LEP, Site.VDA, Site.ICDC, Site.APDC};
	private static Site[] EMAIL_SITES = {Site.LEP, Site.VDA};
	private static Site[] AVAS_SITES = {Site.ICDC, Site.APDC};
	private static Site[] CTE_SITES = {Site.LEP};
//	private static Site[] R11_SITES = {Site.VDA};
	
	
	private static Service[] services = {
			new Service("S-1  User SDP Home", Service.Type.USER, "sdp1.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-2  Admin SDP Home", Service.Type.USER, "sdp2.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-3  User SDP Home", Service.Type.USER, "sdp1.email-courriel.canada.ca", 80, EMAIL_SITES),
			new Service("S-4  Admin SDP Home", Service.Type.USER, "sdp2.email-courriel.canada.ca", 80, EMAIL_SITES),
			new Service("S-5  PROD SSPR", Service.Type.USER, "ss.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-6  SDP MSME", Service.Type.USER, "ms.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-7  SDP AVAS MT", Service.Type.USER, "am.email-courriel.canada.ca", 443,EMAIL_SITES),
			new Service("S-8  SDP AVAS QT", Service.Type.USER,	"aq.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-9  SDP AVAS RPT", Service.Type.USER, "ar.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-10 SDP MSME QT", Service.Type.USER, "mq.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-11 SDP Bulk Load", Service.Type.USER, "bl.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-12 SDP DRA", Service.Type.USER,	"dr.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-13 SDP ITSM", Service.Type.USER,	"it.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-14 SDP SEIM", Service.Type.USER,	"si.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-15 SCIM", Service.Type.APPLICATION, "sc.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-16 SDP Redirect", Service.Type.USER, "redirect.email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-17 SDP Redirect", Service.Type.USER, "redirect.email-courriel.canada.ca", 80, EMAIL_SITES),

			new Service("S-18 PROD Exchange", Service.Type.BOTH, "email-courriel.canada.ca", 443, EMAIL_SITES),
			new Service("S-19 PROD Exchange", Service.Type.USER, "email-courriel.canada.ca", 80, EMAIL_SITES),
//			new Service("S-23 PROD Titus", Service.Type.USER, "tt.email-courriel.canada.ca", 993, EMAIL_SITES),
			new Service("S-20 PROD SMTP", Service.Type.APPLICATION, "smtp.email-courriel.canada.ca", 587, AVAS_SITES),
			new Service("S-21 PROD POP3", Service.Type.APPLICATION, "pop.email-courriel.canada.ca", 110, EMAIL_SITES),
			new Service("S-22 PROD POP3S", Service.Type.APPLICATION, "pop.email-courriel.canada.ca", 995, EMAIL_SITES),
			new Service("S-23 PROD IMAP4", Service.Type.APPLICATION, "imap.email-courriel.canada.ca", 143, EMAIL_SITES),
			new Service("S-24 PROD IMAP4S", Service.Type.APPLICATION, "imap.email-courriel.canada.ca", 993, EMAIL_SITES),

			new Service("S-25 CTE Exchange", Service.Type.BOTH, "ctst.email-courriel.canada.ca", 443, CTE_SITES),
			new Service("S-26 CTE Exchange", Service.Type.USER, "ctst.email-courriel.canada.ca", 80, CTE_SITES),
			new Service("S-27 CTE SDP", Service.Type.USER, "sdp1.ctst.email-courriel.canada.ca", 443, CTE_SITES),
//			new Service("S-32 CTE Titus", Service.Type.USER, "tt.ctst.email-courriel.canada.ca", 993, CTE_SITES),
			new Service("S-28 CTE SMTP", Service.Type.APPLICATION, "smtp.ctst.email-courriel.canada.ca", 587, AVAS_SITES),
			new Service("S-29 CTE POP3", Service.Type.APPLICATION, "pop.ctst.email-courriel.canada.ca", 110, CTE_SITES),
			new Service("S-30 CTE POP3S", Service.Type.APPLICATION, "pop.ctst.email-courriel.canada.ca", 995, CTE_SITES),
			new Service("S-31 CTE IMAP4", Service.Type.APPLICATION, "imap.ctst.email-courriel.canada.ca", 143, CTE_SITES),
			new Service("S-32 CTE IMAP4S", Service.Type.APPLICATION, "imap.ctst.email-courriel.canada.ca", 993, CTE_SITES)//,
//			new Service("S-33 SSC Relay", Service.Type.APPLICATION, "smtp.email-courriel.canada.ca", 25, AVAS_SITES)
		};
	

	public static void main(String[] args) throws UnknownHostException {
		Service.Type testType = Service.Type.BOTH;
		
		if (args.length > 0) {
			if ("-u".equals(args[0])) {
				testType = Service.Type.USER;
			} else if ("-a".equals(args[0])) {
				testType = Service.Type.APPLICATION;
			}
		}
		
		
		int numPassed = 0;

		Site[] sitesToTest;

		if (testType == Service.Type.USER)	{
			sitesToTest = EMAIL_SITES;
		} else {
			sitesToTest = ALL_SITES;
		}
		
		int totalTests = sitesToTest.length + aliases.length + 2;
		for (Service service : services) {
			if (testType == Service.Type.BOTH
					|| (testType == Service.Type.APPLICATION && service.isApplicationService())
					|| (testType == Service.Type.USER && service.isUserService())) {
				totalTests += service.getNumTests();
			}
		}
		
		System.out.println("your.email@canada.ca Connectivity Tester");
		System.out.println("Version 2.3, January 7, 2016");
		System.out.println();
		
		InetAddress localhost = InetAddress.getLocalHost();
		if (localhost instanceof Inet6Address) {
			System.out.println ("Sorry, IPv6 is not supported yet");
			return;
		}
		
		System.out.println("Running Connectivity test at " + new Date() 
		                    + " from " + localhost.getCanonicalHostName()
				            + " (" + localhost.getHostAddress() + ")");

		DNSResolver dns = new DNSResolver();
		
		String ptrName = String.format("%d.%d.%d.%d.in-addr.arpa.",
                localhost.getAddress()[3] & 0xFF,
                localhost.getAddress()[2] & 0xFF,
                localhost.getAddress()[1] & 0xFF,
                localhost.getAddress()[0] & 0xFF
                );
		
		System.out.println("Using local DNS server(s): "+ dns.getLocalServers());
		System.out.println("Reverse DNS lookup returned "+ dns.lookup(ptrName, "PTR"));
	
		System.out.println();
		
		ConnectivityTest connectivityTest = new ConnectivityTest();

		numPassed += connectivityTest.siteReachabilityTest(System.out, sitesToTest);
		System.out.println();

		numPassed += connectivityTest.nameServerTest(System.out, dns);
		System.out.println();
		
		numPassed += connectivityTest.aliasTest(System.out, dns);
		System.out.println();
		
		numPassed += connectivityTest.serviceTest(System.out, testType);
		System.out.println();

		if (testType != Service.Type.APPLICATION) {
			totalTests++;
			numPassed += connectivityTest.birchTest(System.out, dns);
			System.out.println();
		}
		
		System.out.println ("Overall Result: " + numPassed + "/"+ totalTests + " tests passed.");
	}
	
	private int nameServerTest(PrintStream out, DNSResolver dns) {
		int numPassed = 0;
		
		out.println("Verifying Nameserver resolution / Vérifier la résolution du Serveur de nom");
		out.println("==========================================================================");
		
		out.print("D-1 canada.ca nameservers found: ");
		long time = System.currentTimeMillis();
		String canadaNS = dns.lookup("canada.ca", "NS");
		time = System.currentTimeMillis() - time;
		if (canadaNS == null) {
			out.println("NOT FOUND");
		} else {
			out.println(canadaNS + " (" + time + "ms)");
			numPassed++;
		}

		out.print("D-2 email-courriel.canada.ca nameservers found: ");
		time = System.currentTimeMillis();
		String emailNS = dns.lookup("email-courriel.canada.ca", "NS");
		time = System.currentTimeMillis() - time;
		if (emailNS == null) {
			out.println("NOT FOUND");
		} else {
			out.println(emailNS + " (" + time + "ms)");
			numPassed++;
		}
		out.println();
		out.println (numPassed + "/2 nameserver tests passed.");
		return numPassed;
		
	}
	
	private int siteReachabilityTest (PrintStream out, Site[] sites) {
		int numPassed = 0;
		
		out.println("Testing Site Reachability / Tester l’accessibilité du Site");
		out.println("==========================================================");

		int i = 1;
		for (Site site : sites) {
			try {
				out.print("R-" + i++ + " Connect to " + site.getName() +": ");
				Socket socket = new Socket();
				long time = System.currentTimeMillis();
				socket.connect(new InetSocketAddress(site.getTestAddr(), site.getTestPort()), 2000);
				time = System.currentTimeMillis() - time;
				socket.close();
				out.println("PASS (" + time + "ms)");
				numPassed++;
			} catch (IOException e) {
				out.println("FAIL");
			}
		}

		out.println();
		out.println (numPassed + "/"+ ALL_SITES.length + " reachability tests passed.");
		
		return numPassed;
	}
	
	private int aliasTest(PrintStream out, DNSResolver dns) {
		int numPassed = 0;

		out.println("Testing all DNS aliases / Tester tous les alias DSN");
		out.println("===================================================");
			
		int i = 1;
		for (String alias : aliases) {
			out.print(String.format("A-%-2d %-40s--> ", i++, alias));
			long time = System.currentTimeMillis();
			String cnames = dns.lookup(alias, "CNAME");
			time = System.currentTimeMillis() - time;
			if (cnames != null) {
				out.println(cnames + " (" + time + "ms)");
				numPassed++;
			} else {
				out.println ("Lookup failed");
			}
		}
		
		out.println();
		out.println (numPassed + "/"+ aliases.length + " alias tests passed.");
		return numPassed;
	}
	
	private int serviceTest(PrintStream out, Service.Type types) {
		int numPassed = 0;
		int totalTests = 0;
		
		switch (types) {
			case BOTH:
				out.println("Testing all service endpoints / Test de tous les paramètre de service");
				out.println("=====================================================================");
				break;
			case USER:
				out.println("Testing user service endpoints / Test d'utilisateur les paramètre de service");
				out.println("============================================================================");
				break;
			case APPLICATION:
				out.println("Testing application service endpoints / Test d'application les paramètre de service");
				out.println("===================================================================================");
				break;
		}
		
		for (Service service : services) {
			if (types == Service.Type.BOTH
				|| (types == Service.Type.APPLICATION && service.isApplicationService())
				|| (types == Service.Type.USER && service.isUserService())) {
				totalTests += service.getNumTests();
				numPassed += service.test(out);
			}
		}
		
		out.println();
		out.println (numPassed + "/"+ totalTests + " service tests passed.");
		return numPassed;
	}
		
	private int birchTest(PrintStream out, DNSResolver dns) {
		int numPassed = 0;
		
		out.println("Checking for birch.int.bell.ca issue");
		out.println("====================================");
		
		try {
			InetAddress.getAllByName("email-courriel.birch.int.bell.ca");
			out.println("DNS search for email-courriel.birch.int.bell.ca resolved. This is not good.");
		} catch (UnknownHostException e) {
			out.println("DNS search for email-courriel.birch.int.bell.ca failed. This is good.");
			numPassed++;
		}
		
		return numPassed;
	}
}

package ca.gc.ssc.eti.spf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

public class SOAMiner {

	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		DNSResolver resolver = new DNSResolver();

		String domainName;
		while ((domainName = reader.readLine()) != null) {

			if (domainName.isEmpty()) {
				System.out.println();
				continue;
			} else if (domainName.startsWith("-")) {
				System.out.print(domainName.substring(1));
				continue;
			} else {
				System.out.print("\t");
			}
			
			List<String> mtas = resolver.lookup(domainName, "MX");
			for (int i = 0 ; i < mtas.size(); i++) {
				String mta = mtas.get(i);
				mta = mta.replaceFirst("[\\s0-9]*", "");
				mta = mta.substring(0, mta.length() - 1);
				mtas.set(i, mta);
			}
			Collections.sort(mtas);
			

			List<String> spfs = resolver.lookup(domainName, "TXT");
			for (Iterator<String> iterator = spfs.iterator(); iterator.hasNext();) {
				if (!iterator.next().toLowerCase().startsWith("\"v=spf1")) {
					iterator.remove();
				}
			}
			if (spfs.size() > 1) {
				System.err.println("Duuplicate SPF records for " + domainName);
			}
			
			List<String> dmarcs = resolver.lookup("_dmarc." + domainName, "TXT");
			if (dmarcs.size() > 1) {
				System.err.println("Duuplicate DMARC records for " + domainName);
			}

			String master = "";
			String contact ="";
			StringTokenizer soa = null;
			String authority = domainName;
			do {
				List<String> soas = resolver.lookup(authority, "SOA");
				if (soas.size() == 1) {
					soa = new StringTokenizer(soas.get(0));
					master = soa.nextToken();
					master = master.substring(0, master.length() - 1);
					contact = soa.nextToken();
					contact = contact.replaceFirst("\\.", "@");
					contact = contact.substring(0, contact.length() - 1);
				} else {
					authority = authority.substring(authority.indexOf('.') + 1);
				}
			} while (soa == null);		

			System.out.print(domainName.toLowerCase() + "\t");
			
			
			if (mtas.isEmpty()) {
				System.out.print("None!!!" + "\t");
			} else {
				System.out.print(mtas.get(0) + "\t");
			}
			
			if (spfs.isEmpty()) {
				System.out.print("None!" + "\t");
			} else {
				System.out.print(spfs.get(0) + "\t");
			}

			if (dmarcs.isEmpty()) {
				System.out.print("None!");
			} else {
				System.out.print(dmarcs.get(0));
			}
			
			System.out.println("\t" + master + "\t" + contact);
			for (int i = 1; i < mtas.size(); i++) {
				System.out.println("\t\t" + mtas.get(i));
			}
		}
		reader.close();
	}
}

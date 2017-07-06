package ca.gc.ssc;

import java.io.UnsupportedEncodingException;
import de.agitos.dkim.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

public class AuthLoad implements Runnable {
	
	static int THREADS = 350;
	static int COUNT = 10;
	private static Session session;

	public static void main(String [] args) throws InterruptedException {
		Thread [] threads = new Thread[THREADS]; 

        System.out.println(new Date() + " Starting Threads");
		long start = System.currentTimeMillis();
		connect();
		
	      for (int i = 0 ; i < THREADS; i++) {
				AuthLoad client = new AuthLoad();
				threads[i] = new Thread(client);
				threads[i].setName("Thread " + i);
			}
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
		
        long duration = System.currentTimeMillis() - start;
        double thoroughput = (double)(COUNT * THREADS) / (double)duration * 1000;

        System.out.println(new Date() + " Run Complete");
        System.out.println(String.format("%d threads authenticated %d times in %d ms (%.2f transactions/sec)", THREADS, COUNT * THREADS, duration, thoroughput));

}

public static void connect() {    
	      // Get system properties
			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.required", "true");
//			props.put("mail.smtp.host", "smtp.email-courriel.canada.ca");
//			props.put("mail.smtp.host", "205.195.242.152");
//			props.put("mail.smtp.host", "205.195.243.152");
			props.put("mail.smtp.port", "588");
   		props.put("mail.smtp.host", "localhost");
//			props.put("mail.smtp.host", "ironport.etilab.net");
//			props.put("mail.smtp.host", "mx1.canada.ca");
			props.put("mail.smtp.localhost", "viking.hq.xsilo.com");
			props.put("mail.smtp.auth.mechanisms", "PLAIN");
//			props.put("mail.smtp.from", "anonymous@localhost");
//			props.put("mail.smtp.from", "Douglas.Harris@canada.ca");
//    		props.put("mail.smtp.sendpartial", "true");
//			props.put("mail.from", "Do Not Reply / Ne Pas =?iso-8859-1?Q?R=E9pondre?= (SSC/SPC) <SSC.AIT-TOIA.SPC@ctst.canada.ca>");
			
	      Authenticator authenticator = new Authenticator() {
	    	  protected PasswordAuthentication getPasswordAuthentication() {
//	    		  return new PasswordAuthentication("SSC.AIT-TOIA.SPC@ctst.canada.ca", "3A+s6A~s8c6R%N=sh7+F6J+v@s2Y~K");
//	    		  return new PasswordAuthentication("ssc.ait-toia.spc@etilab.net", "YourEmail2017");
	    		  return new PasswordAuthentication("test.application.test@canada.ca", "=*8+J$t%@--@76--");
//	    		  return new PasswordAuthentication("test.application.test@canada.ca", "$25368469%54&9M8");
	    	  }
	      };

	      // Get the default Session object.
	      session = Session.getInstance(props, authenticator);
//	      session.setDebug(true);

}
     public void run() {
	      try{
	    	  for (int i = 0; i < COUNT; i++) {
	  			Transport transport = session.getTransport("smtp");
	            transport.connect();
	           // System.out.print(".");
	    	  transport.close();
	    	  }
	         
	      }catch (MessagingException mex) {
	         mex.printStackTrace();
	      } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	}


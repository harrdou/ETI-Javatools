package ca.gc.ssc;

import java.io.UnsupportedEncodingException;
import de.agitos.dkim.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

public class SMTPClient implements Runnable {
	
	static int THREADS = 1;
	static int COUNT = 1;
	private Session session;
	private Transport transport;

	public static void main(String [] args) throws InterruptedException {
		Thread [] threads = new Thread[THREADS]; 

        System.out.println(new Date() + " Opening Connections");
        for (int i = 0 ; i < THREADS; i++) {
			SMTPClient client = new SMTPClient();
			client.connect();
			threads[i] = new Thread(client);
			threads[i].setName("Thread " + i);
		}

        System.out.println(new Date() + " Starting Threads");
		long start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
        long duration = System.currentTimeMillis() - start;
        double thoroughput = (double)(COUNT * THREADS) / (double)duration * 1000;

        System.out.println(new Date() + " Run Complete");
        System.out.println(String.format("%d threads sent %d messages in %d ms (%.2f msg/sec)", THREADS, COUNT * THREADS, duration, thoroughput));

}

public void connect() {    
	      // Get system properties
			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.required", "true");
//			props.put("mail.smtp.host", "smtp.email-courriel.canada.ca");
//			props.put("mail.smtp.host", "205.195.242.152");
//			props.put("mail.smtp.host", "205.195.243.152");
			props.put("mail.smtp.port", "587");
    		props.put("mail.smtp.host", "localhost");
//			props.put("mail.smtp.host", "ironport.etilab.net");
//			props.put("mail.smtp.host", "mx1.canada.ca");
			props.put("mail.smtp.localhost", "viking.hq.xsilo.com");
//			props.put("mail.smtp.auth.mechanisms", "PLAIN");
//			props.put("mail.smtp.from", "anonymous@localhost");
//			props.put("mail.smtp.from", "Douglas.Harris@canada.ca");
//    		props.put("mail.smtp.sendpartial", "true");
//			props.put("mail.from", "Do Not Reply / Ne Pas =?iso-8859-1?Q?R=E9pondre?= (SSC/SPC) <SSC.AIT-TOIA.SPC@ctst.canada.ca>");
			
	      Authenticator authenticator = new Authenticator() {
	    	  protected PasswordAuthentication getPasswordAuthentication() {
//	    		  return new PasswordAuthentication("SSC.AIT-TOIA.SPC@ctst.canada.ca", "3A+s6A~s8c6R%N=sh7+F6J+v@s2Y~K");
	    		  return new PasswordAuthentication("SSC.ait-toia.SPC@etilab.net", "YourEmail2017");
//	    		  return new PasswordAuthentication("test.application.test@canada.ca", "a3A+s6A~s8c6R%N=sh7+F6J+v@s2Y~");
//	    		  return new PasswordAuthentication("test.application.test@canada.ca", "$25368469%54&9M8");
	    	  }
	      };

	      // Get the default Session object.
	      session = Session.getInstance(props, authenticator);
	      session.setDebug(true);
	      transport = null;

	      try {
			transport = session.getTransport("smtp");
            transport.connect();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

}
     public void run() {
	      try{
	    	  // Create a DKIM Signer
		     DKIMSigner dkimSigner1 = new DKIMSigner("xsilosystems.com", "default", "C:/Users/dharris/Work/ETI/SMTPTester/keys/private.key.der");
	    	 DKIMSigner dkimSigner2 = new DKIMSigner("canada.ca", "key2", "C:/Users/dharris/Work/ETI/SMTPTester/keys/key2.der");
	    	 DKIMSigner dkimSigner3 = new DKIMSigner("canada.ca", "key3", "C:/Users/dharris/Work/ETI/SMTPTester/keys/key3.der");
	    	 DKIMSigner dkimSigner4 = new DKIMSigner("etilab.net", "key3", "C:/Users/dharris/Work/ETI/SMTPTester/keys/key3.der");
	    	 DKIMSigner dkimSigner5 = new DKIMSigner("canada.ca", "dc81sah0001", "C:/Users/dharris/Work/ETI/SMTPTester/keys/key2.der");
   	 
	         // Create a default MimeMessage object.
	         MimeMessage message = new MimeMessage(session);

	         message.setFrom();
	         // Set Sender: header field of the header.
	         message.setFrom(new InternetAddress("SSC.DoNotReply-NePasRepondre.SPC@canada.ca", "Test Application (Test/Test)"));

	         // Set From: header field of the header.
//	         message.setFrom(new InternetAddress("Douglas.Harris@canada.ca", "Doug Harris (SSC/SPC)"));

	         // Set Reply-To: header field of the header.
//	         message.setReplyTo(new InternetAddress[] {new InternetAddress("SSC.DoNotReply-NePasRepondre.SPC@ctst.canada.ca", "AI Toolkit (SSC/SPC)")});
	         
	         // Set To: header field of the header.
	         message.addRecipient(Message.RecipientType.TO,
                     new InternetAddress("catchall@etilab.net", "Doug Harris (SSC/SPC)"));


/*	         final int RECIPIENTS = 1;
	         InternetAddress [] addresses = new InternetAddress[RECIPIENTS];
	         for (int i = 1; i <= RECIPIENTS; i++) {
	        	 addresses[i-1] = new InternetAddress("User" + i + "@test"+ i % 10 +".maildump.xyz", "User " + i);
	         }
        	 message.setRecipients(Message.RecipientType.TO, addresses);
*/
	         
	         // Set Subject: header field
	         message.setSubject("Test Message");

	         // Now set the actual message
	         StringBuilder body = new StringBuilder();
         
	         for (int l = 0 ; l < 1; l++) {
	        	 for (int c = 0 ; c <=60; c++)
	        		 body.append('X');
	        	 body.append("\n");
	         }
	         message.setText(body.toString());

//	         message.addHeader("Resent-Sender", "<Doug@xsilo.com>");
//	         message.addHeader("Resent-From", "<Doug@xsilo.com>");
//	         message.addHeader("Sender", "<Doug@xsilo.com>");
//	         message.addHeader("Received", "from localhost ([127.0.1.1]) by vikingi.etilab.net");
//	         message.addHeader("Received", "from vikingii.etilab.net (HELO Viking.hq.xsilo.com) (127.0.0.1) by vikingi.etilab.net");
//	         message.addHeader("DKIM-Signature", "d=canada.ca; SDFSFSD");
//	         message = new SMTPDKIMMessage((MimeMessage)message, dkimSigner1);     
//	         message = new SMTPDKIMMessage((MimeMessage)message, dkimSigner2);     
//	         message = new SMTPDKIMMessage((MimeMessage)message, dkimSigner3);     
//	         message = new SMTPDKIMMessage((MimeMessage)message, dkimSigner4);     
//	         message = new SMTPDKIMMessage((MimeMessage)message, dkimSigner5);
	         // Send message

	         System.out.println(new Date() + " " + Thread.currentThread().getName() + " started.");
  	 		 long start = System.currentTimeMillis();
	         for (int i = 1; i <= COUNT; i++) {
//	        	 message.setRecipient(Message.RecipientType.TO,
//                         new InternetAddress("User" + i + "@test"+ i % 10 +".maildump.xyz", "User " + i));
//		         Transport.send(message);
		         transport.sendMessage(message, message.getAllRecipients());
//		         System.out.print('.');
//		         if (i % 100 == 0) System.out.println();
	         }
	         long duration = System.currentTimeMillis() - start;
	         double thoroughput = (double)COUNT / (double)duration * 1000;
	         System.out.println(String.format(new Date() + " " + Thread.currentThread().getName() + " complete. Sent %d messages in %d ms (%.2f msg/sec)", COUNT, duration, thoroughput));

	         transport.close();
	         
	      }catch (MessagingException | UnsupportedEncodingException mex) {
	         mex.printStackTrace();
	      } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	}


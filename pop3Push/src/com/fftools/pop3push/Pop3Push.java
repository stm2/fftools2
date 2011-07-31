package com.fftools.pop3push;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.fftools.OutTextClass;
import com.fftools.tool1.Server;
import com.fftools.tool1.Settings;
import com.fftools.utils.FileCopy;



/*
 * pop3Fetcher soll Mails von einem POP3-Konto abrufen und 
 * bearbeiten.
 * 1. monopol befehle "password" müssen in das entsprechende Verzeichnis
 * 2. Eressea Report #[] -> CRs in reporte/actuelle
 * 3. Behandlung von Nachforderungen, Selektionen bitte usw
 * 4. merker: stoppNachfordern
 */

public class Pop3Push {

	private static final OutTextClass outText = OutTextClass.getInstance();

	private static Settings s = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// sets the output file
    	outText.setFile("FFTools_log_pop3Push_" + FileCopy.getDateSDay());
    	outText.addOutLine(FileCopy.getDateS() + " start pop3Push");
    	
    	// arg check
    	/*
    	 * 1 - servername
    	 * 2 - subject
    	 * 3 -  to[,to]
    	 * 4 - bodyfile (txt)
    	 * 5 - [Attachment file]
    	 */
    	
    	if (args.length<4){
    		// usage
    		System.out.println("pop3Push wrong Syntax. use:");
    		System.out.println("pop3Push servername subject to[,to] bodyText [AttachmentFileName]");
    		System.exit(1);
    	}
    	String serverName = args[0];
    	String subject = args[1];
    	String toStringOriginal = args[2];
    	String bodyText = args[3];
    	String test = "\n";
    	bodyText = bodyText.replace("\\n", test);
    	
    	String attachmentFile = null;
    	File anhangFile = null;
    	if (args.length==5){
    		attachmentFile = args[4];
    		// Angang check
        	anhangFile = new File(attachmentFile);
        	if (!anhangFile.exists() || !anhangFile.isFile()){
        		outText.addOutLine("Attachment non exist: " + attachmentFile + " (ignoring it)");
        		attachmentFile = null;
        		anhangFile=null;
        	}
    	}
    	
    	
    	
    	
		try {
			s = new Settings("toolsettings.config");
	    	s.sayStatus();
			
	    	Server server = s.getServer(serverName);
	    	if (server==null){
	    		outText.addOutLine("server " +  serverName + " nicht definiert.");
	    		System.exit(1);
	    	}
	    	
	    	
	    	MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );

	        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
	        String actFrom = server.getSender();
	        if (actFrom!=null){
	        	addressFrom=new InternetAddress(actFrom);
	        }
	        msg.setFrom( addressFrom );

	        
	        String[] receivers = toStringOriginal.split(",");
	        for (String receiver:receivers){
	        	if (receiver.length()>5){
			        InternetAddress addressTo = new InternetAddress(receiver);
			        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        	} else {
	        		outText.addOutLine("ignored receipient: " + receiver);
	        	}
	        }
	        msg.setSubject(subject);
	        if (attachmentFile==null || anhangFile==null){
	        	// keinen Anhang - plain text only
	        	msg.setContent(bodyText, "text/plain" );
	        } else {
	        	// mit einem Anhang !
	        	MimeMultipart content = new MimeMultipart();
	        	// normale Textnachricht
	        	MimeBodyPart text = new MimeBodyPart();
	  	        text.setContent(bodyText, "text/plain");
	  	        content.addBodyPart(text);
	  	        // der Anhang
	  	        BodyPart anhang = new MimeBodyPart();
			    anhang.setFileName(anhangFile.getName());
			    InputStream is = new FileDataSource(anhangFile).getInputStream();
				DataSource dh = new ByteArrayDataSource(is,"application/other");
				anhang.setDataHandler(new DataHandler(dh));
				content.addBodyPart(anhang);

				// final
				msg.setContent(content);

	        }
	        Transport.send( msg );
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
            System.exit(1);
        }
        outText.addOutLine(FileCopy.getDateS() + " pop3Push OK\n");
	}
}

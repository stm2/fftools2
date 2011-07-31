package com.fftools.tool2;



import java.io.BufferedReader;
import java.io.File;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import magellan.library.io.file.FileType;
import magellan.library.io.file.FileTypeFactory;
import magellan.library.utils.Encoding;

import com.fftools.OutTextClass;
import com.fftools.tool1.Server;
import com.fftools.tool1.Settings;
import com.fftools.tool1.myFaction;
import com.fftools.utils.FileCopy;

public class SendFileMail implements Runnable{
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private File myF = null;
	private int waitTime = 0;
	private Settings settings = null;
	
	
	public SendFileMail(Settings _s) {
		this.settings = _s;
		
	}
	
	/**
	public void run() {
		// TODO Auto-generated method stub
		if (this.myF == null) {
			outText.addOutLine("Thread SendFileMail started without File!");
			return;
		}
		if (this.waitTime==0) {
			outText.addOutLine("Thread SendFileMail started without waitTime!");
			return;
		}
		
		// umbenannt ist die Datei schon...
		// jetzt schlafen
		try {
			outText.addOutLine(FileCopy.getDateS() + " Thread start sleeping for File" + this.myF.getPath() + " Time:" + this.waitTime);
			Thread.sleep(this.waitTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			outText.addOutLine("Thread SendFileMail interrupted!");
			// e.printStackTrace();
			outText.addOutLine(e.toString());
		}
		
		// und wieder da..nu bearbeiten
		outText.addOutLine(FileCopy.getDateS() +  " Thread awaked for sending orders:" + this.myF.getAbsolutePath());
		// faction herausfinden (wir brauchen den richtigen Absender)
		String fileName = this.myF.getPath();
		int ordersPos = fileName.indexOf("_");
		if (ordersPos<0) {
			outText.addOutLine(FileCopy.getDateS() +  " no orders_ keyword detected in:" + this.myF.getAbsolutePath());
			return;
		}
		fileName = fileName.substring(ordersPos + 1);
		ordersPos = fileName.indexOf("_");
		if (ordersPos<0) {
			outText.addOutLine(FileCopy.getDateS() +  " no _ after faction detected in:" + this.myF.getAbsolutePath());
			return;
		}
		String factionName = fileName.substring(0,ordersPos);
		myFaction myF = this.settings.findFaction(factionName);
		if (myF==null) {
			outText.addOutLine(FileCopy.getDateS() +  " faction " + factionName + " not found for:" + this.myF.getAbsolutePath());
			return;
		}
		
		MailMessage mailMessage=null;
        try {
        	outText.addOutLine(FileCopy.getDateS() +  " trying to get new mailMessage");
			mailMessage = new MailMessage(myF.getSMTPHost());
			outText.addOutLine(FileCopy.getDateS() +  " trying to AUTH with SERVER");
			mailMessage.sendAUTH_PLAIN(this.settings.getDirectory("secret1"));
			outText.addOutLine(FileCopy.getDateS() +  " setting sender:" + myF.getEmail());
			mailMessage.from(myF.getEmail());
			outText.addOutLine(FileCopy.getDateS() +  " setting recipient:" + this.settings.getDirectory("eressea_server_email"));
			mailMessage.to(this.settings.getDirectory("eressea_server_email"));
			outText.addOutLine(FileCopy.getDateS() +  " setting recipient: backup to sf");
			mailMessage.to("sf@fietefietz.de");
			outText.addOutLine(FileCopy.getDateS() +  " setting Date");
			mailMessage.setDate();
			outText.addOutLine(FileCopy.getDateS() +  " setting Header");
			mailMessage.setHeader("Content-Type", "text/plain; charset=" +
					Encoding.ISO.toString());
			outText.addOutLine(FileCopy.getDateS() +  " setting Subject");
			mailMessage.setSubject("ERESSEA BEFEHLE");

			
		} catch(IOException e) {
			
			outText.addOutLine(e.toString());
			return;
		}

		try {
			Writer mailWriter = null;
			outText.addOutLine(FileCopy.getDateS() +  " body: new mail writer: ");
			mailWriter = new OutputStreamWriter(mailMessage.getPrintStream(),
					Encoding.ISO.toString());
			outText.addOutLine(FileCopy.getDateS() +  " reading and writing orders");
			/**
			FileReader fR = new FileReader(this.myF.getAbsolutePath());
			BufferedReader bR = new BufferedReader(fR);
			
			// FileType fT = new FileType(this.myF,false);
			FileType fT = FileTypeFactory.singleton().createFileType(this.myF, false);
			BufferedReader rT = new BufferedReader(fT.createReader());
			
			String line = null;
			int cnter = 0;
			while ((line = rT.readLine()) != null) {
				mailWriter.write(line + "\n");
				cnter += 1;
	         }
	         rT.close();
			mailWriter.flush();
			outText.addOutLine(FileCopy.getDateS() +  " msg send and close, wrote " + cnter + " lines");
			mailMessage.sendAndClose();
			outText.addOutLine(FileCopy.getDateS() +  " closing mailwriter");
			mailWriter.close();
		} catch(IOException e) {
			outText.addOutLine(FileCopy.getDateS() +  " " + e.toString());
			return;
		}
	}
	*/
	
	
	public void run() {
		// TODO Auto-generated method stub
		if (this.myF == null) {
			outText.addOutLine("Thread SendFileMail started without File!");
			return;
		}
		if (this.waitTime==0) {
			outText.addOutLine("Thread SendFileMail started without waitTime!");
			return;
		}
		
		// umbenannt ist die Datei schon...
		// jetzt schlafen
		try {
			outText.addOutLine(FileCopy.getDateS() + " Thread start sleeping for File" + this.myF.getPath() + " Time:" + this.waitTime);
			Thread.sleep(this.waitTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			outText.addOutLine("Thread SendFileMail interrupted!");
			// e.printStackTrace();
			outText.addOutLine(e.toString());
		}
		
		// und wieder da..nu bearbeiten
		outText.addOutLine(FileCopy.getDateS() +  " Thread awaked for sending orders:" + this.myF.getAbsolutePath());
		// faction herausfinden (wir brauchen den richtigen Absender)
		String fileName = this.myF.getPath();
		int ordersPos = fileName.indexOf("_");
		if (ordersPos<0) {
			outText.addOutLine(FileCopy.getDateS() +  " no orders_ keyword detected in:" + this.myF.getAbsolutePath());
			return;
		}
		fileName = fileName.substring(ordersPos + 1);
		ordersPos = fileName.indexOf("_");
		if (ordersPos<0) {
			outText.addOutLine(FileCopy.getDateS() +  " no _ after faction detected in:" + this.myF.getAbsolutePath());
			return;
		}
		String factionName = fileName.substring(0,ordersPos);
		myFaction myF = this.settings.findFaction(factionName);
		if (myF==null) {
			outText.addOutLine(FileCopy.getDateS() +  " faction " + factionName + " not found for:" + this.myF.getAbsolutePath());
			return;
		}
		
		
		String serverOUT = this.settings.getDirectory("serverOUT");
		Server server = this.settings.getServer(serverOUT);
		outText.addOutLine(FileCopy.getDateS() +  " sending Befehlsdatei für " + myF.getName());
		try {
		
			MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );
	    	
	        InternetAddress addressFrom=new InternetAddress(myF.getEmail());
	        msg.setFrom( addressFrom );
	        String emailAdress = this.settings.getDirectory("eressea_server_email");
	        InternetAddress addressTo = new InternetAddress(emailAdress);
	        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        
	        // und noch sicherheitskopie
	        emailAdress = this.settings.getDirectory("befehlskopie");
	        if (emailAdress!=null && emailAdress.length()>0){
		        addressTo = new InternetAddress(emailAdress);
		        msg.addRecipient( Message.RecipientType.CC, addressTo );
	        }
	        
	        msg.setSubject("ERESSEA BEFEHLE");
	        
	        String bodyText = "";
	        
	        FileType fT = FileTypeFactory.singleton().createFileType(this.myF, false);
			BufferedReader rT = new BufferedReader(fT.createReader());
			
			String line = null;
			int cnter = 0;
			while ((line = rT.readLine()) != null) {
				bodyText +=line + "\n";
				cnter += 1;
	        }
	        rT.close();

	        msg.setContent(bodyText, "text/plain; charset=" + Encoding.ISO.toString() );
	        Transport.send( msg );
	        outText.addOutLine(FileCopy.getDateS() +  " sending OK ");
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
			outText.addOutLine(FileCopy.getDateS() +  " sending NOT OK:\n" + exc.toString());
            System.exit(1);
        }

		
	}
	
	
	public void setFile(File _f){
		this.myF = _f;
	}
	
	public void setWaitTime(int _i) {
		this.waitTime = _i;
	}

}

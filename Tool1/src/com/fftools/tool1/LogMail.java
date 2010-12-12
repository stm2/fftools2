package com.fftools.tool1;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import magellan.library.utils.Encoding;

import com.fftools.OutTextClass;
import com.fftools.utils.FileCopy;


/**
 * Ein log, welches abschliessend als Mail rausgeht
 * @author Fiete
 *
 */
public class LogMail {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private String mailBody = "";
	
	private String mySubject = "Monopolserver: Befehlseingang";
	
	private Settings settings = null;
	
	
	public LogMail(Settings _s) {
		this.settings = _s;
		
	}
	
	
	
	public void addLog(String line){
		this.addLog(line, true);
	}
	
	public void addLog(String line,boolean Linebreak){
		this.mailBody += line;
		if (Linebreak){
			this.mailBody +="\n";
		}
	}
	
	/**
	 * allways with line break
	 * @param line
	 */
	public void insertAtBeginn(String line){
		this.mailBody = line + "\n" + this.mailBody;
	}
	
	
	
	public void setSubject(String name){
		this.mySubject = name;
	}
	
	
	public void cancel(String reason){
		this.mailBody = "ABBRUCH der Befehlsbearbeitung\n\n" + reason + "\n" + this.mailBody;
		this.mySubject += " (Fehler!)";
		this.sendLog();
	}
	
	public void overAndOut(){
		this.mailBody = "Befehlsbearbeitung OK\n\nLog:\n" + this.mailBody;
		this.sendLog();
	}
	/**
	public boolean sendLog(){
		boolean erg = true;
		
		MailMessage mailMessage=null;
		 try {
	        	// outText.addOutLine(FileCopy.getDateS() +  " trying to get new mailMessage");
				mailMessage = new MailMessage("dasmonopol.de");
				// outText.addOutLine(FileCopy.getDateS() +  " trying to AUTH with SERVER");
				mailMessage.sendAUTH_PLAIN(this.settings.getDirectory("secret1"));
				// outText.addOutLine(FileCopy.getDateS() +  " setting sender:" + myF.getEmail());
				mailMessage.from("Monopol<monopol@dasmonopol.de>");
				// outText.addOutLine(FileCopy.getDateS() +  " setting recipient:" + this.settings.getDirectory("eressea_server_email"));
				mailMessage.to("Monopol-Server@googlegroups.com");
				// outText.addOutLine(FileCopy.getDateS() +  " setting recipient: backup to sf");
				mailMessage.to("sf@fietefietz.de");
				// outText.addOutLine(FileCopy.getDateS() +  " setting Date");
				mailMessage.setDate();
				// outText.addOutLine(FileCopy.getDateS() +  " setting Header");
				mailMessage.setHeader("Content-Type", "text/plain; charset=" +
						Encoding.ISO.toString());
				// outText.addOutLine(FileCopy.getDateS() +  " setting Subject");
				mailMessage.setSubject(this.mySubject);
				
				Writer mailWriter = null;
				// outText.addOutLine(FileCopy.getDateS() +  " body: new mail writer: ");
				mailWriter = new OutputStreamWriter(mailMessage.getPrintStream(),
						Encoding.ISO.toString());
				
				if (this.mailBody.length()>2){
					mailWriter.write(this.mailBody);
				} else {
					mailWriter.write("kein log zum senden!");
				}
				
				mailWriter.flush();
				mailMessage.sendAndClose();
				mailWriter.close();
				
			} catch(IOException e) {
				
				outText.addOutLine(e.toString());
				return false;
			}

		return erg;
	}
	*/
	
	public boolean sendLog(){
		boolean erg = true;
		
		String serverOUT = this.settings.getDirectory("serverOUT");
		Server server = this.settings.getServer(serverOUT);
		outText.addOutLine(FileCopy.getDateS() +  " sending MailLog");
		try {
		
			MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );
	    	
	        InternetAddress addressFrom=new InternetAddress("monopol@dasmonopol.de");
	        
	        String actFrom = server.getSender();
	        if (actFrom!=null){
	        	addressFrom=new InternetAddress(actFrom);
	        }
	        
	        msg.setFrom( addressFrom );
	        
	        String emailAdress = this.settings.getDirectory("MonopolBefehlseingangInfo");
	        InternetAddress addressTo = new InternetAddress(emailAdress);
	        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        
	        // und noch sicherheitskopie
	        emailAdress = this.settings.getDirectory("befehlskopie");
	        if (emailAdress!=null && emailAdress.length()>0){
		        addressTo = new InternetAddress(emailAdress);
		        msg.addRecipient( Message.RecipientType.CC, addressTo );
	        }
	        
	        msg.setSubject(this.mySubject);

	        msg.setContent(this.mailBody, "text/plain; charset=" + Encoding.ISO.toString() );
	        Transport.send( msg );
	        outText.addOutLine(FileCopy.getDateS() +  " sending OK ");
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
			outText.addOutLine(FileCopy.getDateS() +  " sending NOT OK:\n" + exc.toString());
            System.exit(1);
        }
		return erg;
	}
	
}

package com.fftools.pop3fetcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.fftools.OutTextClass;
import com.fftools.pop3push.MailAuthenticator;
import com.fftools.tool1.Server;
import com.fftools.tool1.Settings;
import com.fftools.tool1.User;
import com.fftools.utils.FileCopy;



/*
 * pop3Fetcher soll Mails von einem POP3-Konto abrufen und 
 * bearbeiten.
 * 1. monopol befehle "password" müssen in das entsprechende Verzeichnis
 * 2. Eressea Report #[] -> CRs in reporte/actuelle
 * 3. Behandlung von Nachforderungen, Selektionen bitte usw
 * 4. merker: stoppNachfordern
 */

public class Pop3Fetcher {

	private static final OutTextClass outText = OutTextClass.getInstance();

	private static Settings s = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// sets the output file
    	outText.setFile("FFTools_log_pop3Fetcher_" + FileCopy.getDateSDay());
    	outText.addOutLine(FileCopy.getDateS() + " start pop3Fetcher");
		try {
			s = new Settings("toolsettings.config");
	    	s.sayStatus();
			processMails();
	    	
	    	
	    	
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
            System.exit(1);
        }
        // outText.addOutLine(FileCopy.getDateS() + " pop3Fetcher OK\n");
	}
	
	/**
	 * testProc zum Auflisten der Mails auf dem POP3 Konto
	 */
	private static void processMails() throws Exception
 {
		System.out.println( "starting" );
		
		
		Server monopolServer = s.getServer("monopol");
		if (monopolServer==null){
			System.out.println( "Server monopol nicht definiert.");
			return;
		}

		Session session = Session.getDefaultInstance(new Properties());
		Store store = session.getStore("pop3");
		store.connect(monopolServer.getHost(),monopolServer.getUser(),monopolServer.getPass());
		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		Message message[] = folder.getMessages();

	    for ( int i = 0; i < message.length; i++ )
	    {
	      Message m = message[i];
	      
	      
	      outText.addOutLine("inspecting Message: " + i + " from: " + m.getFrom()[0] + " with Subject: " + m.getSubject() + ", sent at:" + m.getSentDate().toString());
	            
	      if (!isProcessed(m, monopolServer)){
		      if (m.getContent()!=null && m.getContent() instanceof Multipart) {
			      Multipart mp = (Multipart) m.getContent();
			      
			      for ( int j = 0; j < mp.getCount(); j++ )
			      {
				      Part part = mp.getBodyPart( j );
				      outText.addOutLine( "\nPart: " + j);
			        
			          MimeBodyPart mimePart = (MimeBodyPart)part;
			          
			          
			          boolean isEresseaReport=true;
			          // Check Betreff
			          if (!(m.getSubject().indexOf("Eressea Report #")>-1)){
			        	  isEresseaReport = false;
			        	  outText.addOutLine( ": no eressea report.\n");
			          }
			          if (isEresseaReport){
			        	  // Untersuchung FileName
			        	  if (mimePart.getFileName()==null || !mimePart.getFileName().endsWith(".zip")){
			        		  // kein Zip drinnne
			        		  isEresseaReport=false;
			        		  outText.addOutLine( ": eressea report with no zip inside.\n");
			        	  }
			          }
			          if (isEresseaReport){
			              processEresseaReport(m, mimePart);	  
			          }

			      }
			     
			      
		      } else {
		    	  // System.out.println( "Content=null or no Multipart");
		      }

		      // Auswertung nur auf Grund des Subjects!
		      
		      boolean isMonopolBefehl=true;
		      // Check Betreff
	          if (!(m.getSubject().toLowerCase().startsWith("monopol befehle"))){
	        	  isMonopolBefehl = false;
	          }
	          User u = null;
	          if (isMonopolBefehl){
	        	  // Untersuchung Passwort
	        	  // Passwort extrahieren
	        	  isMonopolBefehl=false;
	        	  if (m.getSubject().length()>16){
	        		  String checkPass=m.getSubject().substring(16);
	        		  checkPass = checkPass.trim().replace("\"", "");
	        		  u = s.getUser(checkPass);
	        		  if (u!=null){
	        			  isMonopolBefehl=true;	  
	        		  } else {
	        			  // unbekanntes passwort!
	        			  reportWrongPasswort(m);
	        		  }
	        	  }
	          }
	          if (isMonopolBefehl){
	              // Zug gefunden
	        	  searchForZipInBefehlsMail(m,u);
	          }

	    	  boolean isNachforderung=true;
	    	  // Check Betreff
	          if (!(m.getSubject().toLowerCase().startsWith("nachforderung"))){
	        	  isNachforderung = false;
	          }
	          
	          u = null;
	          if (isNachforderung){
	        	  // Untersuchung Passwort
	        	  // Passwort extrahieren
	        	  isNachforderung=false;
	        	  if (m.getSubject().length()>14){
	        		  String checkPass=m.getSubject().substring(14);
	        		  checkPass = checkPass.trim().replace("\"", "");
	        		  u = s.getUser(checkPass);
	        		  if (u!=null){
	        			  isNachforderung=true;	  
	        		  } else {
	        			  // unbekanntes passwort!
	        			  reportWrongPasswort(m);
	        		  }
	        	  }
	          }
	          if (isNachforderung){
	              // Zug gefunden
	        	  processMonopolNachforderung(m);
	          }
		      
		      
	          // Selektionen bitte
	          if ((m.getSubject().toLowerCase().equals("selektionen bitte"))){
	        	  processSelektionenBitte(m);
	          }
	          
		      
		      // als processed setzen
		      setIsProcessed(m, monopolServer);
	      } else {
	    	  outText.addOutLine("already processed (" + getIDtag(m) + ")");
	      }
	    }
	    folder.close( false );
	    store.close();
	    
	    outText.addOutLine("pop3FetcherMain finished.");
	    
	}
	
	/**
	 * checkt im procDir des Servers, ob diese Message bereits bearbeitet wurde
	 * @param m
	 * @return
	 */
	private static boolean isProcessed(Message m, Server server){
		if (server.getProcDir()==null || server.getProcDir().length()==0){
			return false;
		}		
		
		// gibt es eine txt-file im procDir?
		String procDirS = server.getProcDir();
		File procDir = new File(procDirS);
		if (!procDir.exists() || !procDir.isDirectory()){
			// outText.addOutLine("ReportDir nicht vorhanden oder kein Dir: " + procDir);
			return false;
		}
		String[] fileNames = procDir.list();
		if (fileNames==null || fileNames.length==0){
			// outText.addOutLine("procDir ohne Dateien, Listing leer");
			return false;
		}
		
		for (String s : fileNames){
			String search = getIDtag(m) + ".txt";
			if (s.endsWith(search)){
				return true;
			}
		}
		return false;
	}
	
	private static boolean setIsProcessed(Message m, Server server){
		if (server.getProcDir()==null || server.getProcDir().length()==0){
			return false;
		}		
		
		// gibt es eine txt-file im procDir?
		String procDirS = server.getProcDir();
		File procDir = new File(procDirS);
		if (!procDir.exists() || !procDir.isDirectory()){
			outText.addOutLine("ReportDir nicht vorhanden oder kein Dir: " + procDir);
			return false;
		}
		String[] fileNames = procDir.list();
		
		for (String s : fileNames){
			String search = getIDtag(m) + ".txt";
			if (s.endsWith(search)){
				// schon vorhanden
				outText.addOutLine("setIsProcessed obwohl txt vorhanden");
				return false;
			}
		}
		
		// Datei anlegen
		FileWriter fw = null;
		try {
			String newFileName = procDirS + File.separator + getIDtag(m)+".txt";
			// String newFileName = getIDtag(m)+".txt";
			fw = new FileWriter(newFileName,true);
		} catch (IOException ioexec){
			outText.addOutLine("setIsProcessed kann txt nicht anlegen: " + ioexec.toString());
			return false;
		}
		// Datei beschreiben
		try {
			fw.write(FileCopy.getDateS() + "\n");
			fw.flush();
			fw.close();
		} catch (IOException ioexec){
			outText.addOutLine("setIsProcessed kann in txt nicht schreiben");
			return false;
		}
		
		
		
		return false;
	}
	
	
	
	/**
	 * liefert zu einer Message ein Dateinamen....zum ID
	 * @param m
	 * @return
	 */
	private static String getIDtag(Message m){
		// proc+Datum+sender+subject
		String erg="proc_";
		// Datum
		try {
			Date dat = m.getSentDate();
			SimpleDateFormat fr = new SimpleDateFormat("yyyyMMdd_HHmmss");
	  	  	String dateS =  fr.format(dat);
	  	  	erg+=dateS+"_";
		} catch (MessagingException mex){
			erg+="error_";
			outText.addOutLine("Message has no Sent Date: " + mex.toString());
		}
		// sender
		try {
			Address[] sender = m.getFrom();
			Address sender1 = sender[0];
			
			String sender1S = sender1.toString();
			sender1S = sender1S.replace("@", "_");
			sender1S = sender1S.replace(" ", "_");
			sender1S = sender1S.replace("<", "_");
			sender1S = sender1S.replace(">", "_");
			sender1S = sender1S.replace("?", "");
			sender1S = sender1S.replace("=", "");
			erg+=sender1S+"_";
		} catch (MessagingException mex){
			erg+="unknown_";
			outText.addOutLine("Message has wrong From: " + mex.toString());
		}
		
		try {
			if (m.getSubject()!=null && m.getSubject().length()>0){
				String toAdd = m.getSubject();
				toAdd = toAdd.replace("\"","");
				toAdd = toAdd.replace("?","");
				toAdd = toAdd.replace("=","");
				erg+=toAdd;
			}
		} catch (MessagingException mex){
			erg+="unknown";
			outText.addOutLine("Message has no subject(?): " + mex.toString());
		}
		
		// reicht noch nicht, wenn multipart, dann auch noch die Namen der Anhänge
		try {
			if (m.getContent()!=null && m.getContent() instanceof Multipart) {
			      Multipart mp = (Multipart) m.getContent();
			      for ( int j = 0; j < mp.getCount(); j++ )
			      {
				      Part part = mp.getBodyPart( j );
				      String toAdd = part.getFileName();
				      if (toAdd!=null && toAdd.length()>0){
				    	  toAdd = toAdd.replace(".","_");
				    	  erg+="_" + toAdd;
				      }
			      }    
			}
		} catch (Exception mex){
			erg+="err_mimepart";
			outText.addOutLine("Messagepart not analyzed: " + mex.toString());
		}
		return erg;
	}
	
	
	/**
	 * Schreibt das zip in den Report-Ordner
	 * (Auspacken?)
	 * @param m
	 * @param part
	 */
	private static void processEresseaReport(Message m, MimeBodyPart part){
		String reportDir = s.getDirectory("reports");
		workOnZipFFTools(reportDir, part, false);
	}
	
	/**
	 * beachrichtigt NachfordernMailInfo und Absender, dass das Passwort nicht erkannt wurde
	 * @param m
	 */
	private static void reportWrongPasswort(Message m){
		
		String serverName = "monopol_gdr";
		
		try {
			
	    	Server server = s.getServer(serverName);
	    	if (server==null){
	    		outText.addOutLine("server " +  serverName + " nicht definiert.");
	    		System.exit(1);
	    	}
	    	
	    	
	    	MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );

	        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
	        msg.setFrom( addressFrom );

	        InternetAddress addressTo = new InternetAddress(s.getDirectory("NachfordernMailInfo"));
	        msg.addRecipient( Message.RecipientType.TO, addressTo );
	        msg.addRecipients( Message.RecipientType.TO, m.getFrom());
	        	
	        msg.setSubject("Monopol Server hat Passwort nicht erkannt!");
	        
	        String bodyText = "Der Betreff der Mail war:\n";
	        bodyText+=m.getSubject()+"\n";
	        bodyText+="Das angegebene Passwort ist dem Server nicht bekannt.\n";
	        bodyText+="Mit Gruessen vom Monopol\n";
        	// keinen Anhang - plain text only
        	msg.setContent(bodyText, "text/plain" );
	        
	        Transport.send( msg );
	        outText.addOutLine("Wrong Passwort processed.");
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
            System.exit(1);
        }
	}

	
	/**
	 * bearbeitet einen MimeBodyPart mit zugZip mit Befehlen und bereits geprüften Passwort
	 * @param m
	 * @param part
	 * @param u
	 */
	private static void processMonopolBefehl(MimeBodyPart part,User u){
		String zugDir = s.getDirectory("zuege") + File.separator + u.getName();
		workOnZipFFTools(zugDir, part, true);
	}
	
	/**
	 * sichert die zip in das dir und packt den CR aus
	 * wenn delZip=true wird zip anschliessend gelöscht
	 * @param directory
	 * @param part
	 * @param delZip
	 */
	private static void workOnZipFFTools(String directory,MimeBodyPart part,boolean delZip){
		
		try {
			String newFileName = directory + File.separator + part.getFileName();
			InputStream is = part.getInputStream();
			File newFile = new File(newFileName);
			FileOutputStream fwOne = new FileOutputStream(newFile);
			byte b[] = new byte[is.available()];
			is.read(b);
			fwOne.write(b);
			fwOne.close();
			outText.addOutLine("saved: " + newFileName);
			// CR auspacken
			FileInputStream fis = new FileInputStream(newFileName);
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			while((entry = zin.getNextEntry()) != null) {
			    // extract data
			    // open output streams
				if (entry.getName().endsWith(".cr")){
					int BUFFER = 2048;
					FileOutputStream fos = new FileOutputStream(directory + File.separator + entry.getName());
					BufferedOutputStream dest = new  BufferedOutputStream(fos, BUFFER);
					int count = 0;
					byte data[]= new byte[BUFFER];
					while ((count = zin.read(data, 0, BUFFER)) != -1) {
					   //System.out.write(x);
					   dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
					outText.addOutLine("extracted: " + entry.getName());
				}
			}
			zin.close();
			
			if (delZip){
				newFile.delete();
			}
			
		} catch (MessagingException mesx) {
			outText.addOutLine("workOnZipFFTools:" + mesx.toString());
			return;
		} catch (IOException iex){
			outText.addOutLine("workOnZipFFTools:" + iex.toString());
			return;
		}

	}
	
	/**
	 * wickelt die Nachforderung ab
	 * @param m
	 * @param part
	 */
	private static void processMonopolNachforderung(Message m){
		try {
			String serverName="monopol_gdr";
			
	    	Server server = s.getServer(serverName);
	    	if (server==null){
	    		outText.addOutLine("server " +  serverName + " nicht definiert.");
	    		System.exit(1);
	    	}
	    	
	    	
	    	MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
	    	
	    	Properties props = new Properties();
	    	props.put("mail.smtp.host", server.getHost());
	    	props.put("mail.smtp.auth", "true");
	    	// Session session = Session.getDefaultInstance(props,auth);
	    	Session session = Session.getInstance(props,auth);
	    	
	    	Message msg = new MimeMessage( session );

	        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
	        msg.setFrom( addressFrom );

	        
	       
			msg.addRecipients(Message.RecipientType.TO, m.getFrom());
	        	
	        msg.setSubject("Nachgeforderter Monopol Report");
	        
        	// mit einem Anhang !
        	MimeMultipart content = new MimeMultipart();
        	// normale Textnachricht
        	MimeBodyPart text = new MimeBodyPart();
        	
        	String bodytext = "Der aktuelle Monopol-Report befindet sich im Anhang.\nMit besten Gruessen vom Monopol";
        	
  	        text.setContent(bodytext, "text/plain");
  	        content.addBodyPart(text);
  	        // der Anhang
  	        BodyPart anhang = new MimeBodyPart();
		    anhang.setFileName("monopol.zip");
		    File anhangFile = new File(s.getDirectory("monopolreportzip"));
		    InputStream is = new FileDataSource(anhangFile).getInputStream();
			DataSource dh = new ByteArrayDataSource(is,"application/other");
			anhang.setDataHandler(new DataHandler(dh));
			content.addBodyPart(anhang);

			// final
			msg.setContent(content);

	        
	        Transport.send( msg );
	        outText.addOutLine("Nachforderung versand.");
		} catch (Throwable exc) { // any fatal error
            // outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt 
			System.out.println(exc.toString());
            System.exit(1);
        }
	}
	
	/**
	 * wickelt nachgeforderte Selektionen ab
	 * mittels aufruf des SelectionInfo2.sh scriptes
	 * (das wird auch beim normalen Aufruf nach script lauf genutzt)
	 * @param m
	 */
	private static void processSelektionenBitte(Message m){
		
		Address[] a;
		outText.addOutLine("Selektionen angefordert.");
		try {
			a = m.getReplyTo();
			if (a==null || a.length==0){
				outText.addOutLine("Fehler: kein replyTo");
				return;
			}
			
		} catch (MessagingException e){
			outText.addOutLine("Fehler: " + e.toString());
			return;
		}
		InternetAddress from = (InternetAddress) a[0];
		String fromS = from.getAddress();
		outText.addOutLine("Selektionen angefordert von: " + fromS);
		String command = "/home/monopol/eressea/scripts/SelectionInfo2.sh " + fromS;
		outText.addOutLine("executing command: " + command);
		try {
		    java.lang.Runtime.getRuntime().exec( command);
		}
		catch ( java.io.IOException e ) {
		    outText.addOutLine( "Error: " + e.toString());
		    return;
		}

		outText.addOutLine("SelektionenBitte finished");
	}
	
	/**
	 * Mail is ok in subject and password
	 * @param m
	 */
	private static void searchForZipInBefehlsMail(Message m, User u){
		StringBuffer logText= new StringBuffer();
		try {
			logText.append("Lieber Monopolist, \n");
			logText.append("von " + m.getFrom()[0] + " wurde eine gueltige Befehlsmail empfangen.\n");
			logText.append("Diese soll Befehle von  " + u.getName() + " enthalten.\n\n");
			logText.append("Dafuer dankt das Monopol Dir.\n\n");
			logText.append("Hier nun ein Kurzreport ueber den Versuch, irgendetwas sinnvolles mit Deiner Mail anzustellen.\n");
			/*
			if (m.getContent()!=null && m.getContent() instanceof Multipart) {
			      Multipart mp = (Multipart) m.getContent();
			      onlyMonopolBefehlBetreff=false;
			      zipFound=false;
			      for ( int j = 0; j < mp.getCount(); j++ )
			*/
			boolean runOK = true;
			if (m.getContent()==null){
				logText.append("Deine Mail hat leider gar keinen Inhalt.\n");
				outText.addOutLine( "Error: Mail ohne Inhalt");
				runOK=false;
			}
			if (runOK && !(m.getContent() instanceof Multipart)){
				logText.append("Deine Mail ist keine Multipart-Mail, das hatten wir hier aber erwartet.\n");
				outText.addOutLine( "Error: Nix Multipart");
				runOK=false;
			}
			if (runOK){
				logText.append("Deine Mail hat einen Multipart-Bestandteil, der wird weiter untersucht.\n");
				outText.addOutLine( "Multipart OK");
				Multipart mp = (Multipart)m.getContent();
				boolean retVal = searchForZipInMultiPart(mp,logText,u,1);
				
				if (retVal) {
					logText.append("\nFazit:\nYep, ZIP gefunden.\n\n");
				} else {
					logText.append("\nFazit:\nLeider kein ZIP gefunden.\n\n");
				}
				
				logText.append("\nHoffentlich war das aussagefaehig. Weil von dieser Seite wars dass...\n\n");
				logText.append("Mit besten Gruessen vom Monopol\n");
				
				String serverName = "monopol_gdr";
				outText.addOutLine( "searchForZipInBefehlsMail: trying to inform sender. ");
				
					
		    	Server server = s.getServer(serverName);
		    	if (server==null){
		    		outText.addOutLine("server " +  serverName + " nicht definiert.");
		    		System.exit(1);
		    	}
		    	
		    	outText.addOutLine( "getting MailAuth. ");
		    	MailAuthenticator auth = new MailAuthenticator(server.getUser(), server.getPass());
		    	
		    	Properties props = new Properties();
		    	props.put("mail.smtp.host", server.getHost());
		    	props.put("mail.smtp.auth", "true");
		    	
		    	
		    	// outText.addOutLine( "getting DefaultInstance.\n");
		    	// Session session = Session.getDefaultInstance(props,auth);
		    	
		    	outText.addOutLine( "getting Instance. ");
		    	Session session = Session.getInstance(props,auth);
		    	
		    	Message msg = new MimeMessage( session );

		        InternetAddress addressFrom = new InternetAddress("monopol@gdr-group.com");
		        msg.setFrom( addressFrom );
		        InternetAddress addressTo = new InternetAddress(s.getDirectory("MonopolBefehlseingangInfo"));
		        // msg.addRecipient( Message.RecipientType.TO, addressTo );
		        
		        addressTo = new InternetAddress("sf@fietefietz.de");
		        msg.addRecipient( Message.RecipientType.TO, addressTo );
		        
		        msg.addRecipients( Message.RecipientType.TO, m.getFrom());
		        	
		        msg.setSubject("Befehle im Posteingang!");
		        
		        
		        
	        	// keinen Anhang - plain text only
	        	msg.setContent(logText.toString(), "text/plain" );
	        	outText.addOutLine( "content:\n" + logText + "\n end Content\n");
		        
	        	outText.addOutLine( "sending. ");
	        	
		        Transport.send( msg );
		        outText.addOutLine("Befehle im Posteingang processed. ");

			}
			
			
		} catch (Throwable exc) { // any fatal error
			System.out.println(exc.toString());
			outText.addOutLine(exc.toString());
		}
	}
	
	/**
	 * Durchsucht das Multipart
	 * wenn zip->alles klar
	 * wenn wieder Multipart-bestandteil->rekursiver Aufruf
	 * @param mp
	 * @param logText
	 */
	private static boolean searchForZipInMultiPart(Multipart mp, StringBuffer logText, User u, Integer suchtiefe){
		logText.append("Bearbeite einen Multipart-bestandteil (Suchtiefe " + suchtiefe + "):\n");
		outText.addOutLine( "Bearbeite einen Multipart-bestandteil (Suchtiefe " + suchtiefe + "):");
		boolean retVal = false;
		try {
			for ( int j = 0; j < mp.getCount(); j++ ){
				BodyPart part = mp.getBodyPart( j );
				MimeBodyPart mimePart = (MimeBodyPart)part;
				logText.append("TeilBestandteil: " + j + "\n");
				outText.addOutLine( "TeilBestandteil: " + j);

				if (mimePart.getFileName()==null || !mimePart.getFileName().endsWith(".zip")){
					logText.append("kein Zip-File\n");
					outText.addOutLine( "kein Zip-File");
				} else {
					// zipFile gefunden
					// processMonopolBefehl(mimePart, u);
					logText.append("Datei gefunden: " + mimePart.getFileName() + ", uebergebe an Zipper. \n");
					outText.addOutLine( "Datei gefunden: " + mimePart.getFileName() + ", uebergebe an Entzipper.");
					retVal=true;
					processMonopolBefehl(mimePart,u);
				}
				
				if (mimePart.getContent() instanceof Multipart){
					logText.append("neues Multipart objekt\n");
					outText.addOutLine( "neues Multipart objekt");
					Multipart mp2 = (Multipart)mimePart.getContent();
					retVal=searchForZipInMultiPart(mp2,logText,u,suchtiefe+1);
				} else {
					logText.append("kein neues Multipart objekt\n");
					outText.addOutLine( "kein neues Multipart objekt");
				}
				
			}
			logText.append("Fertig mit einen Multipart-bestandteil (Suchtiefe " + suchtiefe + ").\n");
		} catch (Throwable exc) { // any fatal error
			System.out.println(exc.toString());
			outText.addOutLine(exc.toString());
		}
		return retVal;
	}
}

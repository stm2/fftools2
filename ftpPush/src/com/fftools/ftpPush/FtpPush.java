package com.fftools.ftpPush;

import java.io.File;
import java.io.FileInputStream;

import com.fftools.OutTextClass;
import com.fftools.tool1.Server;
import com.fftools.tool1.Settings;
import com.fftools.utils.FileCopy;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FtpPush {
	
	private static final OutTextClass outText = OutTextClass.getInstance();

	private static Settings s = null;
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Parameter
		// 1 server
		// 2 source
		// 3 target
		
		// sets the output file
    	outText.setFile("FFTools_log_ftpPush_" + FileCopy.getDateSDay());
    	outText.addOutLine(FileCopy.getDateS() + " start ftpPush");
    	
    	if (args.length<3){
    		// usage
    		System.out.println("ftpPush wrong Syntax. use:");
    		System.out.println("ftpPush servername source target");
    		System.exit(1);
    	}
    	String serverName = args[0];
    	String source = args[1];
    	String target = args[2];
    	
    	// Source check
    	File sourceFile = new File(source);
    	if (!sourceFile.exists() || !sourceFile.isFile()){
    		outText.addOutLine("Source-file non exist: " + sourceFile + " (exiting)");
    		System.exit(1);
    		sourceFile=null;
    	} else {
    		outText.addOutLine("checking source file: OK");
    	}
		
    	s = new Settings("toolsettings.config");
    	s.sayStatus();
		
    	Server server = s.getServer(serverName);
    	if (server==null){
    		outText.addOutLine("server " +  serverName + " nicht definiert.");
    		System.exit(1);
    	}
    	
    	FTPClient ftp = new FTPClient();
    	
    	try
    	{
    		outText.addOutLine( "Connecting to ftp server" );
    	    ftp.connect(server.getHost());
    	    if(!ftp.login(server.getUser(), server.getPass()))
    	    {
    	    	outText.addOutLine( "Login failed" );
    	        ftp.logout();
    	        return;
    	    }
    	    int reply = ftp.getReplyCode();
    	    outText.addOutLine( "Connect returned: " + reply );
    	    if (!FTPReply.isPositiveCompletion(reply)) {
    	        ftp.disconnect();
    	        outText.addOutLine( "Connection failed" );
    	        return;
    	    }
    	    ftp.enterLocalPassiveMode(); 
    	    FileInputStream in = new FileInputStream(source);
    	    ftp.setFileType(FTP.BINARY_FILE_TYPE);
    	    outText.addOutLine( "Uploading File" );
    	    boolean store = ftp.storeFile(target,in);
    	    if (store){
    	    	outText.addOutLine( "Uploading succeeded" );
    	    } else {
    	    	outText.addOutLine( "Uploading failed" );
    	    }
    	    in.close();
    	    ftp.logout();
    	    ftp.disconnect();
    	} catch(Exception ex) {
    		outText.addOutLine("Error Msg from FTP Client");
    		outText.addOutLine(ex.toString());
    		outText.addOutLine("\n");
    	    // ex.printStackTrace();
    	}
    	outText.addOutLine("exiting ftpPush\n");
	}

}

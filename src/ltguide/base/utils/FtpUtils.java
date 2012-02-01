package ltguide.base.utils;


import java.net.*;
import java.io.*;

// code from A simple Java FTP connection + file download and upload | AjaxApp.com http://bit.ly/zMhfJt

public class FtpUtils
{

  /** The URL connection object */
  private URLConnection m_client;

  /** The FTP host/server to be connected */
  private String host;

  /** The FTP user */
  private String user;

  /** The FTP user’s password */
  private String password;

  /** The remote file that needs to be uploaded or downloaded */
  private String remoteFile;

  /** The previous error message triggered after a method is called */
  private String erMesg;

  /** The previous success message after any method is called */
  private String succMesg;

  public FtpUtils(){}

  /** Setter method for the FTP host/server */
  public void setHost (String host)
  {
    this.host = host;
  }

  /** Setter method for the FTP user */
  public void setUser (String user)
  {
    this.user = user;
  }

  /** Setter method for the FTP user’s password */
  public void setPassword (String p)
  {
    this.password = p;
  }

  /** Setter method for the remote file, this must include the sub-directory path relative
   to the user’s home directory, e.g you’e going to download a file that is within a sub directory
   called “sdir”, and the file is named “d.txt”, so you shall include the path as “sdir/d.txt”
  */
  public void setRemoteFile (String d)
  {
    this.remoteFile = d;
  }

  /** The method that returns the last message of success of any method call */
  public synchronized String getLastSuccessMessage()
  {
    if (succMesg==null ) return ""; return succMesg;
  }

  /** The method that returns the last message of error resulted from any exception of any method call */
  public synchronized String getLastErrorMessage()
  {
    if (erMesg==null ) return ""; return erMesg;
  }

  /** The method that handles file uploading, this method takes the absolute file path
   of a local file to be uploaded to the remote FTP server, and the remote file will then
   be transfered to the FTP server and saved as the relative path name specified in method setRemoteFile
   @param localfilename – the local absolute file name of the file in local hard drive that needs to
  	FTP over
  */
  public synchronized boolean uploadFile (String localfilename)
  {
    try{

      InputStream is = new FileInputStream(localfilename);
      BufferedInputStream bis = new BufferedInputStream(is);
      OutputStream os =m_client.getOutputStream();
      BufferedOutputStream bos = new BufferedOutputStream(os);
      byte[] buffer = new byte[1024];
      int readCount;

      while( (readCount = bis.read(buffer)) > 0)
      {
            bos.write(buffer, 0, readCount);
      }
      bos.close();

      this.succMesg = "Uploaded!";

      return true;
    }
    catch(Exception ex)
    {
      StringWriter sw0= new StringWriter ();
      PrintWriter p0= new PrintWriter ( sw0, true );
      ex.printStackTrace ( p0 );
      erMesg = sw0.getBuffer().toString ();

      return false;
    }
  }

  /** The method to download a file and save it onto the local drive of the client in the specified absolut path
   @param localfilename – the local absolute file name that the file needs to be saved as */
  public synchronized boolean downloadFile (String localfilename)
  {
    try{
      InputStream is = m_client.getInputStream();
      BufferedInputStream bis = new BufferedInputStream(is);

      OutputStream os = new FileOutputStream(localfilename);
      BufferedOutputStream bos = new BufferedOutputStream(os);

      byte[] buffer = new byte[1024];
      int readCount;

      while( (readCount = bis.read(buffer)) > 0)
      {
        bos.write(buffer, 0, readCount);
      }
      bos.close();
      is.close (); // close the FTP inputstream
      this.succMesg = "Downloaded!";

      return true;
    }catch(Exception ex)
    {
      StringWriter sw0= new StringWriter ();
      PrintWriter p0= new PrintWriter ( sw0, true );
      ex.printStackTrace ( p0 );
      erMesg = sw0.getBuffer().toString ();

      return false;
    }
  }

  /** The method that connects to the remote FTP server */
  public synchronized boolean connect()
  {
    try{

    URL url = new URL("ftp://"+user+":"+password+"@"+host+"/"+remoteFile+";type=i");
    m_client = url.openConnection();

    return true;

    }
    catch(Exception ex)
    {
      StringWriter sw0= new StringWriter ();
      PrintWriter p0= new PrintWriter ( sw0, true );
      ex.printStackTrace ( p0 );
      erMesg = sw0.getBuffer().toString ();
      return false;
    }
  }


}

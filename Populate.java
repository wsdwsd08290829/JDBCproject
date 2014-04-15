import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.sql.*;
public class Populate{
  private String dbms, serverName,portNumber, userName, password, dbName;
  public Connection con;
  public Statement stmt;
  public Populate() throws IOException,SQLException{
      connectionInit();
      con = this.getConnection();
      stmt = con.createStatement();
  }
  public void say(String s){
    System.out.println(s);
  }
  //read params for connection
  public void connectionInit() throws IOException{ 
      BufferedReader ir = new BufferedReader(new FileReader("dbparams.txt"));
      this.serverName = ir.readLine();
      this.portNumber = ir.readLine();
      this.dbName = ir.readLine();
      this.userName = ir.readLine();
      this.password = ir.readLine();
      this.dbms = "mysql";
  }
  public Connection getConnection() throws SQLException {
      Connection conn = null;
      Properties connectionProps = new Properties();
      connectionProps.put("user", this.userName);
      connectionProps.put("password", this.password);

      if (this.dbms.equals("mysql")) {
          conn = DriverManager.getConnection(
                     "jdbc:" + this.dbms + "://" +
                     this.serverName +
                     ":" + this.portNumber + "/"+this.dbName,
                     connectionProps);
      } else if (this.dbms.equals("derby")) {   //change to oracle later
          conn = DriverManager.getConnection(
                     "jdbc:" + this.dbms + ":" +
                     this.dbName +
                     ";create=true",
                     connectionProps);
      }
      System.out.println("Connected to database");
      return conn;
  }
  public void readFilesFromZip() throws ZipException, IOException,SQLException{
    File f = new File("cs585_20133_assignment_3_data_v2.zip");
    ZipFile zf = new ZipFile(f);
    Enumeration<?> zfEntries =  zf.entries();
    while(zfEntries.hasMoreElements()){
      ZipEntry zfEntry = (ZipEntry)zfEntries.nextElement();
        String name = zfEntry.getName();
        if (name.indexOf(".") > 0){
            name = name.substring(0, name.lastIndexOf("."));
        }
        say(name);
        //long size = zfEntry.getSize();
         createTable(name); //table name cannot have . 
        //say(org.apache.commons.io.FilenameUtils.removeExtension(name));
        switch(name){
          case "earnings.csv":say("earnings");break;
          default: say("others");
        }
        InputStream is = zf.getInputStream(zfEntry);
        //Scanner csvScanner = new Scanner(is);
        BufferedReader csvScanner = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        //File file = new File(name); //not correct, trying to find file outside zip
        //BufferedReader csvf = new BufferedReader(new FileReader(file)); //need to eat Reader, not suit for this situation
        String line = null;
        /*while((line=csvScanner.readLine()) != null){
          say(line);
        } */
        say("================================================================================================================================================================================================================");
    }

  }
  public void createTable(String tableName)throws SQLException{
      stmt.executeUpdate ("CREATE TABLE IF NOT EXISTS " + tableName
                + " (I INTEGER, WORD VARCHAR(20), SQUARE INTEGER, "
                + " SQUAREROOT DOUBLE)");
  }
  public static void main(String[] args) throws SQLException{ // throws SQLException for stmt.close()
    Statement stmt = null;
    try{
      Populate pop = new Populate(); 
        
      stmt = pop.con.createStatement();
      pop.readFilesFromZip();
    }catch(SQLException e){
      System.out.println("error connection");
      System.out.println(e.getSQLState());
      System.out.println(e.getErrorCode());
    }catch(IOException e){
      System.out.println("Failed to read file, check your dbparam.txt format");
      e.printStackTrace();
    }catch(Exception e){
      System.out.println(e.getMessage());
    }finally{
      if(stmt != null){
        stmt.close();  
      }
    }
    
  }
}

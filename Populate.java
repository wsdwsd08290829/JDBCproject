import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.sql.*;
import java.text.*;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
public class Populate{
    final String DELIMITER = ",";
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
        BufferedReader ir = new BufferedReader(new FileReader("dbparams.secret"));
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
        while(zfEntries.hasMoreElements()){ // loop through each file
            ZipEntry zfEntry = (ZipEntry)zfEntries.nextElement();
            String name = zfEntry.getName();
            /*if (name.indexOf(".") > 0){ //another method
              name = name.substring(0, name.lastIndexOf("."));
              }*/
            name = FilenameUtils.removeExtension(name);  //use apache io library
            name = name.substring(0,1).toUpperCase() + name.substring(1);
            //long size = zfEntry.getSize();
            createTable(name); //table name cannot have . 
            InputStream is = zf.getInputStream(zfEntry);
            //Scanner csvScanner = new Scanner(is);
            BufferedReader csv = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            //File file = new File(name); //not correct, trying to find file outside zip
            //BufferedReader csvf = new BufferedReader(new FileReader(file)); //need to eat Reader, not suit for this situation
            saveFileDataToTable(csv, name);
        }//readFileFromZip()
    }
    public void saveFileDataToTable(BufferedReader csv, String tableName) throws SQLException,IOException{

        PreparedStatement pstmt = null;
        try{

            switch(tableName){
                case "Earnings":
                    System.out.println("filling " + tableName);
                    saveEarnings(csv,tableName, pstmt);break;
                case "Players": 
                    System.out.println("filling " + tableName);
                    savePlayers(csv, tableName, pstmt); break; 
                case "Tournaments": 
                    System.out.println("filling " + tableName);
                    saveTournaments(csv, tableName, pstmt);    break; 
                case "Teams": 
                    System.out.println("filling " + tableName);
                    saveTeams(csv, tableName, pstmt); break; 
                case "Matches_v2": 
                    System.out.println("filling " + tableName);
                    saveMatches(csv, tableName, pstmt);   break; 
                case "Members": 
                    System.out.println("filling " + tableName);
                    saveMembers(csv, tableName, pstmt);             break; 


                default:break;
            }  
        }catch(Exception e){
            e.printStackTrace();
            e.getMessage();
        }finally{
            if(pstmt != null){
                pstmt.close();
            }   }
    }      
    public void saveTournaments(BufferedReader csv, String tableName, PreparedStatement pstmt) throws SQLException,IOException{
        String line = null; 
        String sql = "insert into " + tableName + " values " +  "(?,?,?,?)";
        pstmt = con.prepareStatement(sql);     
        String[] columns; 
        while((line=csv.readLine()) != null){ //each line of that csv file 
            line = line.replaceAll("\"", "");
            columns = line.split(DELIMITER);
            pstmt.setInt(1, Integer.parseInt(columns[0]));
            pstmt.setString(2,  columns[1]);
            pstmt.setString(3, columns[2]);
            pstmt.setBoolean(4, Boolean.parseBoolean(columns[3]));
            pstmt.executeUpdate();
            //con.commit();
        } 

    }        
    public void saveEarnings(BufferedReader csv, String tableName, PreparedStatement pstmt) throws SQLException,IOException{
        String line = null; 
        String sql = "insert into " + tableName + " (tournment, player, prize_money,position) values " +  "(?,?,?,?)";
        pstmt = con.prepareStatement(sql);     
        String[] columns; 
        while((line=csv.readLine()) != null){ //each line of that csv file 
            line = line.replaceAll("\"", "");
            columns = line.split(DELIMITER);
            pstmt.setInt(1, Integer.parseInt(columns[0]));
            pstmt.setInt(2,  Integer.parseInt(columns[1]));
            pstmt.setInt(3, Integer.parseInt(columns[2]));
            pstmt.setInt(4, Integer.parseInt(columns[3]));
            pstmt.executeUpdate();
            //con.commit();
        } 
    }      
    public void savePlayers(BufferedReader csv, String tableName, PreparedStatement pstmt) throws SQLException,IOException,ParseException{
        String line = null; 
        String sql = "insert into " + tableName + " (player_id, tag, real_name, nationality, birthday, game_race) values (?,?,?,?,?,?)";  
        pstmt = con.prepareStatement(sql);    
        String[] columns; 
        
            java.sql.Date sqlDate = null;
        while((line=csv.readLine()) != null){ //each line of that csv file 
            //line = StringEscapeUtils.unescapeCsv(line);
            line = line.replaceAll("\"", "");
            columns = line.split(DELIMITER);
            sqlDate = stringToSQLDate(columns[4],"yyyy-MM-dd");

            pstmt.setInt(1, Integer.parseInt(columns[0]));
            pstmt.setString(2, columns[1]);
            pstmt.setString(3, columns[2]);
            pstmt.setString(4, columns[3]);
            pstmt.setDate(5, sqlDate);
            pstmt.setString(6, columns[5]);
            pstmt.executeUpdate();
            //    con.commit();
        }
    }  
    public void saveMembers(BufferedReader csv, String tableName, PreparedStatement pstmt) throws SQLException,IOException,ParseException{
        String line = null; 
        String sql = "insert into " + tableName + " values (?,?,?,?)";  
        pstmt = con.prepareStatement(sql);    
        java.sql.Date sqlDate= null;

        String[] columns = new String[4]; 
        while((line=csv.readLine()) != null){ //each line of that csv file 
            //line = StringEscapeUtils.unescapeCsv(line);
            line = line.replaceAll("\"", "");
            String[] temp = line.split(DELIMITER);// the array size is truncked instead of declared 
            for(int i =0; i< temp.length; i++){
                columns[i] = temp[i];
            }
            pstmt.setInt(1, Integer.parseInt(columns[0]));
            pstmt.setInt(2,Integer.parseInt(columns[1]));
            sqlDate = stringToSQLDate(columns[2],"yyyy-MM-dd");                                                                   
            pstmt.setDate(3, sqlDate);
            sqlDate = stringToSQLDate(columns[3],"yyyy-MM-dd");                                                                   
            pstmt.setDate(4, sqlDate);
            pstmt.executeUpdate();
            //    con.commit();
        }

    }  
    public void saveMatches(BufferedReader csv, String tableName, PreparedStatement pstmt) throws SQLException,IOException,ParseException{
        String line = null; 
        String sql = "insert into " + tableName + " values (?,?,?,?,?,?,?,?)";  
        pstmt = con.prepareStatement(sql);    
        java.sql.Date sqlDate= null;

        String[] columns = new String[8]; 
        while((line=csv.readLine()) != null){ //each line of that csv file 
            //line = StringEscapeUtils.unescapeCsv(line);
            line = line.replaceAll("\"", "");
            String[] temp = line.split(DELIMITER);// the array size is truncked instead of declared 
            for(int i =0; i< temp.length; i++){
                columns[i] = temp[i];
            }
            pstmt.setInt(1, Integer.parseInt(columns[0]));
            sqlDate = stringToSQLDate(columns[1],"yyyy-MM-dd");                                                                   
            pstmt.setDate(2,sqlDate);
            pstmt.setInt(3,Integer.parseInt(columns[2]));
            pstmt.setInt(4,Integer.parseInt(columns[3]));
            pstmt.setInt(5,Integer.parseInt(columns[4]));
            pstmt.setInt(6,Integer.parseInt(columns[5]));
            pstmt.setInt(7,Integer.parseInt(columns[6]));
            pstmt.setBoolean(8, Boolean.parseBoolean(columns[7]));     
            pstmt.executeUpdate();
            //    con.commit();
        }

    }  
    public void saveTeams(BufferedReader csv, String tableName, PreparedStatement pstmt) throws SQLException,IOException,ParseException{
        String line = null; 
        String sql = "insert into " + tableName + " values (?,?,?,?)";  
        pstmt = con.prepareStatement(sql);    
        java.sql.Date sqlDate= null;

        String[] columns = new String[4]; 
        while((line=csv.readLine()) != null){ //each line of that csv file 
            //line = StringEscapeUtils.unescapeCsv(line);
            line = line.replaceAll("\"", "");
            String[] temp = line.split(DELIMITER);// the array size is truncked instead of declared 
            for(int i =0; i< temp.length; i++){
                columns[i] = temp[i];
            }
            pstmt.setInt(1, Integer.parseInt(columns[0]));
            pstmt.setString(2,columns[1]);
            sqlDate = stringToSQLDate(columns[2],"yyyy-MM-dd");                                                                   
            pstmt.setDate(3, sqlDate);
            sqlDate = stringToSQLDate(columns[3],"yyyy-MM-dd");                                                                   
            pstmt.setDate(4, sqlDate);
            pstmt.executeUpdate();
            //    con.commit();
        }

    }  

    public java.sql.Date stringToSQLDate(String dateStr, String format) throws ParseException{
        if(dateStr == null){return null;}else{
            SimpleDateFormat sdf1 = new SimpleDateFormat(format);
            java.util.Date date = sdf1.parse(dateStr);
            java.sql.Date sqlDate = new java.sql.Date(date.getTime()); 
            return sqlDate; 
        }
    }
    public void createTable(String tableName)throws SQLException{
        //dropAllTable();
        dropTable(tableName);

        switch(tableName){
            case "Earnings":
                stmt.executeUpdate (
                        "CREATE TABLE IF NOT EXISTS " + tableName
                        + " (tournment INTEGER not NULL, player INTEGER, prize_money INTEGER, "
                        + " position integer,"
                        + " primary key(tournment,player))");break; 
            case "Tournaments":
                stmt.executeUpdate (
                        "CREATE TABLE IF NOT EXISTS " + tableName
                        + " (tournment_id INTEGER not NULL, name varchar(100), "
                        + " region varchar(2), major boolean, "
                        + " primary key(tournment_id))");break; 

            case "Players":
                stmt.executeUpdate (
                        "CREATE TABLE IF NOT EXISTS " + tableName
                        + " (player_id INTEGER not NULL,tag varchar(20), real_name varchar(40), "
                        + " nationality varchar(2), birthday Date not null, game_race varchar(1), "
                        + " primary key(player_id))");break; 
            case "Members":
                stmt.executeUpdate (
                        "CREATE TABLE IF NOT EXISTS " + tableName
                        + " (player INTEGER not NULL,team varchar(20), start_date date, end_date date,"
                        + " primary key(player, start_date))");break;  
            case "Teams":
                stmt.executeUpdate (
                        "CREATE TABLE IF NOT EXISTS " + tableName
                        + " (team_id INTEGER not NULL,team varchar(40), founded date, disbanded date,"
                        + " primary key(team_id))");break;  
            case "Matches_v2":
                stmt.executeUpdate (
                        "CREATE TABLE IF NOT EXISTS " + tableName
                        + " (match_id INTEGER not NULL,date DATE, tournament integer, "
                        + " playerA integer, playerB integer, scoreA integer, scoreB integer, "
                        + " offline Boolean, "
                        + " primary key(match_id))");break;  
            default:;
        }

    }
    public void dropAllTable() throws SQLException{
        DatabaseMetaData md = con.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", null);
        while(rs.next()){
            String tableName = rs.getString(3);
            stmt.executeUpdate("Drop table if exists "+ tableName );
        }
    }
    public void dropTable(String tableName) throws SQLException{
        stmt.executeUpdate("Drop table if exists "+ tableName );
    }                                              
    public static void main(String[] args) throws SQLException{ // throws SQLException for stmt.close()
        Statement stmt = null;
        try{
            Populate pop = new Populate(); 

            stmt = pop.con.createStatement();
            pop.readFilesFromZip();
        }catch(SQLException e){
            System.out.println("sql error");
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

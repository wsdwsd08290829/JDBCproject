import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.sql.*;
import java.text.*;
public class Query{
    final String DELIMITER = ",";
    private String dbms, serverName,portNumber, userName, password, dbName;
    public Connection con;
    public Statement stmt;
   public Query(String paramsfile){
			try{
        connectionInit(paramsfile);
        con = this.getConnection();
        stmt = con.createStatement();
			}catch(SQLException e){
            System.out.println("sql error");
            System.out.println(e.getSQLState());
            System.out.println(e.getErrorCode());
						System.out.println(e.getMessage());
        }catch(IOException e){
            System.out.println("Failed to read file, check your dbparam.txt format");
            e.printStackTrace();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
    public void say(String s){
        System.out.println(s);
    }
    //read params from file for connection
    public void connectionInit(String paramsfile) throws IOException{ 
        BufferedReader ir = new BufferedReader(new FileReader(paramsfile));
        this.serverName = ir.readLine();
        this.portNumber = ir.readLine();
        this.dbName = ir.readLine();
        this.userName = ir.readLine();
        this.password = ir.readLine();
        this.dbms = "mysql";
    }
		//create connection, set con and stmt
    public Connection getConnection() throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", this.userName);
        connectionProps.put("password", this.password);
				System.out.println("jdbc:" + this.dbms + "://" +
                    this.serverName +
                    ":" + this.portNumber + "/"+this.dbName + " "+ password + " "+ userName);
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
		//not used here
		public java.sql.Date stringToSQLDate(String dateStr, String format) throws ParseException{
        if(dateStr == null){return null;}else{
            SimpleDateFormat sdf1 = new SimpleDateFormat(format);
            java.util.Date date = sdf1.parse(dateStr);
            java.sql.Date sqlDate = new java.sql.Date(date.getTime()); 
            return sqlDate; 
        }
    }
		/*
		*	if field not exist exit; other wise do nothing
		*/
		public void validate_input(int playerid, int teamid) throws SQLException{
				//validate player and team exist;
				String exist = "select player from Members where player="+playerid;
				ResultSet validate_rs = stmt.executeQuery(exist);
				if(!validate_rs.isBeforeFirst()){
					System.out.println("player not exist yet");
					System.exit(1);
				}
				exist = "select team from Members where team="+teamid;
				validate_rs = stmt.executeQuery(exist);
				if(!validate_rs.isBeforeFirst()){
					System.out.println("team not exist yet");
					System.exit(1);
				}
				return ;
		}
		//$java -classpath .;mysql-connector-java-5.1.29-bin.jar Hw3 dbparams.txt q2 1660 35
		//re membership of player 1660 to new team 35. deal with earlier membership of team;
		public void solveQ2(String[] args){
			int playerid=Integer.parseInt(args[2]);
			int teamid=Integer.parseInt(args[3]);
			System.out.println(playerid+" "+teamid);
			//get date of today
			String addToCurrentTeam = "select * from Members where Members.player=? and Members.team=?";
			String findTeamOfPlayer = "select * from Members where Members.player = ?";
			try{
				validate_input(playerid, teamid);

				//change membership logic start
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				java.util.Date todaydate = new java.util.Date();
				String today = dateFormat.format(todaydate);
				
				PreparedStatement pstmt = con.prepareStatement(addToCurrentTeam);
				pstmt.setInt(1,playerid);
				pstmt.setInt(2,teamid);
				ResultSet rsAddToCurrentTeam = pstmt.executeQuery();
				//if new team is current team and player not left yet, just return
				if(rsAddToCurrentTeam.isBeforeFirst()){  
					while(rsAddToCurrentTeam.next()){  
						int team = rsAddToCurrentTeam.getInt("team");
						String end = rsAddToCurrentTeam.getString("end_date");
						if(end == null){  //not in this team any more
							System.out.println("player is still in this team");
							return;
						}
						System.out.println("team exist " + team);
					}
				}else{ 
					pstmt = con.prepareStatement(findTeamOfPlayer);
					pstmt.setInt(1,playerid);
					ResultSet rs = pstmt.executeQuery();
					System.out.println();
					//if in another team
					if(rs.isBeforeFirst()){  
						while(rs.next()){  //if is member of team
							String start = rs.getString("start_date");
							String end = rs.getString("end_date");
							int oldteam = rs.getInt("team");
							//database return null; should not be print(will return from function)
							System.out.println("the player is or was in team " + oldteam + " end date "+end);
							//if space+space end -> error ???
						 //get current date time with Date()
							if(end == null){ //not leave yet, we make it leave today
								if(start.equals(today)){
									System.out.println("play cannot join two teams in same day, try tomorrow");
									return ;
								}
								System.out.println("player is current member of "+ oldteam);
								String sql = "update Members set end_date= '"+today+"' where Members.player="+ playerid + " and Members.team=" + oldteam;  //!single quote
								System.out.println("player left from team : "+oldteam+" by executing "+ sql);
								stmt.executeUpdate(sql);
							}
							 //get current date time with Calendar()  eg
							/* Calendar cal = Calendar.getInstance();
							 System.out.println(dateFormat.format(cal.getTime())); 
								if(todaydate.before(startdate)){
											System.out.println("yes");
								} */
						}
					}
				}
				//insert: 
				//case1 same team: but left before, different team
				String sql = "insert into Members values ("+ playerid + ", " + teamid +", '"+ today+ "', NULL)";  //!single quote
				System.out.println("player add to new team: " + sql);
				stmt.executeUpdate(sql);
			}catch(Exception e){
				System.out.println(e.getMessage());
			}finally{
				if(stmt != null){
					try{
						stmt.close();  
					}catch(SQLException e){ //ZipException
						System.out.println(e.getMessage());
					}
				}				
			}
		}
		//$java -classpath .;mysql-connector-java-5.1.26-bin.jar Hw3 dbparams.txt q1 1990 10
		//get player info with birthday of 1990 Year, 10 month
		public void solveQ1(String[] args){
			int year = Integer.parseInt(args[2]);
			int month = Integer.parseInt(args[3]);
			String sql = "select real_name, tag, nationality from Players where  year(birthday)= ? and month(birthday)=?";
			try{
				PreparedStatement pstmt = con.prepareStatement(sql);
				pstmt.setInt(1,year);
				pstmt.setInt(2,month);
				ResultSet rs = pstmt.executeQuery();
				int[] columnsize = {30, 10, 10};
				
				displayHeader(columnsize);
				//could save to array of rows use DisplayTable class
				while(rs.next()){
					String realname = rs.getString("real_name");
					String tag = rs.getString("tag");
					String nationality = rs.getString("nationality");
					//displayRow();
					String[] columnvalue = {realname, tag, nationality};
					displayResultRow(columnvalue, columnsize);
				}	
				displayFooter(columnsize);
			}catch(Exception e){
				System.out.println(e.getMessage());
			}finally{
				if(stmt != null){
					try{
						stmt.close();  
					}catch(SQLException e){ //ZipException
						System.out.println(e.getMessage());
					}
				}				
			}
		}
		////////////////////////Unix Table logic////////////////////////////
		
		//could have write a class display table; 
		//eat arrays of rows, each element is array of column; get columnsize from first row
		public void displayResultRow(String[] columnvalue, int[] columnsize){
		//|  name  |   value   | .....
				for(int i=0; i< columnsize.length;i++){
					int leftspace = (columnsize[i]-columnvalue[i].length())/2;
					int rightspace = columnsize[i] - columnvalue[i].length()-leftspace;
					bar(1);    //|   name   
					space(leftspace);
					System.out.print(columnvalue[i]);
					space(rightspace);
				}
				bar(1);//end;
				System.out.println();
		}
		public void displayHeader(int[] columnsize){
			for(int i : columnsize){
				dash(i);
			}
			dash(1);//head
			dash(columnsize.length-1);//middle column bar
			dash(1);//end // compensate for each |
			System.out.println();
		}
		public void displayFooter(int[] columnsize){
			for(int i : columnsize){
				dash(i);
			}
			dash(1);//head
			dash(columnsize.length-1);//middle column bar
			dash(1);//end // compensate for each |
			System.out.println();
		}
		public void space(int n){
			for(int i = 0; i< n;i++){
					System.out.print(" ");
			}
		}
		public void bar(int n){
			for(int i = 0; i< n;i++){
					System.out.print("|");
			}
		}
		public void dash(int n){
			for(int i = 0; i< n;i++){
					System.out.print("-");
			}
		}
		
		////////////////////////////////////////////////////
		public static void usage(){
			System.out.println("$java -classpath .;mysql-connector-java-5.1.26-bin.jar Hw3 dbparams.txt q1 1990 10 ");
			System.out.println("or");
			System.out.println("$java -classpath .;mysql-connector-java-5.1.29-bin.jar Hw3 dbparams.txt q2 1660 35");
			System.exit(1);
		}
   public static void main(String[] args) throws SQLException{ // throws SQLException for stmt.close()
				//simple parse command line
				if(args.length != 4) usage();		
				String params = args[0];
				Query dbq = new Query(args[0]); 
				if(args[1].equals("q1")){
					dbq.solveQ1(args);
				}else if(args[1].equals("q2")){
					dbq.solveQ2(args);
				}else{
					usage();
				}
        try{
           
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }		
}
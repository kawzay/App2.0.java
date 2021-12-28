package query;

import connection.DbConnection;
import dao.SchoolCSV;
import entity.School;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class DbWorker {

    public static String createCountyQuery =
            "CREATE TABLE IF NOT EXISTS County (\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    county TEXT NOT NULL\n" +
                    ");";

    public static String createDistrictQuery =
            "CREATE TABLE IF NOT EXISTS District (\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    district INTEGER NOT NULL,\n" +
                    "    id_county INTEGER,\n" +
                    "    calworks REAL NOT NULL,\n" +
                    "    expenditure REAL NOT NULL,\n" +
                    "    income REAL NOT NULL,\n" +
                    "    FOREIGN KEY (id_county)\n" +
                    "    REFERENCES County (id)\n" +
                    "        ON DELETE SET NULL\n" +
                    ");";

    public static String createGradesQuery =
            "CREATE TABLE IF NOT EXISTS Grades (\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    grades TEXT NOT NULL\n" +
                    ");";

    public static String createSchoolQuery =
            "CREATE TABLE IF NOT EXISTS School (\n" +
                    "    id INTEGER PRIMARY KEY,\n" +
                    "    school TEXT NOT NULL,\n" +
                    "    id_district INTEGER,\n" +
                    "    id_grades INTEGER,\n" +
                    "    students INTEGER NOT NULL,\n" +
                    "    teachers REAL NOT NULL,\n" +
                    "    lunch REAL NOT NULL,\n" +
                    "    computer INTEGER NOT NULL,\n" +
                    "    english REAL NOT NULL,\n" +
                    "    read REAL NOT NULL,\n" +
                    "    math REAL NOT NULL,\n" +
                    "    FOREIGN KEY (id_district)\n" +
                    "    REFERENCES District (id)\n" +
                    "        ON DELETE SET NULL,\n" +
                    "    FOREIGN KEY (id_grades)\n" +
                    "    REFERENCES Grades (id)\n" +
                    "        ON DELETE SET NULL\n" +
                    ");";

    public static String dropCountyQuery = "DROP TABLE IF EXISTS County";

    public static String dropDistrictQuery = "DROP TABLE IF EXISTS District";

    public static String dropGradesQuery = "DROP TABLE IF EXISTS Grades";

    public static String dropSchoolQuery = "DROP TABLE IF EXISTS School";

    public static String insertCountyQuery =
            "INSERT INTO County(county) \n" +
                    "SELECT '%s' \n" +
                    "WHERE NOT EXISTS(SELECT county FROM County WHERE county='%s');";

    public static String insertDistrictQuery =
            "INSERT INTO District(district, id_county, calworks, expenditure, income)\n" +
                    "VALUES('%d',(SELECT id FROM County WHERE county = '%s'),%f,%f,%f);";

    public static String insertGradesQuery =
            "INSERT INTO Grades(grades)\n" +
                    "SELECT '%s' \n" +
                    "WHERE NOT EXISTS(SELECT grades FROM Grades where grades='%s');";

    public static String insertSchoolQuery =
            "INSERT INTO School(school, id_district,id_grades,students,teachers,lunch,computer,english,read,math)\n" +
                    "values('%s',\n" +
                    "(SELECT id FROM District WHERE district = '%d'),\n" +
                    "(SELECT id FROM Grades WHERE grades = '%s'),\n" +
                    "%d,%f,%f,%d,%f,%f,%f);";

    public static String taskFirstQuery =
            "SELECT county, AVG(students)\n" +
                    "FROM School\n" +
                    "INNER JOIN District ON School.id_district = District.id\n" +
                    "INNER JOIN County ON District.id_county = County.id\n" +
                    "GROUP BY county\n" +
                    "ORDER BY county\n" +
                    "LIMIT 10;";

    public static String taskSecondQuery =
            "SELECT county,AVG(expenditure)\n" +
                    "FROM District\n" +
                    "INNER JOIN County ON District.id_county = County.id\n" +
                    "WHERE county IN (%s) AND income > %d\n" +
                    "GROUP BY county;";

    public static String taskThirdQuery =
            "SELECT school\n" +
            "FROM School\n" +
            "WHERE students > %d\n" +
            "AND students < %d\n" +
            "ORDER BY math desc\n" +
            "LIMIT 1;";

    private Connection con;
    private Statement statement;

    public DbWorker(){
        try{
            this.con = DbConnection.getInstance().getConnection();
            this.statement = this.con.createStatement();
        } catch (SQLException e){
            System.out.println("Something went wrong");
        }
    }

    public void addWithCSV(){
        try {
            for (School s : SchoolCSV.schools) {

                this.statement.addBatch(String.format(insertCountyQuery, s.getCounty(),s.getCounty()));

                this.statement.addBatch(String.format(insertDistrictQuery,s.getDistrict(),s.getCounty(),s.getCalworks(),
                        s.getExpenditure(),s.getIncome()));

                this.statement.addBatch(String.format(insertGradesQuery, s.getGrades(),s.getGrades()));

                this.statement.addBatch(String.format(insertSchoolQuery,s.getName(),s.getDistrict(),s.getGrades(),
                        s.getStudents(),s.getTeachers(),s.getLunch(),s.getComputers(),s.getEnglish(),s.getRead(),
                        s.getMath()));

                this.statement.executeBatch();
            }
            System.out.println("All lines added!");
        } catch (SQLException e){
            System.out.println("something went wrong!!");
            e.printStackTrace();
        }

    }

    public void dropTables(){
        String[] dellQueries = new String[]{dropSchoolQuery, dropGradesQuery, dropDistrictQuery, dropCountyQuery};
        try{
            for (String dellQuery : dellQueries) {
                this.statement.executeUpdate(dellQuery);
            }
            System.out.println("Tables droped");
        } catch (SQLException e){
            System.out.println("something went wrong, drop");
        }
    }

    public void createTables(){
        String[] createQueries = new String[]{createCountyQuery, createDistrictQuery,createGradesQuery, createSchoolQuery};
        try{
            for (String createQuery : createQueries) {
                this.statement.executeUpdate(createQuery);
            }

            System.out.println("Tables created");
        } catch (SQLException e){
            System.out.println("something went wrong, drop");
        }
    }

    public TreeMap<String, Double> taskFirst(){

        String query = taskFirstQuery;

        TreeMap<String, Double> resultMap = new TreeMap<>();
        try {
            ResultSet result = this.statement.executeQuery(query);
            while(result.next()){
                resultMap.put(result.getString(1), result.getDouble(2));
                System.out.printf("%s: avg(students) = %f\n", result.getString(1), result.getDouble(2));
            }
        } catch (SQLException e){
            System.out.println("something went wrong, task two");
        }
        return resultMap;
    }

    public void taskSecond(List<String> county, int income){
        String countys = String.join("','",county);
        countys = "'" + countys + "'";
        String query = String.format(taskSecondQuery, countys, income);
        try {
            ResultSet result = this.statement.executeQuery(query);
            while(result.next()){
                System.out.printf("%s: avg(expenditure) = %f\n",
                        result.getString(1), result.getDouble(2));
            }
        } catch (SQLException e){
            e.printStackTrace();
            System.out.println("something went wrong, task two");
        }

    }

    public void thirdTask(int start, int end){
        String query = String.format(taskThirdQuery, start, end);
        try {
            ResultSet result = this.statement.executeQuery(query);
            while(result.next()){
                System.out.printf("%s: between %d and %d \n",
                        result.getString(1),start,end);
            }
        } catch (SQLException e){
            System.out.println("something went wrong, task two");
        }

    }


    public void stop(){
        try{
            this.statement.close();
            this.con.close();
            System.out.println("worker stoped");
        } catch (SQLException e){
            System.out.println("Something went wrong");
        }
    }

}

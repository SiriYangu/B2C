/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.datasource;

/**
 *
 * @author rkipkirui
 */
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import ke.co.count.sirib2c.log.Logging;

/**
 *
 * @author rkipkirui
 */
public final class DBConnection {

    Statement stmt;
    ResultSet rs;
    private Logging logger;
    Connection con = null;

    public DBConnection() {
        logger = new Logging();
        stmt = null;
        rs = null;
    }

    public Connection getConnection() {
        try {
            con = HikariCPDataSource.getConnection();
        } catch (SQLException ex) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        }
        return con;
    }

    public ResultSet query_all(final String query) {
        try {
            con = getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
        } catch (SQLException ex) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        }
        return rs;
    }

    public int rowCount(final String query) {
        int count = 0;

        rs = query_all(query);
        try {
            while (rs.next()) {
                ++count;
            }
        } catch (SQLException ex) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        }

        return count;
    }

    public int update_db(final String query) {
        int i = 0;
        try {
            con = getConnection();
            stmt = con.createStatement();
            i = stmt.executeUpdate(query);
        } catch (SQLException ex) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        } finally {
            try {
                stmt.close();
                con.close();
            } catch (SQLException ex) {
                logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
            }
        }

        return i;
    }

    public void closeConn() {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (con != null) {
                con.close();
            }

        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + e.getMessage() + "\n\n", "", 9);
        }
    }
}

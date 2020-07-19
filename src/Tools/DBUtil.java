package Tools;

import java.sql.*;
import java.util.ResourceBundle;

public class DBUtil {
    static {
        ResourceBundle rb = ResourceBundle.getBundle("info");
        String driver = rb.getString("driver");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = null;
        ResourceBundle rb = ResourceBundle.getBundle("info");
        String url = rb.getString("url");
        String user = rb.getString("user");
        String password = rb.getString("password");
        conn = DriverManager.getConnection(url, user, password);
        return conn;
    }

    public static void close(ResultSet rs, PreparedStatement ps, Connection conn) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        try {
            if (ps != null) {
                ps.close();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}

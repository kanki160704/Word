package Body;

import Tools.DBUtil;
import com.sun.xml.internal.ws.api.server.SDDocument;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class Word {
    public static void writeLog(String str) {
        /*
        * 对于增加单词、复习单词等行为记录日志
         */
        Date reviewDate = new Date();
        String str2 = reviewDate + ": " + str + "\n";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("Log.txt", true);
            fos.write(str2.getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printMap(Map<String, String> map, Scanner s) {
        for (String english : map.keySet()) {
            String chinese = map.get(english);
            System.out.println(english + " --------> " + chinese);
        }
        while (true) {
            System.out.print("生词学习结束请按y：");
            String temp = s.nextLine();
            if (temp.equals("y")) {
                break;
            }
        }

        for (int i = 0; i < 50; i ++) {
            System.out.println("---------------------------");
        }
    }

    private static void reviewNewWords(Map<String, String> map, Scanner s) {
        /*
        * 对于不会的单词，以hashmap形式存放，并且进行复习
         */
        if (map.isEmpty() == true) {
            return;
        }
        else {
            int n = map.size();
            System.out.println("共" + n + "个");
            Map<String, String> newWords = new HashMap<String, String>();
            printMap(map, s);
            for (String english : map.keySet()) {
                String chinese = map.get(english);
                System.out.print(english + "输入y查看中文：");
                while (true) {
                    String temp = s.nextLine();
                    if (temp.equals("y")) {
                        System.out.println(chinese);
                        break;
                    }
                }
                String result = "";
                while (true) {
                    System.out.print("是否认识？认识输入y，不认识输入n：");
                    result = s.nextLine();
                    if (result.equals("y") || result.equals("n")) {
                        if (result.equals("n")) {
                            newWords.put(english, chinese);
                        }
                        break;
                    }
                }
            }
            reviewNewWords(newWords, s);
        }
    }

    private static int getFamiliar(String english, String tableName, PreparedStatement ps, Connection conn) {
        /*
        *   从newword表格中获得familiar值
         */
        String sql = null;
        if (tableName.equals("newword")) {
            sql = "select familiar from newword where word = ?";
        }
        else {
            sql = "select familiar from urgent where word = ?";
        }
        int familiar = 0;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, english);
            ResultSet rs = ps.executeQuery();
            rs.next();
            familiar = rs.getInt("familiar");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return familiar;
    }

    private static void setFamiliar(int familiar, String tableName, PreparedStatement ps, Connection conn, String english) {
        /*
        设置单词english的熟悉度
         */
        String sql = null;
        if (tableName.equals("newword")) {
            sql = "update newword set familiar = ? where TIMESTAMPDiff(hour, lastReview, CURRENT_TIMESTAMP) > 2 AND word = ?";
        }
        else {
            sql = "update urgent set familiar = ? where TIMESTAMPDiff(hour, lastReview, CURRENT_TIMESTAMP) > 2 AND word = ?";
        }
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, Math.max(0, familiar));
            ps.setString(2, english);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void insertWord(PreparedStatement ps, Connection conn, String english, String chinese, String first, String tableName) {
        /*
        将单词插入一张表格中，需要给出单词的中文、英文、首字母
         */
        String sql = "";
        if (tableName.equals("oldword")) {
            sql = "insert into oldword (word, chinese, firstLetter) values (?, ?, ?)";
        }
        else {
            sql = "insert into " + tableName + " (word, chinese, firstLetter, lastReview) values (?, ?, ?, CURRENT_TIMESTAMP)";
        }
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, english);
            ps.setString(2, chinese);
            ps.setString(3, first);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static boolean isInTable(PreparedStatement ps, Connection conn, String english, String tableName) {
        /*
        判断一个单词在不在指定的表格中
         */
        String sql = "select count(*) from " + tableName + " where word = '" + english + "'";
        try {
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int n = rs.getInt("count(*)");
            rs.close();
            if (n != 0) {
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public static String getChinese(PreparedStatement ps, Connection conn, String english, String tableName) {
        String sql = "select * from " + tableName + " where word = '" + english + "'";
        try {
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            rs.next();
            String result = rs.getString("chinese");
            rs.close();
            return result;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static void updateChinese(PreparedStatement ps, Connection conn, String english, String chinese, String tableName) {
        String sql = "update " + tableName + " set chinese = '" + chinese + "' where word = '" + english + "'";
        try {
            ps = conn.prepareStatement(sql);
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void insert(PreparedStatement ps, Connection conn, Scanner s, String tableName) {
        /*
        获得输入的单词的中英文
         */
        int i = 0;
        while (true) {
            i ++;
            System.out.print("请输入想要添加的英文，输入e返回：");
            String english = s.nextLine();
            if (english.equals("e")) {
                writeLog("添加了" + i + "生词");
                return;
            }
            boolean isIn = isInTable(ps, conn, english, tableName);
            if (isIn == false) {
                String first = english.substring(0, 1);
                System.out.print("请输入想要添加的中文：");
                String chinese = s.nextLine();
                insertWord(ps, conn, english, chinese, first, tableName);
            }
            else {
                System.out.print("原先的中文是：");
                String chinese = getChinese(ps, conn, english, tableName);
                System.out.println(chinese);
                System.out.print("是否需要更改，是请输入想要修改的中文，否选择n：");
                String input = s.nextLine();
                if (input.equals("n")) {
                    i --;
                    continue;
                }
                else {
                    updateChinese(ps, conn, english, input, tableName);
                }
            }
        }
    }

    private static void deleteWord(String english, PreparedStatement ps, Connection conn, String tableName) {
        /*
        * 从表中删除英语为English的单词
         */
        String sql = "delete from " + tableName + " where word = ?";
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, english);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void delete(PreparedStatement ps, Connection conn, Scanner s, String tableName) {
        System.out.print("请输入想要删除的英语：");
        String english = s.nextLine();
        deleteWord(english, ps, conn, tableName);
    }

    private static void exchange(String english, String ori, String des, PreparedStatement ps, Connection conn) {
        /*
        * 将ori表中的一个单词删除，并且加入到des表中
         */
        String sql = "select * from " + ori + " where word = ?";
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, english);
            ResultSet rs = ps.executeQuery();
            rs.next();
            String engName = rs.getString("word");
            String chiName = rs.getString("chinese");
            String first = rs.getString("firstLetter");
            insertWord(ps, conn, engName, chiName, first, des);
            deleteWord(engName, ps, conn, ori);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void reviewYesterday(ResultSet rs, PreparedStatement ps, Connection conn, Scanner s) {
        /*
        复习前两天不会的生词，这些生词记录在yesterday表格中，调用此函数时先将记录日期与当前时间的时间差大于1天的删除，之后遍历表格复习。
         */
        System.out.println("复习前一天的生词");
        String sql = "delete from yesterday where TIMESTAMPDiff(hour, lastReview, CURRENT_TIMESTAMP) > 48";

        try {
            ps = conn.prepareStatement(sql);
            ps.execute();
            int row = getRowNumber(ps, conn, "yesterday");
            System.out.println(row);
            review(rs, ps, conn, s, "yesterday", row);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void review(ResultSet rs, PreparedStatement ps, Connection conn, Scanner s, String tableName, int n) {
        /*
        *   随机抽取n个单词复习，并将不会的存放在unknown words中，n个单词复习完再复习不会的单词，直到unknown words为空
         */
        System.out.println("开始复习");
        Map<String, String> unknownWords = new HashMap<>();
        String sql = "select * from " + tableName + " where TIMESTAMPDiff(hour, lastReview, CURRENT_TIMESTAMP) > 2 order by rand() limit ?";
        try {
            int count = 1;
            ps = conn.prepareStatement(sql);
            ps.setInt(1, n);
            rs = ps.executeQuery();
            while (rs.next()) {
                while (true) {
                    String english = rs.getString("word");
                    System.out.print(count + "/" + n + "个：" + english + "输入y查看中文：");
                    count ++;
                    String temp = s.nextLine();
                    if (temp.equals("y")) {
                        String chinese = rs.getString("chinese");
                        System.out.println(chinese);
                        String result = "";
                        while (true) {
                            System.out.print("是否认识？认识输入y，不认识输入n：");
                            result = s.nextLine();
                            if (result.equals("y") || result.equals("n")) {
                                if (result.equals("n")) {
                                    unknownWords.put(english, chinese);
                                    if (!tableName.equals("yesterday")) {
                                        /*
                                        如果不在yesterday表格中复习，遇到生词要加入到yesterday中
                                         */
                                        insertWord(ps, conn, english, chinese, english.substring(0, 1), "yesterday");
                                    }
                                }
                                break;
                            }
                        }
                        update(conn, ps, result, tableName, english);
                        break;
                    }
                    else {
                        System.out.println("无效输入");
                        count --;
                    }
                }
            }

            System.out.println("复习结束，以下是生词学习：");
            reviewNewWords(unknownWords, s);
            String log = new String(": 复习了" + n + "单词");
            writeLog(log);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void update(Connection conn, PreparedStatement ps, String isKnown, String tableName, String english) {
        /*
        * newword表和urgent表是一样的，对于yesterday表格，如果回答no，就将最后更新的时间更新
        * 如果单词在已学习的表格中，并且认识的话，不做任何修改。
        * 如果单词在已学习的表格中，并且不认识，那么将该单词放到生词表中，设置熟悉值为0.
        * 如果单词在生词表中，并且认识，那么熟悉值+1. 如果两次复习时间小于2小时，则不增加。如果熟悉值更新后到了20，就从生词表中删除，并且放入已学习。
        * 如果单词在生词表中，并且不认识，那么熟悉值 -5，如果小于等于0则取0.
        * */

        if (tableName.equals("oldword")) {
            if (isKnown.equals("y")) {
                return;
            }
            else {
                exchange(english, "oldword", "urgent", ps, conn);
            }
        }
        else if (tableName.equals("newword") || tableName.equals("urgent")) {
            /*
            * 在生词表中先获取熟悉值
            *
             */
            int familiar = getFamiliar(english, tableName, ps, conn);

            if (isKnown.equals("n")) {
                familiar = familiar - 5;
            }
            else if (isKnown.equals("y") && familiar + 1 < 20) {
                familiar++;
            }
            else {
                exchange(english, "newword", "oldword", ps, conn);
                return;
            }
            setFamiliar(familiar, tableName, ps, conn, english);
            if (isKnown.equals("y") && familiar + 1 < 20) {
                /*
                如果已经掌握了就把时间更新到现在，因为这样可以让以后review的时候不在抽到
                 */
                updateLastReview(ps, conn, tableName, english);
            }
        }
        else if (tableName.equals("yesterday") && isKnown.equals("n")) {
            updateLastReview(ps, conn, tableName, english);
        }
    }

    public static void updateLastReview(PreparedStatement ps, Connection conn, String tableName, String english) {
        String sql = "update " + tableName + " set lastReview = current_timestamp where word = '" + english + "'";
        try {
            ps = conn.prepareStatement(sql);
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static int getRowNumber(PreparedStatement ps, Connection conn, String tableName) {
        int result = 0;
        String sql = "select * from " + tableName;
        try {
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result ++;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    public static void init(Scanner s) {
        /*
        * 初始化程序
         */
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        for (int i = 0; i < 3; i ++) {
            boolean b = login(rs, ps, conn, s);
            if (b == true) {
                System.out.println("登录成功");
                break;
            }
            else if (i == 2) {
                System.out.println("账号密码错误三次，登录失败");
                return;
            }
        }

        while (true) {
            System.out.print("输入想要进行的操作，添加单词输入i，删除单词输入d，复习单词输入r，复习前一天生词输入y，退出输入e：");
            String str = s.nextLine();
            if (str.equals("e")) {
                break;
            }
            else if (str.equals("i")) {
                System.out.print("想要在那一张表格中新增，newword或者urgent: ");
                String temp = s.nextLine();
                if (temp.equals("newword") || temp.equals("urgent")) {
                    insert(ps, conn, s, temp);
                }
            }
            else if (str.equals("d")) {
                System.out.print("想要删除的单词在old还是new中？old表输入o，new表输入n：");
                String tableName = s.nextLine();
                if (tableName.equals("o")) {
                    tableName = "oldword";
                    delete(ps, conn, s, tableName);
                }
                else if (tableName.equals("n")) {
                    tableName = "newword";
                    delete(ps, conn, s, tableName);
                }
                else {
                    System.out.print("查无此表");
                }
            }
            else if (str.equals("r")) {
                System.out.print("请输入复习的单词数：");
                int n = Integer.parseInt(s.nextLine());
                System.out.print("需要在哪一张表中复习？old表输入o，new表输入n，urgent输入u：");
                String tableName = s.nextLine();
                if (tableName.equals("o")) {
                    tableName = "oldword";
                    review(rs, ps, conn, s, tableName, n);
                }
                else if (tableName.equals("n")) {
                    tableName = "newword";
                    review(rs, ps, conn, s, tableName, n);
                }
                else if (tableName.equals("u")) {
                    tableName = "urgent";
                    review(rs, ps, conn, s, tableName, n);
                }
                else {
                    System.out.print("查无此表");
                }
            }
            else if (str.equals("y")) {
                reviewYesterday(rs, ps, conn, s);
            }
            else {
                System.out.println("无效操作");
            }
        }

        DBUtil.close(rs, ps, conn);
    }

    private static boolean login(ResultSet rs, PreparedStatement ps, Connection conn, Scanner s) {
        /*
        * 判断用户登录名和密码是否正确
         */
        System.out.print("输入用户名：");
        String name = s.nextLine();
        System.out.print("输入密码：");
        String pass = s.nextLine();
        String sql = "select * from login where username = ? && userPassword = ?";
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, pass);
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("密码正确");
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        System.out.println("账号或密码错误");
        return false;
    }

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        init(s);
        s.close();
    }
}

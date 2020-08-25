package Body;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Reading {
    public static void main(String[] args) throws Exception{
        Scanner s = new Scanner(System.in);
        System.out.print("复习填空还是阅读，填空v阅读r：");
        String choice = s.nextLine();
        System.out.print("最多复习到第几篇：");
        int max = Integer.parseInt(s.nextLine());
        System.out.print("复习多少篇：");
        int n = Integer.parseInt(s.nextLine());
        reviewReading(choice, max, n, s);
    }

    public static void printArr(int[] arr) {
        for (int i = 0; i < arr.length; i ++) {
            System.out.print(arr[i] + " ,");
        }
    }

    public static int[] getRandomValue(int n) {
        int[] result = new int[n];
        Map<Integer, Boolean> map = new HashMap<>();
        for (int i = 0; i < n; i ++) {
            int temp = (int)((Math.random() * n) + 1);
            if (map.keySet().contains(temp)) {
                i --;
                continue;
            }
            else {
                map.put(temp, true);
                result[i] = temp;
            }
        }
        return result;
    }

    private static void reviewReading(String choice, int max, int n, Scanner s) throws Exception{
        String rootForPassage = null;
        String rootForSol = null;
        String log = null;
        if (!(choice.equals("v") || choice.equals("r"))) {
            System.out.println("输入错误");
        }
        int[] sequence = getRandomValue(max);
        if (choice.equals("r")) {
            log = "复习了" + n + "篇阅读";
            rootForPassage = "src\\Passage\\P";
            rootForSol = "src\\Passage\\S";
        }
        else {
            log = "复习了" + n + "个section的填空";
            rootForPassage = "src\\Verbal\\V";
            rootForSol = "src\\Verbal\\SV";
        }
        for (int i = 0; i < n; i ++) {
            String num = String.valueOf(sequence[i]);
            String passage = rootForPassage + num + ".txt";
            String sol = rootForSol + num + ".txt";
            showPassage(passage);
            String input = null;
            while ("y".equals(input) == false) {
                System.out.print("按y查看答案:");
                input = s.nextLine();
            }
            showPassage(sol);
            String input2 = null;
            while ("y".equals(input2) == false) {
                System.out.print("按y继续:");
                input2= s.nextLine();
            }
        }
        Word.writeLog(log);
    }

    private static void showString(String str) {
        if (str.length() <= 150) {
            System.out.println(str);
        }
        else {
            int n = str.indexOf(" ", 150);
            if (n == -1) {
                System.out.println(str);
                return;
            }
            String s1 = str.substring(0, n);
            System.out.println(s1);
            String s2 = null;
            try {
                s2 = str.substring(n + 1);
            } catch (StringIndexOutOfBoundsException e) {
                return;
            }
            showString(s2);
        }
    }

    private static void showPassage(String location) throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(location));
        String line = null;
        while ((line = br.readLine()) != null) {
            showString(line);
        }
        br.close();
    }
}

package com.paradox.addictsclock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    public static String sendGet(String url, String param) throws Exception {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);

            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();

            Map<String, List<String>> map = connection.getHeaderFields();
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }

            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            while ((line = in.readLine()) != null)
                result.append(line);

        } catch (Exception e) {
            throw new Exception(e);
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
        return result.toString();
    }

    public static String sendPost(String url, String param) throws Exception {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(url);

            URLConnection conn = realUrl.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

            conn.setDoOutput(true);
            conn.setDoInput(true);

            out = new PrintWriter(conn.getOutputStream());
            out.print(param);
            out.flush();

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }

        } catch (Exception e) {
            throw new Exception(e);
        }
        finally{
            if(out!=null){
                out.close();
            }
            if(in!=null){
                in.close();
            }
        }
        return result.toString();
    }
}
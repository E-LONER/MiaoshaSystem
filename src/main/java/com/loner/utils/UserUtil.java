package com.loner.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.loner.domain.UserInfo;

public class UserUtil {

    private static void createUser(int count) throws Exception{
        List<UserInfo> users = new ArrayList<UserInfo>(count);
        //生成用户
        for(int i=0;i<count;i++) {
            UserInfo user = new UserInfo();
            user.setId(18380226934L+i);
            user.setLoginCount(1);
            user.setNickname("user"+i);
            user.setRegisterDate(new Date());
            user.setSalt("loner");
            user.setPassword(MD5Util.inputPassToDbPass("loner225", user.getSalt()));
            users.add(user);
        }
        System.out.println("create user");
		//插入数据库
//		Connection conn = DBUtil.getConn();
//		String sql = "insert into user_info(loginCount, nickname, registerDate, salt, password, id)values(?,?,?,?,?,?)";
//		PreparedStatement pstmt = conn.prepareStatement(sql);
//		for(int i=0;i<users.size();i++) {
//			UserInfo user = users.get(i);
//			pstmt.setInt(1, user.getLoginCount());
//			pstmt.setString(2, user.getNickname());
//			pstmt.setTimestamp(3, new Timestamp(user.getRegisterDate().getTime()));
//			pstmt.setString(4, user.getSalt());
//			pstmt.setString(5, user.getPassword());
//			pstmt.setLong(6, user.getId());
//			pstmt.addBatch();
//		}
//		pstmt.executeBatch();
//		pstmt.close();
//		conn.close();
//		System.out.println("insert to db");
        //登录，生成token
        String urlString = "http://121.36.51.121:9999/login/dologin";
        File file = new File("C:\\Users\\E-loner\\Desktop\\user_cookies.txt");
        if(file.exists()) {
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        file.createNewFile();
        raf.seek(0);


        for(int i=0;i<users.size();i++) {
            UserInfo user = users.get(i);
            URL url = new URL(urlString);
            HttpURLConnection co = (HttpURLConnection)url.openConnection();
            co.setRequestMethod("POST");
            co.setDoOutput(true);
            OutputStream out = co.getOutputStream();

            String params = "phoneNum="+user.getId()+"&password="+MD5Util.inputPassToFormPass("loner225");
            out.write(params.getBytes());
            out.flush();
            InputStream inputStream = co.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte buff[] = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buff)) >= 0) {
                bout.write(buff, 0 ,len);
            }
            inputStream.close();
            bout.close();
            byte[] res=bout.toByteArray();
            String response = new String(res);
            //String response=res.toString();
            System.out.println("回复："+response);
            JSONObject jo = JSON.parseObject(response);
            String token = jo.getString("data");
            System.out.println("jo是："+jo);
            System.out.println("create token : " + user.getId());

            String row = user.getId()+","+token;
            raf.seek(raf.length());
            raf.write(row.getBytes());
            raf.write("\r\n".getBytes());
            System.out.println("write to file : " + token);
        }
        raf.close();

        System.out.println("over");
    }

    public static void main(String[] args)throws Exception {
        createUser(5000);
    }
}


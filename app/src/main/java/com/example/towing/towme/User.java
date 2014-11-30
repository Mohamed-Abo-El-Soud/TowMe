package com.example.towing.towme;

import android.content.Context;
import android.location.Location;

import java.util.Random;

/**
 * Created by Mohamed on 14-11-29.
 */
public class User {

    public String mUserName;
    private String mPassWord;
    public Location mLocation;

    private static final String USER_NAME_KEY = ".usernamekey";
    private static final String PASS_WORD_KEY = ".passwordKey";

    public User(){
        mUserName = new RandomString(5).nextString();
        mPassWord = new RandomString(5).nextString();
    }

    public User( Location location ){
        mLocation = location;
    }

    public User(String userName,String passWord){
        mUserName = userName;
        mPassWord = passWord;
    }

    public User(String userName,String passWord, Location location){
        mUserName = userName;
        mPassWord = passWord;
        mLocation = location;
    }

    public String getUserName(){
        return mUserName;
    }

    private String getPassWord() {
        return mPassWord;
    }
    private Boolean setPassword(String passWord){
        mPassWord = passWord;
        return true;
    }
    private Boolean setUserName (String userName){
        mUserName = userName;
        return true;
    }

    public Boolean setLocation (Location location){
        mLocation = location;
        return true;
    }
    public Location getLocation() {
        return mLocation;
    }

    public boolean storeUser(Context context){
        boolean check1 =
        Utilites.storeString(context,USER_NAME_KEY,mUserName);
        boolean check2 =
        Utilites.storeString(context,PASS_WORD_KEY,mPassWord);
        return check1 && check2;
    }
    public static User retrieveUser(Context context){
        String userName = Utilites.retrieveString(context,USER_NAME_KEY);
        String passWord = Utilites.retrieveString(context,PASS_WORD_KEY);
        if ((userName == null)||(passWord == null)) return null;
        return new User(userName,passWord);
    }

    public String getPassWordByPass (){
        return mPassWord;
    }

    public static class RandomString {

        private static final char[] symbols;

        static {
            StringBuilder tmp = new StringBuilder();
            for (char ch = '0'; ch <= '9'; ++ch)
                tmp.append(ch);
            for (char ch = 'a'; ch <= 'z'; ++ch)
                tmp.append(ch);
            symbols = tmp.toString().toCharArray();
        }

        private final Random random = new Random();

        private final char[] buf;

        public RandomString(int length) {
            if (length < 1)
                throw new IllegalArgumentException("length < 1: " + length);
            buf = new char[length];
        }

        public String nextString() {
            for (int idx = 0; idx < buf.length; ++idx)
                buf[idx] = symbols[random.nextInt(symbols.length)];
            return new String(buf);
        }
    }

}

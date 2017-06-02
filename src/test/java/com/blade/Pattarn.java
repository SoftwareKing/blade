package com.blade;

import com.blade.kit.StringKit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author biezhi
 *         2017/6/3
 */
public class Pattarn {

    public static void main(String[] args) {

//        String reg = "^(/user1/([^/]+))$(/user2/([^/]+))$(/user3/([^/]+)/([^/]+))$";
        String reg = "^(/user1/([^/]+))|(/user2/([^/]+))|(/user3/([^/]+)/([^/]+))$";

        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher("/user1/24");
        boolean matched = matcher.matches();
        System.out.println(matched);
    }
}

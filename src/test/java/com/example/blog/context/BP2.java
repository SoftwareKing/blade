package com.example.blog.context;

import com.blade.ioc.Ioc;
import com.blade.ioc.annotation.Bean;
import com.blade.ioc.annotation.Order;
import com.blade.lifecycle.BeanProcessor;

/**
 * @author biezhi
 *         2017/6/1
 */
@Bean
@Order(1)
public class BP2 implements BeanProcessor {

    @Override
    public void register(Ioc ioc) {
        System.out.println("bp2 -> " + 1);
    }

}

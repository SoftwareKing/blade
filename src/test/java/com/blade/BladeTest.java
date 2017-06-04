package com.blade;

import org.junit.Before;
import org.junit.Test;

/**
 * @author biezhi
 *         2017/6/4
 */
public class BladeTest extends BaseTestCase {

    @Test
    public void testAppName() {
        start(
                app.appName("demo1").openMonitor(false)
        );
    }

}

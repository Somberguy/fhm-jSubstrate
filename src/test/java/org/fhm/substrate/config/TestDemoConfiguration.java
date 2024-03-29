package org.fhm.substrate.config;

import org.fhm.substrate.annotation.Configuration;
import org.fhm.substrate.annotation.Value;

/**
 * @since 2024/2/20-3:42 PM
 * @author Somberguy
 */
@Configuration("test.demo")
public class TestDemoConfiguration extends AbstractDemoConfiguration {

    @Value("desc")
    private String desc;

    @Value("lucky.number")
    private Integer luckyNumber;

    @Value("bean.name")
    private String beanName;


    public String getDesc() {
        return desc;
    }

    public Integer getLuckyNumber() {
        return luckyNumber;
    }

    public String getBeanName() {
        return beanName;
    }
}

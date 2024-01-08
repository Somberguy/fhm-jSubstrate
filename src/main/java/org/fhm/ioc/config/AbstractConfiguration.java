package org.fhm.ioc.config;

import org.fhm.ioc.ability.IActuator;
import org.fhm.ioc.ability.ILoggerHandler;
import org.fhm.ioc.annotation.Configuration;
import org.fhm.ioc.annotation.Value;
import org.fhm.ioc.constant.Common;
import org.fhm.ioc.manager.Bootstrap;
import org.fhm.ioc.service.LoggerHandler;
import org.fhm.ioc.util.IOCExceptionUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @Classname AbstractConfiguration
 * @Description TODO Unified planning abstract configuration class
 * @Date 2023/10/14 10:24
 * @Created by 月光叶
 */
public abstract class AbstractConfiguration implements IActuator {

    /**
     * Default configuration container
     */
    public static Map<String, Object> defaultConfigContainer = new HashMap<>();
    /**
     * Configuration container
     */
    public static Map<String, Object> configContainer = new HashMap<>();
    public static Map<String, InputStream> resource = new HashMap<>();
    public static Map<String, Object> configObj = new HashMap<>();
    /**
     * Record the processed configuration file
     */
    private static Set<String> recordConfig = new HashSet<>();
    private final ILoggerHandler logger = LoggerHandler.getLogger(AbstractConfiguration.class);
    /*
     * Configuration name
     */
    private final String CONFIG_FILE_NAME;

    protected AbstractConfiguration(String configFileName) {
        this.CONFIG_FILE_NAME = configFileName;
    }

    public static void clearMemory() {
        resource = null;
        recordConfig = null;
        configContainer = null;
        defaultConfigContainer = null;
        configObj = null;
        System.gc();
    }

    /**
     * Configuration into
     */
    private void readConfigurationValue() {
        if (recordConfig.stream().anyMatch(CONFIG_FILE_NAME::equals)) {
            return;
        }
        try (
                InputStream is = this.getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)
        ) {
            if (Objects.isNull(is)) {
                if (!getResourceOfCp(configContainer)) {
                    logger.warn("the user configuration file {} was not scanned", CONFIG_FILE_NAME);
                }
            } else {
                getConfigBr(is, configContainer);
            }
        } catch (IOException e) {
            throw IOCExceptionUtil.generateConfigurationException(e);
        }
        InputStream is = resource.get(Common.DEFAULT_KEYWORD.getName() + CONFIG_FILE_NAME);
        if (is == null) {
            logger.warn("the default configuration file {} was not scanned", CONFIG_FILE_NAME);
        } else {
            getConfigBr(is, defaultConfigContainer);
        }
        recordConfig.add(CONFIG_FILE_NAME);
    }

    private boolean getResourceOfCp(Map<String, Object> configContainer) {
        try (InputStream is = Files.newInputStream(Paths.get("." + File.separator + CONFIG_FILE_NAME))) {
            getConfigBr(is, configContainer);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void getConfigBr(InputStream is, Map<String, Object> configContainer) {
        try (
                Reader r = new InputStreamReader(is, Bootstrap.charset);
                BufferedReader br = new BufferedReader(r)
        ) {
            String str;
            while (Objects.nonNull((str = br.readLine()))) {
                if (str.isEmpty()) {
                    continue;
                }
                if (str.toCharArray()[0] == 35) {
                    continue;
                }
                if (!str.contains("=")) {
                    logger.error("file:" + CONFIG_FILE_NAME + " value " + str + " is error");
                    continue;
                }
                String[] split = str.split("=");
                String key = split[0];
                if (split.length == 1) {
                    logger.error("{} is not config", key);
                    continue;
                }
                if (split.length != 2) {
                    logger.error("file:" + CONFIG_FILE_NAME + " value " + str + " is error");
                    continue;
                }
                configContainer.putIfAbsent(key, split[1]);
            }
        } catch (IOException e) {
            throw IOCExceptionUtil.generateConfigurationException("read config file :" + CONFIG_FILE_NAME + " fail", e);
        }
    }


    private void completeConfigurationSetup(AbstractConfiguration config) {
        Class<?> clazz = config.getClass();
        Configuration anno = clazz.getAnnotation(Configuration.class);
        if (null == anno) {
            throw IOCExceptionUtil.generateConfigurationException("the configuration is missing annotation");
        }
        String name;
        if ((name = anno.value()).isEmpty()) {
            throw IOCExceptionUtil.generateConfigurationException("the annotation of configuration name is not empty");
        }
        Stream.of(clazz.getDeclaredFields()).filter(
                field -> Arrays.stream(field.getAnnotations())
                        .anyMatch(annotation -> annotation instanceof Value)
        ).forEach(
                field -> {
                    String fieldName = field.getName();
                    String configFieldName = getConfigFieldName(field);
                    String setMethodName = "set" +
                            fieldName.substring(0, 1).toUpperCase(Locale.ROOT) +
                            fieldName.substring(1);
                    Object result;
                    if ((result = configContainer.get(name + "." + configFieldName)) == null || result.toString().isEmpty()) {
                        result = defaultConfigContainer.get(name + "." + configFieldName);
                    }
                    if (result == null) {
                        logger.warn("the field {} of {} is not config", configFieldName, clazz);
                        return;
                    }
                    final Object finalResult = result;
                    AtomicBoolean isFind = new AtomicBoolean(false);
                    Stream.of(clazz.getMethods()).forEach(method -> {
                        if (setMethodName.equals(method.getName()) && method.getParameterCount() == 1) {
                            isFind.set(true);
                            Class<?> parameterType = method.getParameterTypes()[0];
                            Object value;
                            if (Integer.class.isAssignableFrom(parameterType)
                                    || "int".equals(parameterType.getName())) {
                                try {
                                    value = Integer.parseInt(finalResult.toString());
                                } catch (Exception e) {
                                    logger.error("class {} type cast {} fail", clazz, parameterType);
                                    throw IOCExceptionUtil
                                            .generateConfigurationException(e.getMessage(), e);
                                }
                            } else {
                                value = finalResult;
                            }
                            try {
                                method.invoke(config, value);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw IOCExceptionUtil
                                        .generateConfigurationException(fieldName + " do not hava " + setMethodName + " method");
                            }
                        }
                    });
                    if (!isFind.get()) {
                        logger.warn("configuration class {} does not have a setter method for property {}", clazz, fieldName);
                    }

                });
    }

    private String getConfigFieldName(Field field) {
        AtomicReference<String> configFieldName = new AtomicReference<>();
        Stream.of(field.getAnnotations()).forEach(
                annotation -> {
                    if (annotation instanceof Value) {
                        configFieldName.set(((Value) annotation).value());
                    }
                }
        );
        return configFieldName.get();
    }


    @Override
    public void action(Object object) {
        if (Objects.isNull(object)) {
            throw IOCExceptionUtil.generateConfigurationException("the length of params is warning");
        }
        if (!(object instanceof AbstractConfiguration)) {
            throw IOCExceptionUtil.generateConfigurationException("the type of params is warning");
        }
        readConfigurationValue();
        completeConfigurationSetup((AbstractConfiguration) object);
    }
}

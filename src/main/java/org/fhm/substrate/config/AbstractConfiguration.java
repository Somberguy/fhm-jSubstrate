package org.fhm.substrate.config;

import org.fhm.substrate.ability.IActuator;
import org.fhm.substrate.annotation.Configuration;
import org.fhm.substrate.annotation.Value;
import org.fhm.substrate.constant.Common;
import org.fhm.substrate.constant.DataTypeMark;
import org.fhm.substrate.constant.VMParameters;
import org.fhm.substrate.service.LoggerHandler;
import org.fhm.substrate.standard.ILogger;
import org.fhm.substrate.util.IOCExceptionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @since 2023/10/14 10:24
 * @author Somberguy
 */
public abstract class AbstractConfiguration implements IActuator {

    /**
     * Default configuration container
     */
    public static Map<String, String> defaultConfigContainer = new HashMap<>();
    /**
     * Configuration container
     */
    public static Map<String, String> configContainer = new HashMap<>();
    public static Map<String, InputStream> resource = new HashMap<>();
    public static Map<String, Object> configObj = new HashMap<>();
    /**
     * Record the processed configuration file
     */
    private static Set<String> recordConfig = new HashSet<>();
    private final ILogger logger = LoggerHandler.getLogger(AbstractConfiguration.class);
    /*
     * Configuration file name
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
                if (!getResourceOfEnv(configContainer)) {
                    logger.warn("the user configuration file {} was not scanned", CONFIG_FILE_NAME);
                }
            } else {
                paddingConfigurationMember(is, configContainer);
            }
        } catch (IOException e) {
            throw IOCExceptionUtil.generateConfigurationException(e);
        }
        InputStream is = resource.get(Common.DEFAULT_KEYWORD.getName() + CONFIG_FILE_NAME);
        if (is == null) {
            logger.warn("the default configuration file {} was not scanned", CONFIG_FILE_NAME);
        } else {
            paddingConfigurationMember(is, defaultConfigContainer);
        }
        recordConfig.add(CONFIG_FILE_NAME);
    }

    private boolean getResourceOfEnv(Map<String, String> configContainer) {
        return VMParameters.CONFIG_FILE_PATH.use((name, v) -> {
            try (InputStream is = Files.newInputStream(Paths.get(v + CONFIG_FILE_NAME))) {
                paddingConfigurationMember(is, configContainer);
                return true;
            } catch (IOException e) {
                logger.warn(e);
                return false;
            }
        });
    }

    private void paddingConfigurationMember(InputStream is, Map<String, String> configContainer) {
        try {
            Properties properties = new Properties();
            properties.load(is);
            Enumeration<?> iter = properties.propertyNames();
            while (iter.hasMoreElements()) {
                String key = iter.nextElement().toString();
                configContainer.putIfAbsent(key, properties.getProperty(key));
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
                    String configFieldName = getConfigFieldName(field);
                    String result;
                    if (
                            ((result = configContainer.get(name + "." + configFieldName)) == null || result.isEmpty()) &&
                                    ((result = defaultConfigContainer.get(name + "." + configFieldName)) == null || result.isEmpty())
                    ) {
                        logger.warn("the field [ {}.{} ] of {} is not configured", name, configFieldName, clazz);
                    } else {
                        field.setAccessible(true);
                        try {
                            field.set(config, DataTypeMark.obtainData(result));
                        } catch (IllegalAccessException e) {
                            logger.error("class {} value {} cast fail", clazz, result);
                            throw IOCExceptionUtil
                                    .generateConfigurationException(e.getMessage(), e);
                        }
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

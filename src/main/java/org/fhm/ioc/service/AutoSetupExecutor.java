package org.fhm.ioc.service;

import org.fhm.ioc.ability.ILoggerHandler;
import org.fhm.ioc.annotation.Component;
import org.fhm.ioc.annotation.Configuration;
import org.fhm.ioc.annotation.Setup;
import org.fhm.ioc.config.AbstractConfiguration;
import org.fhm.ioc.constant.Common;
import org.fhm.ioc.util.ClazzUtil;
import org.fhm.ioc.util.IOCExceptionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @Classname AutoSetupExecutor
 * @Description TODO
 * @Date 2023/10/26 22:33
 * @Created by 月光叶
 */
public class AutoSetupExecutor {

    private final ILoggerHandler logger = LoggerHandler.getLogger(AutoSetupExecutor.class);

    public Map<Object, Map<Field, Class<?>>> setupInterfaceAndAbstractObjs;

    private Map<Object, Map<Field, Class<?>>> setupMapObjs;

    private Map<String, Object> objContainer = new HashMap<>();

    public static AutoSetupExecutor getInstance() {
        return Instance.instance;
    }

    public void initialAutoSetupContainer() {
        int size = objContainer.size();
        setupMapObjs = new HashMap<>(size, 1);
        setupInterfaceAndAbstractObjs = new HashMap<>(size, 1);
    }

    public void autoSetup() {
        objContainer.forEach((k, v) -> {
            Class<?> clazz = v.getClass();
            setupValueAndFilter(v, clazz, objContainer);
        });
    }

    private void setupValueAndFilter(Object v, Class<?> clazz, Map<String, Object> objContainer) {
        Field[] fields = clazz.getDeclaredFields();
        Stream.of(fields).forEach(field -> {
            if (!field.isAnnotationPresent(Setup.class)) {
                return;
            }
            Class<?> fieldType;
            if (Map.class.isAssignableFrom((fieldType = field.getType()))) {
                Type type = field.getGenericType();
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (
                            actualTypeArguments.length == 2 &&
                                    "java.lang.String".equals(actualTypeArguments[0].getTypeName())
                    ) {
                        String clazzName = "";
                        try {

                            Class<?> ata =
                                    IOCClassLoader.getInstance()
                                            .loadClass(
                                                    (clazzName = actualTypeArguments[1]
                                                            .getTypeName()
                                                            .replaceAll(
                                                                    Common.UNKNOWN_PARADIGM_SIGNS.getName(),
                                                                    "")
                                                    )
                                            );
                            if (objContainer.values().stream()
                                    .anyMatch(o -> ata.isAssignableFrom(o.getClass()))
                            ) {
                                setupMapObjs.computeIfAbsent(v, m -> new HashMap<>())
                                        .put(field, ata);
                            }
                        } catch (ClassNotFoundException e) {
                            logger.error(e, "failed to load " + clazzName);
                            throw IOCExceptionUtil.generateAutoSetupException(e);
                        }
                    }
                }
            } else if (Modifier.isAbstract(fieldType.getModifiers()) || fieldType.isInterface()) {
                if (v.getClass().isAnnotationPresent(Configuration.class)) {
                    logger.warn("configuration obj can not inject class by interface or abstract");
                } else {
                    setupInterfaceAndAbstractObjs
                            .computeIfAbsent(
                                    v,
                                    map -> new HashMap<>()
                            )
                            .put(field, fieldType);
                }
            } else {
                ClazzUtil.setClazzValue(v, objContainer.get(field.getType().getName()), field);
            }
        });
        Class<?> superClazz;
        if (!(superClazz = clazz.getSuperclass()).getName().equals(Object.class.getName())) {
            setupValueAndFilter(v, superClazz, objContainer);
        }
    }

    public void autoSetupMapObj() {
        setupMapObjs.forEach((o, map) -> map.forEach((field, c) -> {
            Map<String, Object> objMap = new HashMap<>();
            objContainer.values()
                    .forEach(obj -> {
                        Class<?> objC;
                        if (c.isAssignableFrom((objC = obj.getClass()))) {
                            Component component;
                            String name;
                            if ((component = objC.getAnnotation(Component.class)) != null
                                    && !(name = component.value()).isEmpty()
                            ) {
                                objMap.put(name, obj);
                            } else {
                                objMap.put(objC.getName(), obj);
                            }
                        }
                    });
            ClazzUtil.setClazzValue(o, objMap, field);
        }));
    }


    public void beanDistribute() {
        objContainer.forEach((k, v) -> {
            if (v instanceof AbstractConfiguration) {
                AbstractConfiguration.configObj.put(k, v);
            } else {
                BeanOptimizer.getInstance().addBean(k, v);
            }
        });
        objContainer = null;
    }

    public Map<String, Object> getObjContainer() {
        return objContainer;
    }

    private static final class Instance {
        private static final AutoSetupExecutor instance = new AutoSetupExecutor();
    }
}

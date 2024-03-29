package org.fhm.substrate.constant;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @since 2024/1/8-11:12 AM
 * @author Somberguy
 */
public enum VMParameters {

    VM_OPTIONS_FILE_PATH("fhm.substrate.vm.options.file.path"),

    CONFIG_FILE_PATH("fhm.substrate.config.file.path"),

    REGISTRY_BEAN_DIR_PATH("fhm.substrate.registry.bean.dir.path"),

    REGISTRY_PACKAGE_NAME("fhm.substrate.registry.package.name");

    private final String name;

    private String value;

    VMParameters(String name) {
        this.name = name;
        setValue(name);
    }

    public boolean use(BiFunction<String, String, Boolean> function) {
        return !value.isEmpty() && function.apply(name, value);
    }

    public void use(BiConsumer<String, String> consumer) {
        if (!value.isEmpty())
            consumer.accept(name, value);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    private void setValue(String name) {
        String v;
        value = Objects.nonNull((v = System.getProperty(name))) ? v : "";
    }

    public void resetValue(String value) {
        this.value = value;
    }
}
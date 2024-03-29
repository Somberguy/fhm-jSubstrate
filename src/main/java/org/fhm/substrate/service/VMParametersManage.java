package org.fhm.substrate.service;

import org.fhm.substrate.constant.VMParameters;
import org.fhm.substrate.standard.ILogger;
import org.fhm.substrate.standard.IStarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.Objects;

/**
 * @since 2024/1/30-11:41 AM
 * @author Somberguy
 */
public class VMParametersManage {

    private static final ILogger logger = LoggerHandler.getLogger(VMParametersManage.class);

    private static final String IOC_VM_OPTIONS_FILE_NAME = "substrate.vmoptions";

    public static VMParametersManage getInstance() {
        return Instance.instance;
    }

    public void readVMOptionsFileParameters(Class<? extends IStarter> starterClazz) {
        VMParameters.VM_OPTIONS_FILE_PATH.use((name, v) -> {
            if (v.equals(".")) {
                CodeSource codeSource = starterClazz.getProtectionDomain().getCodeSource();
                if (Objects.nonNull(codeSource)) {
                    String fileName = codeSource.getLocation().getFile();
                    File file = new File(fileName.substring(0, fileName.lastIndexOf(File.separator) + 1));
                    File[] files;
                    if (file.exists() && Objects.nonNull((files = file.listFiles()))) {
                        for (File f : files) {
                            if (f.isFile() && f.getName().equals(IOC_VM_OPTIONS_FILE_NAME))
                                collectVMParameters(f);
                        }
                    }
                }
            } else {
                File file = new File(v);
                if (file.exists() && file.isFile() && file.getName().equals(IOC_VM_OPTIONS_FILE_NAME))
                    collectVMParameters(file);
                else
                    logger.warn("the configured substrate.vmoptions file does not exist.");
            }
        });
    }

    private void collectVMParameters(File file) {
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String content;
            while (Objects.nonNull((content = br.readLine()))) {
                parseLineContent(content);
            }
        } catch (IOException e) {
            logger.error("VM options file error, {}, {}", e.getMessage(), e);
        }
    }

    private void parseLineContent(String content) {
        String[] result = content.substring(content.indexOf("-D") + 2).split("=");
        if (result.length == 2) {
            setVMParameter(result[0], result[1]);
        }
    }

    private void setVMParameter(String name, String v) {
        for (VMParameters parameter : VMParameters.values()) {
            if (parameter.getName().equals(name)) {
                parameter.resetValue(v);
            }
        }
    }

    private static final class Instance {
        private static final VMParametersManage instance = new VMParametersManage();
    }


}

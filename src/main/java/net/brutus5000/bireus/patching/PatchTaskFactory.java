package net.brutus5000.bireus.patching;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.HashMap;

@Slf4j
public class PatchTaskFactory {
    @Getter
    private static PatchTaskFactory instance = new PatchTaskFactory();

    private HashMap<Integer, Class> patchTaskVersions = new HashMap<>();

    public PatchTaskFactory() {
        patchTaskVersions.put(1, PatchTaskV1.class);
    }

    public void add(Integer version, Class patchTaskClass) {
        if (patchTaskClass.getSuperclass() != PatchTask.class) {
            throw new IllegalArgumentException(MessageFormat.format("Class `{0}` is not a subclass of PatchTask", patchTaskClass.getName()));
        }

        log.debug("Added class `{}` as version `{}`", patchTaskClass.getName(), version);
        patchTaskVersions.put(version, patchTaskClass);
    }

    @SneakyThrows
    public PatchTask create(Integer version) {
        Class patchTaskClass = patchTaskVersions.get(version);
        if (patchTaskClass == null) {
            throw new IllegalArgumentException(MessageFormat.format("No patch task matching version `{0}`", version));
        }

        return (PatchTask) patchTaskClass.newInstance();
    }
}

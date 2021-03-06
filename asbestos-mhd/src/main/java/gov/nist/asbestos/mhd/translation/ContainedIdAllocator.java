package gov.nist.asbestos.mhd.translation;

import java.util.HashMap;
import java.util.Map;

public class ContainedIdAllocator {
    private Map<Class<?>, Integer> indexMap = new HashMap<>();

    public String newId(Class<?> clas) {
        if (!indexMap.containsKey(clas))
            indexMap.put(clas, 1);
        int index = indexMap.get(clas);
        indexMap.put(clas, index + 1);
        return "#" + clas.getSimpleName().toLowerCase() + index;
    }

}

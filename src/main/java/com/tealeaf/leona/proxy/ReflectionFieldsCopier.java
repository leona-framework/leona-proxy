package org.lyora.leona.proxy;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionFieldsCopier {
    private final Map<Class<?>, List<Field>> reflectionEligibleFields = new HashMap<>();

    /**
     * Copies eligible fields from the source object to the target object using reflection.
     * Eligible fields are non-static and non-final fields of the same type or a subtype.
     * This method caches the eligible fields for future use to improve performance.
     *
     * @param source The source object from which fields will be copied.
     * @param target The target object to which fields will be copied.
     * @throws IllegalArgumentException If the source or target object is null.
     */
    public <S, T extends S> void copyFieldValues(T source, S target) {
        Class<?> originalClass = source.getClass();
        List<Field> eligibleCopyFields = reflectionEligibleFields.get(originalClass);

        // If cached fields are available, copy values directly from the cache
        if (eligibleCopyFields != null) {
            for (int i = 0; i < eligibleCopyFields.size(); i++) {
                Field field = eligibleCopyFields.get(i);
                try {
                    Object value = FieldUtils.readField(field, source, true);
                    FieldUtils.writeField(field, target, value);
                } catch (IllegalAccessException e) {
                    // If access is not allowed (for whatever reason), remove the field from the cache
                    eligibleCopyFields.remove(i--);
                }
            }
            return;
        }

        Field[] allFields = originalClass.getDeclaredFields();
        final List<Field> determinedEligibleCopyFields = new ArrayList<>();
        // Iterate through all fields to find eligible fields
        for (Field field : allFields) {
            // Skip static and final fields
            int modifiers = field.getModifiers();
            if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) continue;
            try {
                Object value = FieldUtils.readField(field, source, true);
                FieldUtils.writeField(field, target, value);
                determinedEligibleCopyFields.add(field);
            } catch (IllegalAccessException ignored) {
            }
        }

        reflectionEligibleFields.put(originalClass, determinedEligibleCopyFields);
    }
}

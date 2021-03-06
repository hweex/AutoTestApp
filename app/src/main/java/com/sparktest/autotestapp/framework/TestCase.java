package com.sparktest.autotestapp.framework;


import com.sparktest.autotestapp.framework.annotation.Description;

public class TestCase extends Test {
    private Class testClass;

    public String getDescription() {
        Description desc = (Description) testClass.getAnnotation(Description.class);
        if (desc != null) {
            return desc.value();
        }
        return testClass.getSimpleName();
    }

    public TestCase(Class<?> testClass) {
        this.testClass = testClass;
    }

    public Class<?> getTestClass() {
        return testClass;
    }
}

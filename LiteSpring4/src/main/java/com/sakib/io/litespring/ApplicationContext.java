package com.sakib.io.litespring;

import com.sakib.io.litespring.annotation.Autowired;
import com.sakib.io.litespring.annotation.Component;
import com.sakib.io.litespring.annotation.RequestMapping;
import com.sakib.io.litespring.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {
    private final Map<String, Object> beanFactory = new HashMap<>();
    private final int TOMCAT_PORT = 8080;
    private TomCatConfig tomCatConfig;


    private static ApplicationContext applicationContext;

    private ApplicationContext() {
        tomCatConfig = new TomCatConfig(TOMCAT_PORT);
    }

    public static synchronized ApplicationContext getInstance() {
        if(applicationContext == null) {
            applicationContext = new ApplicationContext();
        }
        return applicationContext;
    }

    protected void createSpringContainer(List<Class<?>> classes) {
        try {
            beanCreates(classes);
            injectDependencies(classes);
            DispatcherServlet dispatcherServlet = new DispatcherServlet(findControllerMethods(classes));
            tomCatConfig.registerServlet(
                    dispatcherServlet, dispatcherServlet.getClass().getSimpleName(), "/");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected List<ControllerMethod> findControllerMethods(List<Class<?>> classes) {
        List<ControllerMethod> controllerMethods = new ArrayList<>();

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(RestController.class)) {
                RestController restController = clazz.getAnnotation(RestController.class);
                for(Method method : clazz.getDeclaredMethods()) {
                    if(method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                        String mappedUrl = restController.url() + requestMapping.url();
                        ControllerMethod controllerMethod = ControllerMethod.builder()
                                .clazz(clazz)
                                .method(method)
                                .methodType(requestMapping.type())
                                .url(mappedUrl)
                                .instance(beanFactory.get(clazz.getSimpleName()))
                                .build();
                        System.out.println("controllerMethod = " + controllerMethod);
                        controllerMethods.add(controllerMethod);
                    }
                }
            }
        }
        return controllerMethods;
    }



    protected void beanCreates(List<Class<?>> classes) throws Exception {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class) ||
                    clazz.isAnnotationPresent(RestController.class)) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                beanFactory.put(clazz.getSimpleName(), instance);
            }
        }
    }

    protected void injectDependencies(List<Class<?>> classes) throws IllegalAccessException {
        for(Class<?> clazz : classes) {
            if(clazz.isAnnotationPresent(Component.class) ||
                    clazz.isAnnotationPresent(RestController.class)) {

                Object bean = beanFactory.get(clazz.getSimpleName());

                Field[] fields = bean.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        Object instance = beanFactory.get(field.getType().getSimpleName());
                        field.setAccessible(true);
                        field.set(bean, instance);
                    }
                }
            }
        }
    }

    public Object getBean(Class<?> clazz) {
        return beanFactory.get(clazz.getSimpleName());
    }

    public Object getBean(String name) {
        return beanFactory.get(name);
    }
}

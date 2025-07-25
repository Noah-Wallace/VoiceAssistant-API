package com.voiceassistantapi.rest;

import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(VoiceInterpretResource.class);
        resources.add(JacksonFeature.class); // Register Jackson for POJO mapping
        return resources;
    }
}
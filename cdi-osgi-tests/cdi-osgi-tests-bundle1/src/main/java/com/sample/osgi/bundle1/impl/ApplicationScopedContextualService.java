package com.sample.osgi.bundle1.impl;

import com.sample.osgi.bundle1.api.ContextualService;
import com.sample.osgi.bundle1.api.Name;
import org.osgi.cdi.api.extension.annotation.Publish;

import javax.enterprise.context.ApplicationScoped;

@Publish
@Name("Application")
@ApplicationScoped
public class ApplicationScopedContextualService implements ContextualService {

    private final long id = System.nanoTime();

    @Override
    public long getId() {
        return id;
    }
}

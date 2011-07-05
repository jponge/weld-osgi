package org.osgi.cdi.test;

import com.sample.osgi.bundle1.api.*;
import com.sample.osgi.bundle1.util.EventListener;
import com.sample.osgi.bundle1.util.ServiceProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.cdi.api.extension.Service;
import org.osgi.cdi.api.integration.CDIContainer;
import org.osgi.cdi.api.integration.CDIContainerFactory;
import org.osgi.cdi.test.util.Environment;
import org.osgi.framework.*;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import java.io.Serializable;
import java.util.Collection;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(JUnit4TestRunner.class)
public class MetaFilterTest {

    @Configuration
    public static Option[] configure() {
        return options(
                Environment.CDIOSGiEnvironment(
                        mavenBundle("com.sample.osgi","cdi-osgi-tests-bundle1").version("1.0-SNAPSHOT"),
                        mavenBundle("com.sample.osgi","cdi-osgi-tests-bundle2").version("1.0-SNAPSHOT"),
                        mavenBundle("com.sample.osgi","cdi-osgi-tests-bundle3").version("1.0-SNAPSHOT")
                )
        );
    }

    @Test
    public void metaFilterTest(BundleContext context) throws InterruptedException, InvalidSyntaxException, BundleException {
        Environment.waitForEnvironment(context);
        ServiceReference ref = context.getServiceReference(TestPublished.class.getName());
        TestPublished test = (TestPublished) context.getService(ref);
        if (test != null) {
            PropertyService serv1 = test.getService();
            PropertyService serv2 = test.getService2();
            Assert.assertTrue(serv1.whoAmI().equals(serv2.whoAmI()));
        } else {
            Assert.fail("No test bean available");
        }
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sample.osgi.service1;

import com.sample.osgi.api.DummyService;
import javax.inject.Named;

/**
 *
 * @author mathieuancelin
 */
@Named
public class DummyServiceImpl implements DummyService {

    @Override
    public int times(int base, int time) {
        return base * time;
    }

}

/**
 *   Copyright 2014 Nortal AS
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.nortal.petit.beanmapper;

import org.junit.Assert;
import org.junit.Test;

import com.nortal.petit.beanmapper.fixture.BeanMappingTestBean;
import com.nortal.petit.beanmapper.fixture.IdLessTestBean;

public class BeanMappingTest {

    @Test
    public void testTable__fullBeanMapping() {
        BeanMapping<BeanMappingTestBean> bm = BeanMappings.get(BeanMappingTestBean.class);

        Assert.assertNotNull(bm);
        Assert.assertNotNull(bm.id());

        Assert.assertEquals("test_schema.bean_mapping_test", bm.table());

    }
    
    @Test
    public void testIdLessBean() {
        BeanMapping<IdLessTestBean> bm = BeanMappings.get(IdLessTestBean.class);

        Assert.assertNotNull(bm);
        Assert.assertNull(bm.id());

        Assert.assertEquals("test_schema.id_less_tester", bm.table());
    }

}

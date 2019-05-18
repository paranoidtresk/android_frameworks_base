/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tests.rollback.host;

import static org.junit.Assert.assertTrue;

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Runs the staged rollback tests.
 */
@RunWith(DeviceJUnit4ClassRunner.class)
public class StagedRollbackTest extends BaseHostJUnit4Test {

    private static final String TEST_APEX_PKG = "com.android.tests.rollback.testapex";

    /**
     * Runs the given phase of a test by calling into the device.
     * Throws an exception if the test phase fails.
     * <p>
     * For example, <code>runPhase("testApkOnlyEnableRollback");</code>
     */
    private void runPhase(String phase) throws Exception {
        assertTrue(runDeviceTests("com.android.tests.rollback",
                    "com.android.tests.rollback.StagedRollbackTest",
                    phase));
    }

    /**
     * Tests staged rollbacks involving only apks.
     */
    @Test
    public void testApkOnly() throws Exception {
        runPhase("testApkOnlyEnableRollback");
        getDevice().reboot();
        runPhase("testApkOnlyCommitRollback");
        getDevice().reboot();
        runPhase("testApkOnlyConfirmRollback");
    }

    /**
     * Tests watchdog triggered staged rollbacks involving only apks.
     */
    @Test
    public void testBadApkOnly() throws Exception {
        runPhase("testBadApkOnlyEnableRollback");
        getDevice().reboot();
        runPhase("testBadApkOnlyConfirmEnableRollback");
        try {
            // This is expected to fail due to the device being rebooted out
            // from underneath the test. If this fails for reasons other than
            // the device reboot, those failures should result in failure of
            // the testApkOnlyConfirmRollback phase.
            CLog.logAndDisplay(LogLevel.INFO, "testBadApkOnlyTriggerRollback is expected to fail");
            runPhase("testBadApkOnlyTriggerRollback");
        } catch (AssertionError e) {
            // AssertionError is expected.
        }

        getDevice().waitForDeviceAvailable();
        runPhase("testApkOnlyConfirmRollback");
    }

    /**
     * Tests staged rollbacks involving only apex.
     */
    @Test
    public void testApexOnly() throws Exception {
        installTestApexV1();
        runPhase("testApexOnlyEnableRollback");
        getDevice().reboot();
        runPhase("testApexOnlyCommitRollback");
        getDevice().reboot();
        runPhase("testApexOnlyConfirmRollback");
    }

    /**
     * Tests staged rollbacks involving apk and apex.
     */
    @Test
    public void testApkAndApex() throws Exception {
        installTestApexV1();
        runPhase("testApkAndApexEnableRollback");
        getDevice().reboot();
        runPhase("testApkAndApexCommitRollback");
        getDevice().reboot();
        runPhase("testApkAndApexConfirmRollback");
    }

    /**
     * Tests that apex update expires existing rollbacks for that apex.
     */
    @Test
    public void testApexRollbackExpiration() throws Exception {
        installTestApexV1();
        runPhase("testApexRollbackExpirationEnableRollback");
        getDevice().reboot();
        runPhase("testApexRollbackExpirationUpdateApex");
        getDevice().reboot();
        runPhase("testApexRollbackExpirationConfirmExpiration");
    }

    /**
     * Do whatever is necessary to get version 1 of the test apex installed on
     * the device. Try to do so without extra reboots where possible to keep
     * the test execution time down.
     */
    private void installTestApexV1() throws Exception {
        for (ITestDevice.ApexInfo apexInfo : getDevice().getActiveApexes()) {
            if (TEST_APEX_PKG.equals(apexInfo.name)) {
                if (apexInfo.versionCode == 1) {
                    return;
                }
                getDevice().uninstallPackage(TEST_APEX_PKG);
                getDevice().reboot();
                break;
            }
        }

        runPhase("installTestApexV1");
        getDevice().reboot();
    }
}

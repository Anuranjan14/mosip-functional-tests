
package io.mosip.registration.test;

import static org.testng.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.AuthorizationDTO;
import io.mosip.registration.service.login.impl.LoginServiceImpl;
import io.mosip.registration.util.BaseConfiguration;
import io.mosip.registration.util.CommonUtil;
import io.mosip.registration.util.ConstantValues;
import io.mosip.registration.util.TestCaseReader;
import io.mosip.registration.util.TestDataGenerator;

/**
 * @author Tabish Khan
 * 
 *         This test class implements the unit tests for the services exposed by
 *         the LoginService class
 *
 */
public class LoginServiceScreenAuthorizationTest extends BaseConfiguration implements ITest {

	@Autowired
	private LoginServiceImpl loginService;

	@Autowired
	TestDataGenerator dataGenerator;

	@Autowired
	TestCaseReader testCaseReader;

	@Autowired
	CommonUtil commonUtil;

	/**
	 * Declaring CenterID,StationID global
	 */
	private static String centerID = null;
	private static String stationID = null;

	private static final Logger logger = AppConfig.getLogger(LoginServiceScreenAuthorizationTest.class);
	private static final String serviceName = "LoginService";
	private static final String testDataFileName = "LoginServiceTestData";
	private static final String testCasePropertyFileName = "condition";
	protected static String mTestCaseName = "";
	private static final String subServiceName = "ScreenAuthorization";

	@BeforeMethod
	public void setUp() {
		baseSetUp();
		centerID = (String) ApplicationContext.map().get(ConstantValues.CENTERIDLBL);
		stationID = (String) ApplicationContext.map().get(ConstantValues.STATIONIDLBL);
	}

	/**
	 * 
	 * @return
	 * 
	 * 		Defines data source for obtaining screen authorization
	 */
	@DataProvider(name = "screenAuthorizationDataProvider")
	public Object[][] readTestCase() {
		// String testParam = context.getCurrentXmlTest().getParameter("testType");
		String testType = "regression";
		if (testType.equalsIgnoreCase("smoke"))
			return testCaseReader.readTestCases(serviceName + "/" + subServiceName, "smoke");
		else
			return testCaseReader.readTestCases(serviceName + "/" + subServiceName, "regression");
	}

	/**
	 * 
	 * @param testCaseName
	 * @param object
	 * 
	 *            Defines test for obtaining screen authorization
	 */
	@Test(dataProvider = "screenAuthorizationDataProvider", alwaysRun = true)
	public void testGetScreenAuthorizationDetails(String testCaseName, JSONObject object) {
		try {
			logger.info(this.getClass().getName(), ConstantValues.MODULE_ID, ConstantValues.MODULE_NAME, testCaseName);
			mTestCaseName = testCaseName;
			logger.info(this.getClass().getName(), ConstantValues.MODULE_ID, ConstantValues.MODULE_NAME,
					"testGetScreenAuthorizationDetails invoked!");
			List<String> roleSet = new ArrayList<>();
			String subServiceName = "screenAuthorization";
			Properties prop = commonUtil.readPropertyFile(serviceName + "/" + subServiceName, testCaseName,
					testCasePropertyFileName);
			String roles = dataGenerator.getYamlData(serviceName, testDataFileName, "roleList",
					prop.getProperty("roleList"));
			logger.info(this.getClass().getName(), ConstantValues.MODULE_ID, ConstantValues.MODULE_NAME, roles);
			for (String role : roles.split(",")) {
				roleSet.add(role);
			}
			AuthorizationDTO authorizationDTO = loginService.getScreenAuthorizationDetails(roleSet);
			if (testCaseName.contains("invalid")) {
				logger.info(this.getClass().getName(), ConstantValues.MODULE_ID, ConstantValues.MODULE_NAME,
						"Size:" + authorizationDTO.getAuthorizationScreenId().size());

			} else
				assertNotNull(authorizationDTO.getAuthorizationScreenId());

		} catch (Exception exception) {
			logger.debug("LOGIN SERVICE", "AUTOMATION", "REG", ExceptionUtils.getStackTrace(exception));
			Reporter.log(ExceptionUtils.getStackTrace(exception));
		}
	}

	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, LoginServiceScreenAuthorizationTest.mTestCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}

	@Override
	public String getTestName() {
		return this.mTestCaseName;
	}

}

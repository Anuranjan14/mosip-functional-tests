package io.mosip.testscripts;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.admin.fw.util.AdminTestException;
import io.mosip.admin.fw.util.AdminTestUtil;
import io.mosip.admin.fw.util.TestCaseDTO;
import io.mosip.authentication.fw.dto.OutputValidationDto;
import io.mosip.authentication.fw.precon.JsonPrecondtion;
import io.mosip.authentication.fw.util.AuthenticationTestException;
import io.mosip.authentication.fw.util.OutputValidationUtil;
import io.mosip.authentication.fw.util.ReportUtil;
import io.mosip.authentication.fw.util.RestClient;
//import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.kernel.core.exception.NoSuchAlgorithmException;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.util.KernelAuthentication;
import io.mosip.kernel.util.KeycloakUserManager;
import io.mosip.service.BaseTestCase;
import io.restassured.response.Response;

public class AddIdentity extends AdminTestUtil implements ITest {
	private static final Logger logger = Logger.getLogger(SimplePost.class);
	protected String testCaseName = "";
	public Response response = null;

	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
		
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseDTO.setInputTemplate(AdminTestUtil.modifySchemaGenerateHbs());
		String uin = JsonPrecondtion
				.getValueFromJson(
						RestClient.getRequestWithCookie(ApplnURI + "/v1/idgenerator/uin", MediaType.APPLICATION_JSON,
								MediaType.APPLICATION_JSON, COOKIENAME,
								new KernelAuthentication().getTokenByRole(testCaseDTO.getRole())).asString(),
						"response.uin");

		DateFormat dateFormatter = new SimpleDateFormat("YYYYMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String timestampValue = dateFormatter.format(cal.getTime());
		String genRid = "27847" + RandomStringUtils.randomNumeric(10) + timestampValue;

		// filterHbs(testCaseDTO);
		if (testCaseName.equals("Resident_AddIdentity_Valid_Params_AddUser_smoke_Pos")) {

			KeycloakUserManager.removeVidUser();
			HashMap<String, List<String>> attrmap = new HashMap<String, List<String>>();
			List<String> list = new ArrayList<String>();
			list.add(uin);
			attrmap.put("individual_id", list);
			list = new ArrayList<String>();
			String token = AdminTestUtil.generateTokenID(uin, props.getProperty("partner_Token_Id"));
			list.add(token);
			attrmap.put("ida_token", list);
			KeycloakUserManager.createVidUsers(propsKernel.getProperty("new_Resident_User"),
					propsKernel.getProperty("new_Resident_Password"), propsKernel.getProperty("new_Resident_Role"),
					attrmap);
		}

		// testCaseDTO = AdminTestUtil.filterHbs(testCaseDTO);
		String jsonInput = testCaseDTO.getInput();

	
		/*
		 * int maxSupportedLang = 10; for(int i=BaseTestCase.languageList.size();
		 * i<maxSupportedLang; i++) { String preFix = ", { \"language\": \"$"; String
		 * suFix = "LANG$\", \"value\": "; int langNumber = i+1;
		 * 
		 * String eleMent ="\"FR\" }"; jsonInput = jsonInput.replace(preFix + langNumber
		 * + suFix+ eleMent, "");
		 * 
		 * eleMent ="\"Female\" }"; jsonInput = jsonInput.replace(preFix + langNumber +
		 * suFix+ eleMent, "");
		 * 
		 * eleMent ="\"Mrs Madhu.GN\" }"; jsonInput = jsonInput.replace(preFix +
		 * langNumber + suFix+ eleMent, "");
		 * 
		 * 
		 * eleMent ="\"Line1\" }"; jsonInput = jsonInput.replace(preFix + langNumber +
		 * suFix+ eleMent, "");
		 * 
		 * eleMent ="\"Line2\" }"; jsonInput = jsonInput.replace(preFix + langNumber +
		 * suFix+ eleMent, "");
		 * 
		 * eleMent ="\"Line3\" }"; jsonInput = jsonInput.replace(preFix + langNumber +
		 * suFix+ eleMent, ""); }
		 */
		
		String inputJson = getJsonFromTemplate(jsonInput, testCaseDTO.getInputTemplate(), false);

		inputJson = inputJson.replace("$UIN$", uin);
		inputJson = inputJson.replace("$RID$", genRid);
		// inputJson = inputJson.replace("$SCHEMAVERSION$",
		// props.getProperty("idSchemaVersion"));

		response = postWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
				testCaseDTO.getRole(), testCaseDTO.getTestCaseName());

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()));
		Reporter.log(ReportUtil.getOutputValiReport(ouputValid));

		if (!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");
		if (testCaseDTO.getTestCaseName().contains("_Pos")) {
			writeAutoGeneratedId(testCaseDTO.getTestCaseName(), "UIN", uin);
			writeAutoGeneratedId(testCaseDTO.getTestCaseName(), "RID", genRid);
		}

	}

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	public void waittime() {

		try {
			logger.info("waiting for" + props.getProperty("Delaytime") + " mili secs after UIN Generation In IDREPO"); //
			Thread.sleep(Long.parseLong(props.getProperty("Delaytime")));
		} catch (Exception e) {
			logger.error("Exception : " + e.getMessage());
		}

	}
}

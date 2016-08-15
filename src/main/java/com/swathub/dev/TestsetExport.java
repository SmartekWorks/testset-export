package com.swathub.dev;

import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class TestsetExport {
	private static String apiGet(URIBuilder url, String user, String pass, JSONObject proxy) throws Exception {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(url.getHost(), url.getPort()),
				new UsernamePasswordCredentials(user, pass));
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider)
				.build();

		String result = null;
		try {
			HttpGet httpget = new HttpGet(url.build());
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				result = EntityUtils.toString(response.getEntity());
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Usage: java -jar TestsetExport.jar <config file> <target path>");
			return;
		}

		File configFile = new File(args[0]);
		if (!configFile.exists() || configFile.isDirectory()) {
			System.out.println("Config file is not exist.");
			return;
		}

		File targetFolder = new File(args[1]);
		if (!targetFolder.exists() && !targetFolder.mkdirs()) {
			System.out.println("Create target folder error.");
			return;
		}

		JSONObject config = new JSONObject(FileUtils.readFileToString(configFile, "UTF-8"));

		URIBuilder testsetUrl = new URIBuilder(config.getString("serverUrl"));
		testsetUrl.setPath("/api/" + config.getString("workspaceOwner") + "/" +
				config.getString("workspaceName") + "/sets/" + config.getString("setID"));
		String testsetResult = apiGet(testsetUrl, config.getString("username"), config.getString("apiKey"), null);
		if (testsetResult == null) {
			System.out.println("Testset not exists, file will not be created.");
			return;
		}
		JSONObject testset = new JSONObject(testsetResult);

		URIBuilder getUrl = new URIBuilder(config.getString("serverUrl"));
		getUrl.setPath("/api/" + config.getString("workspaceOwner") + "/" +
				config.getString("workspaceName") + "/sets/" + config.getString("setID") + "/scenarios");

		String results = apiGet(getUrl, config.getString("username"), config.getString("apiKey"), null);
		if (results == null) {
			System.out.println("Config file is not correct.");
			return;
		}
		JSONArray scenarios = new JSONArray(results);
		JSONArray newScenarios = new JSONArray();
		for (int i = 0; i < scenarios.length(); i++) {
			JSONObject scenario = scenarios.getJSONObject(i);
			JSONArray testcases = scenario.getJSONArray("testcases");
			scenario.remove("testcases");
			for (int j = 0; j < testcases.length(); j++) {
				JSONObject testcase = testcases.getJSONObject(j);
				testcase.remove("results");
				scenario.append("testcases", testcase);
			}

			newScenarios.put(scenario);
		}

		testset.put("scenarios", newScenarios);

		File jsonFile = new File(targetFolder, testset.getString("name") + ".json");
		FileUtils.writeStringToFile(jsonFile, testset.toString(4), "utf-8");

		System.out.println(testset.getString("name") + ".json is created.");
		System.out.println("");
	}
}

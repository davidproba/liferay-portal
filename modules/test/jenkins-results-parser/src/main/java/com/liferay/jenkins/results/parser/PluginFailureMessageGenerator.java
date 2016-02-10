package com.liferay.jenkins.results.parser;

/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

import com.liferay.jenkins.results.parser.failure.BaseFailureMessageGenerator;

import org.apache.tools.ant.Project;

import org.json.JSONObject;

/**
 * @author Peter Yoo
 */
public class PluginFailureMessageGenerator extends BaseFailureMessageGenerator {

	@Override
	public String getMessage(
			String buildURL, String consoleOutput, Project project)
		throws Exception {

		if (!buildURL.contains("portal-acceptance")) {
			return null;
		}

		JSONObject jsonObject = JenkinsResultsParserUtil.toJSONObject(
			JenkinsResultsParserUtil.getLocalURL(buildURL + "api/json"));

		String jobVariant = JenkinsResultsParserUtil.getJobVariant(jsonObject);

		if (!buildURL.contains("plugins") && !jobVariant.contains("plugins")) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		sb.append("<p>To include a plugin fix for this pull request, ");
		sb.append("please edit your <a href=\\\"https://github.com/");
		sb.append(project.getProperty("github.pull.request.head.username"));
		sb.append("/");
		sb.append(project.getProperty("portal.repository"));
		sb.append("/blob/");
		sb.append(project.getProperty("github.pull.request.head.branch"));
		sb.append("/git-commit-plugins\\\">git-commit-plugins</a>. ");

		sb.append("Click <a href=\\\"https://in.liferay.com/web/");
		sb.append("global.engineering/blog/-/blogs/new-tests-for-the-");
		sb.append("pull-request-tester-\\\">here</a> for more details");
		sb.append(".</p>");

		int end = consoleOutput.indexOf("merge-test-results:");

		sb.append(getConsoleOutputSnippet(consoleOutput, end));

		return sb.toString();
	}

}